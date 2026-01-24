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
	ErrInvalidAmount              = errors.New("amount must be greater than zero")
	ErrInvalidCurrency            = errors.New("currency must be exactly 3 characters")
	ErrInvalidTransactionType     = errors.New("invalid transaction type")
	ErrDestinationAccountRequired = errors.New("destination account required for transfer")
	ErrDestinationAccountNotFound = errors.New("destination account not found")
)

type TransactionService struct {
	repo        *repository.TransactionRepository
	accountRepo *repository.AccountRepository
}

func New(repo *repository.TransactionRepository) *TransactionService {
	return &TransactionService{
		repo: repo,
	}
}

func NewWithAccountRepo(repo *repository.TransactionRepository, accountRepo *repository.AccountRepository) *TransactionService {
	return &TransactionService{
		repo:        repo,
		accountRepo: accountRepo,
	}
}

func (s *TransactionService) GetTransactions() ([]model.Transaction, error) {
	transactions, err := s.repo.GetAll()
	if err != nil {
		return nil, fmt.Errorf("get transactions: %w", err)
	}
	return transactions, nil
}

func (s *TransactionService) GetTransactionsByUser(userID uint) ([]model.Transaction, error) {
	transactions, err := s.repo.GetByUserID(userID)
	if err != nil {
		return nil, fmt.Errorf("get transactions by user: %w", err)
	}
	return transactions, nil
}

func (s *TransactionService) GetTransactionsByAccount(accountID, userID uint) ([]model.Transaction, error) {
	transactions, err := s.repo.GetByAccountID(accountID, userID)
	if err != nil {
		return nil, fmt.Errorf("get transactions by account: %w", err)
	}
	return transactions, nil
}

func (s *TransactionService) GetTransaction(id uint) (*model.Transaction, error) {
	transaction, err := s.repo.GetByID(id)
	if err != nil {
		return nil, fmt.Errorf("get transaction: %w", err)
	}
	return transaction, nil
}

func (s *TransactionService) CreateTransaction(tx *model.Transaction) (*model.Transaction, error) {
	// Validate amount
	if tx.Amount.LessThanOrEqual(decimal.Zero) {
		return nil, ErrInvalidAmount
	}

	// Validate currency
	if len(tx.Currency) != 3 {
		return nil, ErrInvalidCurrency
	}

	// Validate transaction type
	if !model.IsValidTransactionType(tx.Type) {
		return nil, ErrInvalidTransactionType
	}

	// Set defaults
	if tx.TransactionDate.IsZero() {
		tx.TransactionDate = time.Now()
	}
	if tx.Status == "" {
		tx.Status = model.TransactionStatusCompleted
	}

	// Update account balances if accountRepo is available
	if s.accountRepo != nil {
		if err := s.updateBalances(tx); err != nil {
			return nil, err
		}
	}

	created, err := s.repo.Create(tx)
	if err != nil {
		return nil, fmt.Errorf("create transaction: %w", err)
	}

	return created, nil
}

func (s *TransactionService) updateBalances(tx *model.Transaction) error {
	// Get source account
	account, err := s.accountRepo.GetByID(tx.AccountID)
	if err != nil {
		return ErrAccountNotFound
	}

	// Verify ownership
	if account.UserID != tx.UserID {
		return ErrAccountAccessDenied
	}

	// Apply to balance based on type
	switch tx.Type {
	case model.TransactionTypeIncome:
		account.Balance = account.Balance.Add(tx.Amount)
	case model.TransactionTypeExpense:
		account.Balance = account.Balance.Sub(tx.Amount)
	case model.TransactionTypeTransfer:
		if tx.DestinationAccountID == nil {
			return ErrDestinationAccountRequired
		}

		// Subtract from source
		account.Balance = account.Balance.Sub(tx.Amount)

		// Add to destination
		destAccount, err := s.accountRepo.GetByID(*tx.DestinationAccountID)
		if err != nil {
			return ErrDestinationAccountNotFound
		}

		if destAccount.UserID != tx.UserID {
			return ErrAccountAccessDenied
		}

		destAccount.Balance = destAccount.Balance.Add(tx.Amount)
		if err := s.accountRepo.Update(destAccount); err != nil {
			return fmt.Errorf("update destination account balance: %w", err)
		}
	}

	if err := s.accountRepo.Update(account); err != nil {
		return fmt.Errorf("update account balance: %w", err)
	}

	return nil
}
