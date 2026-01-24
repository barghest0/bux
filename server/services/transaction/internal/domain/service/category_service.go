package service

import (
	"errors"
	"fmt"
	"transaction/internal/data/repository"
	"transaction/internal/domain/model"
)

var (
	ErrCategoryNotFound     = errors.New("category not found")
	ErrCategoryAccessDenied = errors.New("access denied to this category")
	ErrInvalidCategoryType  = errors.New("invalid category type")
	ErrInvalidCategoryName  = errors.New("category name is required")
	ErrCannotDeleteSystem   = errors.New("cannot delete system category")
)

type CategoryService struct {
	repo *repository.CategoryRepository
}

func NewCategoryService(repo *repository.CategoryRepository) *CategoryService {
	return &CategoryService{repo: repo}
}

func (s *CategoryService) CreateCategory(category *model.Category) (*model.Category, error) {
	if category.Name == "" {
		return nil, ErrInvalidCategoryName
	}

	if !model.IsValidCategoryType(category.Type) {
		return nil, ErrInvalidCategoryType
	}

	if err := s.repo.Create(category); err != nil {
		return nil, fmt.Errorf("create category: %w", err)
	}

	return category, nil
}

func (s *CategoryService) GetCategory(id, userID uint) (*model.Category, error) {
	category, err := s.repo.GetByID(id)
	if err != nil {
		return nil, ErrCategoryNotFound
	}

	if category.UserID != userID {
		return nil, ErrCategoryAccessDenied
	}

	return category, nil
}

func (s *CategoryService) GetUserCategories(userID uint) ([]model.Category, error) {
	return s.repo.GetByUserID(userID)
}

func (s *CategoryService) GetCategoriesByType(userID uint, ctype model.CategoryType) ([]model.Category, error) {
	if !model.IsValidCategoryType(ctype) {
		return nil, ErrInvalidCategoryType
	}
	return s.repo.GetByType(userID, ctype)
}

func (s *CategoryService) UpdateCategory(id, userID uint, updates *model.Category) (*model.Category, error) {
	category, err := s.GetCategory(id, userID)
	if err != nil {
		return nil, err
	}

	if updates.Name != "" {
		category.Name = updates.Name
	}
	if updates.Icon != "" {
		category.Icon = updates.Icon
	}
	if updates.Color != "" {
		category.Color = updates.Color
	}
	if updates.SortOrder != 0 {
		category.SortOrder = updates.SortOrder
	}

	if err := s.repo.Update(category); err != nil {
		return nil, fmt.Errorf("update category: %w", err)
	}

	return category, nil
}

func (s *CategoryService) DeleteCategory(id, userID uint) error {
	category, err := s.GetCategory(id, userID)
	if err != nil {
		return err
	}

	if category.IsSystem {
		return ErrCannotDeleteSystem
	}

	return s.repo.Delete(id)
}

func (s *CategoryService) CreateDefaultCategories(userID uint) error {
	// Check if user already has categories
	hasCategories, err := s.repo.HasUserCategories(userID)
	if err != nil {
		return fmt.Errorf("check user categories: %w", err)
	}

	if hasCategories {
		return nil // Already has categories, skip
	}

	// Create default categories for user
	categories := make([]model.Category, len(model.DefaultCategories))
	for i, c := range model.DefaultCategories {
		categories[i] = model.Category{
			UserID:    userID,
			Name:      c.Name,
			Type:      c.Type,
			Icon:      c.Icon,
			Color:     c.Color,
			SortOrder: c.SortOrder,
			IsSystem:  c.IsSystem,
		}
	}

	if err := s.repo.CreateBatch(categories); err != nil {
		return fmt.Errorf("create default categories: %w", err)
	}

	return nil
}
