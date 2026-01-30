package http

import (
	"net/http"
	"time"
	"transaction/internal/domain/service"
	"transaction/internal/presentation/http/dto"
	"transaction/internal/presentation/http/middleware"

	"github.com/gin-gonic/gin"
)

type AnalyticsHTTP struct {
	service *service.AnalyticsService
}

func NewAnalyticsHTTP(r *gin.Engine, s *service.AnalyticsService) {
	h := &AnalyticsHTTP{service: s}

	analytics := r.Group("/analytics")
	analytics.Use(middleware.AuthMiddleware())
	{
		analytics.GET("/summary", h.GetSummary)
	}
}

func (h *AnalyticsHTTP) GetSummary(ctx *gin.Context) {
	userID, ok := ctx.Get("userID")
	if !ok {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	// Parse date range
	fromStr := ctx.Query("from")
	toStr := ctx.Query("to")

	var from, to time.Time
	var err error

	if fromStr != "" {
		from, err = time.Parse("2006-01-02", fromStr)
		if err != nil {
			ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid 'from' date format, use YYYY-MM-DD"})
			return
		}
	} else {
		// Default: beginning of current year
		now := time.Now()
		from = time.Date(now.Year(), 1, 1, 0, 0, 0, 0, time.UTC)
	}

	if toStr != "" {
		to, err = time.Parse("2006-01-02", toStr)
		if err != nil {
			ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid 'to' date format, use YYYY-MM-DD"})
			return
		}
		// Include the entire day
		to = to.Add(24*time.Hour - time.Nanosecond)
	} else {
		to = time.Now()
	}

	summary, err := h.service.GetSummary(userID.(uint), from, to)
	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	// Build response
	byCategory := make([]dto.CategorySummary, len(summary.ByCategory))
	for i, c := range summary.ByCategory {
		byCategory[i] = dto.CategorySummary{
			CategoryID:    c.CategoryID,
			CategoryName:  c.CategoryName,
			CategoryIcon:  c.CategoryIcon,
			CategoryColor: c.CategoryColor,
			Type:          c.Type,
			Total:         c.Total.String(),
			Count:         c.Count,
		}
	}

	byMonth := make([]dto.MonthlySummary, len(summary.ByMonth))
	for i, m := range summary.ByMonth {
		net := m.Income.Sub(m.Expense)
		byMonth[i] = dto.MonthlySummary{
			Year:    m.Year,
			Month:   m.Month,
			Income:  m.Income.String(),
			Expense: m.Expense.String(),
			Net:     net.String(),
		}
	}

	net := summary.TotalIncome.Sub(summary.TotalExpense)

	ctx.JSON(http.StatusOK, dto.TransactionSummaryResponse{
		TotalIncome:  summary.TotalIncome.String(),
		TotalExpense: summary.TotalExpense.String(),
		Net:          net.String(),
		ByCategory:   byCategory,
		ByMonth:      byMonth,
	})
}
