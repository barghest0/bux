package main

import (
	"fmt"
	"log/slog"
	"os"
	"transaction/internal/data/repository"
	"transaction/internal/domain/service"
	"transaction/internal/infra/db"
	"transaction/internal/presentation/http"
	"transaction/pkg/config"
	"transaction/pkg/logger"
	"transaction/pkg/logger/sl"

	"github.com/gin-gonic/gin"
)

func main() {
	cfg := config.MustLoad()
	log := logger.SetupLogger(cfg.Env)

	log.Info("Server started with", slog.String("env", cfg.Env))

	postgres, err := db.New(cfg.Postgres)

	if err != nil {
		log.Error("Failed to init storage", sl.Err(err))
		os.Exit(1)
	}

	if err := db.Migrate(postgres); err != nil {
		log.Error("Error in migration", sl.Err(err))
	}

	// Account
	accountRepo := repository.NewAccountRepository(postgres)
	accountService := service.NewAccountService(accountRepo)

	// Category
	categoryRepo := repository.NewCategoryRepository(postgres)
	categoryService := service.NewCategoryService(categoryRepo)

	// Transaction (with account repo for balance updates)
	txRepo := repository.New(postgres)
	txService := service.NewWithAccountRepo(txRepo, accountRepo)

	// Analytics
	analyticsService := service.NewAnalyticsService(txRepo)

	// Budget
	budgetRepo := repository.NewBudgetRepository(postgres)
	budgetService := service.NewBudgetService(budgetRepo)

	r := gin.Default()
	http.New(r, txService)
	http.NewAccountHTTP(r, accountService)
	http.NewCategoryHTTP(r, categoryService)
	http.NewAnalyticsHTTP(r, analyticsService)
	http.NewBudgetHTTP(r, budgetService)
	http.NewExportHTTP(r, txService)

	if err := r.Run(fmt.Sprintf(":%d", cfg.HTTPServer.Port)); err != nil {
		log.Error("Unable to start the server: ", sl.Err(err))
	}
}
