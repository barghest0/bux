package http

type TransactionURI struct {
	ID uint `uri:"id" binding:"required"`
}
