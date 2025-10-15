package main

import (
	"fmt"
	"identity/internal/data/repository"
	"identity/internal/domain/service"
	"identity/internal/infra/db"
	"identity/internal/presentation/http"
	"identity/pkg/config"
	"identity/pkg/logger"
	"identity/pkg/logger/sl"
	"log/slog"
	"os"

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

	repo := repository.New(postgres)
	service := service.New(repo)

	r := gin.Default()
	http.New(r, service)

	if err := r.Run(fmt.Sprintf(":%d", cfg.HTTPServer.Port)); err != nil {
		log.Error("Unable to start the server: ", sl.Err(err))
	}

}
