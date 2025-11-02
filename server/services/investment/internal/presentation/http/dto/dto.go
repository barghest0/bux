package dto

type CreateBrokerRequest struct {
	UserID string `json:"user_id" binding:"required"`
	Name   string `json:"name" binding:"required"`
}

type CreatePortfolioRequest struct {
	UserID       string `json:"user_id" binding:"required"`
	BrokerID     uint   `json:"broker_id" binding:"required"`
	Name         string `json:"name" binding:"required"`
	BaseCurrency string `json:"base_currency"`
}

type CreateTradeRequest struct {
	PortfolioID uint    `json:"portfolio_id" binding:"required"`
	SecurityID  uint    `json:"security_id" binding:"required"`
	Side        string  `json:"side" binding:"required,oneof=buy sell"`
	Quantity    float64 `json:"quantity" binding:"required,gt=0"`
	Price       float64 `json:"price" binding:"required,gt=0"`
	Fee         float64 `json:"fee"`
	TradeDate   string  `json:"trade_date" binding:"required"`
}
