package dto

import "transaction/internal/domain/model"

type CategoryResponse struct {
	ID        uint   `json:"id"`
	Name      string `json:"name"`
	Type      string `json:"type"`
	Icon      string `json:"icon"`
	Color     string `json:"color"`
	ParentID  *uint  `json:"parent_id,omitempty"`
	SortOrder int    `json:"sort_order"`
	IsSystem  bool   `json:"is_system"`
}

type CreateCategoryRequest struct {
	Name      string `json:"name" binding:"required"`
	Type      string `json:"type" binding:"required"`
	Icon      string `json:"icon"`
	Color     string `json:"color"`
	ParentID  *uint  `json:"parent_id"`
	SortOrder int    `json:"sort_order"`
}

type UpdateCategoryRequest struct {
	Name      string `json:"name"`
	Icon      string `json:"icon"`
	Color     string `json:"color"`
	SortOrder *int   `json:"sort_order"`
}

func CategoryFromModel(c model.Category) CategoryResponse {
	return CategoryResponse{
		ID:        c.ID,
		Name:      c.Name,
		Type:      string(c.Type),
		Icon:      c.Icon,
		Color:     c.Color,
		ParentID:  c.ParentID,
		SortOrder: c.SortOrder,
		IsSystem:  c.IsSystem,
	}
}

func CategoryListFromModel(categories []model.Category) []CategoryResponse {
	res := make([]CategoryResponse, len(categories))
	for i, c := range categories {
		res[i] = CategoryFromModel(c)
	}
	return res
}
