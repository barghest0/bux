package service

import (
	"fmt"
	"transaction/internal/data/repository"
	"transaction/internal/domain/model"
)

type TransactionService struct {
	repo *repository.TransactionRepository
}

func New(repo *repository.TransactionRepository) *TransactionService {
	return &TransactionService{
		repo: repo,
	}
}

func (s *TransactionService) GetTransactions() (*[]model.Transaction, error) {
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
