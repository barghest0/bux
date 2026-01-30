package db

import (
	"transaction/internal/domain/model"

	"gorm.io/gorm"
)

func Migrate(db *gorm.DB) error {
	err := db.AutoMigrate(
		&model.Account{},
		&model.Category{},
		&model.Transaction{},
		&model.Budget{},
	)

	if err != nil {
		return err
	}

	return nil
}
