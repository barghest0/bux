package http

import (
	"net/http"
	"strconv"
	"transaction/internal/domain/model"
	"transaction/internal/domain/service"
	"transaction/internal/presentation/http/dto"
	"transaction/internal/presentation/http/middleware"

	"github.com/gin-gonic/gin"
)

type TransactionHTTP struct {
	service *service.TransactionService
}

func New(r *gin.Engine, s *service.TransactionService) {
	h := &TransactionHTTP{
		service: s,
	}

	transactions := r.Group("/transactions")
	transactions.Use(middleware.AuthMiddleware())
	{
		transactions.GET("", h.GetTransactions)
		transactions.POST("", h.CreateTransaction)
		transactions.GET("/:id", h.GetTransaction)
	}
}

func (h *TransactionHTTP) GetTransactions(ctx *gin.Context) {
	userID, ok := ctx.Get("userID")
	if !ok {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	// Check if pagination is requested
	usePagination := ctx.Query("page") != ""
	pg := dto.ParsePagination(ctx)

	// Check for account_id query param
	accountIDStr := ctx.Query("account_id")
	if accountIDStr != "" {
		accountID, err := strconv.ParseUint(accountIDStr, 10, 32)
		if err != nil {
			ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid account_id"})
			return
		}

		if usePagination {
			transactions, total, err := h.service.GetTransactionsByAccountPaginated(uint(accountID), userID.(uint), pg.Limit(), pg.Offset())
			if err != nil {
				ctx.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
				return
			}
			ctx.JSON(http.StatusOK, dto.NewPaginatedResponse(dto.FromModelList(transactions), pg.Page, pg.PageSize, int(total)))
			return
		}

		transactions, err := h.service.GetTransactionsByAccount(uint(accountID), userID.(uint))
		if err != nil {
			ctx.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
			return
		}
		ctx.JSON(http.StatusOK, dto.FromModelList(transactions))
		return
	}

	if usePagination {
		transactions, total, err := h.service.GetTransactionsByUserPaginated(userID.(uint), pg.Limit(), pg.Offset())
		if err != nil {
			ctx.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
			return
		}
		ctx.JSON(http.StatusOK, dto.NewPaginatedResponse(dto.FromModelList(transactions), pg.Page, pg.PageSize, int(total)))
		return
	}

	transactions, err := h.service.GetTransactionsByUser(userID.(uint))
	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusOK, dto.FromModelList(transactions))
}

func (h *TransactionHTTP) GetTransaction(ctx *gin.Context) {
	var uri TransactionURI
	if err := ctx.ShouldBindUri(&uri); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
		return
	}

	tx, err := h.service.GetTransaction(uri.ID)
	if err != nil {
		ctx.JSON(http.StatusNotFound, gin.H{"error": "transaction not found"})
		return
	}

	ctx.JSON(http.StatusOK, dto.FromModel(*tx))
}

func (h *TransactionHTTP) CreateTransaction(ctx *gin.Context) {
	var req dto.CreateTransactionRequest
	if err := ctx.ShouldBindJSON(&req); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	amount, err := req.ParseAmount()
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid amount format"})
		return
	}

	transactionDate, err := req.ParseTransactionDate()
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid transaction_date format"})
		return
	}

	userID, ok := ctx.Get("userID")
	if !ok {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	transaction := &model.Transaction{
		UserID:               userID.(uint),
		AccountID:            req.AccountID,
		DestinationAccountID: req.DestinationAccountID,
		Type:                 model.TransactionType(req.Type),
		Amount:               amount,
		Currency:             req.Currency,
		CategoryID:           req.CategoryID,
		Description:          req.Description,
		TransactionDate:      transactionDate,
	}

	tx, err := h.service.CreateTransaction(transaction)
	if err != nil {
		status := http.StatusInternalServerError
		if err == service.ErrInvalidAmount ||
			err == service.ErrInvalidCurrency ||
			err == service.ErrInvalidTransactionType ||
			err == service.ErrDestinationAccountRequired {
			status = http.StatusBadRequest
		} else if err == service.ErrAccountNotFound || err == service.ErrDestinationAccountNotFound {
			status = http.StatusNotFound
		} else if err == service.ErrAccountAccessDenied {
			status = http.StatusForbidden
		}
		ctx.JSON(status, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusCreated, dto.FromModel(*tx))
}
