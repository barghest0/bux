package dto

import (
	"transaction/internal/domain/model"

	"github.com/shopspring/decimal"
)

type AccountResponse struct {
	ID        uint   `json:"id"`
	Type      string `json:"type"`
	Name      string `json:"name"`
	Currency  string `json:"currency"`
	Balance   string `json:"balance"`
	Icon      string `json:"icon"`
	Color     string `json:"color"`
	IsActive  bool   `json:"is_active"`
	SortOrder int    `json:"sort_order"`
}

type CreateAccountRequest struct {
	Type      string `json:"type" binding:"required"`
	Name      string `json:"name" binding:"required"`
	Currency  string `json:"currency" binding:"required,len=3"`
	Balance   string `json:"balance"`
	Icon      string `json:"icon"`
	Color     string `json:"color"`
	SortOrder int    `json:"sort_order"`
}

type UpdateAccountRequest struct {
	Name      string `json:"name"`
	Icon      string `json:"icon"`
	Color     string `json:"color"`
	SortOrder *int   `json:"sort_order"`
}

func (r *CreateAccountRequest) ParseBalance() (decimal.Decimal, error) {
	if r.Balance == "" {
		return decimal.Zero, nil
	}
	return decimal.NewFromString(r.Balance)
}

func AccountFromModel(a model.Account) AccountResponse {
	return AccountResponse{
		ID:        a.ID,
		Type:      string(a.Type),
		Name:      a.Name,
		Currency:  a.Currency,
		Balance:   a.Balance.String(),
		Icon:      a.Icon,
		Color:     a.Color,
		IsActive:  a.IsActive,
		SortOrder: a.SortOrder,
	}
}

func AccountListFromModel(accounts []model.Account) []AccountResponse {
	res := make([]AccountResponse, len(accounts))
	for i, a := range accounts {
		res[i] = AccountFromModel(a)
	}
	return res
}
