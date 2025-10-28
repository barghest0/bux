package http

import (
	"net/http"
	"transaction/internal/domain/service"
	"transaction/internal/presentation/http/dto"
	"transaction/internal/presentation/http/middleware"

	"github.com/gin-gonic/gin"
)

type UserHTTP struct {
	service *service.TransactionService
}

func New(r *gin.Engine, s *service.TransactionService) {
	h := &UserHTTP{
		service: s,
	}

	auth := r.Group("/transactions")
	auth.Use(middleware.AuthMiddleware())
	{
		auth.GET("/", h.Transactions)
		auth.GET("/:id", h.Transactions)
	}

}

func (h *UserHTTP) Transactions(ctx *gin.Context) {
	transactions, err := h.service.GetTransactions()
	if err != nil {
		ctx.JSON(http.StatusConflict, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusCreated, dto.FromModelList(transactions))
}

func (h *UserHTTP) Transaction(ctx *gin.Context) {
	var uri TransactionURI
	if err := ctx.ShouldBindUri(&uri); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
		return
	}

	tx, err := h.service.GetTransaction(uri.ID)

	if err != nil {
		ctx.JSON(http.StatusNotFound, gin.H{"error": "transaction not found"})
		return
	}

	ctx.JSON(http.StatusOK, dto.FromModel(*tx))
}
