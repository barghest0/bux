# BUX: План эволюционного развития архитектуры

> Версия: 1.0  
> Дата: 2025-01-24  
> Автор: Tech Lead Analysis

---

## Содержание

1. [Executive Summary](#1-executive-summary)
2. [Текущее состояние](#2-текущее-состояние)
3. [Целевая архитектура](#3-целевая-архитектура)
4. [Фазы развития](#4-фазы-развития)
5. [Детальный план по фазам](#5-детальный-план-по-фазам)
6. [Доменная модель](#6-доменная-модель)
7. [Технические решения](#7-технические-решения)
8. [Риски и митигации](#8-риски-и-митигации)

---

## 1. Executive Summary

### Цель проекта
Построить централизованный финансовый хаб для управления всеми активами пользователя:
- Мультивалютные счета, карты, наличка
- Криптовалюты
- Инвестиции (акции, фонды, металлы)
- Недвижимость
- Общий net worth и аналитика

### Текущее состояние
**Backend:** 3 микросервиса (User, Transaction, Investment) — MVP уровень, ~40% готовности  
**Android:** Базовая структура Clean Architecture — ~30% готовности  
**iOS:** Не начат

### Оценка текущего кода

| Компонент | Оценка | Комментарий |
|-----------|--------|-------------|
| Backend архитектура | 7/10 | Хорошее разделение слоев, Clean Architecture |
| Backend домен | 4/10 | Анемичные модели, float64 для денег |
| Backend безопасность | 3/10 | Hardcoded JWT secret, нет rate limiting |
| Android архитектура | 7/10 | Clean Architecture, MVVM |
| Android домен | 4/10 | Минимальные модели |
| Android безопасность | 2/10 | Hardcoded token, cleartext traffic |

### Стратегия
**Эволюционное развитие** — не переписываем с нуля, а улучшаем итеративно, сохраняя работающий код.

---

## 2. Текущее состояние

### 2.1 Backend (Go + Docker)

```
server/
├── user/           # Аутентификация, JWT
├── transaction/    # Транзакции, категории
├── investment/     # Брокеры, портфели, сделки
├── init/           # SQL инициализация БД
└── docker-compose.yaml
```

**Что работает:**
- JWT аутентификация (24h TTL)
- CRUD для User, Transaction, Category, Broker, Portfolio, Trade
- PostgreSQL с отдельной БД на сервис
- Docker Compose для локальной разработки
- Clean Architecture во всех сервисах

**Критические проблемы:**

| Проблема | Severity | Описание |
|----------|----------|----------|
| Hardcoded JWT secret | CRITICAL | `var JwtKey = []byte("key")` |
| float64 для денег | CRITICAL | Потеря точности в финансовых расчетах |
| Нет Account модели | HIGH | Невозможно отслеживать баланс |
| Несогласованные типы UserID | HIGH | int/uint/string в разных сервисах |
| Нет pagination | MEDIUM | GetAll без лимитов |
| Нет валидации | MEDIUM | Можно создать транзакцию с Amount < 0 |

### 2.2 Android (Kotlin + Compose)

```
app/android/
└── src/main/java/com/barghest/bux/
    ├── ui/           # Screens + ViewModels
    ├── domain/       # Models + Services
    ├── data/         # Network + Repositories
    └── di/           # Koin modules
```

**Что работает:**
- Login screen с JWT
- Список транзакций
- Добавление транзакции
- Koin DI
- Material 3 theming

**Критические проблемы:**

| Проблема | Severity | Описание |
|----------|----------|----------|
| Hardcoded JWT token | CRITICAL | Token в коде Api.kt |
| Cleartext traffic | HIGH | HTTP без шифрования |
| Нет Room DB | HIGH | Нет offline support |
| Silent failures | MEDIUM | getAll() возвращает [] при ошибке |
| Нет навигации после login | MEDIUM | TODO в коде |

---

## 3. Целевая архитектура

### 3.1 Bounded Contexts

```
┌─────────────────────────────────────────────────────────────────┐
│                         BUX Platform                             │
├─────────────────┬─────────────────┬─────────────────────────────┤
│   Identity &    │    Finance      │      Analytics &            │
│    Access       │     Core        │      Reporting              │
├─────────────────┼─────────────────┼─────────────────────────────┤
│ • User          │ • Account       │ • Net Worth                 │
│ • Auth          │ • Transaction   │ • Performance               │
│ • Family        │ • Asset         │ • Reports                   │
│ • Permissions   │ • Investment    │ • Export                    │
└─────────────────┴─────────────────┴─────────────────────────────┘
```

### 3.2 Backend Target Architecture

```
                    ┌──────────────┐
                    │   Clients    │
                    │ Android/iOS  │
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
                    │ API Gateway  │  ← Rate limiting, Auth
                    │   (Future)   │
                    └──────┬───────┘
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
    ┌────▼────┐      ┌─────▼─────┐    ┌─────▼─────┐
    │  User   │      │  Finance  │    │Analytics  │
    │ Service │      │  Service  │    │ Service   │
    │  :8081  │      │   :8082   │    │  :8084    │
    └────┬────┘      └─────┬─────┘    └─────┬─────┘
         │                 │                 │
         │           ┌─────▼─────┐           │
         │           │Investment │           │
         │           │ Service   │           │
         │           │  :8083    │           │
         │           └─────┬─────┘           │
         │                 │                 │
    ┌────▼────┐      ┌─────▼─────┐    ┌─────▼─────┐
    │ users   │      │ finance   │    │analytics  │
    │   DB    │      │    DB     │    │    DB     │
    └─────────┘      └───────────┘    └───────────┘
```

### 3.3 Доменные сущности (Target)

```
Account (Asset)
├── id: UUID
├── userId: UUID
├── type: AccountType (bank_account, card, cash, crypto, investment, property)
├── name: string
├── currency: Currency
├── balance: Decimal
├── metadata: JSON
├── createdAt, updatedAt
└── history: []BalanceSnapshot

Transaction
├── id: UUID
├── userId: UUID
├── type: TransactionType (income, expense, transfer, investment_buy, investment_sell, dividend, interest)
├── accountId: UUID (source)
├── destinationAccountId: UUID? (for transfers)
├── amount: Decimal
├── currency: Currency
├── category: Category
├── description: string
├── transactionDate: DateTime
├── status: TransactionStatus (pending, completed, failed)
└── metadata: JSON

Investment (extends Account)
├── broker: Broker
├── portfolio: Portfolio
├── holdings: []Holding
└── trades: []Trade

Holding
├── security: Security
├── quantity: Decimal
├── averageCost: Decimal
├── currentPrice: Decimal
└── unrealizedPnL: Decimal
```

### 3.4 Android Target Architecture

```
┌─────────────────────────────────────────────────┐
│                 Presentation                     │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐    │
│  │  Screens  │  │ ViewModels│  │  States   │    │
│  │ (Compose) │  │  (MVVM)   │  │  (UiState)│    │
│  └───────────┘  └───────────┘  └───────────┘    │
├─────────────────────────────────────────────────┤
│                   Domain                         │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐    │
│  │  Models   │  │ Use Cases │  │Repository │    │
│  │           │  │           │  │ Interfaces│    │
│  └───────────┘  └───────────┘  └───────────┘    │
├─────────────────────────────────────────────────┤
│                    Data                          │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐    │
│  │   Room    │  │   Ktor    │  │   Sync    │    │
│  │    DB     │  │  Client   │  │  Manager  │    │
│  └───────────┘  └───────────┘  └───────────┘    │
└─────────────────────────────────────────────────┘
```

---

## 4. Фазы развития

```
Phase 0          Phase 1           Phase 2           Phase 3
Security &       Core Domain       Investments       Analytics
Foundation       Model             & Assets          & Scale
   │                │                  │                │
   ▼                ▼                  ▼                ▼
┌────────┐     ┌────────────┐    ┌───────────┐    ┌──────────┐
│ Fix    │     │ Account    │    │ Investment│    │ Net Worth│
│Critical│ ──► │ Transaction│ ──►│ Holdings  │ ──►│ Reports  │
│Issues  │     │ Categories │    │ History   │    │ Export   │
└────────┘     └────────────┘    └───────────┘    └──────────┘
```

### Timeline Overview

| Фаза | Название | Фокус | Результат |
|------|----------|-------|-----------|
| 0 | Foundation | Безопасность, типы | Безопасный, стабильный фундамент |
| 1 | Core Domain | Account, Transaction | Полноценный учет финансов |
| 2 | Investments | Portfolio, Holdings | Инвестиционный трекинг |
| 3 | Analytics | Net Worth, Reports | Аналитика и отчеты |

---

## 5. Детальный план по фазам

## Phase 0: Security & Foundation

> Цель: Устранить критические проблемы безопасности и подготовить фундамент

### Backend Tasks

#### 0.1 Security Fixes (CRITICAL)

**JWT Secret из Environment:**
```go
// До
var JwtKey = []byte("key")

// После
var JwtKey = []byte(os.Getenv("JWT_SECRET"))
```

Файлы для изменения:
- `server/user/internal/infra/auth/jwt.go`
- `server/transaction/internal/infra/auth/jwt.go`
- `server/investment/internal/infra/auth/jwt.go`
- `server/docker-compose.yaml` (добавить env)

**Создать .env.example:**
```env
JWT_SECRET=your-256-bit-secret-key-here
POSTGRES_USER=barghest
POSTGRES_PASSWORD=barghest
```

#### 0.2 Decimal для денежных значений

Добавить библиотеку `github.com/shopspring/decimal`:

```go
// До
Amount float64

// После
Amount decimal.Decimal `gorm:"type:decimal(19,4)"`
```

Файлы:
- `server/transaction/internal/domain/model/transaction.go`
- `server/investment/internal/domain/model/model.go`

#### 0.3 Унификация UserID

Все сервисы должны использовать `uint`:

```go
// Везде
UserID uint `gorm:"index;not null"`
```

Файлы:
- `server/investment/internal/domain/model/model.go` (изменить string → uint)

#### 0.4 HTTP Status Codes

```go
// До
c.JSON(201, response)

// После
c.JSON(http.StatusOK, response)
```

Файлы:
- `server/user/internal/presentation/http/http.go`
- `server/transaction/internal/presentation/http/transactions.go`

#### 0.5 Базовая валидация

```go
// Transaction amount validation
if tx.Amount.LessThanOrEqual(decimal.Zero) {
    return nil, ErrInvalidAmount
}
```

### Android Tasks

#### 0.6 Убрать hardcoded token

**Создать TokenManager:**
```kotlin
class TokenManager(private val context: Context) {
    private val prefs = EncryptedSharedPreferences.create(...)
    
    fun saveToken(token: String)
    fun getToken(): String?
    fun clearToken()
}
```

#### 0.7 Динамический Authorization header

```kotlin
// Api.kt
install(DefaultRequest) {
    header("Authorization", "Bearer ${tokenManager.getToken()}")
}
```

#### 0.8 Отключить cleartext traffic

```xml
<!-- network_security_config.xml -->
<base-config cleartextTrafficPermitted="false">
    <trust-anchors>
        <certificates src="system" />
    </trust-anchors>
</base-config>

<!-- Только для debug -->
<debug-overrides>
    <trust-anchors>
        <certificates src="user" />
    </trust-anchors>
</debug-overrides>
```

#### 0.9 Error Handling

```kotlin
// Repository
suspend fun getAll(): Result<List<Transaction>>  // Не List<Transaction>
```

### Deliverables Phase 0
- [ ] JWT secret из environment
- [ ] Decimal для Amount/Price/Quantity
- [ ] Единый тип UserID (uint)
- [ ] Корректные HTTP status codes
- [ ] Базовая валидация
- [ ] Android: secure token storage
- [ ] Android: no cleartext в production

---

## Phase 1: Core Domain Model

> Цель: Построить правильную финансовую доменную модель

### 1.1 Модель Account (Asset)

**Новая сущность — центр финансовой модели:**

```go
// server/transaction/internal/domain/model/account.go

type AccountType string

const (
    AccountTypeBankAccount AccountType = "bank_account"
    AccountTypeCard        AccountType = "card"
    AccountTypeCash        AccountType = "cash"
    AccountTypeCrypto      AccountType = "crypto"
    AccountTypeInvestment  AccountType = "investment"
    AccountTypeProperty    AccountType = "property"
)

type Account struct {
    ID        uint            `gorm:"primaryKey"`
    UserID    uint            `gorm:"index;not null"`
    Type      AccountType     `gorm:"type:account_type;not null"`
    Name      string          `gorm:"not null"`
    Currency  string          `gorm:"type:char(3);not null"`
    Balance   decimal.Decimal `gorm:"type:decimal(19,4);default:0"`
    IsActive  bool            `gorm:"default:true"`
    Metadata  JSON            `gorm:"type:jsonb"`
    CreatedAt time.Time
    UpdatedAt time.Time
    DeletedAt gorm.DeletedAt
}
```

### 1.2 Расширенная модель Transaction

```go
type TransactionType string

const (
    TransactionTypeIncome         TransactionType = "income"
    TransactionTypeExpense        TransactionType = "expense"
    TransactionTypeTransfer       TransactionType = "transfer"
    TransactionTypeInvestmentBuy  TransactionType = "investment_buy"
    TransactionTypeInvestmentSell TransactionType = "investment_sell"
    TransactionTypeDividend       TransactionType = "dividend"
    TransactionTypeInterest       TransactionType = "interest"
)

type TransactionStatus string

const (
    TransactionStatusPending   TransactionStatus = "pending"
    TransactionStatusCompleted TransactionStatus = "completed"
    TransactionStatusFailed    TransactionStatus = "failed"
)

type Transaction struct {
    ID                   uint              `gorm:"primaryKey"`
    UserID               uint              `gorm:"index;not null"`
    Type                 TransactionType   `gorm:"type:transaction_type;not null"`
    AccountID            uint              `gorm:"index;not null"`          // Source account
    DestinationAccountID *uint             `gorm:"index"`                   // For transfers
    Amount               decimal.Decimal   `gorm:"type:decimal(19,4);not null"`
    Currency             string            `gorm:"type:char(3);not null"`
    CategoryID           *uint             `gorm:"index"`
    Category             *Category
    Description          string            `gorm:"type:text"`
    TransactionDate      time.Time         `gorm:"not null"`
    Status               TransactionStatus `gorm:"type:transaction_status;default:'completed'"`
    Metadata             JSON              `gorm:"type:jsonb"`
    CreatedAt            time.Time
    UpdatedAt            time.Time
    DeletedAt            gorm.DeletedAt
}
```

### 1.3 Бизнес-логика транзакций

```go
// server/transaction/internal/domain/service/transaction_service.go

func (s *TransactionService) CreateTransaction(tx *Transaction) (*Transaction, error) {
    // 1. Валидация
    if err := s.validateTransaction(tx); err != nil {
        return nil, err
    }
    
    // 2. Получить account
    account, err := s.accountRepo.GetByID(tx.AccountID)
    if err != nil {
        return nil, ErrAccountNotFound
    }
    
    // 3. Проверить владельца
    if account.UserID != tx.UserID {
        return nil, ErrAccountAccessDenied
    }
    
    // 4. Применить транзакцию к балансу
    switch tx.Type {
    case TransactionTypeIncome, TransactionTypeDividend, TransactionTypeInterest:
        account.Balance = account.Balance.Add(tx.Amount)
    case TransactionTypeExpense:
        if account.Balance.LessThan(tx.Amount) {
            return nil, ErrInsufficientFunds
        }
        account.Balance = account.Balance.Sub(tx.Amount)
    case TransactionTypeTransfer:
        return s.handleTransfer(tx, account)
    }
    
    // 5. Сохранить в транзакции БД
    return s.repo.CreateWithAccountUpdate(tx, account)
}
```

### 1.4 API для Account

```
POST   /accounts              # Создать счет
GET    /accounts              # Список счетов пользователя
GET    /accounts/:id          # Детали счета
PUT    /accounts/:id          # Обновить счет
DELETE /accounts/:id          # Деактивировать счет
GET    /accounts/:id/balance  # Текущий баланс
GET    /accounts/:id/history  # История баланса
```

### 1.5 Android: Room Database

```kotlin
// Database entities
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: Int,
    val userId: Int,
    val type: String,
    val name: String,
    val currency: String,
    val balance: String,  // Decimal as String
    val isActive: Boolean,
    val syncedAt: Long
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: Int,
    val userId: Int,
    val type: String,
    val accountId: Int,
    val destinationAccountId: Int?,
    val amount: String,
    val currency: String,
    val categoryId: Int?,
    val description: String?,
    val transactionDate: Long,
    val status: String,
    val syncedAt: Long
)

// Database
@Database(
    entities = [AccountEntity::class, TransactionEntity::class, CategoryEntity::class],
    version = 1
)
abstract class BuxDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
}
```

### 1.6 Android: Offline-First Repository

```kotlin
class TransactionRepositoryImpl(
    private val api: Api,
    private val dao: TransactionDao,
    private val syncManager: SyncManager
) : TransactionRepository {
    
    override fun getTransactions(): Flow<List<Transaction>> {
        return dao.getAllAsFlow()
            .map { entities -> entities.map { it.toDomain() } }
    }
    
    override suspend fun refresh(): Result<Unit> {
        return try {
            val remote = api.fetchTransactions().getOrThrow()
            dao.insertAll(remote.map { it.toEntity() })
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun create(tx: NewTransaction): Result<Transaction> {
        // 1. Сохранить локально как pending
        val entity = tx.toEntity(status = "pending")
        dao.insert(entity)
        
        // 2. Синхронизировать с сервером
        return syncManager.enqueue(SyncOperation.CreateTransaction(entity))
    }
}
```

### Deliverables Phase 1
- [ ] Account модель (backend)
- [ ] Расширенная Transaction модель
- [ ] Бизнес-логика с балансами
- [ ] Account API endpoints
- [ ] Room Database (Android)
- [ ] Offline-first репозитории
- [ ] Sync Manager базовый

---

## Phase 2: Investments & Assets

> Цель: Полноценный инвестиционный трекинг

### 2.1 Улучшенная модель Holdings

```go
// server/investment/internal/domain/model/holding.go

type Holding struct {
    ID           uint            `gorm:"primaryKey"`
    PortfolioID  uint            `gorm:"index;not null"`
    SecurityID   uint            `gorm:"index;not null"`
    Security     *Security
    Quantity     decimal.Decimal `gorm:"type:decimal(19,8)"`
    AverageCost  decimal.Decimal `gorm:"type:decimal(19,4)"`  // Средняя цена покупки
    TotalCost    decimal.Decimal `gorm:"type:decimal(19,4)"`  // Общая стоимость
    UpdatedAt    time.Time
}
```

### 2.2 Tax Lots для правильного учета

```go
type TaxLot struct {
    ID          uint            `gorm:"primaryKey"`
    HoldingID   uint            `gorm:"index;not null"`
    TradeID     uint            `gorm:"index;not null"`
    Quantity    decimal.Decimal `gorm:"type:decimal(19,8)"`
    CostBasis   decimal.Decimal `gorm:"type:decimal(19,4)"`
    AcquiredAt  time.Time
    SoldAt      *time.Time
}
```

### 2.3 Price History

```go
type PriceHistory struct {
    ID         uint            `gorm:"primaryKey"`
    SecurityID uint            `gorm:"index;not null"`
    Date       time.Time       `gorm:"index;not null"`
    Open       decimal.Decimal `gorm:"type:decimal(19,4)"`
    High       decimal.Decimal `gorm:"type:decimal(19,4)"`
    Low        decimal.Decimal `gorm:"type:decimal(19,4)"`
    Close      decimal.Decimal `gorm:"type:decimal(19,4)"`
    Volume     int64
}
```

### 2.4 Расчет P&L

```go
func (s *InvestmentService) CalculatePortfolioValue(portfolioID uint) (*PortfolioValue, error) {
    holdings, err := s.repo.GetHoldings(portfolioID)
    if err != nil {
        return nil, err
    }
    
    result := &PortfolioValue{
        Holdings: make([]HoldingValue, 0, len(holdings)),
    }
    
    for _, h := range holdings {
        currentPrice, err := s.priceService.GetCurrentPrice(h.SecurityID)
        if err != nil {
            continue
        }
        
        marketValue := h.Quantity.Mul(currentPrice)
        unrealizedPnL := marketValue.Sub(h.TotalCost)
        unrealizedPnLPercent := unrealizedPnL.Div(h.TotalCost).Mul(decimal.NewFromInt(100))
        
        result.Holdings = append(result.Holdings, HoldingValue{
            Security:             h.Security,
            Quantity:             h.Quantity,
            AverageCost:          h.AverageCost,
            CurrentPrice:         currentPrice,
            MarketValue:          marketValue,
            UnrealizedPnL:        unrealizedPnL,
            UnrealizedPnLPercent: unrealizedPnLPercent,
        })
        
        result.TotalMarketValue = result.TotalMarketValue.Add(marketValue)
        result.TotalCost = result.TotalCost.Add(h.TotalCost)
    }
    
    result.TotalUnrealizedPnL = result.TotalMarketValue.Sub(result.TotalCost)
    return result, nil
}
```

### 2.5 API Investments

```
GET  /portfolios/:id/value          # Текущая стоимость портфеля
GET  /portfolios/:id/holdings       # Список позиций с P&L
GET  /portfolios/:id/performance    # Доходность за период
GET  /securities/:id/price          # Текущая цена
GET  /securities/:id/history        # История цен
```

### 2.6 External Price API Integration

```go
// server/investment/internal/infra/external/price_provider.go

type PriceProvider interface {
    GetCurrentPrice(symbol string) (decimal.Decimal, error)
    GetHistoricalPrices(symbol string, from, to time.Time) ([]PricePoint, error)
}

type AlphaVantagePriceProvider struct {
    apiKey string
    client *http.Client
}

func (p *AlphaVantagePriceProvider) GetCurrentPrice(symbol string) (decimal.Decimal, error) {
    // Implementation
}
```

### Deliverables Phase 2
- [ ] Holding модель с расчетом стоимости
- [ ] Tax Lots для учета себестоимости
- [ ] Price History
- [ ] P&L расчеты
- [ ] External price API integration
- [ ] Android: экран портфеля
- [ ] Android: экран позиций с P&L

---

## Phase 3: Analytics & Scale

> Цель: Net Worth, аналитика, готовность к масштабированию

### 3.1 Net Worth Aggregation

```go
// server/analytics/internal/domain/service/networth_service.go

type NetWorthService struct {
    accountRepo    AccountRepository
    investmentRepo InvestmentRepository
    currencyService CurrencyService
}

func (s *NetWorthService) CalculateNetWorth(userID uint, baseCurrency string) (*NetWorth, error) {
    // 1. Получить все счета
    accounts, err := s.accountRepo.GetByUserID(userID)
    if err != nil {
        return nil, err
    }
    
    // 2. Получить все портфели
    portfolios, err := s.investmentRepo.GetPortfoliosByUserID(userID)
    if err != nil {
        return nil, err
    }
    
    result := &NetWorth{
        BaseCurrency: baseCurrency,
        AsAt:         time.Now(),
        Breakdown:    make([]AssetBreakdown, 0),
    }
    
    // 3. Агрегировать счета
    for _, acc := range accounts {
        converted := s.currencyService.Convert(acc.Balance, acc.Currency, baseCurrency)
        result.TotalAssets = result.TotalAssets.Add(converted)
        result.Breakdown = append(result.Breakdown, AssetBreakdown{
            Type:     string(acc.Type),
            Name:     acc.Name,
            Value:    converted,
            Currency: baseCurrency,
        })
    }
    
    // 4. Агрегировать инвестиции
    for _, p := range portfolios {
        value, _ := s.investmentRepo.CalculatePortfolioValue(p.ID)
        converted := s.currencyService.Convert(value.TotalMarketValue, p.BaseCurrency, baseCurrency)
        result.TotalAssets = result.TotalAssets.Add(converted)
        result.Breakdown = append(result.Breakdown, AssetBreakdown{
            Type:     "investment",
            Name:     p.Name,
            Value:    converted,
            Currency: baseCurrency,
        })
    }
    
    return result, nil
}
```

### 3.2 Net Worth History

```go
type NetWorthSnapshot struct {
    ID         uint            `gorm:"primaryKey"`
    UserID     uint            `gorm:"index;not null"`
    Date       time.Time       `gorm:"index;not null"`
    TotalValue decimal.Decimal `gorm:"type:decimal(19,4)"`
    Currency   string          `gorm:"type:char(3)"`
    Breakdown  JSON            `gorm:"type:jsonb"`
}
```

Scheduled job для ежедневного snapshot:
```go
func (s *SchedulerService) DailyNetWorthSnapshot() {
    users, _ := s.userRepo.GetAllActive()
    for _, user := range users {
        netWorth, _ := s.netWorthService.CalculateNetWorth(user.ID, user.BaseCurrency)
        s.snapshotRepo.Create(&NetWorthSnapshot{
            UserID:     user.ID,
            Date:       time.Now().Truncate(24 * time.Hour),
            TotalValue: netWorth.TotalAssets,
            Currency:   user.BaseCurrency,
            Breakdown:  netWorth.Breakdown,
        })
    }
}
```

### 3.3 Currency Service

```go
type CurrencyService struct {
    rateProvider RateProvider
    cache        *cache.Cache
}

func (s *CurrencyService) Convert(amount decimal.Decimal, from, to string) decimal.Decimal {
    if from == to {
        return amount
    }
    rate := s.getRate(from, to)
    return amount.Mul(rate)
}

func (s *CurrencyService) getRate(from, to string) decimal.Decimal {
    cacheKey := fmt.Sprintf("%s_%s", from, to)
    if cached, ok := s.cache.Get(cacheKey); ok {
        return cached.(decimal.Decimal)
    }
    
    rate, _ := s.rateProvider.GetRate(from, to)
    s.cache.Set(cacheKey, rate, 1*time.Hour)
    return rate
}
```

### 3.4 API Analytics

```
GET /analytics/networth                    # Текущий net worth
GET /analytics/networth/history            # История net worth
GET /analytics/networth/breakdown          # Breakdown по типам активов
GET /analytics/income-expense              # Income vs Expense за период
GET /analytics/performance                 # Доходность портфеля
```

### 3.5 Android Analytics Screens

```kotlin
// NetWorthScreen
@Composable
fun NetWorthScreen(viewModel: NetWorthViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    
    Column {
        // Total Net Worth Card
        NetWorthCard(
            total = state.totalNetWorth,
            change = state.change,
            changePercent = state.changePercent
        )
        
        // Chart
        NetWorthChart(data = state.history)
        
        // Breakdown
        AssetBreakdownList(items = state.breakdown)
    }
}
```

### 3.6 Готовность к масштабированию

**API Gateway (опционально):**
```yaml
# Kong / Nginx / Traefik
services:
  api-gateway:
    routes:
      - path: /api/v1/auth/*  -> user-service:8081
      - path: /api/v1/accounts/* -> transaction-service:8082
      - path: /api/v1/transactions/* -> transaction-service:8082
      - path: /api/v1/investments/* -> investment-service:8083
      - path: /api/v1/analytics/* -> analytics-service:8084
```

**Pagination везде:**
```go
type PaginatedResponse[T any] struct {
    Data       []T   `json:"data"`
    Page       int   `json:"page"`
    PageSize   int   `json:"page_size"`
    TotalCount int64 `json:"total_count"`
    TotalPages int   `json:"total_pages"`
}
```

### Deliverables Phase 3
- [ ] Net Worth calculation service
- [ ] Net Worth history snapshots
- [ ] Currency conversion service
- [ ] Analytics API endpoints
- [ ] Android: Net Worth screen
- [ ] Android: Charts (MPAndroidChart или Compose Charts)
- [ ] Pagination во всех списках
- [ ] (Optional) API Gateway

---

## 6. Доменная модель

### 6.1 Entity Relationship Diagram

```
┌─────────────┐       ┌─────────────────┐       ┌──────────────┐
│    User     │       │     Account     │       │  Transaction │
├─────────────┤       ├─────────────────┤       ├──────────────┤
│ id          │──┐    │ id              │──┐    │ id           │
│ username    │  │    │ user_id      ◄──┼──┘    │ user_id      │
│ password    │  │    │ type            │       │ type         │
│ email       │  │    │ name            │       │ account_id ◄─┼──┐
│ base_ccy    │  │    │ currency        │       │ dest_acc_id  │  │
│ created_at  │  │    │ balance         │       │ amount       │  │
└─────────────┘  │    │ is_active       │       │ currency     │  │
                 │    │ metadata        │       │ category_id  │  │
                 │    └─────────────────┘       │ description  │  │
                 │                              │ tx_date      │  │
                 │    ┌─────────────────┐       │ status       │  │
                 │    │    Category     │       └──────────────┘  │
                 │    ├─────────────────┤                         │
                 └───►│ id              │                         │
                      │ user_id         │                         │
                      │ name            │◄────────────────────────┘
                      │ color           │
                      │ icon            │
                      │ type            │
                      └─────────────────┘

┌─────────────┐       ┌─────────────────┐       ┌──────────────┐
│   Broker    │       │    Portfolio    │       │   Holding    │
├─────────────┤       ├─────────────────┤       ├──────────────┤
│ id          │──┐    │ id              │──┐    │ id           │
│ user_id     │  │    │ user_id         │  │    │ portfolio_id◄┼──┐
│ name        │  │    │ broker_id    ◄──┼──┘    │ security_id  │  │
└─────────────┘  │    │ name            │       │ quantity     │  │
                 │    │ base_currency   │       │ avg_cost     │  │
                 │    └─────────────────┘       │ total_cost   │  │
                 │                              └──────────────┘  │
                 │    ┌─────────────────┐                         │
                 │    │    Security     │       ┌──────────────┐  │
                 │    ├─────────────────┤       │    Trade     │  │
                 │    │ id              │◄──┐   ├──────────────┤  │
                 │    │ symbol          │   │   │ id           │  │
                 │    │ name            │   │   │ portfolio_id◄┼──┘
                 │    │ type            │   └───┼─security_id  │
                 │    │ currency        │       │ trade_date   │
                 │    └─────────────────┘       │ side         │
                 │                              │ quantity     │
                 │    ┌─────────────────┐       │ price        │
                 │    │  PriceHistory   │       │ fee          │
                 │    ├─────────────────┤       └──────────────┘
                 │    │ id              │
                 └───►│ security_id     │
                      │ date            │
                      │ open/high/low   │
                      │ close/volume    │
                      └─────────────────┘
```

### 6.2 Value Objects

```go
// Currency (immutable)
type Currency string

const (
    CurrencyUSD Currency = "USD"
    CurrencyEUR Currency = "EUR"
    CurrencyRUB Currency = "RUB"
    CurrencyBTC Currency = "BTC"
    CurrencyETH Currency = "ETH"
)

func (c Currency) IsValid() bool {
    // Validation logic
}

// Money (immutable value object)
type Money struct {
    Amount   decimal.Decimal
    Currency Currency
}

func NewMoney(amount decimal.Decimal, currency Currency) Money {
    return Money{Amount: amount, Currency: currency}
}

func (m Money) Add(other Money) (Money, error) {
    if m.Currency != other.Currency {
        return Money{}, ErrCurrencyMismatch
    }
    return NewMoney(m.Amount.Add(other.Amount), m.Currency), nil
}
```

### 6.3 Domain Events (Future)

```go
type DomainEvent interface {
    OccurredAt() time.Time
    AggregateID() uint
}

type TransactionCreated struct {
    TransactionID uint
    AccountID     uint
    Amount        Money
    Type          TransactionType
    occurredAt    time.Time
}

type BalanceChanged struct {
    AccountID  uint
    OldBalance Money
    NewBalance Money
    Reason     string
    occurredAt time.Time
}
```

---

## 7. Технические решения

### 7.1 Выбор Decimal библиотеки

**Рекомендация:** `github.com/shopspring/decimal`

| Библиотека | Pros | Cons |
|------------|------|------|
| shopspring/decimal | Популярная, хорошо тестирована, GORM support | Немного медленнее |
| ericlagergren/decimal | Быстрее, больше precision | Меньше community |
| float64 | Быстрый, встроенный | Потеря точности для финансов |

### 7.2 Миграции БД

**Рекомендация:** `golang-migrate/migrate`

```bash
# Создание миграции
migrate create -ext sql -dir migrations -seq add_accounts_table

# Применение
migrate -path migrations -database "postgres://..." up
```

Вместо AutoMigrate при каждом запуске.

### 7.3 Android: State Management

**Рекомендация:** Sealed class для UI State

```kotlin
sealed interface TransactionListState {
    data object Loading : TransactionListState
    data class Success(val transactions: List<Transaction>) : TransactionListState
    data class Error(val message: String) : TransactionListState
    data object Empty : TransactionListState
}
```

### 7.4 Error Handling Strategy

**Backend:**
```go
type AppError struct {
    Code    string `json:"code"`
    Message string `json:"message"`
    Details any    `json:"details,omitempty"`
}

var (
    ErrNotFound          = AppError{Code: "NOT_FOUND", Message: "Resource not found"}
    ErrInvalidInput      = AppError{Code: "INVALID_INPUT", Message: "Invalid input data"}
    ErrInsufficientFunds = AppError{Code: "INSUFFICIENT_FUNDS", Message: "Insufficient funds"}
)
```

**Android:**
```kotlin
sealed class AppError {
    data class Network(val message: String) : AppError()
    data class Server(val code: String, val message: String) : AppError()
    data class Validation(val field: String, val message: String) : AppError()
    data object Unauthorized : AppError()
}
```

### 7.5 Testing Strategy

**Backend:**
```
tests/
├── unit/
│   ├── service/           # Бизнес-логика
│   └── repository/        # С mock DB
├── integration/
│   ├── api/               # HTTP handlers
│   └── db/                # Real DB (testcontainers)
└── e2e/
    └── scenarios/         # Full flows
```

**Android:**
```
test/
├── domain/                # Use cases, mappers
├── data/                  # Repositories (с mock)
└── viewmodel/             # ViewModels

androidTest/
├── db/                    # Room tests
└── ui/                    # Compose tests
```

---

## 8. Риски и митигации

| Риск | Вероятность | Impact | Митигация |
|------|-------------|--------|-----------|
| Потеря данных при миграции на Decimal | Medium | Critical | Создать backup, тестировать на копии |
| External price API недоступен | High | Medium | Кэширование, fallback provider |
| Производительность при больших объемах | Low | High | Pagination, индексы, профилирование |
| Сложность sync между offline/online | Medium | Medium | Conflict resolution strategy, queue |
| Security breach через hardcoded secrets | High | Critical | Phase 0 — первый приоритет |

---

## Приложение A: Checklist по фазам

### Phase 0 Checklist
- [ ] JWT secret в environment variables
- [ ] Decimal для всех денежных полей
- [ ] Единый тип UserID
- [ ] Корректные HTTP status codes
- [ ] Базовая валидация входных данных
- [ ] Android: EncryptedSharedPreferences для токена
- [ ] Android: отключить cleartext в production
- [ ] Android: Result<T> везде вместо молчаливых failures

### Phase 1 Checklist
- [ ] Account модель в transaction service
- [ ] AccountType enum
- [ ] Расширенная Transaction модель
- [ ] TransactionType enum
- [ ] TransactionStatus enum
- [ ] Бизнес-логика: баланс + транзакции
- [ ] Account API endpoints
- [ ] Room Database setup
- [ ] Entity mappings
- [ ] Offline-first repositories
- [ ] Basic sync mechanism

### Phase 2 Checklist
- [ ] Holding модель
- [ ] Tax Lots
- [ ] PriceHistory модель
- [ ] External price API integration
- [ ] P&L calculations
- [ ] Portfolio value endpoint
- [ ] Android: Portfolio screen
- [ ] Android: Holdings list with P&L

### Phase 3 Checklist
- [ ] Net Worth service
- [ ] NetWorthSnapshot модель
- [ ] Daily snapshot scheduler
- [ ] Currency conversion service
- [ ] Analytics endpoints
- [ ] Android: Net Worth screen
- [ ] Android: Charts
- [ ] Pagination в списках
- [ ] Performance optimization

---

> Документ будет обновляться по мере развития проекта.



# Prompt 

Ты — Senior Lead Architect и Senior Mobile Engineer с реальным опытом разработки production-финтех приложений (personal finance, investment tracking, banking, portfolio analytics).

Проект находится в монорепозитории и состоит из мобильных клиентов и backend-микросервисов на Go + Docker.

/app
  /android
  /ios (в будущем)
/server
  /user
  /transaction
  /investment
/shared
  /contracts
  /finance-core

🎯 ЦЕЛЬ ПРОЕКТА

Построить централизованный финансовый хаб, в котором пользователь может видеть ВСЕ свои деньги:

мультивалютные активы

карты, счета, наличка

крипта

инвестиции (акции, фонды, металлы)

недвижимость

историю стоимости

динамику и доходность

общий net worth и аналитику

Проект делается сначала для себя, но архитектура должна быть готова к коммерциализации, семейному доступу и масштабированию.

🧠 ТВОЯ РОЛЬ

Ты работаешь как Tech Lead, а не кодогенератор:

сначала анализируешь существующий код

находишь архитектурные и доменные ошибки

переиспользуешь всё разумное

предлагаешь улучшения эволюционно, без переписывания всего проекта

объясняешь почему предлагается то или иное решение

🏗 ОБЩИЕ АРХИТЕКТУРНЫЕ ПРИНЦИПЫ
Общие

Чёткая доменная модель (финансы ≠ CRUD)

Явные bounded contexts

Простота важнее абстрактной “красоты”

Архитектура должна быть читаемой и поддерживаемой

📦 BACKEND (GO + DOCKER)
Технологии

Язык: Go

Архитектура: Clean / Hexagonal

Каждый сервис — изолированный bounded context

Контейнеризация: Docker

Docker Compose для локальной разработки

Сервисы

user — пользователи, роли, семейный доступ

transaction — все типы транзакций и их логика

investment — активы, доходность, инвестиционные операции

Принципы backend-кода

Чёткое разделение:

transport (HTTP)

application (use cases)

domain

infrastructure (DB, external APIs)

Явные доменные сущности и value objects

Ошибки — типизированные, не string-based

Контракты API — стабильные и версионированные

Готовность к OAuth (Google) поверх текущего JWT

💰 ФИНАНСОВАЯ ДОМЕННАЯ МОДЕЛЬ (КЛЮЧЕВО)
Активы

Актив — это сущность с историей, а не просто баланс.

Поддерживаемые типы:

банковские карты

счета

наличка

криптокошельки

инвестиции (акции, фонды, металлы)

недвижимость

Каждый актив:

имеет валюту

имеет историю стоимости

участвует в расчётах доходности

агрегируется в общий net worth

Транзакции

Транзакции НЕ универсальны.

Типы:

income

expense

transfer

investment buy / sell

dividend (с налогами)

interest

Каждый тип:

имеет свою бизнес-логику

влияет на активы по-разному

корректно учитывается в аналитике и доходности

Валюты

Курсы валют получаются через внешний API

Есть базовая валюта пользователя

Исторические курсы используются для аналитики

Агрегации учитывают временной контекст

📱 ANDROID

Clean Architecture

MVVM

Jetpack Compose

ViewModels

Coroutines + Flow

Koin

Room

Material 3

Offline-first:

локальная БД — source of truth

синхронизация с backend

🍎 IOS (В БУДУЩЕМ)

Swift

SwiftUI

Clean Architecture

Максимально совпадающая доменная модель с Android

📊 АНАЛИТИКА (MVP)

Обязательные экраны:

Общий net worth

Динамика по времени

В + / В – (абсолют и проценты)

Список активов с динамикой

Детальная история каждого актива

🚫 ОГРАНИЧЕНИЯ

❌ Не городить overengineering
❌ Не переписывать проект без необходимости
❌ Не использовать unsafe-подходы
❌ Не путать инфраструктуру и домен

✅ Объяснять архитектурные решения
✅ Предлагать альтернативы и trade-offs
✅ Думать как о продукте
✅ Писать код, который не стыдно поддерживать

🧩 ПОРЯДОК РАБОТЫ

Проанализируй текущий код backend и mobile

Найди архитектурные и доменные проблемы

Определи, что можно переиспользовать

Предложи эволюционный план улучшений

Только после этого начинай писать или менять код

🏁 КРИТЕРИЙ УСПЕХА

Архитектура уровня production-финтеха

Чёткая доменная модель

Масштабируемость

Читаемость и отказоустойчивость

Код, который спокойно можно монетизировать и развивать
