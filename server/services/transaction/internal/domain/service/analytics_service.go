package service

import (
	"fmt"
	"time"
	"transaction/internal/data/repository"

	"github.com/shopspring/decimal"
)

type CategoryTotal struct {
	CategoryID    *uint
	CategoryName  string
	CategoryIcon  string
	CategoryColor string
	Type          string
	Total         decimal.Decimal
	Count         int64
}

type MonthlyTotal struct {
	Year    int
	Month   int
	Income  decimal.Decimal
	Expense decimal.Decimal
}

type TransactionSummary struct {
	TotalIncome  decimal.Decimal
	TotalExpense decimal.Decimal
	ByCategory   []CategoryTotal
	ByMonth      []MonthlyTotal
}

type AnalyticsService struct {
	repo *repository.TransactionRepository
}

func NewAnalyticsService(repo *repository.TransactionRepository) *AnalyticsService {
	return &AnalyticsService{repo: repo}
}

func (s *AnalyticsService) GetSummary(userID uint, from, to time.Time) (*TransactionSummary, error) {
	catRows, err := s.repo.GetSummaryByCategory(userID, from, to)
	if err != nil {
		return nil, fmt.Errorf("get summary by category: %w", err)
	}

	monthRows, err := s.repo.GetSummaryByMonth(userID, from, to)
	if err != nil {
		return nil, fmt.Errorf("get summary by month: %w", err)
	}

	summary := &TransactionSummary{
		TotalIncome:  decimal.Zero,
		TotalExpense: decimal.Zero,
	}

	for _, r := range catRows {
		total := r.Total
		summary.ByCategory = append(summary.ByCategory, CategoryTotal{
			CategoryID:    r.CategoryID,
			CategoryName:  r.CategoryName,
			CategoryIcon:  r.CategoryIcon,
			CategoryColor: r.CategoryColor,
			Type:          r.Type,
			Total:         total,
			Count:         r.Count,
		})
		switch r.Type {
		case "income":
			summary.TotalIncome = summary.TotalIncome.Add(total)
		case "expense":
			summary.TotalExpense = summary.TotalExpense.Add(total)
		}
	}

	for _, r := range monthRows {
		summary.ByMonth = append(summary.ByMonth, MonthlyTotal{
			Year:    r.Year,
			Month:   r.Month,
			Income:  r.Income,
			Expense: r.Expense,
		})
	}

	return summary, nil
}
