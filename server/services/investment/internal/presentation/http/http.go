package http

import (
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

	investments := r.Group("/investments")
	investments.Use(middleware.AuthMiddleware())
	{
		r.POST("/brokers", h.CreateBroker)
		r.POST("/portfolios", h.CreatePortfolio)
		r.POST("/trades", h.CreateTrade)
		r.GET("/portfolios/:id/holdings", h.GetHoldings)
	}

}
func (h *InvestmentHandler) CreateBroker(c *gin.Context) {
	var req dto.CreateBrokerRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	b, err := h.service.CreateBroker(req.UserID, req.Name)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusCreated, b)
}

func (h *InvestmentHandler) CreatePortfolio(c *gin.Context) {
	var req dto.CreatePortfolioRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	p, err := h.service.CreatePortfolio(req.UserID, req.BrokerID, req.Name, req.BaseCurrency)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusCreated, p)
}

func (h *InvestmentHandler) CreateTrade(c *gin.Context) {
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

	// Parse decimal values
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

	t, err := h.service.CreateTrade(req.PortfolioID, req.SecurityID, req.Side, qty, price, fee, date)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusCreated, t)
}
func (h *InvestmentHandler) GetHoldings(ctx *gin.Context) {

	var uri PortfolioURI
	if err := ctx.ShouldBindUri(&uri); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
		return
	}

	holdings, err := h.service.CalculateHoldings(uri.ID)
	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	ctx.JSON(http.StatusOK, holdings)
}
