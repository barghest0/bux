package model

import (
	"time"

	"gorm.io/gorm"
)

type Broker struct {
	ID        uint           `gorm:"primaryKey"`
	UserID    string         `gorm:"index;not null"`
	Name      string         `gorm:"not null"`
	CreatedAt time.Time      `gorm:"autoCreateTime"`
	DeletedAt gorm.DeletedAt `gorm:"index"`
}

type Portfolio struct {
	ID           uint           `gorm:"primaryKey"`
	UserID       string         `gorm:"index;not null"`
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
	ID          uint           `gorm:"primaryKey"`
	PortfolioID uint           `gorm:"index;not null"`
	SecurityID  uint           `gorm:"index;not null"`
	TradeDate   time.Time      `gorm:"not null"`
	Side        string         `gorm:"type:trade_side;not null"`
	Quantity    float64        `gorm:"not null"`
	Price       float64        `gorm:"not null"`
	Fee         float64        `gorm:"default:0"`
	CreatedAt   time.Time      `gorm:"autoCreateTime"`
	DeletedAt   gorm.DeletedAt `gorm:"index"`
}
