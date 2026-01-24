# Phase 1: Core Domain Model — Детальные задачи

> Приоритет: HIGH  
> Цель: Построить правильную финансовую доменную модель с Account как центром

---

## Обзор

Phase 1 фокусируется на создании правильной доменной модели для финтех-приложения:

1. **Account** — центральная сущность, представляющая любой актив пользователя
2. **Transaction** — расширенная модель с типами, статусами и связью с Account
3. **Room Database** — offline-first хранилище на Android

## Backend Tasks

### P1.1 Account Model

**Создать файл:** `server/services/transaction/internal/domain/model/account.go`

```go
package model

import (
    "time"
    
    "github.com/shopspring/decimal"
    "gorm.io/gorm"
)

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
    ID        uint            `gorm:"primaryKey" json:"id"`
    UserID    uint            `gorm:"index;not null" json:"user_id"`
    Type      AccountType     `gorm:"type:varchar(20);not null" json:"type"`
    Name      string          `gorm:"not null" json:"name"`
    Currency  string          `gorm:"type:char(3);not null" json:"currency"`
    Balance   decimal.Decimal `gorm:"type:decimal(19,4);default:0" json:"balance"`
    Icon      string          `gorm:"type:varchar(50)" json:"icon"`
    Color     string          `gorm:"type:char(7)" json:"color"`
    IsActive  bool            `gorm:"default:true" json:"is_active"`
    SortOrder int             `gorm:"default:0" json:"sort_order"`
    CreatedAt time.Time       `json:"created_at"`
    UpdatedAt time.Time       `json:"updated_at"`
    DeletedAt gorm.DeletedAt  `gorm:"index" json:"-"`
}
```

### P1.2 Account Repository

**Создать файл:** `server/services/transaction/internal/data/repository/account_repository.go`

```go
package repository

import (
    "transaction/internal/domain/model"
    "gorm.io/gorm"
)

type AccountRepository struct {
    db *gorm.DB
}

func NewAccountRepository(db *gorm.DB) *AccountRepository {
    return &AccountRepository{db: db}
}

func (r *AccountRepository) Create(account *model.Account) error {
    return r.db.Create(account).Error
}

func (r *AccountRepository) GetByID(id uint) (*model.Account, error) {
    var account model.Account
    err := r.db.First(&account, id).Error
    return &account, err
}

func (r *AccountRepository) GetByUserID(userID uint) ([]model.Account, error) {
    var accounts []model.Account
    err := r.db.Where("user_id = ?", userID).
        Order("sort_order ASC, created_at DESC").
        Find(&accounts).Error
    return accounts, err
}

func (r *AccountRepository) Update(account *model.Account) error {
    return r.db.Save(account).Error
}

func (r *AccountRepository) Delete(id uint) error {
    return r.db.Delete(&model.Account{}, id).Error
}

func (r *AccountRepository) UpdateBalance(id uint, newBalance decimal.Decimal) error {
    return r.db.Model(&model.Account{}).
        Where("id = ?", id).
        Update("balance", newBalance).Error
}
```

### P1.3 Account Service

**Создать файл:** `server/services/transaction/internal/domain/service/account_service.go`

```go
package service

import (
    "errors"
    "fmt"
    "transaction/internal/data/repository"
    "transaction/internal/domain/model"
    
    "github.com/shopspring/decimal"
)

var (
    ErrAccountNotFound     = errors.New("account not found")
    ErrAccountAccessDenied = errors.New("access denied to this account")
    ErrInvalidAccountType  = errors.New("invalid account type")
    ErrInvalidAccountName  = errors.New("account name is required")
)

type AccountService struct {
    repo *repository.AccountRepository
}

func NewAccountService(repo *repository.AccountRepository) *AccountService {
    return &AccountService{repo: repo}
}

func (s *AccountService) CreateAccount(account *model.Account) (*model.Account, error) {
    if account.Name == "" {
        return nil, ErrInvalidAccountName
    }
    
    if !isValidAccountType(account.Type) {
        return nil, ErrInvalidAccountType
    }
    
    if account.Currency == "" {
        account.Currency = "RUB"
    }
    
    if account.Balance.IsZero() {
        account.Balance = decimal.Zero
    }
    
    if err := s.repo.Create(account); err != nil {
        return nil, fmt.Errorf("create account: %w", err)
    }
    
    return account, nil
}

func (s *AccountService) GetAccount(id, userID uint) (*model.Account, error) {
    account, err := s.repo.GetByID(id)
    if err != nil {
        return nil, ErrAccountNotFound
    }
    
    if account.UserID != userID {
        return nil, ErrAccountAccessDenied
    }
    
    return account, nil
}

func (s *AccountService) GetUserAccounts(userID uint) ([]model.Account, error) {
    return s.repo.GetByUserID(userID)
}

func (s *AccountService) UpdateAccount(id, userID uint, updates *model.Account) (*model.Account, error) {
    account, err := s.GetAccount(id, userID)
    if err != nil {
        return nil, err
    }
    
    if updates.Name != "" {
        account.Name = updates.Name
    }
    if updates.Icon != "" {
        account.Icon = updates.Icon
    }
    if updates.Color != "" {
        account.Color = updates.Color
    }
    
    if err := s.repo.Update(account); err != nil {
        return nil, fmt.Errorf("update account: %w", err)
    }
    
    return account, nil
}

func (s *AccountService) DeleteAccount(id, userID uint) error {
    _, err := s.GetAccount(id, userID)
    if err != nil {
        return err
    }
    
    return s.repo.Delete(id)
}

func isValidAccountType(t model.AccountType) bool {
    switch t {
    case model.AccountTypeBankAccount,
        model.AccountTypeCard,
        model.AccountTypeCash,
        model.AccountTypeCrypto,
        model.AccountTypeInvestment,
        model.AccountTypeProperty:
        return true
    }
    return false
}
```

### P1.4 Account HTTP Handlers

**Создать файл:** `server/services/transaction/internal/presentation/http/accounts.go`

```go
package http

import (
    "net/http"
    "transaction/internal/domain/model"
    "transaction/internal/domain/service"
    
    "github.com/gin-gonic/gin"
    "github.com/shopspring/decimal"
)

type AccountHandler struct {
    service *service.AccountService
}

func NewAccountHandler(s *service.AccountService) *AccountHandler {
    return &AccountHandler{service: s}
}

type CreateAccountRequest struct {
    Type     string `json:"type" binding:"required"`
    Name     string `json:"name" binding:"required"`
    Currency string `json:"currency" binding:"required,len=3"`
    Balance  string `json:"balance"`
    Icon     string `json:"icon"`
    Color    string `json:"color"`
}

type AccountResponse struct {
    ID        uint   `json:"id"`
    Type      string `json:"type"`
    Name      string `json:"name"`
    Currency  string `json:"currency"`
    Balance   string `json:"balance"`
    Icon      string `json:"icon"`
    Color     string `json:"color"`
    IsActive  bool   `json:"is_active"`
    SortOrder int    `json:"sort_order"`
}

func toAccountResponse(a *model.Account) AccountResponse {
    return AccountResponse{
        ID:        a.ID,
        Type:      string(a.Type),
        Name:      a.Name,
        Currency:  a.Currency,
        Balance:   a.Balance.String(),
        Icon:      a.Icon,
        Color:     a.Color,
        IsActive:  a.IsActive,
        SortOrder: a.SortOrder,
    }
}

func (h *AccountHandler) CreateAccount(c *gin.Context) {
    var req CreateAccountRequest
    if err := c.ShouldBindJSON(&req); err != nil {
        c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
        return
    }
    
    userID, _ := c.Get("userID")
    
    balance := decimal.Zero
    if req.Balance != "" {
        var err error
        balance, err = decimal.NewFromString(req.Balance)
        if err != nil {
            c.JSON(http.StatusBadRequest, gin.H{"error": "invalid balance format"})
            return
        }
    }
    
    account := &model.Account{
        UserID:   userID.(uint),
        Type:     model.AccountType(req.Type),
        Name:     req.Name,
        Currency: req.Currency,
        Balance:  balance,
        Icon:     req.Icon,
        Color:    req.Color,
        IsActive: true,
    }
    
    created, err := h.service.CreateAccount(account)
    if err != nil {
        c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
        return
    }
    
    c.JSON(http.StatusCreated, toAccountResponse(created))
}

func (h *AccountHandler) GetAccounts(c *gin.Context) {
    userID, _ := c.Get("userID")
    
    accounts, err := h.service.GetUserAccounts(userID.(uint))
    if err != nil {
        c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
        return
    }
    
    response := make([]AccountResponse, len(accounts))
    for i, a := range accounts {
        response[i] = toAccountResponse(&a)
    }
    
    c.JSON(http.StatusOK, response)
}

func (h *AccountHandler) GetAccount(c *gin.Context) {
    var uri struct {
        ID uint `uri:"id" binding:"required"`
    }
    if err := c.ShouldBindUri(&uri); err != nil {
        c.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
        return
    }
    
    userID, _ := c.Get("userID")
    
    account, err := h.service.GetAccount(uri.ID, userID.(uint))
    if err != nil {
        c.JSON(http.StatusNotFound, gin.H{"error": err.Error()})
        return
    }
    
    c.JSON(http.StatusOK, toAccountResponse(account))
}

func (h *AccountHandler) UpdateAccount(c *gin.Context) {
    var uri struct {
        ID uint `uri:"id" binding:"required"`
    }
    if err := c.ShouldBindUri(&uri); err != nil {
        c.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
        return
    }
    
    var req struct {
        Name  string `json:"name"`
        Icon  string `json:"icon"`
        Color string `json:"color"`
    }
    if err := c.ShouldBindJSON(&req); err != nil {
        c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
        return
    }
    
    userID, _ := c.Get("userID")
    
    updates := &model.Account{
        Name:  req.Name,
        Icon:  req.Icon,
        Color: req.Color,
    }
    
    account, err := h.service.UpdateAccount(uri.ID, userID.(uint), updates)
    if err != nil {
        c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
        return
    }
    
    c.JSON(http.StatusOK, toAccountResponse(account))
}

func (h *AccountHandler) DeleteAccount(c *gin.Context) {
    var uri struct {
        ID uint `uri:"id" binding:"required"`
    }
    if err := c.ShouldBindUri(&uri); err != nil {
        c.JSON(http.StatusBadRequest, gin.H{"error": "invalid id"})
        return
    }
    
    userID, _ := c.Get("userID")
    
    if err := h.service.DeleteAccount(uri.ID, userID.(uint)); err != nil {
        c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
        return
    }
    
    c.JSON(http.StatusNoContent, nil)
}
```

### P1.5 Extended Transaction Model

**Обновить:** `server/services/transaction/internal/domain/model/transaction.go`

```go
package model

import (
    "time"
    
    "github.com/shopspring/decimal"
    "gorm.io/gorm"
)

type TransactionType string

const (
    TransactionTypeIncome   TransactionType = "income"
    TransactionTypeExpense  TransactionType = "expense"
    TransactionTypeTransfer TransactionType = "transfer"
)

type TransactionStatus string

const (
    TransactionStatusPending   TransactionStatus = "pending"
    TransactionStatusCompleted TransactionStatus = "completed"
    TransactionStatusFailed    TransactionStatus = "failed"
)

type Transaction struct {
    ID                   uint              `gorm:"primaryKey" json:"id"`
    UserID               uint              `gorm:"index;not null" json:"user_id"`
    AccountID            uint              `gorm:"index;not null" json:"account_id"`
    Account              *Account          `gorm:"foreignKey:AccountID" json:"-"`
    DestinationAccountID *uint             `gorm:"index" json:"destination_account_id,omitempty"`
    DestinationAccount   *Account          `gorm:"foreignKey:DestinationAccountID" json:"-"`
    Type                 TransactionType   `gorm:"type:varchar(20);not null" json:"type"`
    Status               TransactionStatus `gorm:"type:varchar(20);default:'completed'" json:"status"`
    Amount               decimal.Decimal   `gorm:"type:decimal(19,4);not null" json:"amount"`
    Currency             string            `gorm:"type:char(3);default:'RUB'" json:"currency"`
    CategoryID           *uint             `gorm:"index" json:"category_id,omitempty"`
    Category             *Category         `gorm:"foreignKey:CategoryID" json:"category,omitempty"`
    Description          string            `gorm:"type:text" json:"description"`
    TransactionDate      time.Time         `gorm:"not null" json:"transaction_date"`
    CreatedAt            time.Time         `json:"created_at"`
    UpdatedAt            time.Time         `json:"updated_at"`
    DeletedAt            gorm.DeletedAt    `gorm:"index" json:"-"`
}
```

### P1.6 Transaction Business Logic with Balances

**Обновить:** `server/services/transaction/internal/domain/service/service.go`

```go
// Добавить методы для работы с балансами

func (s *TransactionService) CreateTransaction(tx *model.Transaction, accountRepo *repository.AccountRepository) (*model.Transaction, error) {
    // Валидация
    if tx.Amount.LessThanOrEqual(decimal.Zero) {
        return nil, ErrInvalidAmount
    }
    
    // Получить счет
    account, err := accountRepo.GetByID(tx.AccountID)
    if err != nil {
        return nil, errors.New("account not found")
    }
    
    // Проверить владельца
    if account.UserID != tx.UserID {
        return nil, errors.New("access denied")
    }
    
    // Применить к балансу в зависимости от типа
    switch tx.Type {
    case model.TransactionTypeIncome:
        account.Balance = account.Balance.Add(tx.Amount)
    case model.TransactionTypeExpense:
        account.Balance = account.Balance.Sub(tx.Amount)
    case model.TransactionTypeTransfer:
        if tx.DestinationAccountID == nil {
            return nil, errors.New("destination account required for transfer")
        }
        // Списать с исходного
        account.Balance = account.Balance.Sub(tx.Amount)
        // Зачислить на целевой
        destAccount, err := accountRepo.GetByID(*tx.DestinationAccountID)
        if err != nil {
            return nil, errors.New("destination account not found")
        }
        destAccount.Balance = destAccount.Balance.Add(tx.Amount)
        if err := accountRepo.Update(destAccount); err != nil {
            return nil, err
        }
    }
    
    // Сохранить обновленный баланс
    if err := accountRepo.Update(account); err != nil {
        return nil, err
    }
    
    // Установить дату если не указана
    if tx.TransactionDate.IsZero() {
        tx.TransactionDate = time.Now()
    }
    
    // Установить статус
    if tx.Status == "" {
        tx.Status = model.TransactionStatusCompleted
    }
    
    // Сохранить транзакцию
    created, err := s.repo.Create(tx)
    if err != nil {
        return nil, err
    }
    
    return created, nil
}
```

---

## Android Tasks

### P1.7 Room Database Setup

**Добавить зависимости в** `app/android/gradle/libs.versions.toml`:

```toml
[versions]
room = "2.6.1"

[libraries]
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
```

**Добавить в** `app/android/app/build.gradle.kts`:

```kotlin
dependencies {
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
```

### P1.8 Room Entities

**Создать:** `app/android/app/src/main/java/com/barghest/bux/data/local/entity/AccountEntity.kt`

```kotlin
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: Int,
    val userId: Int,
    val type: String,
    val name: String,
    val currency: String,
    val balance: String,
    val icon: String?,
    val color: String?,
    val isActive: Boolean,
    val sortOrder: Int,
    val syncedAt: Long = System.currentTimeMillis()
)
```

**Создать:** `app/android/app/src/main/java/com/barghest/bux/data/local/entity/TransactionEntity.kt`

```kotlin
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: Int,
    val userId: Int,
    val accountId: Int,
    val destinationAccountId: Int?,
    val type: String,
    val status: String,
    val amount: String,
    val currency: String,
    val categoryId: Int?,
    val description: String?,
    val transactionDate: Long,
    val syncedAt: Long = System.currentTimeMillis()
)
```

### P1.9 DAOs

**Создать:** `app/android/app/src/main/java/com/barghest/bux/data/local/dao/AccountDao.kt`

```kotlin
@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE userId = :userId ORDER BY sortOrder ASC")
    fun getAccountsByUser(userId: Int): Flow<List<AccountEntity>>
    
    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Int): AccountEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>)
    
    @Update
    suspend fun update(account: AccountEntity)
    
    @Delete
    suspend fun delete(account: AccountEntity)
    
    @Query("DELETE FROM accounts")
    suspend fun deleteAll()
}
```

### P1.10 BuxDatabase

**Создать:** `app/android/app/src/main/java/com/barghest/bux/data/local/BuxDatabase.kt`

```kotlin
@Database(
    entities = [AccountEntity::class, TransactionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BuxDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    
    companion object {
        @Volatile
        private var INSTANCE: BuxDatabase? = null
        
        fun getDatabase(context: Context): BuxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BuxDatabase::class.java,
                    "bux_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

### P1.11 Account Domain Model

**Создать:** `app/android/app/src/main/java/com/barghest/bux/domain/model/Account.kt`

```kotlin
data class Account(
    val id: Int,
    val type: AccountType,
    val name: String,
    val currency: String,
    val balance: BigDecimal,
    val icon: String?,
    val color: String?,
    val isActive: Boolean,
    val sortOrder: Int
)

enum class AccountType(val value: String) {
    BANK_ACCOUNT("bank_account"),
    CARD("card"),
    CASH("cash"),
    CRYPTO("crypto"),
    INVESTMENT("investment"),
    PROPERTY("property");
    
    companion object {
        fun fromValue(value: String): AccountType {
            return entries.find { it.value == value } ?: BANK_ACCOUNT
        }
    }
}
```

### P1.12 Account List Screen

**Создать:** `app/android/app/src/main/java/com/barghest/bux/ui/screens/accounts/AccountsScreen.kt`

```kotlin
@Composable
fun AccountsScreen(
    navController: NavController,
    viewModel: AccountsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Счета") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_account") }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить счет")
            }
        }
    ) { padding ->
        when (val currentState = state) {
            is AccountsState.Loading -> LoadingState()
            is AccountsState.Empty -> EmptyState()
            is AccountsState.Error -> ErrorState(currentState.message) { viewModel.refresh() }
            is AccountsState.Success -> AccountsList(currentState.accounts)
        }
    }
}
```

---

## Checklist

### Backend
- [ ] P1.1: Account model
- [ ] P1.2: AccountType enum и миграция
- [ ] P1.3: Account repository и service
- [ ] P1.4: Account API endpoints
- [ ] P1.5: Расширить Transaction модель
- [ ] P1.6: TransactionType и TransactionStatus enums
- [ ] P1.7: Бизнес-логика с балансами

### Android
- [ ] P1.8: Room Database setup
- [ ] P1.9: Account и Transaction entities
- [ ] P1.10: DAOs и offline-first repositories
- [ ] P1.11: Account list screen
- [ ] P1.12: Add/Edit Account screen

---

## Следующие шаги

После Phase 1 проект будет иметь:
- Полноценную модель Account для любых типов активов
- Транзакции, связанные со счетами и влияющие на баланс
- Offline-first хранилище на Android

Переход к Phase 2: Investments & Assets.
