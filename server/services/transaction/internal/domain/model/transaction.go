package model

import (
	"time"

	"github.com/shopspring/decimal"
	"gorm.io/gorm"
)

type TransactionType string

const (
	TransactionTypeIncome   TransactionType = "income"
	TransactionTypeExpense  TransactionType = "expense"
	TransactionTypeTransfer TransactionType = "transfer"
)

type TransactionStatus string

const (
	TransactionStatusPending   TransactionStatus = "pending"
	TransactionStatusCompleted TransactionStatus = "completed"
	TransactionStatusFailed    TransactionStatus = "failed"
)

type Transaction struct {
	ID                   uint              `gorm:"primaryKey" json:"id"`
	UserID               uint              `gorm:"index;not null" json:"user_id"`
	AccountID            uint              `gorm:"index;not null" json:"account_id"`
	Account              *Account          `gorm:"foreignKey:AccountID" json:"-"`
	DestinationAccountID *uint             `gorm:"index" json:"destination_account_id,omitempty"`
	DestinationAccount   *Account          `gorm:"foreignKey:DestinationAccountID" json:"-"`
	Type                 TransactionType   `gorm:"type:varchar(20);not null;default:'expense'" json:"type"`
	Status               TransactionStatus `gorm:"type:varchar(20);not null;default:'completed'" json:"status"`
	Amount               decimal.Decimal   `gorm:"type:decimal(19,4);not null" json:"amount"`
	Currency             string            `gorm:"type:char(3);default:'RUB'" json:"currency"`
	CategoryID           *uint             `gorm:"index" json:"category_id,omitempty"`
	Category             *Category         `gorm:"foreignKey:CategoryID" json:"category,omitempty"`
	Description          string            `gorm:"type:text" json:"description"`
	TransactionDate      time.Time         `gorm:"not null" json:"transaction_date"`
	CreatedAt            time.Time         `json:"created_at"`
	UpdatedAt            time.Time         `json:"updated_at"`
	DeletedAt            gorm.DeletedAt    `gorm:"index" json:"-"`
}

func IsValidTransactionType(t TransactionType) bool {
	switch t {
	case TransactionTypeIncome, TransactionTypeExpense, TransactionTypeTransfer:
		return true
	}
	return false
}

func IsValidTransactionStatus(s TransactionStatus) bool {
	switch s {
	case TransactionStatusPending, TransactionStatusCompleted, TransactionStatusFailed:
		return true
	}
	return false
}
