package dto

import (
	"time"
	"transaction/internal/domain/model"

	"github.com/shopspring/decimal"
)

type TransactionResponse struct {
	ID                   uint              `json:"id"`
	AccountID            uint              `json:"account_id"`
	DestinationAccountID *uint             `json:"destination_account_id,omitempty"`
	Type                 string            `json:"type"`
	Status               string            `json:"status"`
	Amount               string            `json:"amount"`
	Currency             string            `json:"currency"`
	Description          string            `json:"description"`
	TransactionDate      time.Time         `json:"transaction_date"`
	Category             *CategoryResponse `json:"category,omitempty"`
}

type CreateTransactionRequest struct {
	AccountID            uint   `json:"account_id" binding:"required"`
	DestinationAccountID *uint  `json:"destination_account_id"`
	Type                 string `json:"type" binding:"required"`
	Amount               string `json:"amount" binding:"required"`
	Currency             string `json:"currency" binding:"required,len=3"`
	Description          string `json:"description"`
	CategoryID           *uint  `json:"category_id"`
	TransactionDate      string `json:"transaction_date"`
}

func (r *CreateTransactionRequest) ParseAmount() (decimal.Decimal, error) {
	return decimal.NewFromString(r.Amount)
}

func (r *CreateTransactionRequest) ParseTransactionDate() (time.Time, error) {
	if r.TransactionDate == "" {
		return time.Now(), nil
	}
	return time.Parse(time.RFC3339, r.TransactionDate)
}

func FromModel(tx model.Transaction) TransactionResponse {
	var category *CategoryResponse

	if tx.Category != nil {
		cat := CategoryFromModel(*tx.Category)
		category = &cat
	}

	return TransactionResponse{
		ID:                   tx.ID,
		AccountID:            tx.AccountID,
		DestinationAccountID: tx.DestinationAccountID,
		Type:                 string(tx.Type),
		Status:               string(tx.Status),
		Amount:               tx.Amount.String(),
		Currency:             tx.Currency,
		Description:          tx.Description,
		TransactionDate:      tx.TransactionDate,
		Category:             category,
	}
}

func FromModelList(txs []model.Transaction) []TransactionResponse {
	res := make([]TransactionResponse, len(txs))
	for i, t := range txs {
		res[i] = FromModel(t)
	}
	return res
}
