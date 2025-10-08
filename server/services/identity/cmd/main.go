package main

import (
	"fmt"
	"identity/pkg/config"
	"identity/pkg/logger"
	"identity/pkg/logger/sl"
	"log/slog"

	"github.com/gin-gonic/gin"
)

func main() {
	cfg := config.MustLoad()
	log := logger.SetupLogger(cfg.Env)

	log.Info("Server started with", slog.String("env", cfg.Env))

	// storage, err := postgres.New(cfg.Postgres)
	//
	// if err != nil {
	// 	log.Error("Failed to init storage", sl.Err(err))
	// 	os.Exit(1)
	// }
	//
	// if err := db.Migrate(storage); err != nil {
	// 	log.Error("Error in migration", sl.Err(err))
	// }
	//
	// repo := repository.New(storage)
	// service := service.New(repo, auth.NewJWTService(auth.JwtKey))
	//

	r := gin.Default()
	if err := r.Run(fmt.Sprintf(":%d", cfg.HTTPServer.Port)); err != nil {
		log.Error("Unable to start the server: ", sl.Err(err))
	}

}
