package repository

import (
	"time"
	"transaction/internal/domain/model"

	"gorm.io/gorm"
)

type RecurringTransactionRepository struct {
	db *gorm.DB
}

func NewRecurringTransactionRepository(db *gorm.DB) *RecurringTransactionRepository {
	return &RecurringTransactionRepository{db}
}

func (r *RecurringTransactionRepository) Create(rt *model.RecurringTransaction) (*model.RecurringTransaction, error) {
	if err := r.db.Create(rt).Error; err != nil {
		return nil, err
	}
	if err := r.db.Preload("Category").First(rt, rt.ID).Error; err != nil {
		return nil, err
	}
	return rt, nil
}

func (r *RecurringTransactionRepository) GetByID(id uint) (*model.RecurringTransaction, error) {
	var rt model.RecurringTransaction
	if err := r.db.Preload("Category").First(&rt, id).Error; err != nil {
		return nil, err
	}
	return &rt, nil
}

func (r *RecurringTransactionRepository) GetByUserID(userID uint) ([]model.RecurringTransaction, error) {
	var rts []model.RecurringTransaction
	if err := r.db.Preload("Category").
		Where("user_id = ?", userID).
		Order("next_date ASC").
		Find(&rts).Error; err != nil {
		return nil, err
	}
	return rts, nil
}

func (r *RecurringTransactionRepository) GetDue(before time.Time) ([]model.RecurringTransaction, error) {
	var rts []model.RecurringTransaction
	if err := r.db.Preload("Category").
		Where("is_active = ? AND next_date <= ?", true, before).
		Find(&rts).Error; err != nil {
		return nil, err
	}
	return rts, nil
}

func (r *RecurringTransactionRepository) Update(rt *model.RecurringTransaction) (*model.RecurringTransaction, error) {
	if err := r.db.Save(rt).Error; err != nil {
		return nil, err
	}
	if err := r.db.Preload("Category").First(rt, rt.ID).Error; err != nil {
		return nil, err
	}
	return rt, nil
}

func (r *RecurringTransactionRepository) Delete(id uint) error {
	return r.db.Delete(&model.RecurringTransaction{}, id).Error
}
