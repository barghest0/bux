package model

import (
	"time"

	"github.com/shopspring/decimal"
	"gorm.io/gorm"
)

type Transaction struct {
	ID          uint            `gorm:"primaryKey" json:"id"`
	UserID      uint            `json:"user_id" gorm:"index;not null"`
	Amount      decimal.Decimal `json:"amount" gorm:"type:decimal(19,4);not null"`
	Currency    string         `json:"currency" gorm:"type:char(3);default:'USD'"`
	CategoryID  *uint          `json:"category_id" gorm:"index"`
	Category    *Category      `json:"category,omitempty" gorm:"foreignKey:CategoryID"`
	Tag         string         `json:"tag" gorm:"type:varchar(50)"`
	Description string         `json:"description" gorm:"type:text"`
	CreatedAt   time.Time      `json:"created_at"`
	UpdatedAt   time.Time      `json:"updated_at"`
	DeletedAt   gorm.DeletedAt `gorm:"index" json:"-"`
}
