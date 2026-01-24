package dto

import "github.com/shopspring/decimal"

type CreateBrokerRequest struct {
	UserID uint   `json:"user_id" binding:"required"`
	Name   string `json:"name" binding:"required"`
}

type CreatePortfolioRequest struct {
	UserID       uint   `json:"user_id" binding:"required"`
	BrokerID     uint   `json:"broker_id" binding:"required"`
	Name         string `json:"name" binding:"required"`
	BaseCurrency string `json:"base_currency"`
}

type CreateTradeRequest struct {
	PortfolioID uint   `json:"portfolio_id" binding:"required"`
	SecurityID  uint   `json:"security_id" binding:"required"`
	Side        string `json:"side" binding:"required,oneof=buy sell"`
	Quantity    string `json:"quantity" binding:"required"` // Decimal as string
	Price       string `json:"price" binding:"required"`    // Decimal as string
	Fee         string `json:"fee"`                         // Decimal as string
	TradeDate   string `json:"trade_date" binding:"required"`
}

// ParseQuantity parses quantity string to decimal
func (r *CreateTradeRequest) ParseQuantity() (decimal.Decimal, error) {
	return decimal.NewFromString(r.Quantity)
}

// ParsePrice parses price string to decimal
func (r *CreateTradeRequest) ParsePrice() (decimal.Decimal, error) {
	return decimal.NewFromString(r.Price)
}

// ParseFee parses fee string to decimal, returns zero if empty
func (r *CreateTradeRequest) ParseFee() (decimal.Decimal, error) {
	if r.Fee == "" {
		return decimal.Zero, nil
	}
	return decimal.NewFromString(r.Fee)
}
