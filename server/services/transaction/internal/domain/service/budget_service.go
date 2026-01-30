package service

import (
	"errors"
	"fmt"
	"time"
	"transaction/internal/data/repository"
	"transaction/internal/domain/model"

	"github.com/shopspring/decimal"
)

var (
	ErrBudgetNotFound      = errors.New("budget not found")
	ErrBudgetAccessDenied  = errors.New("access denied to this budget")
	ErrInvalidBudgetPeriod = errors.New("invalid budget period")
)

type BudgetStatus struct {
	BudgetID      uint
	CategoryID    uint
	CategoryName  string
	CategoryIcon  string
	CategoryColor string
	BudgetAmount  decimal.Decimal
	SpentAmount   decimal.Decimal
	Remaining     decimal.Decimal
	SpentPercent  decimal.Decimal
	Period        string
	Currency      string
}

type BudgetService struct {
	repo *repository.BudgetRepository
}

func NewBudgetService(repo *repository.BudgetRepository) *BudgetService {
	return &BudgetService{repo: repo}
}

func (s *BudgetService) CreateBudget(budget *model.Budget) (*model.Budget, error) {
	if budget.Amount.LessThanOrEqual(decimal.Zero) {
		return nil, ErrInvalidAmount
	}
	if !model.IsValidBudgetPeriod(budget.Period) {
		return nil, ErrInvalidBudgetPeriod
	}
	if budget.Currency == "" {
		budget.Currency = "RUB"
	}
	if err := s.repo.Create(budget); err != nil {
		return nil, fmt.Errorf("create budget: %w", err)
	}
	return budget, nil
}

func (s *BudgetService) GetBudgets(userID uint) ([]model.Budget, error) {
	return s.repo.GetByUserID(userID)
}

func (s *BudgetService) UpdateBudget(id, userID uint, amount decimal.Decimal, period model.BudgetPeriod) (*model.Budget, error) {
	budget, err := s.repo.GetByID(id)
	if err != nil {
		return nil, ErrBudgetNotFound
	}
	if budget.UserID != userID {
		return nil, ErrBudgetAccessDenied
	}
	if !amount.IsZero() {
		budget.Amount = amount
	}
	if period != "" && model.IsValidBudgetPeriod(period) {
		budget.Period = period
	}
	if err := s.repo.Update(budget); err != nil {
		return nil, fmt.Errorf("update budget: %w", err)
	}
	return budget, nil
}

func (s *BudgetService) DeleteBudget(id, userID uint) error {
	budget, err := s.repo.GetByID(id)
	if err != nil {
		return ErrBudgetNotFound
	}
	if budget.UserID != userID {
		return ErrBudgetAccessDenied
	}
	return s.repo.Delete(id)
}

func (s *BudgetService) GetBudgetStatus(userID uint) ([]BudgetStatus, error) {
	now := time.Now()
	from := time.Date(now.Year(), now.Month(), 1, 0, 0, 0, 0, time.UTC)
	to := from.AddDate(0, 1, 0).Add(-time.Nanosecond)

	rows, err := s.repo.GetBudgetStatus(userID, from, to)
	if err != nil {
		return nil, fmt.Errorf("get budget status: %w", err)
	}

	statuses := make([]BudgetStatus, len(rows))
	for i, r := range rows {
		budgetAmt := decimal.NewFromFloat(r.BudgetAmount)
		spentAmt := decimal.NewFromFloat(r.SpentAmount)
		remaining := budgetAmt.Sub(spentAmt)
		var pct decimal.Decimal
		if !budgetAmt.IsZero() {
			pct = spentAmt.Div(budgetAmt).Mul(decimal.NewFromInt(100))
		}
		statuses[i] = BudgetStatus{
			BudgetID:      r.BudgetID,
			CategoryID:    r.CategoryID,
			CategoryName:  r.CategoryName,
			CategoryIcon:  r.CategoryIcon,
			CategoryColor: r.CategoryColor,
			BudgetAmount:  budgetAmt,
			SpentAmount:   spentAmt,
			Remaining:     remaining,
			SpentPercent:  pct,
			Period:        r.Period,
			Currency:      r.Currency,
		}
	}
	return statuses, nil
}
