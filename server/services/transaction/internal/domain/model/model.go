package model

import (
	"time"

	"gorm.io/gorm"
)

type Transaction struct {
	ID              uint           `gorm:"primaryKey" json:"id"`
	UserID          uint           `json:"user_id" gorm:"index;not null"`                     // связь с пользователем
	Amount          float64        `json:"amount" gorm:"not null"`                            // сумма транзакции
	Currency        string         `json:"currency" gorm:"type:char(3);default:'USD'"`        // валюта, 3 символа, по умолчанию USD
	Category        string         `json:"category" gorm:"type:varchar(50)"`                  // категория (еда, транспорт и т.п.)
	Tag             string         `json:"tag" gorm:"type:varchar(50)"`                       // опциональный тег
	Description     string         `json:"description" gorm:"type:text"`                      // описание
	TransactionType string         `json:"transaction_type" gorm:"type:varchar(10);not null"` // income / expense
	CreatedAt       time.Time      `json:"created_at"`
	UpdatedAt       time.Time      `json:"updated_at"`
	DeletedAt       gorm.DeletedAt `gorm:"index" json:"-"`
}
