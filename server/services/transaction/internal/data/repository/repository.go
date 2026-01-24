package repository

import (
	"transaction/internal/domain/model"

	"gorm.io/gorm"
)

type TransactionRepository struct {
	db *gorm.DB
}

func New(db *gorm.DB) *TransactionRepository {
	return &TransactionRepository{db}
}

func (r *TransactionRepository) Create(transaction *model.Transaction) (*model.Transaction, error) {
	if err := r.db.Create(transaction).Error; err != nil {
		return nil, err
	}

	if err := r.db.Preload("Category").First(transaction, transaction.ID).Error; err != nil {
		return nil, err
	}

	return transaction, nil
}

func (r *TransactionRepository) GetByID(id uint) (*model.Transaction, error) {
	var tx model.Transaction
	if err := r.db.Preload("Category").First(&tx, id).Error; err != nil {
		return nil, err
	}
	return &tx, nil
}

func (r *TransactionRepository) GetAll() ([]model.Transaction, error) {
	var txs []model.Transaction
	if err := r.db.Preload("Category").Order("transaction_date DESC").Find(&txs).Error; err != nil {
		return nil, err
	}
	return txs, nil
}

func (r *TransactionRepository) GetByUserID(userID uint) ([]model.Transaction, error) {
	var txs []model.Transaction
	if err := r.db.Preload("Category").
		Where("user_id = ?", userID).
		Order("transaction_date DESC").
		Find(&txs).Error; err != nil {
		return nil, err
	}
	return txs, nil
}

func (r *TransactionRepository) GetByAccountID(accountID, userID uint) ([]model.Transaction, error) {
	var txs []model.Transaction
	if err := r.db.Preload("Category").
		Where("(account_id = ? OR destination_account_id = ?) AND user_id = ?", accountID, accountID, userID).
		Order("transaction_date DESC").
		Find(&txs).Error; err != nil {
		return nil, err
	}
	return txs, nil
}

func (r *TransactionRepository) Update(tx *model.Transaction) (*model.Transaction, error) {
	if err := r.db.Save(tx).Error; err != nil {
		return nil, err
	}
	return tx, nil
}

func (r *TransactionRepository) Delete(id uint) error {
	return r.db.Delete(&model.Transaction{}, id).Error
}
