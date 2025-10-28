package model

import (
	"time"

	"gorm.io/gorm"
)

type Category struct {
	ID        uint           `gorm:"primaryKey" json:"id"`
	UserID    uint           `json:"user_id" gorm:"index;not null"`
	Name      string         `json:"name" gorm:"type:varchar(50);uniqueIndex:user_category_unique"`
	Color     string         `json:"color" gorm:"type:char(7)"`
	Icon      string         `json:"icon" gorm:"type:varchar(50)"`
	Type      string         `json:"type" gorm:"type:varchar(10);not null"`
	CreatedAt time.Time      `json:"created_at"`
	UpdatedAt time.Time      `json:"updated_at"`
	DeletedAt gorm.DeletedAt `gorm:"index" json:"-"`
}
