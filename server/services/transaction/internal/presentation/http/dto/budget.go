package dto

import "transaction/internal/domain/model"

type BudgetResponse struct {
	ID         uint              `json:"id"`
	CategoryID uint              `json:"category_id"`
	Category   *CategoryResponse `json:"category,omitempty"`
	Amount     string            `json:"amount"`
	Currency   string            `json:"currency"`
	Period     string            `json:"period"`
}

type CreateBudgetRequest struct {
	CategoryID uint   `json:"category_id" binding:"required"`
	Amount     string `json:"amount" binding:"required"`
	Currency   string `json:"currency"`
	Period     string `json:"period"`
}

type UpdateBudgetRequest struct {
	Amount string `json:"amount"`
	Period string `json:"period"`
}

type BudgetStatusResponse struct {
	BudgetID      uint   `json:"budget_id"`
	CategoryID    uint   `json:"category_id"`
	CategoryName  string `json:"category_name"`
	CategoryIcon  string `json:"category_icon"`
	CategoryColor string `json:"category_color"`
	BudgetAmount  string `json:"budget_amount"`
	SpentAmount   string `json:"spent_amount"`
	Remaining     string `json:"remaining"`
	SpentPercent  string `json:"spent_percent"`
	Period        string `json:"period"`
	Currency      string `json:"currency"`
}

func BudgetFromModel(b model.Budget) BudgetResponse {
	resp := BudgetResponse{
		ID:         b.ID,
		CategoryID: b.CategoryID,
		Amount:     b.Amount.String(),
		Currency:   b.Currency,
		Period:     string(b.Period),
	}
	if b.Category != nil {
		cat := CategoryFromModel(*b.Category)
		resp.Category = &cat
	}
	return resp
}

func BudgetListFromModel(budgets []model.Budget) []BudgetResponse {
	res := make([]BudgetResponse, len(budgets))
	for i, b := range budgets {
		res[i] = BudgetFromModel(b)
	}
	return res
}
