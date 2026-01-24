# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

### Backend (Docker)
```bash
# Start all services (from server/)
docker-compose up -d

# Rebuild and start
docker-compose up -d --build

# View logs
docker-compose logs -f [service-name]
```

### Android (from app/android/)
```bash
./gradlew build              # Full build
./gradlew assembleDebug      # Debug APK
./gradlew test               # Unit tests
./gradlew connectedAndroidTest  # Instrumented tests
```

### Go Services (individual service development)
```bash
go build -o service ./cmd/main.go
go run ./cmd/main.go
go test ./...
```

## Architecture

**Polyglot microservices architecture:**
- Backend: Go microservices with Gin framework
- Frontend: Android Kotlin with Jetpack Compose
- Database: PostgreSQL (separate DB per service)
- Message Queue: RabbitMQ (configured, not yet integrated)

### Backend Services

| Service | Port | Database |
|---------|------|----------|
| User | 8081 | users |
| Transaction | 8082 | transactions |
| Investment | 8083 | investments |

Each Go service follows clean architecture:
```
service/
├── cmd/main.go              # Entry point
├── config/local.yaml        # Configuration
├── internal/
│   ├── data/repository/     # GORM data layer
│   ├── domain/model/        # Domain models
│   ├── domain/service/      # Business logic
│   ├── infra/auth/          # JWT & password utilities
│   ├── infra/db/            # Database setup & migrations
│   └── presentation/http/   # Gin handlers & middleware
└── pkg/                     # Shared utilities
```

### Android App

Uses MVVM with clean architecture:
```
app/src/main/java/com/barghest/bux/
├── ui/
│   ├── application/         # App, MainActivity, navigation
│   └── screens/             # Compose screens + ViewModels
├── di/appModule.kt          # Koin dependency injection
├── domain/                  # Models and services
└── data/                    # Network, repositories, DTOs
```

**Key libraries:** Ktor Client, Koin DI, Navigation Compose, Material3

**API Base URLs (emulator):**
- User: `http://10.0.2.2:8081`
- Transaction: `http://10.0.2.2:8082`

## Database

PostgreSQL credentials: `barghest/barghest`

Init script at `server/init/01-create-dbs.sql` creates all databases.

## Dependency Management

- **Backend:** Go modules (`go.mod` per service)
- **Android:** Gradle Version Catalog (`app/android/gradle/libs.versions.toml`)
