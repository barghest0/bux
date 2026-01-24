package service

import (
	"fmt"
	"investment/internal/data/repository"
	"investment/internal/domain/model"
	"time"

	"github.com/shopspring/decimal"
)

type InvestmentService struct {
	repo *repository.InvestmentRepository
}

func New(repo *repository.InvestmentRepository) *InvestmentService {
	return &InvestmentService{repo: repo}
}

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

func (s *InvestmentService) CreatePortfolio(userID uint, brokerID uint, name, baseCurrency string) (*model.Portfolio, error) {
	if name == "" {
		return nil, fmt.Errorf("create portfolio: name required")
	}
	if baseCurrency == "" {
		baseCurrency = "USD"
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

func (s *InvestmentService) CreateTrade(portfolioID, securityID uint, side string, qty, price, fee decimal.Decimal, date time.Time) (*model.Trade, error) {
	if side != "buy" && side != "sell" {
		return nil, fmt.Errorf("create trade: invalid side '%s'", side)
	}
	if qty.LessThanOrEqual(decimal.Zero) {
		return nil, fmt.Errorf("create trade: quantity must be greater than zero")
	}
	if price.LessThanOrEqual(decimal.Zero) {
		return nil, fmt.Errorf("create trade: price must be greater than zero")
	}
	if fee.LessThan(decimal.Zero) {
		return nil, fmt.Errorf("create trade: fee cannot be negative")
	}

	t := &model.Trade{
		PortfolioID: portfolioID,
		SecurityID:  securityID,
		TradeDate:   date,
		Side:        side,
		Quantity:    qty,
		Price:       price,
		Fee:         fee,
	}

	if err := s.repo.CreateTrade(t); err != nil {
		return nil, fmt.Errorf("create trade: %w", err)
	}
	return t, nil
}

func (s *InvestmentService) CalculateHoldings(portfolioID uint) (map[uint]decimal.Decimal, error) {
	trades, err := s.repo.GetHoldings(portfolioID)
	if err != nil {
		return nil, fmt.Errorf("calculate holdings: %w", err)
	}

	positions := make(map[uint]decimal.Decimal)
	for _, t := range trades {
		current := positions[t.SecurityID]
		switch t.Side {
		case "buy":
			positions[t.SecurityID] = current.Add(t.Quantity)
		case "sell":
			positions[t.SecurityID] = current.Sub(t.Quantity)
		default:
			return nil, fmt.Errorf("calculate holdings: invalid side '%s' in trade %d", t.Side, t.ID)
		}
	}
	return positions, nil
}
