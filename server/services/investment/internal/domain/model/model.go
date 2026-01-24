package model

import (
	"time"

	"github.com/shopspring/decimal"
	"gorm.io/gorm"
)

type Broker struct {
	ID        uint           `gorm:"primaryKey"`
	UserID    uint           `gorm:"index;not null"`
	Name      string         `gorm:"not null"`
	CreatedAt time.Time      `gorm:"autoCreateTime"`
	DeletedAt gorm.DeletedAt `gorm:"index"`
}

type Portfolio struct {
	ID           uint           `gorm:"primaryKey"`
	UserID       uint           `gorm:"index;not null"`
	BrokerID     uint           `gorm:"index;not null"`
	Name         string         `gorm:"not null"`
	BaseCurrency string         `gorm:"size:3;default:USD"`
	CreatedAt    time.Time      `gorm:"autoCreateTime"`
	DeletedAt    gorm.DeletedAt `gorm:"index"`
}

type Security struct {
	ID        uint           `gorm:"primaryKey"`
	Symbol    string         `gorm:"uniqueIndex;not null"`
	Name      string         `gorm:"not null"`
	Type      string         `gorm:"type:security_type;not null"`
	Currency  string         `gorm:"size:3;not null"`
	CreatedAt time.Time      `gorm:"autoCreateTime"`
	DeletedAt gorm.DeletedAt `gorm:"index"`
}

type Trade struct {
	ID          uint            `gorm:"primaryKey"`
	PortfolioID uint            `gorm:"index;not null"`
	SecurityID  uint            `gorm:"index;not null"`
	TradeDate   time.Time       `gorm:"not null"`
	Side        string          `gorm:"type:trade_side;not null"`
	Quantity    decimal.Decimal `gorm:"type:decimal(19,8);not null"`
	Price       decimal.Decimal `gorm:"type:decimal(19,4);not null"`
	Fee         decimal.Decimal `gorm:"type:decimal(19,4);default:0"`
	CreatedAt   time.Time       `gorm:"autoCreateTime"`
	DeletedAt   gorm.DeletedAt  `gorm:"index"`
}
