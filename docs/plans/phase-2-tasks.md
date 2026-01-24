# Phase 2: Categories, Investments & Sync — Детальные задачи

> Приоритет: HIGH  
> Цель: Категории транзакций, интеграция инвестиций с Account, offline-first sync

---

## Обзор

Phase 2 фокусируется на:

1. **Category** — категоризация транзакций для аналитики
2. **Holding** — текущие позиции портфеля с P&L
3. **Investment-Account интеграция** — инвестиционные портфели как Account type
4. **Price History** — история цен для расчета стоимости
5. **Sync mechanism** — offline-first с conflict resolution

---

## Backend Tasks

### P2.1 Category Model (Transaction Service)

**Создать файл:** `server/services/transaction/internal/domain/model/category.go` (уже существует, расширить)

```go
package model

import (
    "time"
    "gorm.io/gorm"
)

type CategoryType string

const (
    CategoryTypeIncome  CategoryType = "income"
    CategoryTypeExpense CategoryType = "expense"
)

type Category struct {
    ID        uint           `gorm:"primaryKey" json:"id"`
    UserID    uint           `gorm:"index;not null" json:"user_id"`
    Name      string         `gorm:"not null" json:"name"`
    Type      CategoryType   `gorm:"type:varchar(20);not null" json:"type"`
    Icon      string         `gorm:"type:varchar(50)" json:"icon"`
    Color     string         `gorm:"type:char(7)" json:"color"`
    ParentID  *uint          `gorm:"index" json:"parent_id,omitempty"`
    Parent    *Category      `gorm:"foreignKey:ParentID" json:"-"`
    SortOrder int            `gorm:"default:0" json:"sort_order"`
    IsSystem  bool           `gorm:"default:false" json:"is_system"`
    CreatedAt time.Time      `json:"created_at"`
    UpdatedAt time.Time      `json:"updated_at"`
    DeletedAt gorm.DeletedAt `gorm:"index" json:"-"`
}
```

### P2.2 Category Repository & Service

**Файлы:**
- `server/services/transaction/internal/data/repository/category_repository.go`
- `server/services/transaction/internal/domain/service/category_service.go`

```go
// Repository
type CategoryRepository struct {
    db *gorm.DB
}

func (r *CategoryRepository) GetByUserID(userID uint) ([]Category, error)
func (r *CategoryRepository) GetByType(userID uint, ctype CategoryType) ([]Category, error)
func (r *CategoryRepository) Create(category *Category) error
func (r *CategoryRepository) Update(category *Category) error
func (r *CategoryRepository) Delete(id uint) error

// Service
type CategoryService struct {
    repo *CategoryRepository
}

func (s *CategoryService) CreateCategory(category *Category) (*Category, error)
func (s *CategoryService) GetUserCategories(userID uint) ([]Category, error)
func (s *CategoryService) CreateDefaultCategories(userID uint) error
```

### P2.3 Category HTTP Handlers

**API Endpoints:**
```
GET    /categories              # Все категории пользователя
GET    /categories?type=income  # Фильтр по типу
POST   /categories              # Создать категорию
PUT    /categories/:id          # Обновить
DELETE /categories/:id          # Удалить (soft delete)
POST   /categories/defaults     # Создать дефолтные категории
```

### P2.4 Default Categories Seeder

```go
var DefaultCategories = []Category{
    // Income
    {Name: "Зарплата", Type: CategoryTypeIncome, Icon: "work", Color: "#4CAF50"},
    {Name: "Фриланс", Type: CategoryTypeIncome, Icon: "laptop", Color: "#8BC34A"},
    {Name: "Дивиденды", Type: CategoryTypeIncome, Icon: "trending_up", Color: "#009688"},
    {Name: "Проценты", Type: CategoryTypeIncome, Icon: "account_balance", Color: "#00BCD4"},
    {Name: "Подарки", Type: CategoryTypeIncome, Icon: "card_giftcard", Color: "#E91E63"},
    
    // Expense
    {Name: "Продукты", Type: CategoryTypeExpense, Icon: "shopping_cart", Color: "#FF9800"},
    {Name: "Транспорт", Type: CategoryTypeExpense, Icon: "directions_car", Color: "#2196F3"},
    {Name: "Жилье", Type: CategoryTypeExpense, Icon: "home", Color: "#795548"},
    {Name: "Здоровье", Type: CategoryTypeExpense, Icon: "local_hospital", Color: "#F44336"},
    {Name: "Развлечения", Type: CategoryTypeExpense, Icon: "movie", Color: "#9C27B0"},
    {Name: "Рестораны", Type: CategoryTypeExpense, Icon: "restaurant", Color: "#FF5722"},
    {Name: "Одежда", Type: CategoryTypeExpense, Icon: "checkroom", Color: "#673AB7"},
    {Name: "Подписки", Type: CategoryTypeExpense, Icon: "subscriptions", Color: "#3F51B5"},
    {Name: "Образование", Type: CategoryTypeExpense, Icon: "school", Color: "#03A9F4"},
}
```

---

### P2.5 Holding Model (Investment Service)

**Обновить:** `server/services/investment/internal/domain/model/model.go`

```go
type Holding struct {
    ID          uint            `gorm:"primaryKey" json:"id"`
    PortfolioID uint            `gorm:"index;not null" json:"portfolio_id"`
    SecurityID  uint            `gorm:"index;not null" json:"security_id"`
    Security    *Security       `gorm:"foreignKey:SecurityID" json:"security,omitempty"`
    Quantity    decimal.Decimal `gorm:"type:decimal(19,8);not null" json:"quantity"`
    AverageCost decimal.Decimal `gorm:"type:decimal(19,4);not null" json:"average_cost"`
    TotalCost   decimal.Decimal `gorm:"type:decimal(19,4);not null" json:"total_cost"`
    UpdatedAt   time.Time       `json:"updated_at"`
}

type PriceHistory struct {
    ID         uint            `gorm:"primaryKey" json:"id"`
    SecurityID uint            `gorm:"index;not null" json:"security_id"`
    Date       time.Time       `gorm:"index;not null" json:"date"`
    Open       decimal.Decimal `gorm:"type:decimal(19,4)" json:"open"`
    High       decimal.Decimal `gorm:"type:decimal(19,4)" json:"high"`
    Low        decimal.Decimal `gorm:"type:decimal(19,4)" json:"low"`
    Close      decimal.Decimal `gorm:"type:decimal(19,4);not null" json:"close"`
    Volume     int64           `json:"volume"`
}
```

### P2.6 Holding Service с расчетом P&L

```go
type HoldingValue struct {
    Holding              Holding         `json:"holding"`
    CurrentPrice         decimal.Decimal `json:"current_price"`
    MarketValue          decimal.Decimal `json:"market_value"`
    UnrealizedPnL        decimal.Decimal `json:"unrealized_pnl"`
    UnrealizedPnLPercent decimal.Decimal `json:"unrealized_pnl_percent"`
}

type PortfolioValue struct {
    PortfolioID        uint            `json:"portfolio_id"`
    Holdings           []HoldingValue  `json:"holdings"`
    TotalCost          decimal.Decimal `json:"total_cost"`
    TotalMarketValue   decimal.Decimal `json:"total_market_value"`
    TotalUnrealizedPnL decimal.Decimal `json:"total_unrealized_pnl"`
}

func (s *InvestmentService) CalculatePortfolioValue(portfolioID uint) (*PortfolioValue, error) {
    holdings, err := s.holdingRepo.GetByPortfolioID(portfolioID)
    if err != nil {
        return nil, err
    }
    
    result := &PortfolioValue{
        PortfolioID: portfolioID,
        Holdings:    make([]HoldingValue, 0, len(holdings)),
    }
    
    for _, h := range holdings {
        currentPrice, err := s.GetCurrentPrice(h.SecurityID)
        if err != nil {
            continue
        }
        
        marketValue := h.Quantity.Mul(currentPrice)
        unrealizedPnL := marketValue.Sub(h.TotalCost)
        var pnlPercent decimal.Decimal
        if !h.TotalCost.IsZero() {
            pnlPercent = unrealizedPnL.Div(h.TotalCost).Mul(decimal.NewFromInt(100))
        }
        
        result.Holdings = append(result.Holdings, HoldingValue{
            Holding:              h,
            CurrentPrice:         currentPrice,
            MarketValue:          marketValue,
            UnrealizedPnL:        unrealizedPnL,
            UnrealizedPnLPercent: pnlPercent,
        })
        
        result.TotalCost = result.TotalCost.Add(h.TotalCost)
        result.TotalMarketValue = result.TotalMarketValue.Add(marketValue)
    }
    
    result.TotalUnrealizedPnL = result.TotalMarketValue.Sub(result.TotalCost)
    return result, nil
}
```

### P2.7 Update Holdings on Trade

```go
func (s *InvestmentService) ExecuteTrade(trade *Trade) (*Trade, error) {
    // 1. Validate
    if trade.Quantity.LessThanOrEqual(decimal.Zero) {
        return nil, ErrInvalidQuantity
    }
    
    // 2. Get or create holding
    holding, err := s.holdingRepo.GetByPortfolioAndSecurity(trade.PortfolioID, trade.SecurityID)
    if err != nil && !errors.Is(err, gorm.ErrRecordNotFound) {
        return nil, err
    }
    
    if holding == nil {
        holding = &Holding{
            PortfolioID: trade.PortfolioID,
            SecurityID:  trade.SecurityID,
            Quantity:    decimal.Zero,
            AverageCost: decimal.Zero,
            TotalCost:   decimal.Zero,
        }
    }
    
    // 3. Update holding based on trade side
    tradeCost := trade.Quantity.Mul(trade.Price).Add(trade.Fee)
    
    switch trade.Side {
    case "buy":
        newQuantity := holding.Quantity.Add(trade.Quantity)
        newTotalCost := holding.TotalCost.Add(tradeCost)
        holding.Quantity = newQuantity
        holding.TotalCost = newTotalCost
        if !newQuantity.IsZero() {
            holding.AverageCost = newTotalCost.Div(newQuantity)
        }
        
    case "sell":
        if holding.Quantity.LessThan(trade.Quantity) {
            return nil, ErrInsufficientShares
        }
        // Proportionally reduce cost basis
        soldRatio := trade.Quantity.Div(holding.Quantity)
        costReduction := holding.TotalCost.Mul(soldRatio)
        holding.Quantity = holding.Quantity.Sub(trade.Quantity)
        holding.TotalCost = holding.TotalCost.Sub(costReduction)
    }
    
    // 4. Save trade and holding
    if err := s.tradeRepo.Create(trade); err != nil {
        return nil, err
    }
    
    if holding.ID == 0 {
        if err := s.holdingRepo.Create(holding); err != nil {
            return nil, err
        }
    } else {
        if err := s.holdingRepo.Update(holding); err != nil {
            return nil, err
        }
    }
    
    return trade, nil
}
```

### P2.8 Investment API Endpoints

```
# Portfolios
GET    /portfolios                     # Все портфели пользователя
POST   /portfolios                     # Создать портфель
GET    /portfolios/:id                 # Детали портфеля
GET    /portfolios/:id/value           # Стоимость портфеля с P&L
GET    /portfolios/:id/holdings        # Позиции портфеля

# Trades
POST   /portfolios/:id/trades          # Создать сделку
GET    /portfolios/:id/trades          # История сделок

# Securities
GET    /securities                     # Поиск ценных бумаг
GET    /securities/:id                 # Детали бумаги
GET    /securities/:id/price           # Текущая цена
GET    /securities/:id/history         # История цен
```

---

## Android Tasks

### P2.9 Category Entity и DAO

**Файл:** `app/android/app/src/main/java/com/barghest/bux/data/local/entity/CategoryEntity.kt`

```kotlin
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: Int,
    val userId: Int,
    val name: String,
    val type: String,
    val icon: String?,
    val color: String?,
    val parentId: Int?,
    val sortOrder: Int,
    val isSystem: Boolean,
    val syncedAt: Long = System.currentTimeMillis()
)
```

**Файл:** `app/android/app/src/main/java/com/barghest/bux/data/local/dao/CategoryDao.kt`

```kotlin
@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY sortOrder ASC")
    fun getCategoriesByUser(userId: Int): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories WHERE userId = :userId AND type = :type ORDER BY sortOrder ASC")
    fun getCategoriesByType(userId: Int, type: String): Flow<List<CategoryEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)
    
    @Query("DELETE FROM categories WHERE userId = :userId")
    suspend fun deleteAllByUser(userId: Int)
}
```

### P2.10 Category Domain Model

```kotlin
data class Category(
    val id: Int,
    val name: String,
    val type: CategoryType,
    val icon: String?,
    val color: String?,
    val parentId: Int?,
    val sortOrder: Int,
    val isSystem: Boolean
)

enum class CategoryType(val value: String) {
    INCOME("income"),
    EXPENSE("expense");
    
    companion object {
        fun fromValue(value: String) = entries.find { it.value == value } ?: EXPENSE
    }
}
```

### P2.11 Category Screen

**Файл:** `app/android/app/src/main/java/com/barghest/bux/ui/screens/categories/CategoriesScreen.kt`

```kotlin
@Composable
fun CategoriesScreen(
    navController: NavController,
    viewModel: CategoriesViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableStateOf(CategoryType.EXPENSE) }
    
    Scaffold(
        topBar = { TopAppBar(title = { Text("Категории") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_category") }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Tabs: Income / Expense
            TabRow(selectedTabIndex = if (selectedTab == CategoryType.INCOME) 0 else 1) {
                Tab(
                    selected = selectedTab == CategoryType.INCOME,
                    onClick = { selectedTab = CategoryType.INCOME },
                    text = { Text("Доходы") }
                )
                Tab(
                    selected = selectedTab == CategoryType.EXPENSE,
                    onClick = { selectedTab = CategoryType.EXPENSE },
                    text = { Text("Расходы") }
                )
            }
            
            // Category list
            when (val currentState = state) {
                is CategoriesState.Success -> {
                    val filtered = currentState.categories.filter { it.type == selectedTab }
                    CategoryList(categories = filtered)
                }
                // Loading, Error states...
            }
        }
    }
}
```

### P2.12 Update AddTransactionScreen with Category

Обновить `AddTransactionScreen` для выбора категории:

```kotlin
// В AddTransactionUiState
data class AddTransactionUiState(
    // ... existing fields
    val selectedCategory: Category? = null,
    val categories: List<Category> = emptyList()
)

// В AddTransactionScreen
CategoryDropdown(
    categories = state.categories.filter { 
        when (state.type) {
            TransactionType.INCOME -> it.type == CategoryType.INCOME
            else -> it.type == CategoryType.EXPENSE
        }
    },
    selectedCategory = state.selectedCategory,
    onCategorySelected = viewModel::updateCategory
)
```

### P2.13 Sync Manager

**Файл:** `app/android/app/src/main/java/com/barghest/bux/data/sync/SyncManager.kt`

```kotlin
class SyncManager(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    suspend fun syncAll(): Result<Unit> {
        return try {
            // Sync in order: categories, accounts, transactions
            categoryRepository.refreshCategories()
            accountRepository.refreshAccounts()
            transactionRepository.refreshTransactions()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun schedulePeriodicalSync() {
        val workRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "sync",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val syncManager: SyncManager by inject()
    
    override suspend fun doWork(): Result {
        return when (syncManager.syncAll()) {
            is kotlin.Result.Success -> Result.success()
            is kotlin.Result.Failure -> Result.retry()
        }
    }
}
```

---

## Checklist

### Backend - Categories
- [ ] P2.1: Category model с типами и иконками
- [ ] P2.2: Category repository и service
- [ ] P2.3: Category HTTP handlers
- [ ] P2.4: Default categories seeder
- [ ] P2.5: Миграция БД для categories

### Backend - Investments
- [ ] P2.6: Holding model
- [ ] P2.7: PriceHistory model
- [ ] P2.8: Holding service с P&L расчетами
- [ ] P2.9: Update holdings on trade
- [ ] P2.10: Portfolio value endpoint
- [ ] P2.11: Holdings list endpoint

### Android - Categories
- [ ] P2.12: CategoryEntity и CategoryDao
- [ ] P2.13: Category domain model
- [ ] P2.14: CategoryRepository
- [ ] P2.15: CategoriesScreen
- [ ] P2.16: AddCategoryScreen
- [ ] P2.17: Update AddTransactionScreen с категориями

### Android - Sync
- [ ] P2.18: SyncManager
- [ ] P2.19: SyncWorker с WorkManager
- [ ] P2.20: Pull-to-refresh на экранах

### Android - Investments (UI)
- [ ] P2.21: PortfoliosScreen
- [ ] P2.22: PortfolioDetailScreen с holdings
- [ ] P2.23: AddTradeScreen

---

## Следующие шаги

После Phase 2 проект будет иметь:
- Полноценную категоризацию транзакций
- Инвестиционный трекинг с P&L
- Offline-first синхронизацию
- Готовность к Phase 3 (Analytics & Net Worth)

Переход к Phase 3: Analytics & Scale.
