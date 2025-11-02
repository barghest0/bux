package repository

import (
	"investment/internal/domain/model"

	"gorm.io/gorm"
)

type InvestmentRepository struct {
	db *gorm.DB
}

func New(db *gorm.DB) *InvestmentRepository {
	return &InvestmentRepository{db}
}

func (r *InvestmentRepository) CreateBroker(b *model.Broker) error {
	return r.db.Create(b).Error
}

func (r *InvestmentRepository) CreatePortfolio(p *model.Portfolio) error {
	return r.db.Create(p).Error
}

func (r *InvestmentRepository) CreateTrade(t *model.Trade) error {
	return r.db.Create(t).Error
}

func (r *InvestmentRepository) GetHoldings(portfolioID uint) ([]model.Trade, error) {
	var trades []model.Trade
	err := r.db.Where("portfolio_id = ?", portfolioID).Find(&trades).Error
	return trades, err
}
