package db

import (
	"identity/internal/domain/model"

	"gorm.io/gorm"
)

func Migrate(db *gorm.DB) error {
	err := db.AutoMigrate(&model.User{})

	if err != nil {
		return err
	}

	return nil
}
