package service

import (
	"errors"
	"fmt"
	"investment/internal/data/repository"
	"investment/internal/domain/model"
	"time"

	"github.com/shopspring/decimal"
	"gorm.io/gorm"
)

type InvestmentService struct {
	repo *repository.InvestmentRepository
}

func New(repo *repository.InvestmentRepository) *InvestmentService {
	return &InvestmentService{repo: repo}
}

// Broker methods
func (s *InvestmentService) CreateBroker(userID uint, name string) (*model.Broker, error) {
	if name == "" {
		return nil, fmt.Errorf("create broker: name required")
	}
	b := &model.Broker{
		UserID:    userID,
		Name:      name,
		CreatedAt: time.Now(),
	}
	if err := s.repo.CreateBroker(b); err != nil {
		return nil, fmt.Errorf("create broker: %w", err)
	}
	return b, nil
}

func (s *InvestmentService) GetBrokers(userID uint) ([]model.Broker, error) {
	return s.repo.GetBrokersByUserID(userID)
}

// Portfolio methods
func (s *InvestmentService) CreatePortfolio(userID uint, brokerID uint, name, baseCurrency string) (*model.Portfolio, error) {
	if name == "" {
		return nil, fmt.Errorf("create portfolio: name required")
	}
	if baseCurrency == "" {
		baseCurrency = "RUB"
	}
	p := &model.Portfolio{
		UserID:       userID,
		BrokerID:     brokerID,
		Name:         name,
		BaseCurrency: baseCurrency,
		CreatedAt:    time.Now(),
	}
	if err := s.repo.CreatePortfolio(p); err != nil {
		return nil, fmt.Errorf("create portfolio: %w", err)
	}
	return p, nil
}

func (s *InvestmentService) GetPortfolios(userID uint) ([]model.Portfolio, error) {
	return s.repo.GetPortfoliosByUserID(userID)
}

func (s *InvestmentService) GetPortfolio(portfolioID uint) (*model.Portfolio, error) {
	return s.repo.GetPortfolioByID(portfolioID)
}

// Security methods
func (s *InvestmentService) CreateSecurity(symbol, name string, securityType model.SecurityType, currency string) (*model.Security, error) {
	if symbol == "" || name == "" {
		return nil, fmt.Errorf("create security: symbol and name required")
	}
	if !model.IsValidSecurityType(securityType) {
		return nil, fmt.Errorf("create security: invalid type '%s'", securityType)
	}
	if currency == "" {
		currency = "RUB"
	}

	sec := &model.Security{
		Symbol:    symbol,
		Name:      name,
		Type:      securityType,
		Currency:  currency,
		CreatedAt: time.Now(),
	}
	if err := s.repo.CreateSecurity(sec); err != nil {
		return nil, fmt.Errorf("create security: %w", err)
	}
	return sec, nil
}

func (s *InvestmentService) GetSecurity(id uint) (*model.Security, error) {
	return s.repo.GetSecurityByID(id)
}

func (s *InvestmentService) SearchSecurities(query string, securityType model.SecurityType) ([]model.Security, error) {
	return s.repo.SearchSecurities(query, securityType)
}

// Trade methods with automatic holding update
func (s *InvestmentService) ExecuteTrade(portfolioID, securityID uint, side model.TradeSide, qty, price, fee decimal.Decimal, tradeDate time.Time, note string) (*model.Trade, error) {
	if !model.IsValidTradeSide(side) {
		return nil, fmt.Errorf("execute trade: invalid side '%s'", side)
	}
	if qty.LessThanOrEqual(decimal.Zero) {
		return nil, fmt.Errorf("execute trade: quantity must be positive")
	}
	if price.LessThanOrEqual(decimal.Zero) {
		return nil, fmt.Errorf("execute trade: price must be positive")
	}
	if fee.LessThan(decimal.Zero) {
		return nil, fmt.Errorf("execute trade: fee cannot be negative")
	}

	// Create trade
	t := &model.Trade{
		PortfolioID: portfolioID,
		SecurityID:  securityID,
		TradeDate:   tradeDate,
		Side:        side,
		Quantity:    qty,
		Price:       price,
		Fee:         fee,
		Note:        note,
	}

	if err := s.repo.CreateTrade(t); err != nil {
		return nil, fmt.Errorf("execute trade: %w", err)
	}

	// Update holding
	if err := s.recalculateHolding(portfolioID, securityID); err != nil {
		return nil, fmt.Errorf("execute trade: failed to update holding: %w", err)
	}

	return t, nil
}

func (s *InvestmentService) GetTrades(portfolioID uint) ([]model.Trade, error) {
	return s.repo.GetTradesByPortfolioID(portfolioID)
}

func (s *InvestmentService) GetTradesPaginated(portfolioID uint, limit, offset int) ([]model.Trade, int64, error) {
	return s.repo.GetTradesByPortfolioIDPaginated(portfolioID, limit, offset)
}

// Holding methods
func (s *InvestmentService) recalculateHolding(portfolioID, securityID uint) error {
	trades, err := s.repo.GetTradesBySecurityID(portfolioID, securityID)
	if err != nil {
		return err
	}

	if len(trades) == 0 {
		return s.repo.DeleteHolding(portfolioID, securityID)
	}

	// Calculate using FIFO for average cost
	quantity := decimal.Zero
	totalCost := decimal.Zero

	for _, t := range trades {
		tradeCost := t.Quantity.Mul(t.Price).Add(t.Fee)

		if t.Side == model.TradeSideBuy {
			quantity = quantity.Add(t.Quantity)
			totalCost = totalCost.Add(tradeCost)
		} else {
			// For sells, reduce quantity proportionally
			if quantity.IsPositive() {
				avgCost := totalCost.Div(quantity)
				quantity = quantity.Sub(t.Quantity)
				if quantity.IsPositive() {
					totalCost = quantity.Mul(avgCost)
				} else {
					totalCost = decimal.Zero
					quantity = decimal.Zero
				}
			}
		}
	}

	if quantity.IsZero() || quantity.IsNegative() {
		return s.repo.DeleteHolding(portfolioID, securityID)
	}

	avgCost := totalCost.Div(quantity)

	holding := &model.Holding{
		PortfolioID: portfolioID,
		SecurityID:  securityID,
		Quantity:    quantity,
		AverageCost: avgCost,
		TotalCost:   totalCost,
		UpdatedAt:   time.Now(),
	}

	return s.repo.UpsertHolding(holding)
}

func (s *InvestmentService) GetHoldings(portfolioID uint) ([]model.Holding, error) {
	return s.repo.GetHoldingsByPortfolioID(portfolioID)
}

// P&L calculation types
type HoldingWithPnL struct {
	Holding       model.Holding   `json:"holding"`
	CurrentPrice  decimal.Decimal `json:"current_price"`
	MarketValue   decimal.Decimal `json:"market_value"`
	UnrealizedPnL decimal.Decimal `json:"unrealized_pnl"`
	UnrealizedPct decimal.Decimal `json:"unrealized_pct"`
}

type PortfolioSummary struct {
	PortfolioID      uint             `json:"portfolio_id"`
	TotalCost        decimal.Decimal  `json:"total_cost"`
	TotalMarketValue decimal.Decimal  `json:"total_market_value"`
	TotalUnrealPnL   decimal.Decimal  `json:"total_unrealized_pnl"`
	TotalUnrealPct   decimal.Decimal  `json:"total_unrealized_pct"`
	Holdings         []HoldingWithPnL `json:"holdings"`
}

func (s *InvestmentService) CalculatePortfolioValue(portfolioID uint) (*PortfolioSummary, error) {
	holdings, err := s.repo.GetHoldingsByPortfolioID(portfolioID)
	if err != nil {
		return nil, fmt.Errorf("calculate portfolio: %w", err)
	}

	if len(holdings) == 0 {
		return &PortfolioSummary{
			PortfolioID:      portfolioID,
			TotalCost:        decimal.Zero,
			TotalMarketValue: decimal.Zero,
			TotalUnrealPnL:   decimal.Zero,
			TotalUnrealPct:   decimal.Zero,
			Holdings:         []HoldingWithPnL{},
		}, nil
	}

	// Get security IDs
	securityIDs := make([]uint, len(holdings))
	for i, h := range holdings {
		securityIDs[i] = h.SecurityID
	}

	// Get latest prices
	prices, err := s.repo.GetLatestPricesForSecurities(securityIDs)
	if err != nil {
		return nil, fmt.Errorf("calculate portfolio: failed to get prices: %w", err)
	}

	summary := &PortfolioSummary{
		PortfolioID: portfolioID,
		Holdings:    make([]HoldingWithPnL, 0, len(holdings)),
	}

	for _, h := range holdings {
		hwp := HoldingWithPnL{
			Holding: h,
		}

		if price, ok := prices[h.SecurityID]; ok {
			hwp.CurrentPrice = price.Close
			hwp.MarketValue = h.Quantity.Mul(price.Close)
			hwp.UnrealizedPnL = hwp.MarketValue.Sub(h.TotalCost)
			if h.TotalCost.IsPositive() {
				hwp.UnrealizedPct = hwp.UnrealizedPnL.Div(h.TotalCost).Mul(decimal.NewFromInt(100))
			}
		} else {
			// No price data - use cost as market value
			hwp.CurrentPrice = h.AverageCost
			hwp.MarketValue = h.TotalCost
			hwp.UnrealizedPnL = decimal.Zero
			hwp.UnrealizedPct = decimal.Zero
		}

		summary.TotalCost = summary.TotalCost.Add(h.TotalCost)
		summary.TotalMarketValue = summary.TotalMarketValue.Add(hwp.MarketValue)
		summary.Holdings = append(summary.Holdings, hwp)
	}

	summary.TotalUnrealPnL = summary.TotalMarketValue.Sub(summary.TotalCost)
	if summary.TotalCost.IsPositive() {
		summary.TotalUnrealPct = summary.TotalUnrealPnL.Div(summary.TotalCost).Mul(decimal.NewFromInt(100))
	}

	return summary, nil
}

// Price history methods
func (s *InvestmentService) UpdatePrice(securityID uint, date time.Time, open, high, low, closePrice decimal.Decimal, volume int64) error {
	p := &model.PriceHistory{
		SecurityID: securityID,
		Date:       date,
		Open:       open,
		High:       high,
		Low:        low,
		Close:      closePrice,
		Volume:     volume,
	}
	return s.repo.UpsertPriceHistory(p)
}

func (s *InvestmentService) GetLatestPrice(securityID uint) (*model.PriceHistory, error) {
	price, err := s.repo.GetLatestPrice(securityID)
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, nil
		}
		return nil, err
	}
	return price, nil
}

func (s *InvestmentService) GetPriceHistory(securityID uint, from, to time.Time) ([]model.PriceHistory, error) {
	return s.repo.GetPriceHistory(securityID, from, to)
}

// Legacy method for backward compatibility
func (s *InvestmentService) CreateTrade(portfolioID, securityID uint, side string, qty, price, fee decimal.Decimal, date time.Time) (*model.Trade, error) {
	return s.ExecuteTrade(portfolioID, securityID, model.TradeSide(side), qty, price, fee, date, "")
}
