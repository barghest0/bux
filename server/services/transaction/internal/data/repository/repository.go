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
	var user model.Transaction
	if err := r.db.Preload("Category").First(&user, id).Error; err != nil {
		return nil, err
	}
	return &user, nil
}

func (r *TransactionRepository) GetAll() ([]model.Transaction, error) {
	var users []model.Transaction
	if err := r.db.Preload("Category").Find(&users).Error; err != nil {
		return nil, err
	}
	return users, nil
}

func (r *TransactionRepository) Update(user *model.Transaction) (*model.Transaction, error) {
	if err := r.db.Save(user).Error; err != nil {
		return nil, err
	}

	return user, nil

}

func (r *TransactionRepository) Delete(id int) error {
	return r.db.Delete(&model.Transaction{}, id).Error
}
