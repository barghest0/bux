package model

import (
	"time"

	"gorm.io/gorm"
)

type CategoryType string

const (
	CategoryTypeIncome  CategoryType = "income"
	CategoryTypeExpense CategoryType = "expense"
)

type Category struct {
	ID        uint           `gorm:"primaryKey" json:"id"`
	UserID    uint           `gorm:"index;not null" json:"user_id"`
	Name      string         `gorm:"type:varchar(50);not null" json:"name"`
	Type      CategoryType   `gorm:"type:varchar(20);not null" json:"type"`
	Icon      string         `gorm:"type:varchar(50)" json:"icon"`
	Color     string         `gorm:"type:char(7)" json:"color"`
	ParentID  *uint          `gorm:"index" json:"parent_id,omitempty"`
	Parent    *Category      `gorm:"foreignKey:ParentID" json:"-"`
	SortOrder int            `gorm:"default:0" json:"sort_order"`
	IsSystem  bool           `gorm:"default:false" json:"is_system"`
	CreatedAt time.Time      `json:"created_at"`
	UpdatedAt time.Time      `json:"updated_at"`
	DeletedAt gorm.DeletedAt `gorm:"index" json:"-"`
}

func IsValidCategoryType(t CategoryType) bool {
	switch t {
	case CategoryTypeIncome, CategoryTypeExpense:
		return true
	}
	return false
}

var DefaultCategories = []Category{
	// Income
	{Name: "Зарплата", Type: CategoryTypeIncome, Icon: "work", Color: "#4CAF50", IsSystem: true, SortOrder: 1},
	{Name: "Фриланс", Type: CategoryTypeIncome, Icon: "laptop", Color: "#8BC34A", IsSystem: true, SortOrder: 2},
	{Name: "Дивиденды", Type: CategoryTypeIncome, Icon: "trending_up", Color: "#009688", IsSystem: true, SortOrder: 3},
	{Name: "Проценты", Type: CategoryTypeIncome, Icon: "account_balance", Color: "#00BCD4", IsSystem: true, SortOrder: 4},
	{Name: "Подарки", Type: CategoryTypeIncome, Icon: "card_giftcard", Color: "#E91E63", IsSystem: true, SortOrder: 5},
	{Name: "Другое", Type: CategoryTypeIncome, Icon: "more_horiz", Color: "#9E9E9E", IsSystem: true, SortOrder: 99},

	// Expense
	{Name: "Продукты", Type: CategoryTypeExpense, Icon: "shopping_cart", Color: "#FF9800", IsSystem: true, SortOrder: 1},
	{Name: "Транспорт", Type: CategoryTypeExpense, Icon: "directions_car", Color: "#2196F3", IsSystem: true, SortOrder: 2},
	{Name: "Жилье", Type: CategoryTypeExpense, Icon: "home", Color: "#795548", IsSystem: true, SortOrder: 3},
	{Name: "Здоровье", Type: CategoryTypeExpense, Icon: "local_hospital", Color: "#F44336", IsSystem: true, SortOrder: 4},
	{Name: "Развлечения", Type: CategoryTypeExpense, Icon: "movie", Color: "#9C27B0", IsSystem: true, SortOrder: 5},
	{Name: "Рестораны", Type: CategoryTypeExpense, Icon: "restaurant", Color: "#FF5722", IsSystem: true, SortOrder: 6},
	{Name: "Одежда", Type: CategoryTypeExpense, Icon: "checkroom", Color: "#673AB7", IsSystem: true, SortOrder: 7},
	{Name: "Подписки", Type: CategoryTypeExpense, Icon: "subscriptions", Color: "#3F51B5", IsSystem: true, SortOrder: 8},
	{Name: "Образование", Type: CategoryTypeExpense, Icon: "school", Color: "#03A9F4", IsSystem: true, SortOrder: 9},
	{Name: "Другое", Type: CategoryTypeExpense, Icon: "more_horiz", Color: "#9E9E9E", IsSystem: true, SortOrder: 99},
}
