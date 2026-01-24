package http

import (
	"net/http"
	"transaction/internal/domain/model"
	"transaction/internal/domain/service"
	"transaction/internal/presentation/http/dto"
	"transaction/internal/presentation/http/middleware"

	"github.com/gin-gonic/gin"
)

type AccountHTTP struct {
	service *service.AccountService
}

func NewAccountHTTP(r *gin.Engine, s *service.AccountService) {
	h := &AccountHTTP{
		service: s,
	}

	accounts := r.Group("/accounts")
	accounts.Use(middleware.AuthMiddleware())
	{
		accounts.GET("", h.GetAccounts)
		accounts.POST("", h.CreateAccount)
		accounts.GET("/:id", h.GetAccount)
		accounts.PUT("/:id", h.UpdateAccount)
		accounts.DELETE("/:id", h.DeleteAccount)
	}
}

func (h *AccountHTTP) GetAccounts(ctx *gin.Context) {
	userID, ok := ctx.Get("userID")
	if !ok {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	accounts, err := h.service.GetUserAccounts(userID.(uint))
	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusOK, dto.AccountListFromModel(accounts))
}

func (h *AccountHTTP) GetAccount(ctx *gin.Context) {
	var uri struct {
		ID uint `uri:"id" binding:"required"`
	}
	if err := ctx.ShouldBindUri(&uri); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
		return
	}

	userID, ok := ctx.Get("userID")
	if !ok {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	account, err := h.service.GetAccount(uri.ID, userID.(uint))
	if err != nil {
		status := http.StatusInternalServerError
		if err == service.ErrAccountNotFound {
			status = http.StatusNotFound
		} else if err == service.ErrAccountAccessDenied {
			status = http.StatusForbidden
		}
		ctx.JSON(status, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusOK, dto.AccountFromModel(*account))
}

func (h *AccountHTTP) CreateAccount(ctx *gin.Context) {
	var req dto.CreateAccountRequest
	if err := ctx.ShouldBindJSON(&req); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	userID, ok := ctx.Get("userID")
	if !ok {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	balance, err := req.ParseBalance()
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid balance format"})
		return
	}

	account := &model.Account{
		UserID:    userID.(uint),
		Type:      model.AccountType(req.Type),
		Name:      req.Name,
		Currency:  req.Currency,
		Balance:   balance,
		Icon:      req.Icon,
		Color:     req.Color,
		SortOrder: req.SortOrder,
	}

	created, err := h.service.CreateAccount(account)
	if err != nil {
		status := http.StatusInternalServerError
		if err == service.ErrInvalidAccountName || err == service.ErrInvalidAccountType {
			status = http.StatusBadRequest
		}
		ctx.JSON(status, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusCreated, dto.AccountFromModel(*created))
}

func (h *AccountHTTP) UpdateAccount(ctx *gin.Context) {
	var uri struct {
		ID uint `uri:"id" binding:"required"`
	}
	if err := ctx.ShouldBindUri(&uri); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
		return
	}

	var req dto.UpdateAccountRequest
	if err := ctx.ShouldBindJSON(&req); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	userID, ok := ctx.Get("userID")
	if !ok {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	updates := &model.Account{
		Name:  req.Name,
		Icon:  req.Icon,
		Color: req.Color,
	}
	if req.SortOrder != nil {
		updates.SortOrder = *req.SortOrder
	}

	account, err := h.service.UpdateAccount(uri.ID, userID.(uint), updates)
	if err != nil {
		status := http.StatusInternalServerError
		if err == service.ErrAccountNotFound {
			status = http.StatusNotFound
		} else if err == service.ErrAccountAccessDenied {
			status = http.StatusForbidden
		}
		ctx.JSON(status, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusOK, dto.AccountFromModel(*account))
}

func (h *AccountHTTP) DeleteAccount(ctx *gin.Context) {
	var uri struct {
		ID uint `uri:"id" binding:"required"`
	}
	if err := ctx.ShouldBindUri(&uri); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
		return
	}

	userID, ok := ctx.Get("userID")
	if !ok {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	if err := h.service.DeleteAccount(uri.ID, userID.(uint)); err != nil {
		status := http.StatusInternalServerError
		if err == service.ErrAccountNotFound {
			status = http.StatusNotFound
		} else if err == service.ErrAccountAccessDenied {
			status = http.StatusForbidden
		}
		ctx.JSON(status, gin.H{"error": err.Error()})
		return
	}

	ctx.Status(http.StatusNoContent)
}
