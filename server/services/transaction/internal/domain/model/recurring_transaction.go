package model

import (
	"time"

	"github.com/shopspring/decimal"
	"gorm.io/gorm"
)

type RecurrenceFrequency string

const (
	FrequencyDaily   RecurrenceFrequency = "daily"
	FrequencyWeekly  RecurrenceFrequency = "weekly"
	FrequencyMonthly RecurrenceFrequency = "monthly"
	FrequencyYearly  RecurrenceFrequency = "yearly"
)

func IsValidFrequency(f RecurrenceFrequency) bool {
	switch f {
	case FrequencyDaily, FrequencyWeekly, FrequencyMonthly, FrequencyYearly:
		return true
	}
	return false
}

type RecurringTransaction struct {
	ID          uint                `gorm:"primaryKey" json:"id"`
	UserID      uint                `gorm:"index;not null" json:"user_id"`
	AccountID   uint                `gorm:"index;not null" json:"account_id"`
	Account     *Account            `gorm:"foreignKey:AccountID" json:"-"`
	Type        TransactionType     `gorm:"type:varchar(20);not null" json:"type"`
	Amount      decimal.Decimal     `gorm:"type:decimal(19,4);not null" json:"amount"`
	Currency    string              `gorm:"type:char(3);default:'RUB'" json:"currency"`
	CategoryID  *uint               `gorm:"index" json:"category_id,omitempty"`
	Category    *Category           `gorm:"foreignKey:CategoryID" json:"category,omitempty"`
	Description string              `gorm:"type:text" json:"description"`
	Frequency   RecurrenceFrequency `gorm:"type:varchar(20);not null" json:"frequency"`
	NextDate    time.Time           `gorm:"not null" json:"next_date"`
	EndDate     *time.Time          `json:"end_date,omitempty"`
	IsActive    bool                `gorm:"default:true" json:"is_active"`
	CreatedAt   time.Time           `json:"created_at"`
	UpdatedAt   time.Time           `json:"updated_at"`
	DeletedAt   gorm.DeletedAt      `gorm:"index" json:"-"`
}
