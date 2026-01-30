package http

import (
	"net/http"
	"transaction/internal/domain/model"
	"transaction/internal/domain/service"
	"transaction/internal/presentation/http/dto"
	"transaction/internal/presentation/http/middleware"

	"github.com/gin-gonic/gin"
	"github.com/shopspring/decimal"
)

type BudgetHTTP struct {
	service *service.BudgetService
}

func NewBudgetHTTP(r *gin.Engine, s *service.BudgetService) {
	h := &BudgetHTTP{service: s}

	budgets := r.Group("/budgets")
	budgets.Use(middleware.AuthMiddleware())
	{
		budgets.POST("", h.CreateBudget)
		budgets.GET("", h.GetBudgets)
		budgets.PUT("/:id", h.UpdateBudget)
		budgets.DELETE("/:id", h.DeleteBudget)
		budgets.GET("/status", h.GetBudgetStatus)
	}
}

func (h *BudgetHTTP) CreateBudget(ctx *gin.Context) {
	var req dto.CreateBudgetRequest
	if err := ctx.ShouldBindJSON(&req); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	amount, err := decimal.NewFromString(req.Amount)
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid amount format"})
		return
	}

	userID, _ := ctx.Get("userID")

	period := model.BudgetPeriodMonthly
	if req.Period != "" {
		period = model.BudgetPeriod(req.Period)
	}

	currency := "RUB"
	if req.Currency != "" {
		currency = req.Currency
	}

	budget := &model.Budget{
		UserID:     userID.(uint),
		CategoryID: req.CategoryID,
		Amount:     amount,
		Currency:   currency,
		Period:     period,
	}

	created, err := h.service.CreateBudget(budget)
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusCreated, dto.BudgetFromModel(*created))
}

func (h *BudgetHTTP) GetBudgets(ctx *gin.Context) {
	userID, _ := ctx.Get("userID")

	budgets, err := h.service.GetBudgets(userID.(uint))
	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusOK, dto.BudgetListFromModel(budgets))
}

func (h *BudgetHTTP) UpdateBudget(ctx *gin.Context) {
	var uri struct {
		ID uint `uri:"id" binding:"required"`
	}
	if err := ctx.ShouldBindUri(&uri); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
		return
	}

	var req dto.UpdateBudgetRequest
	if err := ctx.ShouldBindJSON(&req); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	userID, _ := ctx.Get("userID")

	var amount decimal.Decimal
	if req.Amount != "" {
		var err error
		amount, err = decimal.NewFromString(req.Amount)
		if err != nil {
			ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid amount format"})
			return
		}
	}

	budget, err := h.service.UpdateBudget(uri.ID, userID.(uint), amount, model.BudgetPeriod(req.Period))
	if err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusOK, dto.BudgetFromModel(*budget))
}

func (h *BudgetHTTP) DeleteBudget(ctx *gin.Context) {
	var uri struct {
		ID uint `uri:"id" binding:"required"`
	}
	if err := ctx.ShouldBindUri(&uri); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
		return
	}

	userID, _ := ctx.Get("userID")

	if err := h.service.DeleteBudget(uri.ID, userID.(uint)); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusNoContent, nil)
}

func (h *BudgetHTTP) GetBudgetStatus(ctx *gin.Context) {
	userID, _ := ctx.Get("userID")

	statuses, err := h.service.GetBudgetStatus(userID.(uint))
	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	response := make([]dto.BudgetStatusResponse, len(statuses))
	for i, s := range statuses {
		response[i] = dto.BudgetStatusResponse{
			BudgetID:      s.BudgetID,
			CategoryID:    s.CategoryID,
			CategoryName:  s.CategoryName,
			CategoryIcon:  s.CategoryIcon,
			CategoryColor: s.CategoryColor,
			BudgetAmount:  s.BudgetAmount.String(),
			SpentAmount:   s.SpentAmount.String(),
			Remaining:     s.Remaining.String(),
			SpentPercent:  s.SpentPercent.StringFixed(1),
			Period:        s.Period,
			Currency:      s.Currency,
		}
	}

	ctx.JSON(http.StatusOK, response)
}
