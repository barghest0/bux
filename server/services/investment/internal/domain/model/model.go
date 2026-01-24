package model

import (
	"time"

	"github.com/shopspring/decimal"
	"gorm.io/gorm"
)

type SecurityType string

const (
	SecurityTypeStock  SecurityType = "stock"
	SecurityTypeETF    SecurityType = "etf"
	SecurityTypeBond   SecurityType = "bond"
	SecurityTypeFund   SecurityType = "fund"
	SecurityTypeCrypto SecurityType = "crypto"
	SecurityTypeMetal  SecurityType = "metal"
)

type TradeSide string

const (
	TradeSideBuy  TradeSide = "buy"
	TradeSideSell TradeSide = "sell"
)

type Broker struct {
	ID        uint           `gorm:"primaryKey" json:"id"`
	UserID    uint           `gorm:"index;not null" json:"user_id"`
	Name      string         `gorm:"not null" json:"name"`
	CreatedAt time.Time      `gorm:"autoCreateTime" json:"created_at"`
	DeletedAt gorm.DeletedAt `gorm:"index" json:"-"`
}

type Portfolio struct {
	ID           uint           `gorm:"primaryKey" json:"id"`
	UserID       uint           `gorm:"index;not null" json:"user_id"`
	BrokerID     uint           `gorm:"index;not null" json:"broker_id"`
	Broker       *Broker        `gorm:"foreignKey:BrokerID" json:"broker,omitempty"`
	Name         string         `gorm:"not null" json:"name"`
	BaseCurrency string         `gorm:"size:3;default:RUB" json:"base_currency"`
	CreatedAt    time.Time      `gorm:"autoCreateTime" json:"created_at"`
	DeletedAt    gorm.DeletedAt `gorm:"index" json:"-"`
}

type Security struct {
	ID        uint           `gorm:"primaryKey" json:"id"`
	Symbol    string         `gorm:"uniqueIndex;not null" json:"symbol"`
	Name      string         `gorm:"not null" json:"name"`
	Type      SecurityType   `gorm:"type:varchar(20);not null" json:"type"`
	Currency  string         `gorm:"size:3;not null" json:"currency"`
	ISIN      string         `gorm:"size:12" json:"isin,omitempty"`
	Exchange  string         `gorm:"size:10" json:"exchange,omitempty"`
	CreatedAt time.Time      `gorm:"autoCreateTime" json:"created_at"`
	DeletedAt gorm.DeletedAt `gorm:"index" json:"-"`
}

type Trade struct {
	ID          uint            `gorm:"primaryKey" json:"id"`
	PortfolioID uint            `gorm:"index;not null" json:"portfolio_id"`
	SecurityID  uint            `gorm:"index;not null" json:"security_id"`
	Security    *Security       `gorm:"foreignKey:SecurityID" json:"security,omitempty"`
	TradeDate   time.Time       `gorm:"not null" json:"trade_date"`
	Side        TradeSide       `gorm:"type:varchar(10);not null" json:"side"`
	Quantity    decimal.Decimal `gorm:"type:decimal(19,8);not null" json:"quantity"`
	Price       decimal.Decimal `gorm:"type:decimal(19,4);not null" json:"price"`
	Fee         decimal.Decimal `gorm:"type:decimal(19,4);default:0" json:"fee"`
	Note        string          `gorm:"type:text" json:"note,omitempty"`
	CreatedAt   time.Time       `gorm:"autoCreateTime" json:"created_at"`
	DeletedAt   gorm.DeletedAt  `gorm:"index" json:"-"`
}

type Holding struct {
	ID          uint            `gorm:"primaryKey" json:"id"`
	PortfolioID uint            `gorm:"uniqueIndex:portfolio_security_unique;not null" json:"portfolio_id"`
	SecurityID  uint            `gorm:"uniqueIndex:portfolio_security_unique;not null" json:"security_id"`
	Security    *Security       `gorm:"foreignKey:SecurityID" json:"security,omitempty"`
	Quantity    decimal.Decimal `gorm:"type:decimal(19,8);not null" json:"quantity"`
	AverageCost decimal.Decimal `gorm:"type:decimal(19,4);not null" json:"average_cost"`
	TotalCost   decimal.Decimal `gorm:"type:decimal(19,4);not null" json:"total_cost"`
	UpdatedAt   time.Time       `json:"updated_at"`
}

type PriceHistory struct {
	ID         uint            `gorm:"primaryKey" json:"id"`
	SecurityID uint            `gorm:"uniqueIndex:security_date_unique;not null" json:"security_id"`
	Date       time.Time       `gorm:"uniqueIndex:security_date_unique;not null;type:date" json:"date"`
	Open       decimal.Decimal `gorm:"type:decimal(19,4)" json:"open"`
	High       decimal.Decimal `gorm:"type:decimal(19,4)" json:"high"`
	Low        decimal.Decimal `gorm:"type:decimal(19,4)" json:"low"`
	Close      decimal.Decimal `gorm:"type:decimal(19,4);not null" json:"close"`
	Volume     int64           `json:"volume"`
}

func IsValidSecurityType(t SecurityType) bool {
	switch t {
	case SecurityTypeStock, SecurityTypeETF, SecurityTypeBond,
		SecurityTypeFund, SecurityTypeCrypto, SecurityTypeMetal:
		return true
	}
	return false
}

func IsValidTradeSide(s TradeSide) bool {
	return s == TradeSideBuy || s == TradeSideSell
}
