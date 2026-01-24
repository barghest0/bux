package repository

import (
	"transaction/internal/domain/model"

	"gorm.io/gorm"
)

type CategoryRepository struct {
	db *gorm.DB
}

func NewCategoryRepository(db *gorm.DB) *CategoryRepository {
	return &CategoryRepository{db: db}
}

func (r *CategoryRepository) Create(category *model.Category) error {
	return r.db.Create(category).Error
}

func (r *CategoryRepository) GetByID(id uint) (*model.Category, error) {
	var category model.Category
	if err := r.db.First(&category, id).Error; err != nil {
		return nil, err
	}
	return &category, nil
}

func (r *CategoryRepository) GetByUserID(userID uint) ([]model.Category, error) {
	var categories []model.Category
	err := r.db.Where("user_id = ?", userID).
		Order("type ASC, sort_order ASC, name ASC").
		Find(&categories).Error
	return categories, err
}

func (r *CategoryRepository) GetByType(userID uint, ctype model.CategoryType) ([]model.Category, error) {
	var categories []model.Category
	err := r.db.Where("user_id = ? AND type = ?", userID, ctype).
		Order("sort_order ASC, name ASC").
		Find(&categories).Error
	return categories, err
}

func (r *CategoryRepository) Update(category *model.Category) error {
	return r.db.Save(category).Error
}

func (r *CategoryRepository) Delete(id uint) error {
	return r.db.Delete(&model.Category{}, id).Error
}

func (r *CategoryRepository) HasUserCategories(userID uint) (bool, error) {
	var count int64
	err := r.db.Model(&model.Category{}).Where("user_id = ?", userID).Count(&count).Error
	return count > 0, err
}

func (r *CategoryRepository) CreateBatch(categories []model.Category) error {
	return r.db.Create(&categories).Error
}
