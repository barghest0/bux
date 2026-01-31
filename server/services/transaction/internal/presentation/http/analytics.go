package http

import (
	"net/http"
	"strconv"
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
		analytics.GET("/insights/trends", h.GetTrends)
		analytics.GET("/insights/top-categories", h.GetTopCategories)
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

func (h *AnalyticsHTTP) GetTrends(ctx *gin.Context) {
	userID, ok := ctx.Get("userID")
	if !ok {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	// Last 6 months by default
	months := 6
	if m := ctx.Query("months"); m != "" {
		if parsed, err := strconv.Atoi(m); err == nil && parsed > 0 && parsed <= 24 {
			months = parsed
		}
	}

	now := time.Now()
	from := time.Date(now.Year(), now.Month()-time.Month(months-1), 1, 0, 0, 0, 0, time.UTC)
	to := now

	summary, err := h.service.GetSummary(userID.(uint), from, to)
	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	trends := make([]dto.TrendItem, len(summary.ByMonth))
	for i, m := range summary.ByMonth {
		var prevExpense, prevIncome string
		if i > 0 {
			prevExpense = summary.ByMonth[i-1].Expense.String()
			prevIncome = summary.ByMonth[i-1].Income.String()
		}
		trends[i] = dto.TrendItem{
			Year:        m.Year,
			Month:       m.Month,
			Income:      m.Income.String(),
			Expense:     m.Expense.String(),
			Net:         m.Income.Sub(m.Expense).String(),
			PrevIncome:  prevIncome,
			PrevExpense: prevExpense,
		}
	}

	ctx.JSON(http.StatusOK, gin.H{"trends": trends})
}

func (h *AnalyticsHTTP) GetTopCategories(ctx *gin.Context) {
	userID, ok := ctx.Get("userID")
	if !ok {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	txType := ctx.DefaultQuery("type", "expense")
	limit := 10
	if l := ctx.Query("limit"); l != "" {
		if parsed, err := strconv.Atoi(l); err == nil && parsed > 0 {
			limit = parsed
		}
	}

	now := time.Now()
	from := time.Date(now.Year(), now.Month(), 1, 0, 0, 0, 0, time.UTC)
	to := now

	if f := ctx.Query("from"); f != "" {
		if parsed, err := time.Parse("2006-01-02", f); err == nil {
			from = parsed
		}
	}
	if t := ctx.Query("to"); t != "" {
		if parsed, err := time.Parse("2006-01-02", t); err == nil {
			to = parsed.Add(24*time.Hour - time.Nanosecond)
		}
	}

	summary, err := h.service.GetSummary(userID.(uint), from, to)
	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	var categories []dto.CategorySummary
	for _, c := range summary.ByCategory {
		if c.Type == txType {
			categories = append(categories, dto.CategorySummary{
				CategoryID:    c.CategoryID,
				CategoryName:  c.CategoryName,
				CategoryIcon:  c.CategoryIcon,
				CategoryColor: c.CategoryColor,
				Type:          c.Type,
				Total:         c.Total.String(),
				Count:         c.Count,
			})
		}
		if len(categories) >= limit {
			break
		}
	}

	ctx.JSON(http.StatusOK, gin.H{"categories": categories})
}
