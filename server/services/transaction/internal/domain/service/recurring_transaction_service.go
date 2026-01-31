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
	ErrInvalidFrequency      = errors.New("invalid recurrence frequency")
	ErrRecurringNotFound     = errors.New("recurring transaction not found")
	ErrRecurringAccessDenied = errors.New("access denied to this recurring transaction")
	ErrRecurringInactive     = errors.New("recurring transaction is inactive")
)

type RecurringTransactionService struct {
	repo        *repository.RecurringTransactionRepository
	txRepo      *repository.TransactionRepository
	accountRepo *repository.AccountRepository
}

func NewRecurringTransactionService(
	repo *repository.RecurringTransactionRepository,
	txRepo *repository.TransactionRepository,
	accountRepo *repository.AccountRepository,
) *RecurringTransactionService {
	return &RecurringTransactionService{
		repo:        repo,
		txRepo:      txRepo,
		accountRepo: accountRepo,
	}
}

func (s *RecurringTransactionService) Create(rt *model.RecurringTransaction) (*model.RecurringTransaction, error) {
	if rt.Amount.LessThanOrEqual(decimal.Zero) {
		return nil, ErrInvalidAmount
	}
	if len(rt.Currency) != 3 {
		return nil, ErrInvalidCurrency
	}
	if !model.IsValidTransactionType(rt.Type) {
		return nil, ErrInvalidTransactionType
	}
	if !model.IsValidFrequency(rt.Frequency) {
		return nil, ErrInvalidFrequency
	}
	if rt.NextDate.IsZero() {
		rt.NextDate = time.Now()
	}
	rt.IsActive = true

	created, err := s.repo.Create(rt)
	if err != nil {
		return nil, fmt.Errorf("create recurring transaction: %w", err)
	}
	return created, nil
}

func (s *RecurringTransactionService) GetByUser(userID uint) ([]model.RecurringTransaction, error) {
	rts, err := s.repo.GetByUserID(userID)
	if err != nil {
		return nil, fmt.Errorf("get recurring transactions: %w", err)
	}
	return rts, nil
}

func (s *RecurringTransactionService) GetByID(id, userID uint) (*model.RecurringTransaction, error) {
	rt, err := s.repo.GetByID(id)
	if err != nil {
		return nil, ErrRecurringNotFound
	}
	if rt.UserID != userID {
		return nil, ErrRecurringAccessDenied
	}
	return rt, nil
}

func (s *RecurringTransactionService) Update(rt *model.RecurringTransaction) (*model.RecurringTransaction, error) {
	if rt.Amount.LessThanOrEqual(decimal.Zero) {
		return nil, ErrInvalidAmount
	}
	if !model.IsValidFrequency(rt.Frequency) {
		return nil, ErrInvalidFrequency
	}
	updated, err := s.repo.Update(rt)
	if err != nil {
		return nil, fmt.Errorf("update recurring transaction: %w", err)
	}
	return updated, nil
}

func (s *RecurringTransactionService) Delete(id, userID uint) error {
	rt, err := s.repo.GetByID(id)
	if err != nil {
		return ErrRecurringNotFound
	}
	if rt.UserID != userID {
		return ErrRecurringAccessDenied
	}
	return s.repo.Delete(id)
}

func (s *RecurringTransactionService) ToggleActive(id, userID uint) (*model.RecurringTransaction, error) {
	rt, err := s.repo.GetByID(id)
	if err != nil {
		return nil, ErrRecurringNotFound
	}
	if rt.UserID != userID {
		return nil, ErrRecurringAccessDenied
	}
	rt.IsActive = !rt.IsActive
	return s.repo.Update(rt)
}

func (s *RecurringTransactionService) Execute(id, userID uint) (*model.RecurringTransaction, error) {
	rt, err := s.repo.GetByID(id)
	if err != nil {
		return nil, ErrRecurringNotFound
	}
	if rt.UserID != userID {
		return nil, ErrRecurringAccessDenied
	}
	if !rt.IsActive {
		return nil, ErrRecurringInactive
	}

	txService := NewWithAccountRepo(s.txRepo, s.accountRepo)
	now := time.Now()
	tx := &model.Transaction{
		UserID:          rt.UserID,
		AccountID:       rt.AccountID,
		Type:            rt.Type,
		Amount:          rt.Amount,
		Currency:        rt.Currency,
		CategoryID:      rt.CategoryID,
		Description:     rt.Description,
		TransactionDate: now,
	}

	if _, err := txService.CreateTransaction(tx); err != nil {
		return nil, err
	}

	baseDate := rt.NextDate
	if baseDate.Before(now) {
		baseDate = now
	}
	rt.NextDate = advanceDate(baseDate, rt.Frequency)
	if rt.EndDate != nil && rt.NextDate.After(*rt.EndDate) {
		rt.IsActive = false
	}

	return s.repo.Update(rt)
}

func (s *RecurringTransactionService) ProcessDue() (int, error) {
	due, err := s.repo.GetDue(time.Now())
	if err != nil {
		return 0, fmt.Errorf("get due recurring transactions: %w", err)
	}

	count := 0
	txService := NewWithAccountRepo(s.txRepo, s.accountRepo)

	for _, rt := range due {
		if rt.EndDate != nil && rt.NextDate.After(*rt.EndDate) {
			rt.IsActive = false
			s.repo.Update(&rt)
			continue
		}

		tx := &model.Transaction{
			UserID:          rt.UserID,
			AccountID:       rt.AccountID,
			Type:            rt.Type,
			Amount:          rt.Amount,
			Currency:        rt.Currency,
			CategoryID:      rt.CategoryID,
			Description:     rt.Description,
			TransactionDate: rt.NextDate,
		}

		if _, err := txService.CreateTransaction(tx); err != nil {
			continue
		}

		rt.NextDate = advanceDate(rt.NextDate, rt.Frequency)
		if rt.EndDate != nil && rt.NextDate.After(*rt.EndDate) {
			rt.IsActive = false
		}
		s.repo.Update(&rt)
		count++
	}

	return count, nil
}

func advanceDate(date time.Time, freq model.RecurrenceFrequency) time.Time {
	switch freq {
	case model.FrequencyDaily:
		return date.AddDate(0, 0, 1)
	case model.FrequencyWeekly:
		return date.AddDate(0, 0, 7)
	case model.FrequencyMonthly:
		return date.AddDate(0, 1, 0)
	case model.FrequencyYearly:
		return date.AddDate(1, 0, 0)
	}
	return date.AddDate(0, 1, 0)
}
