package config

import (
	"log"
	"os"
	"time"

	"github.com/ilyakaznacheev/cleanenv"
)

type Config struct {
	Env        string `yaml:"env" env-default:"local"`
	HTTPServer `yaml:"http_server"`
	Postgres   `yaml:"postgres"`
	RMQ        `yaml:"rmq"`
}

type HTTPServer struct {
	Host        string        `yaml:"host"`
	Port        int           `yaml:"port"`
	Timeout     time.Duration `yaml:"timeout"`
	IdleTimeout time.Duration `yaml:"idle_timeout"`
}

type Postgres struct {
	Host     string `yaml:"host"`
	Port     int    `yaml:"port"`
	User     string `yaml:"user"`
	Password string `yaml:"password"`
	DB       string `yaml:"db"`
}

type RMQ struct {
	URL string `yaml:"url"`
}

type DBConfig struct {
	User     string
	Password string
	Name     string
	Host     string
	Port     string
}

func getEnv(env string) string {
	value, exists := os.LookupEnv(env)
	if !exists {
		log.Fatalf("env does not exist %s", env)
	}
	return value
}

func MustLoad() *Config {
	configPath := getEnv("CONFIG_PATH")

	var cfg Config

	if _, err := os.Stat(configPath); os.IsNotExist(err) {
		log.Fatalf(`config file does not exist %s`, configPath)
	}

	if err := cleanenv.ReadConfig(configPath, &cfg); err != nil {
		log.Fatalf("cannot read config file %s", err)
	}

	return &cfg
}
