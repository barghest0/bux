package http

import (
	"fmt"
	"net/http"
	"time"
	"transaction/internal/domain/service"
	"transaction/internal/presentation/http/middleware"

	"github.com/gin-gonic/gin"
)

type ExportHTTP struct {
	service *service.TransactionService
}

func NewExportHTTP(r *gin.Engine, s *service.TransactionService) {
	h := &ExportHTTP{service: s}

	export := r.Group("/transactions")
	export.Use(middleware.AuthMiddleware())
	{
		export.GET("/export", h.ExportCSV)
	}
}

func (h *ExportHTTP) ExportCSV(ctx *gin.Context) {
	userID, ok := ctx.Get("userID")
	if !ok {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	transactions, err := h.service.GetTransactionsByUser(userID.(uint))
	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	// Filter by date range if provided
	fromStr := ctx.Query("from")
	toStr := ctx.Query("to")
	var from, to time.Time

	if fromStr != "" {
		from, err = time.Parse("2006-01-02", fromStr)
		if err != nil {
			ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid 'from' date format, use YYYY-MM-DD"})
			return
		}
	}
	if toStr != "" {
		to, err = time.Parse("2006-01-02", toStr)
		if err != nil {
			ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid 'to' date format, use YYYY-MM-DD"})
			return
		}
		to = to.Add(24*time.Hour - time.Nanosecond)
	}

	ctx.Header("Content-Type", "text/csv; charset=utf-8")
	ctx.Header("Content-Disposition", fmt.Sprintf("attachment; filename=transactions_%s.csv", time.Now().Format("2006-01-02")))

	// Write BOM for Excel compatibility
	ctx.Writer.Write([]byte{0xEF, 0xBB, 0xBF})

	// CSV header
	ctx.Writer.WriteString("ID,Date,Type,Amount,Currency,Category,Description,Account ID,Status\n")

	for _, tx := range transactions {
		// Apply date filter
		if !from.IsZero() && tx.TransactionDate.Before(from) {
			continue
		}
		if !to.IsZero() && tx.TransactionDate.After(to) {
			continue
		}

		categoryName := ""
		if tx.Category != nil {
			categoryName = tx.Category.Name
		}

		description := tx.Description
		// Escape CSV fields
		description = escapeCSV(description)
		categoryName = escapeCSV(categoryName)

		line := fmt.Sprintf("%d,%s,%s,%s,%s,%s,%s,%d,%s\n",
			tx.ID,
			tx.TransactionDate.Format("2006-01-02 15:04:05"),
			tx.Type,
			tx.Amount.String(),
			tx.Currency,
			categoryName,
			description,
			tx.AccountID,
			tx.Status,
		)
		ctx.Writer.WriteString(line)
	}
}

func escapeCSV(s string) string {
	needsQuoting := false
	for _, c := range s {
		if c == ',' || c == '"' || c == '\n' || c == '\r' {
			needsQuoting = true
			break
		}
	}
	if !needsQuoting {
		return s
	}

	result := "\""
	for _, c := range s {
		if c == '"' {
			result += "\"\""
		} else {
			result += string(c)
		}
	}
	result += "\""
	return result
}
