package dto

import (
	"transaction/internal/domain/model"

	"github.com/shopspring/decimal"
)

type CategoryResponse struct {
	ID    uint   `json:"id"`
	Name  string `json:"name"`
	Color string `json:"color"`
	Icon  string `json:"icon"`
}

type TransactionResponse struct {
	ID          uint              `json:"id"`
	Amount      string            `json:"amount"` // Decimal as string for precision
	Currency    string            `json:"currency"`
	Description string            `json:"description"`
	Category    *CategoryResponse `json:"category"`
}

type CreateTransactionRequest struct {
	Amount      string `json:"amount" binding:"required"` // Accept as string for precision
	Currency    string `json:"currency" binding:"required,len=3"`
	Description string `json:"description"`
	CategoryID  *uint  `json:"category_id"`
}

// ParseAmount parses the amount string into decimal
func (r *CreateTransactionRequest) ParseAmount() (decimal.Decimal, error) {
	return decimal.NewFromString(r.Amount)
}

func FromModel(tx model.Transaction) TransactionResponse {
	var category *CategoryResponse

	if tx.Category != nil {
		category = &CategoryResponse{
			ID:    tx.Category.ID,
			Name:  tx.Category.Name,
			Color: tx.Category.Color,
			Icon:  tx.Category.Icon,
		}
	}

	return TransactionResponse{
		ID:          tx.ID,
		Amount:      tx.Amount.String(),
		Currency:    tx.Currency,
		Description: tx.Description,
		Category:    category,
	}
}

func FromModelList(txs []model.Transaction) []TransactionResponse {
	res := make([]TransactionResponse, len(txs))
	for i, t := range txs {
		res[i] = FromModel(t)
	}
	return res
}
