package service

import (
	"errors"
	"fmt"
	"transaction/internal/data/repository"
	"transaction/internal/domain/model"

	"github.com/shopspring/decimal"
)

var (
	ErrInvalidAmount   = errors.New("amount must be greater than zero")
	ErrInvalidCurrency = errors.New("currency must be exactly 3 characters")
)

type TransactionService struct {
	repo *repository.TransactionRepository
}

func New(repo *repository.TransactionRepository) *TransactionService {
	return &TransactionService{
		repo: repo,
	}
}

func (s *TransactionService) GetTransactions() ([]model.Transaction, error) {
	const tag = "service.GetTransactions"

	transactions, err := s.repo.GetAll()

	if err != nil {
		return nil, fmt.Errorf("%s: %w", tag, err)
	}

	return transactions, nil
}

func (s *TransactionService) GetTransaction(id uint) (*model.Transaction, error) {
	const tag = "service.GetTransaction"

	transaction, err := s.repo.GetByID(id)

	if err != nil {
		return nil, fmt.Errorf("%s: %w", tag, err)
	}

	return transaction, nil
}

func (s *TransactionService) CreateTransaction(transaction *model.Transaction) (*model.Transaction, error) {
	const tag = "service.CreateTransaction"

	// Validate amount
	if transaction.Amount.LessThanOrEqual(decimal.Zero) {
		return nil, fmt.Errorf("%s: %w", tag, ErrInvalidAmount)
	}

	// Validate currency
	if len(transaction.Currency) != 3 {
		return nil, fmt.Errorf("%s: %w", tag, ErrInvalidCurrency)
	}

	transaction, err := s.repo.Create(transaction)

	if err != nil {
		return nil, fmt.Errorf("%s: %w", tag, err)
	}

	return transaction, nil
}
