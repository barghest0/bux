package repository

import (
	"transaction/internal/domain/model"

	"github.com/shopspring/decimal"
	"gorm.io/gorm"
)

type AccountRepository struct {
	db *gorm.DB
}

func NewAccountRepository(db *gorm.DB) *AccountRepository {
	return &AccountRepository{db: db}
}

func (r *AccountRepository) Create(account *model.Account) error {
	return r.db.Create(account).Error
}

func (r *AccountRepository) GetByID(id uint) (*model.Account, error) {
	var account model.Account
	err := r.db.First(&account, id).Error
	if err != nil {
		return nil, err
	}
	return &account, nil
}

func (r *AccountRepository) GetByUserID(userID uint) ([]model.Account, error) {
	var accounts []model.Account
	err := r.db.Where("user_id = ?", userID).
		Order("sort_order ASC, created_at DESC").
		Find(&accounts).Error
	return accounts, err
}

func (r *AccountRepository) GetActiveByUserID(userID uint) ([]model.Account, error) {
	var accounts []model.Account
	err := r.db.Where("user_id = ? AND is_active = ?", userID, true).
		Order("sort_order ASC, created_at DESC").
		Find(&accounts).Error
	return accounts, err
}

func (r *AccountRepository) Update(account *model.Account) error {
	return r.db.Save(account).Error
}

func (r *AccountRepository) Delete(id uint) error {
	return r.db.Delete(&model.Account{}, id).Error
}

func (r *AccountRepository) UpdateBalance(id uint, newBalance decimal.Decimal) error {
	return r.db.Model(&model.Account{}).
		Where("id = ?", id).
		Update("balance", newBalance).Error
}

func (r *AccountRepository) GetTotalBalance(userID uint, currency string) (decimal.Decimal, error) {
	var result struct {
		Total decimal.Decimal
	}
	err := r.db.Model(&model.Account{}).
		Select("COALESCE(SUM(balance), 0) as total").
		Where("user_id = ? AND currency = ? AND is_active = ?", userID, currency, true).
		Scan(&result).Error
	return result.Total, err
}
