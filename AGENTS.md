# Repository Guidelines

## Project Structure & Module Organization
- `app/android/` contains the Android client (Kotlin, Jetpack Compose). Source lives under `app/android/app/src/main/java/com/barghest/bux/`, with UI in `ui/`, data in `data/`, domain in `domain/`, and DI in `di/`.
- `server/` contains Go microservices: `services/user`, `services/transaction`, `services/investment`. Each service follows clean architecture: `cmd/`, `internal/` (data/domain/infra/presentation), `pkg/`, and `config/`.
- `docs/plans/` holds architecture and phase planning notes.
- Database init scripts live in `server/init/` and Docker support files in `server/`.

## Build, Test, and Development Commands
- Backend (from `server/`): `docker-compose up -d` (start services), `docker-compose up -d --build` (rebuild), `docker-compose logs -f <service>` (tail logs).
- Go services (from each service directory): `go run ./cmd/main.go`, `go build -o service ./cmd/main.go`, `go test ./...`.
- Android (from `app/android/`): `./gradlew build`, `./gradlew assembleDebug`, `./gradlew test` (unit), `./gradlew connectedAndroidTest` (instrumented).

## Coding Style & Naming Conventions
- Go: follow standard `gofmt` formatting and `go test` conventions; packages are lowercase, files are snake_case.
- Kotlin: follow official Kotlin style; Compose screens are in `ui/screens/` and ViewModels are `*ViewModel.kt`.
- Keep config in `config/local.yaml` or `.env` files; do not hardcode secrets.

## Testing Guidelines
- Go tests live alongside code as `*_test.go` and run with `go test ./...`.
- Android unit tests live in `app/android/app/src/test/` and instrumented tests in `app/android/app/src/androidTest/`.
- Prefer naming tests after behavior (e.g., `TestCreateAccount` in Go, `AddAccountViewModelTest` in Android).

## Commit & Pull Request Guidelines
- Commit history is informal; use short, imperative summaries. If working on phases, include the phase in the subject line (e.g., “phase 2: add transaction endpoints”).
- PRs should describe the change, list affected services/modules, and include screenshots for UI updates.

## Security & Configuration Tips
- Use `server/.env.example` as a template; store local secrets in `.env` only.
- Default PostgreSQL credentials are `barghest/barghest`; change for production environments.

## Agent-Specific Instructions
- See `CLAUDE.md` for architecture notes and command references.
