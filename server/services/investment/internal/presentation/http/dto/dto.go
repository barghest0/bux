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

type CreateSecurityRequest struct {
	Symbol   string `json:"symbol" binding:"required"`
	Name     string `json:"name" binding:"required"`
	Type     string `json:"type" binding:"required,oneof=stock etf bond fund crypto metal"`
	Currency string `json:"currency"`
	ISIN     string `json:"isin"`
	Exchange string `json:"exchange"`
}

type CreateTradeRequest struct {
	PortfolioID uint   `json:"portfolio_id" binding:"required"`
	SecurityID  uint   `json:"security_id" binding:"required"`
	Side        string `json:"side" binding:"required,oneof=buy sell"`
	Quantity    string `json:"quantity" binding:"required"`
	Price       string `json:"price" binding:"required"`
	Fee         string `json:"fee"`
	TradeDate   string `json:"trade_date" binding:"required"`
	Note        string `json:"note"`
}

func (r *CreateTradeRequest) ParseQuantity() (decimal.Decimal, error) {
	return decimal.NewFromString(r.Quantity)
}

func (r *CreateTradeRequest) ParsePrice() (decimal.Decimal, error) {
	return decimal.NewFromString(r.Price)
}

func (r *CreateTradeRequest) ParseFee() (decimal.Decimal, error) {
	if r.Fee == "" {
		return decimal.Zero, nil
	}
	return decimal.NewFromString(r.Fee)
}

type UpdatePriceRequest struct {
	SecurityID uint   `json:"security_id" binding:"required"`
	Date       string `json:"date" binding:"required"`
	Open       string `json:"open"`
	High       string `json:"high"`
	Low        string `json:"low"`
	Close      string `json:"close" binding:"required"`
	Volume     int64  `json:"volume"`
}

func (r *UpdatePriceRequest) ParseOpen() (decimal.Decimal, error) {
	if r.Open == "" {
		return decimal.Zero, nil
	}
	return decimal.NewFromString(r.Open)
}

func (r *UpdatePriceRequest) ParseHigh() (decimal.Decimal, error) {
	if r.High == "" {
		return decimal.Zero, nil
	}
	return decimal.NewFromString(r.High)
}

func (r *UpdatePriceRequest) ParseLow() (decimal.Decimal, error) {
	if r.Low == "" {
		return decimal.Zero, nil
	}
	return decimal.NewFromString(r.Low)
}

func (r *UpdatePriceRequest) ParseClose() (decimal.Decimal, error) {
	return decimal.NewFromString(r.Close)
}

type SearchSecuritiesQuery struct {
	Query string `form:"q"`
	Type  string `form:"type"`
}
