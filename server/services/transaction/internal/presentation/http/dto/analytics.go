package dto

type CategorySummary struct {
	CategoryID    *uint  `json:"category_id"`
	CategoryName  string `json:"category_name"`
	CategoryIcon  string `json:"category_icon"`
	CategoryColor string `json:"category_color"`
	Type          string `json:"type"`
	Total         string `json:"total"`
	Count         int64  `json:"count"`
}

type MonthlySummary struct {
	Year    int    `json:"year"`
	Month   int    `json:"month"`
	Income  string `json:"income"`
	Expense string `json:"expense"`
	Net     string `json:"net"`
}

type TransactionSummaryResponse struct {
	TotalIncome  string            `json:"total_income"`
	TotalExpense string            `json:"total_expense"`
	Net          string            `json:"net"`
	ByCategory   []CategorySummary `json:"by_category"`
	ByMonth      []MonthlySummary  `json:"by_month"`
}
