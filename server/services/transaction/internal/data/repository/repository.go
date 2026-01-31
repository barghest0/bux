package repository

import (
	"time"
	"transaction/internal/domain/model"

	"github.com/shopspring/decimal"
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

func (r *TransactionRepository) GetByUserIDPaginated(userID uint, limit, offset int) ([]model.Transaction, int64, error) {
	var txs []model.Transaction
	var count int64

	q := r.db.Model(&model.Transaction{}).Where("user_id = ?", userID)
	if err := q.Count(&count).Error; err != nil {
		return nil, 0, err
	}

	if err := r.db.Preload("Category").
		Where("user_id = ?", userID).
		Order("transaction_date DESC").
		Limit(limit).Offset(offset).
		Find(&txs).Error; err != nil {
		return nil, 0, err
	}
	return txs, count, nil
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

func (r *TransactionRepository) GetByAccountIDPaginated(accountID, userID uint, limit, offset int) ([]model.Transaction, int64, error) {
	var txs []model.Transaction
	var count int64

	q := r.db.Model(&model.Transaction{}).
		Where("(account_id = ? OR destination_account_id = ?) AND user_id = ?", accountID, accountID, userID)
	if err := q.Count(&count).Error; err != nil {
		return nil, 0, err
	}

	if err := r.db.Preload("Category").
		Where("(account_id = ? OR destination_account_id = ?) AND user_id = ?", accountID, accountID, userID).
		Order("transaction_date DESC").
		Limit(limit).Offset(offset).
		Find(&txs).Error; err != nil {
		return nil, 0, err
	}
	return txs, count, nil
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

// Analytics aggregation queries

type CategoryTotalRow struct {
	CategoryID    *uint
	CategoryName  string
	CategoryIcon  string
	CategoryColor string
	Type          string
	Total         decimal.Decimal
	Count         int64
}

type MonthlyTotalRow struct {
	Year    int
	Month   int
	Income  decimal.Decimal
	Expense decimal.Decimal
}

func (r *TransactionRepository) GetSummaryByCategory(userID uint, from, to time.Time) ([]CategoryTotalRow, error) {
	var rows []CategoryTotalRow
	err := r.db.Raw(`
		SELECT
			t.category_id,
			COALESCE(c.name, 'Без категории') as category_name,
			COALESCE(c.icon, '') as category_icon,
			COALESCE(c.color, '') as category_color,
			t.type,
			SUM(t.amount) as total,
			COUNT(*) as count
		FROM transactions t
		LEFT JOIN categories c ON c.id = t.category_id
		WHERE t.user_id = ?
		  AND t.type IN ('income', 'expense')
		  AND t.status = 'completed'
		  AND t.transaction_date >= ?
		  AND t.transaction_date <= ?
		  AND t.deleted_at IS NULL
		GROUP BY t.category_id, c.name, c.icon, c.color, t.type
		ORDER BY total DESC
	`, userID, from, to).Scan(&rows).Error
	return rows, err
}

func (r *TransactionRepository) GetSummaryByMonth(userID uint, from, to time.Time) ([]MonthlyTotalRow, error) {
	var rows []MonthlyTotalRow
	err := r.db.Raw(`
		SELECT
			EXTRACT(YEAR FROM transaction_date)::int as year,
			EXTRACT(MONTH FROM transaction_date)::int as month,
			COALESCE(SUM(CASE WHEN type = 'income' THEN amount ELSE 0 END), 0) as income,
			COALESCE(SUM(CASE WHEN type = 'expense' THEN amount ELSE 0 END), 0) as expense
		FROM transactions
		WHERE user_id = ?
		  AND type IN ('income', 'expense')
		  AND status = 'completed'
		  AND transaction_date >= ?
		  AND transaction_date <= ?
		  AND deleted_at IS NULL
		GROUP BY year, month
		ORDER BY year, month
	`, userID, from, to).Scan(&rows).Error
	return rows, err
}
