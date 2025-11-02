package http

type PortfolioURI struct {
	ID uint `uri:"id" binding:"required"`
}
