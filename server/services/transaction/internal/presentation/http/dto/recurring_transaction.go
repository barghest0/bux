package dto

import (
	"time"
	"transaction/internal/domain/model"

	"github.com/shopspring/decimal"
)

type RecurringTransactionResponse struct {
	ID          uint              `json:"id"`
	AccountID   uint              `json:"account_id"`
	Type        string            `json:"type"`
	Amount      string            `json:"amount"`
	Currency    string            `json:"currency"`
	Description string            `json:"description"`
	Frequency   string            `json:"frequency"`
	NextDate    time.Time         `json:"next_date"`
	EndDate     *time.Time        `json:"end_date,omitempty"`
	IsActive    bool              `json:"is_active"`
	Category    *CategoryResponse `json:"category,omitempty"`
	CreatedAt   time.Time         `json:"created_at"`
}

type CreateRecurringTransactionRequest struct {
	AccountID   uint   `json:"account_id" binding:"required"`
	Type        string `json:"type" binding:"required"`
	Amount      string `json:"amount" binding:"required"`
	Currency    string `json:"currency" binding:"required,len=3"`
	Description string `json:"description"`
	CategoryID  *uint  `json:"category_id"`
	Frequency   string `json:"frequency" binding:"required"`
	NextDate    string `json:"next_date" binding:"required"`
	EndDate     string `json:"end_date"`
}

type UpdateRecurringTransactionRequest struct {
	Amount      string `json:"amount"`
	Description string `json:"description"`
	CategoryID  *uint  `json:"category_id"`
	Frequency   string `json:"frequency"`
	NextDate    string `json:"next_date"`
	EndDate     string `json:"end_date"`
}

func (r *CreateRecurringTransactionRequest) ParseAmount() (decimal.Decimal, error) {
	return decimal.NewFromString(r.Amount)
}

func (r *CreateRecurringTransactionRequest) ParseNextDate() (time.Time, error) {
	return time.Parse(time.RFC3339, r.NextDate)
}

func (r *CreateRecurringTransactionRequest) ParseEndDate() (*time.Time, error) {
	if r.EndDate == "" {
		return nil, nil
	}
	t, err := time.Parse(time.RFC3339, r.EndDate)
	if err != nil {
		return nil, err
	}
	return &t, nil
}

func RecurringFromModel(rt model.RecurringTransaction) RecurringTransactionResponse {
	var category *CategoryResponse
	if rt.Category != nil {
		cat := CategoryFromModel(*rt.Category)
		category = &cat
	}
	return RecurringTransactionResponse{
		ID:          rt.ID,
		AccountID:   rt.AccountID,
		Type:        string(rt.Type),
		Amount:      rt.Amount.String(),
		Currency:    rt.Currency,
		Description: rt.Description,
		Frequency:   string(rt.Frequency),
		NextDate:    rt.NextDate,
		EndDate:     rt.EndDate,
		IsActive:    rt.IsActive,
		Category:    category,
		CreatedAt:   rt.CreatedAt,
	}
}

func RecurringFromModelList(rts []model.RecurringTransaction) []RecurringTransactionResponse {
	res := make([]RecurringTransactionResponse, len(rts))
	for i, rt := range rts {
		res[i] = RecurringFromModel(rt)
	}
	return res
}
