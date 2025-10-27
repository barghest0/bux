package db

import (
	"fmt"
	"transaction/pkg/config"

	"gorm.io/driver/postgres"
	"gorm.io/gorm"
)

type Storage struct {
	db *gorm.DB
}

const tag = "storage.posgres.New"

func New(db_config config.Postgres) (*gorm.DB, error) {

	dsn := fmt.Sprintf(
		"host=%s user=%s password=%s dbname=%s port=%d",
		db_config.Host,
		db_config.User,
		db_config.Password,
		db_config.DB,
		db_config.Port,
	)

	db, err := gorm.Open(postgres.Open(dsn), &gorm.Config{})

	if err != nil {
		return nil, fmt.Errorf("%s: %w", tag, err)
	}

	return db, nil
}
