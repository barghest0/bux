package http

import (
	"net/http"
	"strconv"
	"transaction/internal/domain/model"
	"transaction/internal/domain/service"
	"transaction/internal/presentation/http/dto"
	"transaction/internal/presentation/http/middleware"

	"github.com/gin-gonic/gin"
	"github.com/shopspring/decimal"
	"time"
)

type RecurringTransactionHTTP struct {
	service *service.RecurringTransactionService
}

func NewRecurringTransactionHTTP(r *gin.Engine, s *service.RecurringTransactionService) {
	h := &RecurringTransactionHTTP{service: s}

	recurring := r.Group("/recurring-transactions")
	recurring.Use(middleware.AuthMiddleware())
	{
		recurring.GET("", h.GetAll)
		recurring.POST("", h.Create)
		recurring.GET("/:id", h.GetByID)
		recurring.PUT("/:id", h.Update)
		recurring.DELETE("/:id", h.Delete)
		recurring.POST("/:id/toggle", h.ToggleActive)
		recurring.POST("/:id/execute", h.Execute)
		recurring.POST("/process", h.ProcessDue)
	}
}

func (h *RecurringTransactionHTTP) GetAll(ctx *gin.Context) {
	userID, ok := ctx.Get("userID")
	if !ok {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	rts, err := h.service.GetByUser(userID.(uint))
	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusOK, dto.RecurringFromModelList(rts))
}

func (h *RecurringTransactionHTTP) Create(ctx *gin.Context) {
	var req dto.CreateRecurringTransactionRequest
	if err := ctx.ShouldBindJSON(&req); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	amount, err := req.ParseAmount()
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid amount format"})
		return
	}

	nextDate, err := req.ParseNextDate()
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid next_date format, use RFC3339"})
		return
	}

	endDate, err := req.ParseEndDate()
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid end_date format, use RFC3339"})
		return
	}

	userID, ok := ctx.Get("userID")
	if !ok {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	rt := &model.RecurringTransaction{
		UserID:      userID.(uint),
		AccountID:   req.AccountID,
		Type:        model.TransactionType(req.Type),
		Amount:      amount,
		Currency:    req.Currency,
		CategoryID:  req.CategoryID,
		Description: req.Description,
		Frequency:   model.RecurrenceFrequency(req.Frequency),
		NextDate:    nextDate,
		EndDate:     endDate,
	}

	created, err := h.service.Create(rt)
	if err != nil {
		status := http.StatusInternalServerError
		if err == service.ErrInvalidAmount || err == service.ErrInvalidCurrency ||
			err == service.ErrInvalidTransactionType || err == service.ErrInvalidFrequency {
			status = http.StatusBadRequest
		}
		ctx.JSON(status, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusCreated, dto.RecurringFromModel(*created))
}

func (h *RecurringTransactionHTTP) GetByID(ctx *gin.Context) {
	id, err := strconv.ParseUint(ctx.Param("id"), 10, 32)
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
		return
	}

	userID, ok := ctx.Get("userID")
	if !ok {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	rt, err := h.service.GetByID(uint(id), userID.(uint))
	if err != nil {
		status := http.StatusInternalServerError
		if err == service.ErrRecurringNotFound {
			status = http.StatusNotFound
		} else if err == service.ErrRecurringAccessDenied {
			status = http.StatusForbidden
		}
		ctx.JSON(status, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusOK, dto.RecurringFromModel(*rt))
}

func (h *RecurringTransactionHTTP) Update(ctx *gin.Context) {
	id, err := strconv.ParseUint(ctx.Param("id"), 10, 32)
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
		return
	}

	userID, ok := ctx.Get("userID")
	if !ok {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	rt, err := h.service.GetByID(uint(id), userID.(uint))
	if err != nil {
		status := http.StatusInternalServerError
		if err == service.ErrRecurringNotFound {
			status = http.StatusNotFound
		} else if err == service.ErrRecurringAccessDenied {
			status = http.StatusForbidden
		}
		ctx.JSON(status, gin.H{"error": err.Error()})
		return
	}

	var req dto.UpdateRecurringTransactionRequest
	if err := ctx.ShouldBindJSON(&req); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	if req.Amount != "" {
		amount, err := decimal.NewFromString(req.Amount)
		if err != nil {
			ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid amount"})
			return
		}
		rt.Amount = amount
	}
	if req.Description != "" {
		rt.Description = req.Description
	}
	if req.CategoryID != nil {
		rt.CategoryID = req.CategoryID
	}
	if req.Frequency != "" {
		rt.Frequency = model.RecurrenceFrequency(req.Frequency)
	}
	if req.NextDate != "" {
		nextDate, err := time.Parse(time.RFC3339, req.NextDate)
		if err != nil {
			ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid next_date"})
			return
		}
		rt.NextDate = nextDate
	}
	if req.EndDate != "" {
		endDate, err := time.Parse(time.RFC3339, req.EndDate)
		if err != nil {
			ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid end_date"})
			return
		}
		rt.EndDate = &endDate
	}

	updated, err := h.service.Update(rt)
	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusOK, dto.RecurringFromModel(*updated))
}

func (h *RecurringTransactionHTTP) Delete(ctx *gin.Context) {
	id, err := strconv.ParseUint(ctx.Param("id"), 10, 32)
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
		return
	}

	userID, ok := ctx.Get("userID")
	if !ok {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	if err := h.service.Delete(uint(id), userID.(uint)); err != nil {
		status := http.StatusInternalServerError
		if err == service.ErrRecurringNotFound {
			status = http.StatusNotFound
		} else if err == service.ErrRecurringAccessDenied {
			status = http.StatusForbidden
		}
		ctx.JSON(status, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusOK, gin.H{"message": "deleted"})
}

func (h *RecurringTransactionHTTP) ToggleActive(ctx *gin.Context) {
	id, err := strconv.ParseUint(ctx.Param("id"), 10, 32)
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
		return
	}

	userID, ok := ctx.Get("userID")
	if !ok {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	rt, err := h.service.ToggleActive(uint(id), userID.(uint))
	if err != nil {
		status := http.StatusInternalServerError
		if err == service.ErrRecurringNotFound {
			status = http.StatusNotFound
		} else if err == service.ErrRecurringAccessDenied {
			status = http.StatusForbidden
		}
		ctx.JSON(status, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusOK, dto.RecurringFromModel(*rt))
}

func (h *RecurringTransactionHTTP) Execute(ctx *gin.Context) {
	id, err := strconv.ParseUint(ctx.Param("id"), 10, 32)
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
		return
	}

	userID, ok := ctx.Get("userID")
	if !ok {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	rt, err := h.service.Execute(uint(id), userID.(uint))
	if err != nil {
		status := http.StatusInternalServerError
		switch err {
		case service.ErrRecurringNotFound:
			status = http.StatusNotFound
		case service.ErrRecurringAccessDenied:
			status = http.StatusForbidden
		case service.ErrRecurringInactive:
			status = http.StatusBadRequest
		}
		ctx.JSON(status, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusOK, dto.RecurringFromModel(*rt))
}

func (h *RecurringTransactionHTTP) ProcessDue(ctx *gin.Context) {
	count, err := h.service.ProcessDue()
	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusOK, gin.H{"processed": count})
}
