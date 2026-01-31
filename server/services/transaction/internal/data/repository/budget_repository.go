package repository

import (
	"time"
	"transaction/internal/domain/model"

	"github.com/shopspring/decimal"
	"gorm.io/gorm"
)

type BudgetRepository struct {
	db *gorm.DB
}

func NewBudgetRepository(db *gorm.DB) *BudgetRepository {
	return &BudgetRepository{db: db}
}

func (r *BudgetRepository) Create(budget *model.Budget) error {
	return r.db.Create(budget).Error
}

func (r *BudgetRepository) GetByID(id uint) (*model.Budget, error) {
	var budget model.Budget
	err := r.db.Preload("Category").First(&budget, id).Error
	return &budget, err
}

func (r *BudgetRepository) GetByUserID(userID uint) ([]model.Budget, error) {
	var budgets []model.Budget
	err := r.db.Preload("Category").
		Where("user_id = ?", userID).
		Find(&budgets).Error
	return budgets, err
}

func (r *BudgetRepository) Update(budget *model.Budget) error {
	return r.db.Save(budget).Error
}

func (r *BudgetRepository) Delete(id uint) error {
	return r.db.Delete(&model.Budget{}, id).Error
}

type BudgetStatusRow struct {
	BudgetID      uint
	CategoryID    uint
	CategoryName  string
	CategoryIcon  string
	CategoryColor string
	BudgetAmount  decimal.Decimal
	SpentAmount   decimal.Decimal
	Period        string
	Currency      string
}

func (r *BudgetRepository) GetBudgetStatus(userID uint, from, to time.Time) ([]BudgetStatusRow, error) {
	var rows []BudgetStatusRow
	err := r.db.Raw(`
		SELECT
			b.id as budget_id,
			b.category_id,
			c.name as category_name,
			c.icon as category_icon,
			c.color as category_color,
			b.amount as budget_amount,
			COALESCE(SUM(t.amount), 0) as spent_amount,
			b.period,
			b.currency
		FROM budgets b
		JOIN categories c ON c.id = b.category_id
		LEFT JOIN transactions t ON t.category_id = b.category_id
			AND t.user_id = b.user_id
			AND t.type = 'expense'
			AND t.status = 'completed'
			AND t.transaction_date >= ?
			AND t.transaction_date <= ?
			AND t.deleted_at IS NULL
		WHERE b.user_id = ?
		  AND b.deleted_at IS NULL
		GROUP BY b.id, b.category_id, c.name, c.icon, c.color, b.amount, b.period, b.currency
	`, from, to, userID).Scan(&rows).Error
	return rows, err
}
