package dto

import (
	"strconv"

	"github.com/gin-gonic/gin"
)

const (
	DefaultPage     = 1
	DefaultPageSize = 50
	MaxPageSize     = 200
)

type PaginationParams struct {
	Page     int
	PageSize int
}

func (p PaginationParams) Offset() int {
	return (p.Page - 1) * p.PageSize
}

func (p PaginationParams) Limit() int {
	return p.PageSize
}

func ParsePagination(ctx *gin.Context) PaginationParams {
	page := DefaultPage
	pageSize := DefaultPageSize

	if v := ctx.Query("page"); v != "" {
		if p, err := strconv.Atoi(v); err == nil && p > 0 {
			page = p
		}
	}
	if v := ctx.Query("page_size"); v != "" {
		if ps, err := strconv.Atoi(v); err == nil && ps > 0 {
			pageSize = ps
			if pageSize > MaxPageSize {
				pageSize = MaxPageSize
			}
		}
	}

	return PaginationParams{Page: page, PageSize: pageSize}
}

type PaginatedResponse[T any] struct {
	Data       []T `json:"data"`
	Page       int `json:"page"`
	PageSize   int `json:"page_size"`
	TotalCount int `json:"total_count"`
	TotalPages int `json:"total_pages"`
}

func NewPaginatedResponse[T any](data []T, page, pageSize, totalCount int) PaginatedResponse[T] {
	totalPages := totalCount / pageSize
	if totalCount%pageSize != 0 {
		totalPages++
	}
	return PaginatedResponse[T]{
		Data:       data,
		Page:       page,
		PageSize:   pageSize,
		TotalCount: totalCount,
		TotalPages: totalPages,
	}
}
