# BUX: Анализ текущего состояния

> Дата: 2025-01-24  
> Статус: Завершен

---

## Обзор

Проект BUX — финансовый хаб для управления активами. Текущее состояние — ранний MVP.

### Структура проекта

```
bux/
├── app/
│   └── android/          # Kotlin + Compose (802 LOC)
├── server/
│   ├── user/             # Go микросервис :8081
│   ├── transaction/      # Go микросервис :8082
│   ├── investment/       # Go микросервис :8083
│   ├── init/             # SQL init scripts
│   └── docker-compose.yaml
└── docs/
    └── plans/            # Этот документ
```

---

## Backend анализ

### User Service (:8081)

**Доменная модель:**
```go
type User struct {
    ID       int    `gorm:"primaryKey"`
    Username string `gorm:"not null"`
    Password string `gorm:"not null"`  // bcrypt hash
}
```

**API:**
| Endpoint | Method | Auth | Описание |
|----------|--------|------|----------|
| /auth/register | POST | No | Регистрация |
| /auth/login | POST | No | Вход, возвращает JWT |
| /users/ | GET | Yes | Список всех пользователей |
| /users/me | GET | Yes | Текущий пользователь |

**Проблемы:**
- JWT secret hardcoded: `var JwtKey = []byte("key")`
- Нет email, phone, KYC статуса
- Нет created_at/updated_at
- GET /users/ возвращает 201 вместо 200

---

### Transaction Service (:8082)

**Доменные модели:**
```go
type Category struct {
    ID        uint
    UserID    uint
    Name      string
    Color     string  // HEX
    Icon      string
    Type      string  // expense/income
    CreatedAt, UpdatedAt time.Time
    DeletedAt gorm.DeletedAt
}

type Transaction struct {
    ID          uint
    UserID      uint
    Amount      float64  // ПРОБЛЕМА: должен быть Decimal
    Currency    string   // char(3)
    CategoryID  *uint
    Tag         string
    Description string
    CreatedAt, UpdatedAt time.Time
    DeletedAt   gorm.DeletedAt
}
```

**API:**
| Endpoint | Method | Auth | Описание |
|----------|--------|------|----------|
| /transactions | GET | Yes | Список транзакций |
| /transactions | POST | Yes | Создать транзакцию |
| /transactions/:id | GET | Yes | Детали транзакции |

**Проблемы:**
- `Amount float64` — недопустимо для финансов
- Нет TransactionType (income/expense/transfer)
- Нет TransactionStatus (pending/completed)
- Нет связи с Account (баланс)
- GET возвращает 201 вместо 200

---

### Investment Service (:8083)

**Доменные модели:**
```go
type Broker struct {
    ID        uint
    UserID    string  // ПРОБЛЕМА: string вместо uint
    Name      string
    CreatedAt time.Time
    DeletedAt gorm.DeletedAt
}

type Portfolio struct {
    ID           uint
    UserID       string
    BrokerID     uint
    Name         string
    BaseCurrency string  // default: USD
    CreatedAt    time.Time
    DeletedAt    gorm.DeletedAt
}

type Security struct {
    ID       uint
    Symbol   string  // AAPL, TSLA
    Name     string
    Type     string  // ENUM: stock, bond, etf, currency
    Currency string
    CreatedAt time.Time
    DeletedAt gorm.DeletedAt
}

type Trade struct {
    ID          uint
    PortfolioID uint
    SecurityID  uint
    TradeDate   time.Time
    Side        string   // ENUM: buy, sell
    Quantity    float64  // ПРОБЛЕМА
    Price       float64  // ПРОБЛЕМА
    Fee         float64
    CreatedAt   time.Time
    DeletedAt   gorm.DeletedAt
}
```

**API:**
| Endpoint | Method | Auth | Описание |
|----------|--------|------|----------|
| /brokers | POST | Yes | Создать брокера |
| /portfolios | POST | Yes | Создать портфель |
| /trades | POST | Yes | Создать сделку |
| /portfolios/:id/holdings | GET | Yes | Позиции портфеля |

**Проблемы:**
- UserID как string (несовместимо с User service)
- Quantity, Price как float64
- Нет Holding модели (агрегация позиций)
- Нет истории цен
- Нет расчета P&L

---

### Инфраструктура

**Docker Compose:**
```yaml
services:
  user-service:8081
  transaction-service:8082
  investment-service:8083
  postgres:5432
  pgadmin:80

volumes:
  pgdata
  pgadmin-data
```

**БД:** PostgreSQL 16 с отдельной базой на сервис (users, transactions, investments)

**Общее:**
- Clean Architecture во всех сервисах
- Gin HTTP framework
- GORM ORM
- slog logging
- YAML config

---

## Android анализ

### Архитектура

```
app/src/main/java/com/barghest/bux/
├── ui/
│   ├── application/        # App, MainActivity, Navigation
│   └── screens/
│       ├── auth/           # LoginScreen, LoginViewModel
│       ├── main/           # MainScreen, MainViewModel
│       └── transaction/add/# AddTransactionScreen, AddTransactionViewModel
├── domain/
│   ├── model/              # User, Transaction
│   └── service/            # AuthService, TransactionService
├── data/
│   ├── network/            # Api (Ktor)
│   ├── repository/         # AuthRepository, TransactionRepository
│   ├── mapper/             # DTO mappers
│   └── dto/                # Request/Response DTOs
└── di/
    └── appModule.kt        # Koin DI
```

### Доменные модели

```kotlin
data class User(
    val id: Int,
    val username: String
)

data class Transaction(
    val id: Int,
    val amount: Double
)

data class NewTransaction(
    val amount: Double,
    val currency: String
)

enum class TransactionType { INCOME, EXPENSE }  // Не используется!
```

**Проблемы:**
- Минимальные модели без деталей
- TransactionType определен, но не используется
- Нет type, date, category в Transaction

### Network Layer

```kotlin
class Api {
    private val client = HttpClient(CIO) {
        install(DefaultRequest) {
            // ПРОБЛЕМА: Hardcoded token!
            header("Authorization", "Bearer eyJhbGciOi...")
        }
    }
    
    suspend fun fetchTransactions(): Result<List<TransactionResponse>>
    suspend fun postTransaction(request: TransactionRequest): Result<Unit>
    suspend fun login(request: LoginRequest): Result<LoginResponse>
}
```

**Проблемы:**
- JWT token hardcoded в коде
- Cleartext traffic разрешен
- Нет token refresh
- Нет retry logic

### Repository Layer

```kotlin
class TransactionRepository(private val api: Api) {
    suspend fun getAll(): List<Transaction> {
        val result = api.fetchTransactions()
        return result.fold(
            onSuccess = { list -> list.map { it.toDomain() } },
            onFailure = { emptyList() }  // ПРОБЛЕМА: silent failure
        )
    }
}
```

**Проблемы:**
- getAll() возвращает List, не Result → ошибки скрыты
- Нет кэширования
- Нет pagination

### UI Layer

**Screens:**
- LoginScreen — форма входа
- MainScreen — список транзакций
- AddTransactionScreen — добавление транзакции

**State Management:**
- mutableStateOf в ViewModels
- StateFlow для списков

**Проблемы:**
- Нет loading/error states в MainScreen
- Нет навигации после успешного login (TODO в коде)
- Локальное состояние в AddTransactionScreen вместо ViewModel
- Нет валидации форм

### Отсутствует

- Room Database (нет offline support)
- EncryptedSharedPreferences (нет secure storage)
- WorkManager (нет background sync)
- Unit/UI тесты

---

## Сводная таблица проблем

### Критические (Security/Data Integrity)

| # | Проблема | Место | Влияние |
|---|----------|-------|---------|
| 1 | Hardcoded JWT secret | Backend: все сервисы | Компрометация всех токенов |
| 2 | Hardcoded JWT token | Android: Api.kt | Любой может использовать токен |
| 3 | float64 для денег | Backend + Android | Потеря точности в расчетах |
| 4 | Cleartext traffic | Android: network_security | Перехват данных |

### Высокие (Functionality)

| # | Проблема | Место | Влияние |
|---|----------|-------|---------|
| 5 | Нет Account модели | Backend | Невозможно отслеживать баланс |
| 6 | Несовместимые UserID типы | Backend | Service-to-service проблемы |
| 7 | Silent failures | Android: Repository | Пользователь не знает об ошибках |
| 8 | Нет Room DB | Android | Нет offline режима |
| 9 | Нет навигации после login | Android | UX проблема |

### Средние (Code Quality)

| # | Проблема | Место | Влияние |
|---|----------|-------|---------|
| 10 | Неверные HTTP status codes | Backend | API consistency |
| 11 | Нет pagination | Backend + Android | Performance при масштабе |
| 12 | Анемичные domain models | Везде | Бизнес-логика размазана |
| 13 | Services как прокси | Backend + Android | Лишний слой без логики |
| 14 | Нет валидации | Везде | Невалидные данные в БД |

---

## Что можно переиспользовать

### Backend
- Clean Architecture структура (отличная основа)
- Gin HTTP handlers pattern
- GORM repository pattern
- Config/Logger utilities
- Docker + Docker Compose setup
- JWT auth flow (после исправления secret)

### Android
- MVVM + Clean Architecture структура
- Koin DI setup
- Ktor client configuration (после исправления token)
- Material 3 theming
- Navigation graph
- DTO ↔ Domain mappers

---

## Рекомендация

Код имеет хорошую архитектурную основу. Не требуется переписывание — нужна **эволюционная доработка**:

1. **Phase 0:** Исправить критические проблемы безопасности
2. **Phase 1:** Построить правильную доменную модель (Account, rich Transaction)
3. **Phase 2:** Добавить инвестиционную аналитику (Holdings, P&L)
4. **Phase 3:** Net Worth и отчеты

Детальный план см. в [architecture-evolution-plan.md](./architecture-evolution-plan.md).
