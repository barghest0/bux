package dto

import "transaction/internal/domain/model"

type CategoryResponse struct {
	ID    uint   `json:"id"`
	Name  string `json:"name"`
	Color string `json:"color"`
	Icon  string `json:"icon"`
}

type TransactionResponse struct {
	ID          uint              `json:"id"`
	Amount      float64           `json:"amount"`
	Currency    string            `json:"currency"`
	Description string            `json:"description"`
	Category    *CategoryResponse `json:"category"`
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
		Amount:      tx.Amount,
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
