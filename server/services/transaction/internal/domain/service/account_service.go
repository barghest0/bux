package service

import (
	"errors"
	"fmt"
	"transaction/internal/data/repository"
	"transaction/internal/domain/model"

	"github.com/shopspring/decimal"
)

var (
	ErrAccountNotFound     = errors.New("account not found")
	ErrAccountAccessDenied = errors.New("access denied to this account")
	ErrInvalidAccountType  = errors.New("invalid account type")
	ErrInvalidAccountName  = errors.New("account name is required")
)

type AccountService struct {
	repo *repository.AccountRepository
}

func NewAccountService(repo *repository.AccountRepository) *AccountService {
	return &AccountService{repo: repo}
}

func (s *AccountService) CreateAccount(account *model.Account) (*model.Account, error) {
	if account.Name == "" {
		return nil, ErrInvalidAccountName
	}

	if !model.IsValidAccountType(account.Type) {
		return nil, ErrInvalidAccountType
	}

	if account.Currency == "" {
		account.Currency = "RUB"
	}

	if account.Balance.IsZero() {
		account.Balance = decimal.Zero
	}

	account.IsActive = true

	if err := s.repo.Create(account); err != nil {
		return nil, fmt.Errorf("create account: %w", err)
	}

	return account, nil
}

func (s *AccountService) GetAccount(id, userID uint) (*model.Account, error) {
	account, err := s.repo.GetByID(id)
	if err != nil {
		return nil, ErrAccountNotFound
	}

	if account.UserID != userID {
		return nil, ErrAccountAccessDenied
	}

	return account, nil
}

func (s *AccountService) GetUserAccounts(userID uint) ([]model.Account, error) {
	return s.repo.GetByUserID(userID)
}

func (s *AccountService) GetActiveUserAccounts(userID uint) ([]model.Account, error) {
	return s.repo.GetActiveByUserID(userID)
}

func (s *AccountService) UpdateAccount(id, userID uint, updates *model.Account) (*model.Account, error) {
	account, err := s.GetAccount(id, userID)
	if err != nil {
		return nil, err
	}

	if updates.Name != "" {
		account.Name = updates.Name
	}
	if updates.Icon != "" {
		account.Icon = updates.Icon
	}
	if updates.Color != "" {
		account.Color = updates.Color
	}
	if updates.SortOrder != 0 {
		account.SortOrder = updates.SortOrder
	}

	if err := s.repo.Update(account); err != nil {
		return nil, fmt.Errorf("update account: %w", err)
	}

	return account, nil
}

func (s *AccountService) DeactivateAccount(id, userID uint) error {
	account, err := s.GetAccount(id, userID)
	if err != nil {
		return err
	}

	account.IsActive = false
	return s.repo.Update(account)
}

func (s *AccountService) DeleteAccount(id, userID uint) error {
	_, err := s.GetAccount(id, userID)
	if err != nil {
		return err
	}

	return s.repo.Delete(id)
}

func (s *AccountService) UpdateBalance(id uint, newBalance decimal.Decimal) error {
	return s.repo.UpdateBalance(id, newBalance)
}

func (s *AccountService) GetTotalBalance(userID uint, currency string) (decimal.Decimal, error) {
	return s.repo.GetTotalBalance(userID, currency)
}
