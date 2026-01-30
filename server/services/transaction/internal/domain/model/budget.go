package model

import (
	"time"

	"github.com/shopspring/decimal"
	"gorm.io/gorm"
)

type BudgetPeriod string

const (
	BudgetPeriodMonthly BudgetPeriod = "monthly"
	BudgetPeriodYearly  BudgetPeriod = "yearly"
)

type Budget struct {
	ID         uint            `gorm:"primaryKey" json:"id"`
	UserID     uint            `gorm:"index;not null" json:"user_id"`
	CategoryID uint            `gorm:"index;not null" json:"category_id"`
	Category   *Category       `gorm:"foreignKey:CategoryID" json:"category,omitempty"`
	Amount     decimal.Decimal `gorm:"type:decimal(19,4);not null" json:"amount"`
	Currency   string          `gorm:"type:char(3);default:'RUB'" json:"currency"`
	Period     BudgetPeriod    `gorm:"type:varchar(20);default:'monthly'" json:"period"`
	CreatedAt  time.Time       `json:"created_at"`
	UpdatedAt  time.Time       `json:"updated_at"`
	DeletedAt  gorm.DeletedAt  `gorm:"index" json:"-"`
}

func IsValidBudgetPeriod(p BudgetPeriod) bool {
	switch p {
	case BudgetPeriodMonthly, BudgetPeriodYearly:
		return true
	}
	return false
}
