package db

import (
	"investment/internal/domain/model"

	"gorm.io/gorm"
)

func Migrate(db *gorm.DB) error {
	db.Exec(`DO $$
	BEGIN
		IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'security_type') THEN
			CREATE TYPE security_type AS ENUM ('stock','bond','etf','currency');
		END IF;
		IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'trade_side') THEN
			CREATE TYPE trade_side AS ENUM ('buy','sell');
		END IF;
	END$$;`)

	err := db.AutoMigrate(&model.Broker{}, &model.Portfolio{}, &model.Security{}, &model.Trade{})

	if err != nil {
		return err
	}

	return nil
}
