package db

import (
	"transaction/internal/domain/model"

	"gorm.io/gorm"
)

func Migrate(db *gorm.DB) error {
	err := db.AutoMigrate(&model.Category{}, &model.Transaction{})

	if err != nil {
		return err
	}

	return nil
}
