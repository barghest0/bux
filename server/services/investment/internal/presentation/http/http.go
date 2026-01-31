package http

import (
	"investment/internal/domain/model"
	"investment/internal/domain/service"
	"investment/internal/presentation/http/dto"
	"investment/internal/presentation/http/middleware"
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
)

type InvestmentHandler struct {
	service *service.InvestmentService
}

func New(r *gin.Engine, s *service.InvestmentService) {
	h := &InvestmentHandler{service: s}

	api := r.Group("/api")
	api.Use(middleware.AuthMiddleware())
	{
		// Brokers
		api.POST("/brokers", h.CreateBroker)
		api.GET("/brokers", h.GetBrokers)

		// Portfolios
		api.POST("/portfolios", h.CreatePortfolio)
		api.GET("/portfolios", h.GetPortfolios)
		api.GET("/portfolios/:id", h.GetPortfolio)
		api.GET("/portfolios/:id/holdings", h.GetHoldings)
		api.GET("/portfolios/:id/summary", h.GetPortfolioSummary)
		api.GET("/portfolios/:id/value", h.GetPortfolioSummary)
		api.GET("/portfolios/:id/trades", h.GetTrades)

		// Securities
		api.POST("/securities", h.CreateSecurity)
		api.GET("/securities", h.SearchSecurities)
		api.GET("/securities/:id", h.GetSecurity)
		api.GET("/securities/:id/price", h.GetLatestPrice)
		api.GET("/securities/:id/history", h.GetPriceHistory)

		// Trades
		api.POST("/trades", h.ExecuteTrade)

		// Prices
		api.POST("/prices", h.UpdatePrice)
	}
}

// Broker handlers
func (h *InvestmentHandler) CreateBroker(c *gin.Context) {
	var req dto.CreateBrokerRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	userID, ok := c.Get("userID")
	if !ok {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}
	b, err := h.service.CreateBroker(userID.(uint), req.Name)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusCreated, b)
}

func (h *InvestmentHandler) GetBrokers(c *gin.Context) {
	userID, ok := c.Get("userID")
	if !ok {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}
	brokers, err := h.service.GetBrokers(userID.(uint))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, brokers)
}

// Portfolio handlers
func (h *InvestmentHandler) CreatePortfolio(c *gin.Context) {
	var req dto.CreatePortfolioRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	userID, ok := c.Get("userID")
	if !ok {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}
	p, err := h.service.CreatePortfolio(userID.(uint), req.BrokerID, req.Name, req.BaseCurrency)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusCreated, p)
}

func (h *InvestmentHandler) GetPortfolios(c *gin.Context) {
	userID, ok := c.Get("userID")
	if !ok {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}
	portfolios, err := h.service.GetPortfolios(userID.(uint))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, portfolios)
}

func (h *InvestmentHandler) GetPortfolio(c *gin.Context) {
	var uri PortfolioURI
	if err := c.ShouldBindUri(&uri); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
		return
	}
	portfolio, err := h.service.GetPortfolio(uri.ID)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "portfolio not found"})
		return
	}
	c.JSON(http.StatusOK, portfolio)
}

func (h *InvestmentHandler) GetHoldings(c *gin.Context) {
	var uri PortfolioURI
	if err := c.ShouldBindUri(&uri); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
		return
	}
	holdings, err := h.service.GetHoldings(uri.ID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, holdings)
}

func (h *InvestmentHandler) GetPortfolioSummary(c *gin.Context) {
	var uri PortfolioURI
	if err := c.ShouldBindUri(&uri); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
		return
	}
	summary, err := h.service.CalculatePortfolioValue(uri.ID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, summary)
}

func (h *InvestmentHandler) GetTrades(c *gin.Context) {
	var uri PortfolioURI
	if err := c.ShouldBindUri(&uri); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
		return
	}

	if c.Query("page") != "" {
		pg := dto.ParsePagination(c)
		trades, total, err := h.service.GetTradesPaginated(uri.ID, pg.Limit(), pg.Offset())
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
			return
		}
		c.JSON(http.StatusOK, dto.NewPaginatedResponse(trades, pg.Page, pg.PageSize, int(total)))
		return
	}

	trades, err := h.service.GetTrades(uri.ID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, trades)
}

// Security handlers
func (h *InvestmentHandler) CreateSecurity(c *gin.Context) {
	var req dto.CreateSecurityRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	sec, err := h.service.CreateSecurity(req.Symbol, req.Name, model.SecurityType(req.Type), req.Currency)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusCreated, sec)
}

func (h *InvestmentHandler) SearchSecurities(c *gin.Context) {
	var query dto.SearchSecuritiesQuery
	if err := c.ShouldBindQuery(&query); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	securities, err := h.service.SearchSecurities(query.Query, model.SecurityType(query.Type))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, securities)
}

func (h *InvestmentHandler) GetSecurity(c *gin.Context) {
	var uri SecurityURI
	if err := c.ShouldBindUri(&uri); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
		return
	}
	sec, err := h.service.GetSecurity(uri.ID)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "security not found"})
		return
	}
	c.JSON(http.StatusOK, sec)
}

func (h *InvestmentHandler) GetLatestPrice(c *gin.Context) {
	var uri SecurityURI
	if err := c.ShouldBindUri(&uri); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
		return
	}
	price, err := h.service.GetLatestPrice(uri.ID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	if price == nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "no price data"})
		return
	}
	c.JSON(http.StatusOK, price)
}

func (h *InvestmentHandler) GetPriceHistory(c *gin.Context) {
	var uri SecurityURI
	if err := c.ShouldBindUri(&uri); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
		return
	}

	fromStr := c.Query("from")
	toStr := c.Query("to")

	var (
		from time.Time
		to   time.Time
		err  error
	)

	if fromStr != "" {
		from, err = time.Parse("2006-01-02", fromStr)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": "invalid 'from' date format, use YYYY-MM-DD"})
			return
		}
	}

	if toStr != "" {
		to, err = time.Parse("2006-01-02", toStr)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": "invalid 'to' date format, use YYYY-MM-DD"})
			return
		}
		// Include entire day
		to = to.Add(24*time.Hour - time.Nanosecond)
	} else {
		to = time.Now()
	}

	if !from.IsZero() && to.Before(from) {
		c.JSON(http.StatusBadRequest, gin.H{"error": "'to' must be after 'from'"})
		return
	}

	prices, err := h.service.GetPriceHistory(uri.ID, from, to)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, prices)
}

// Trade handler
func (h *InvestmentHandler) ExecuteTrade(c *gin.Context) {
	var req dto.CreateTradeRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	date, err := time.Parse(time.RFC3339, req.TradeDate)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid trade_date format"})
		return
	}

	qty, err := req.ParseQuantity()
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid quantity format"})
		return
	}

	price, err := req.ParsePrice()
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid price format"})
		return
	}

	fee, err := req.ParseFee()
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid fee format"})
		return
	}

	t, err := h.service.ExecuteTrade(req.PortfolioID, req.SecurityID, model.TradeSide(req.Side), qty, price, fee, date, req.Note)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusCreated, t)
}

// Price handler
func (h *InvestmentHandler) UpdatePrice(c *gin.Context) {
	var req dto.UpdatePriceRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	date, err := time.Parse("2006-01-02", req.Date)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid date format, use YYYY-MM-DD"})
		return
	}

	open, _ := req.ParseOpen()
	high, _ := req.ParseHigh()
	low, _ := req.ParseLow()
	closePrice, err := req.ParseClose()
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid close price"})
		return
	}

	if err := h.service.UpdatePrice(req.SecurityID, date, open, high, low, closePrice, req.Volume); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, gin.H{"status": "ok"})
}

type SecurityURI struct {
	ID uint `uri:"id" binding:"required"`
}
