package repository

import (
	"investment/internal/domain/model"
	"time"

	"gorm.io/gorm"
	"gorm.io/gorm/clause"
)

type InvestmentRepository struct {
	db *gorm.DB
}

func New(db *gorm.DB) *InvestmentRepository {
	return &InvestmentRepository{db}
}

// Broker methods
func (r *InvestmentRepository) CreateBroker(b *model.Broker) error {
	return r.db.Create(b).Error
}

func (r *InvestmentRepository) GetBrokersByUserID(userID uint) ([]model.Broker, error) {
	var brokers []model.Broker
	err := r.db.Where("user_id = ?", userID).Find(&brokers).Error
	return brokers, err
}

func (r *InvestmentRepository) GetBrokerByID(id uint) (*model.Broker, error) {
	var broker model.Broker
	err := r.db.First(&broker, id).Error
	return &broker, err
}

// Portfolio methods
func (r *InvestmentRepository) CreatePortfolio(p *model.Portfolio) error {
	return r.db.Create(p).Error
}

func (r *InvestmentRepository) GetPortfoliosByUserID(userID uint) ([]model.Portfolio, error) {
	var portfolios []model.Portfolio
	err := r.db.Preload("Broker").Where("user_id = ?", userID).Find(&portfolios).Error
	return portfolios, err
}

func (r *InvestmentRepository) GetPortfolioByID(id uint) (*model.Portfolio, error) {
	var portfolio model.Portfolio
	err := r.db.Preload("Broker").First(&portfolio, id).Error
	return &portfolio, err
}

// Security methods
func (r *InvestmentRepository) CreateSecurity(s *model.Security) error {
	return r.db.Create(s).Error
}

func (r *InvestmentRepository) GetSecurityByID(id uint) (*model.Security, error) {
	var security model.Security
	err := r.db.First(&security, id).Error
	return &security, err
}

func (r *InvestmentRepository) GetSecurityBySymbol(symbol string) (*model.Security, error) {
	var security model.Security
	err := r.db.Where("symbol = ?", symbol).First(&security).Error
	return &security, err
}

func (r *InvestmentRepository) SearchSecurities(query string, securityType model.SecurityType) ([]model.Security, error) {
	var securities []model.Security
	q := r.db.Where("name ILIKE ? OR symbol ILIKE ?", "%"+query+"%", "%"+query+"%")
	if securityType != "" {
		q = q.Where("type = ?", securityType)
	}
	err := q.Limit(50).Find(&securities).Error
	return securities, err
}

// Trade methods
func (r *InvestmentRepository) CreateTrade(t *model.Trade) error {
	return r.db.Create(t).Error
}

func (r *InvestmentRepository) GetTradesByPortfolioID(portfolioID uint) ([]model.Trade, error) {
	var trades []model.Trade
	err := r.db.Preload("Security").Where("portfolio_id = ?", portfolioID).
		Order("trade_date DESC").Find(&trades).Error
	return trades, err
}

func (r *InvestmentRepository) GetTradesBySecurityID(portfolioID, securityID uint) ([]model.Trade, error) {
	var trades []model.Trade
	err := r.db.Where("portfolio_id = ? AND security_id = ?", portfolioID, securityID).
		Order("trade_date ASC").Find(&trades).Error
	return trades, err
}

// Holding methods
func (r *InvestmentRepository) UpsertHolding(h *model.Holding) error {
	return r.db.Clauses(clause.OnConflict{
		Columns:   []clause.Column{{Name: "portfolio_id"}, {Name: "security_id"}},
		DoUpdates: clause.AssignmentColumns([]string{"quantity", "average_cost", "total_cost", "updated_at"}),
	}).Create(h).Error
}

func (r *InvestmentRepository) GetHoldingsByPortfolioID(portfolioID uint) ([]model.Holding, error) {
	var holdings []model.Holding
	err := r.db.Preload("Security").Where("portfolio_id = ? AND quantity > 0", portfolioID).Find(&holdings).Error
	return holdings, err
}

func (r *InvestmentRepository) GetHolding(portfolioID, securityID uint) (*model.Holding, error) {
	var holding model.Holding
	err := r.db.Where("portfolio_id = ? AND security_id = ?", portfolioID, securityID).First(&holding).Error
	return &holding, err
}

func (r *InvestmentRepository) DeleteHolding(portfolioID, securityID uint) error {
	return r.db.Where("portfolio_id = ? AND security_id = ?", portfolioID, securityID).
		Delete(&model.Holding{}).Error
}

// PriceHistory methods
func (r *InvestmentRepository) UpsertPriceHistory(p *model.PriceHistory) error {
	return r.db.Clauses(clause.OnConflict{
		Columns:   []clause.Column{{Name: "security_id"}, {Name: "date"}},
		DoUpdates: clause.AssignmentColumns([]string{"open", "high", "low", "close", "volume"}),
	}).Create(p).Error
}

func (r *InvestmentRepository) GetLatestPrice(securityID uint) (*model.PriceHistory, error) {
	var price model.PriceHistory
	err := r.db.Where("security_id = ?", securityID).Order("date DESC").First(&price).Error
	return &price, err
}

func (r *InvestmentRepository) GetPriceHistory(securityID uint, from, to time.Time) ([]model.PriceHistory, error) {
	var prices []model.PriceHistory
	err := r.db.Where("security_id = ? AND date >= ? AND date <= ?", securityID, from, to).
		Order("date ASC").Find(&prices).Error
	return prices, err
}

func (r *InvestmentRepository) GetLatestPricesForSecurities(securityIDs []uint) (map[uint]model.PriceHistory, error) {
	if len(securityIDs) == 0 {
		return make(map[uint]model.PriceHistory), nil
	}

	var prices []model.PriceHistory
	subquery := r.db.Model(&model.PriceHistory{}).
		Select("security_id, MAX(date) as max_date").
		Where("security_id IN ?", securityIDs).
		Group("security_id")

	err := r.db.Model(&model.PriceHistory{}).
		Joins("JOIN (?) AS latest ON price_histories.security_id = latest.security_id AND price_histories.date = latest.max_date", subquery).
		Find(&prices).Error

	if err != nil {
		return nil, err
	}

	result := make(map[uint]model.PriceHistory, len(prices))
	for _, p := range prices {
		result[p.SecurityID] = p
	}
	return result, nil
}
