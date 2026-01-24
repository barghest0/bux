package model

import (
	"time"

	"github.com/shopspring/decimal"
	"gorm.io/gorm"
)

type AccountType string

const (
	AccountTypeBankAccount AccountType = "bank_account"
	AccountTypeCard        AccountType = "card"
	AccountTypeCash        AccountType = "cash"
	AccountTypeCrypto      AccountType = "crypto"
	AccountTypeInvestment  AccountType = "investment"
	AccountTypeProperty    AccountType = "property"
)

type Account struct {
	ID        uint            `gorm:"primaryKey" json:"id"`
	UserID    uint            `gorm:"index;not null" json:"user_id"`
	Type      AccountType     `gorm:"type:varchar(20);not null" json:"type"`
	Name      string          `gorm:"not null" json:"name"`
	Currency  string          `gorm:"type:char(3);not null" json:"currency"`
	Balance   decimal.Decimal `gorm:"type:decimal(19,4);default:0" json:"balance"`
	Icon      string          `gorm:"type:varchar(50)" json:"icon"`
	Color     string          `gorm:"type:char(7)" json:"color"`
	IsActive  bool            `gorm:"default:true" json:"is_active"`
	SortOrder int             `gorm:"default:0" json:"sort_order"`
	CreatedAt time.Time       `json:"created_at"`
	UpdatedAt time.Time       `json:"updated_at"`
	DeletedAt gorm.DeletedAt  `gorm:"index" json:"-"`
}

func IsValidAccountType(t AccountType) bool {
	switch t {
	case AccountTypeBankAccount,
		AccountTypeCard,
		AccountTypeCash,
		AccountTypeCrypto,
		AccountTypeInvestment,
		AccountTypeProperty:
		return true
	}
	return false
}
