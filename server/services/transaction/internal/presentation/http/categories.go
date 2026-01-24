package http

import (
	"net/http"
	"transaction/internal/domain/model"
	"transaction/internal/domain/service"
	"transaction/internal/presentation/http/dto"
	"transaction/internal/presentation/http/middleware"

	"github.com/gin-gonic/gin"
)

type CategoryHTTP struct {
	service *service.CategoryService
}

func NewCategoryHTTP(r *gin.Engine, s *service.CategoryService) {
	h := &CategoryHTTP{
		service: s,
	}

	categories := r.Group("/categories")
	categories.Use(middleware.AuthMiddleware())
	{
		categories.GET("", h.GetCategories)
		categories.POST("", h.CreateCategory)
		categories.GET("/:id", h.GetCategory)
		categories.PUT("/:id", h.UpdateCategory)
		categories.DELETE("/:id", h.DeleteCategory)
		categories.POST("/defaults", h.CreateDefaults)
	}
}

func (h *CategoryHTTP) GetCategories(ctx *gin.Context) {
	userID, ok := ctx.Get("userID")
	if !ok {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	// Optional type filter
	typeFilter := ctx.Query("type")

	var categories []model.Category
	var err error

	if typeFilter != "" {
		ctype := model.CategoryType(typeFilter)
		if !model.IsValidCategoryType(ctype) {
			ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid category type"})
			return
		}
		categories, err = h.service.GetCategoriesByType(userID.(uint), ctype)
	} else {
		categories, err = h.service.GetUserCategories(userID.(uint))
	}

	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusOK, dto.CategoryListFromModel(categories))
}

func (h *CategoryHTTP) GetCategory(ctx *gin.Context) {
	var uri struct {
		ID uint `uri:"id" binding:"required"`
	}
	if err := ctx.ShouldBindUri(&uri); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
		return
	}

	userID, ok := ctx.Get("userID")
	if !ok {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	category, err := h.service.GetCategory(uri.ID, userID.(uint))
	if err != nil {
		status := http.StatusInternalServerError
		if err == service.ErrCategoryNotFound {
			status = http.StatusNotFound
		} else if err == service.ErrCategoryAccessDenied {
			status = http.StatusForbidden
		}
		ctx.JSON(status, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusOK, dto.CategoryFromModel(*category))
}

func (h *CategoryHTTP) CreateCategory(ctx *gin.Context) {
	var req dto.CreateCategoryRequest
	if err := ctx.ShouldBindJSON(&req); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	userID, ok := ctx.Get("userID")
	if !ok {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	category := &model.Category{
		UserID:    userID.(uint),
		Name:      req.Name,
		Type:      model.CategoryType(req.Type),
		Icon:      req.Icon,
		Color:     req.Color,
		ParentID:  req.ParentID,
		SortOrder: req.SortOrder,
	}

	created, err := h.service.CreateCategory(category)
	if err != nil {
		status := http.StatusInternalServerError
		if err == service.ErrInvalidCategoryName || err == service.ErrInvalidCategoryType {
			status = http.StatusBadRequest
		}
		ctx.JSON(status, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusCreated, dto.CategoryFromModel(*created))
}

func (h *CategoryHTTP) UpdateCategory(ctx *gin.Context) {
	var uri struct {
		ID uint `uri:"id" binding:"required"`
	}
	if err := ctx.ShouldBindUri(&uri); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
		return
	}

	var req dto.UpdateCategoryRequest
	if err := ctx.ShouldBindJSON(&req); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	userID, ok := ctx.Get("userID")
	if !ok {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	updates := &model.Category{
		Name:  req.Name,
		Icon:  req.Icon,
		Color: req.Color,
	}
	if req.SortOrder != nil {
		updates.SortOrder = *req.SortOrder
	}

	category, err := h.service.UpdateCategory(uri.ID, userID.(uint), updates)
	if err != nil {
		status := http.StatusInternalServerError
		if err == service.ErrCategoryNotFound {
			status = http.StatusNotFound
		} else if err == service.ErrCategoryAccessDenied {
			status = http.StatusForbidden
		}
		ctx.JSON(status, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusOK, dto.CategoryFromModel(*category))
}

func (h *CategoryHTTP) DeleteCategory(ctx *gin.Context) {
	var uri struct {
		ID uint `uri:"id" binding:"required"`
	}
	if err := ctx.ShouldBindUri(&uri); err != nil {
		ctx.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
		return
	}

	userID, ok := ctx.Get("userID")
	if !ok {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	if err := h.service.DeleteCategory(uri.ID, userID.(uint)); err != nil {
		status := http.StatusInternalServerError
		if err == service.ErrCategoryNotFound {
			status = http.StatusNotFound
		} else if err == service.ErrCategoryAccessDenied {
			status = http.StatusForbidden
		} else if err == service.ErrCannotDeleteSystem {
			status = http.StatusBadRequest
		}
		ctx.JSON(status, gin.H{"error": err.Error()})
		return
	}

	ctx.Status(http.StatusNoContent)
}

func (h *CategoryHTTP) CreateDefaults(ctx *gin.Context) {
	userID, ok := ctx.Get("userID")
	if !ok {
		ctx.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	if err := h.service.CreateDefaultCategories(userID.(uint)); err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	// Return all categories after creation
	categories, err := h.service.GetUserCategories(userID.(uint))
	if err != nil {
		ctx.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	ctx.JSON(http.StatusCreated, dto.CategoryListFromModel(categories))
}
