# Phase 0: Security & Foundation — Детальные задачи

> Приоритет: CRITICAL  
> Цель: Устранить уязвимости безопасности и подготовить фундамент

---

## Backend Tasks

### B0.1 JWT Secret из Environment

**Файлы для изменения:**
- `server/user/internal/infra/auth/jwt.go`
- `server/transaction/internal/infra/auth/jwt.go`
- `server/investment/internal/infra/auth/jwt.go`

**Текущий код:**
```go
var JwtKey = []byte("key")
```

**Новый код:**
```go
import "os"

var JwtKey = []byte(getJWTSecret())

func getJWTSecret() string {
    secret := os.Getenv("JWT_SECRET")
    if secret == "" {
        panic("JWT_SECRET environment variable is required")
    }
    if len(secret) < 32 {
        panic("JWT_SECRET must be at least 32 characters")
    }
    return secret
}
```

**Docker Compose изменения** (`server/docker-compose.yaml`):
```yaml
services:
  user-service:
    environment:
      - JWT_SECRET=${JWT_SECRET}
  transaction-service:
    environment:
      - JWT_SECRET=${JWT_SECRET}
  investment-service:
    environment:
      - JWT_SECRET=${JWT_SECRET}
```

**Создать файлы:**
- `server/.env.example`:
```env
JWT_SECRET=your-super-secret-key-at-least-32-characters-long
POSTGRES_USER=barghest
POSTGRES_PASSWORD=barghest
```
- `server/.env` (добавить в .gitignore):
```env
JWT_SECRET=dev-secret-key-minimum-32-characters-here
POSTGRES_USER=barghest
POSTGRES_PASSWORD=barghest
```

---

### B0.2 Decimal для денежных значений

**Добавить зависимость:**
```bash
cd server/transaction && go get github.com/shopspring/decimal
cd server/investment && go get github.com/shopspring/decimal
```

**Transaction Service:**

Файл: `server/transaction/internal/domain/model/transaction.go`
```go
import "github.com/shopspring/decimal"

type Transaction struct {
    ID          uint            `gorm:"primaryKey"`
    UserID      uint            `gorm:"index;not null"`
    Amount      decimal.Decimal `gorm:"type:decimal(19,4);not null"`
    // ... остальные поля
}
```

**Investment Service:**

Файл: `server/investment/internal/domain/model/model.go`
```go
import "github.com/shopspring/decimal"

type Trade struct {
    // ...
    Quantity decimal.Decimal `gorm:"type:decimal(19,8);not null"`
    Price    decimal.Decimal `gorm:"type:decimal(19,4);not null"`
    Fee      decimal.Decimal `gorm:"type:decimal(19,4);default:0"`
    // ...
}
```

**DTO изменения:**

Файл: `server/transaction/internal/presentation/http/dto/transaction.go`
```go
type CreateTransactionRequest struct {
    Amount      string `json:"amount" binding:"required"`  // String для JSON
    Currency    string `json:"currency" binding:"required,len=3"`
    // ...
}

type TransactionResponse struct {
    ID          uint   `json:"id"`
    Amount      string `json:"amount"`  // String для JSON
    // ...
}
```

**Handler изменения:**
```go
func (h *Handler) CreateTransaction(c *gin.Context) {
    var req dto.CreateTransactionRequest
    if err := c.ShouldBindJSON(&req); err != nil {
        c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
        return
    }
    
    amount, err := decimal.NewFromString(req.Amount)
    if err != nil {
        c.JSON(http.StatusBadRequest, gin.H{"error": "invalid amount format"})
        return
    }
    
    tx := &model.Transaction{
        Amount: amount,
        // ...
    }
    // ...
}
```

---

### B0.3 Унификация UserID

**Файл:** `server/investment/internal/domain/model/model.go`

**Текущий код:**
```go
type Broker struct {
    UserID string `gorm:"index;not null"`
}

type Portfolio struct {
    UserID string `gorm:"index;not null"`
}
```

**Новый код:**
```go
type Broker struct {
    UserID uint `gorm:"index;not null"`
}

type Portfolio struct {
    UserID uint `gorm:"index;not null"`
}
```

**Обновить DTO и handlers соответственно.**

---

### B0.4 HTTP Status Codes

**User Service:**

Файл: `server/user/internal/presentation/http/http.go`
```go
// До
c.JSON(201, gin.H{"users": users})

// После
c.JSON(http.StatusOK, gin.H{"users": users})
```

**Transaction Service:**

Файл: `server/transaction/internal/presentation/http/transactions.go`
```go
// До
c.JSON(201, dto.FromModelList(transactions))

// После
c.JSON(http.StatusOK, dto.FromModelList(transactions))
```

---

### B0.5 Базовая валидация

**Transaction Service:**

Файл: `server/transaction/internal/domain/service/service.go`
```go
import (
    "errors"
    "github.com/shopspring/decimal"
)

var (
    ErrInvalidAmount   = errors.New("amount must be greater than zero")
    ErrInvalidCurrency = errors.New("invalid currency code")
)

func (s *TransactionService) CreateTransaction(tx *model.Transaction) (*model.Transaction, error) {
    // Валидация amount
    if tx.Amount.LessThanOrEqual(decimal.Zero) {
        return nil, ErrInvalidAmount
    }
    
    // Валидация currency
    if len(tx.Currency) != 3 {
        return nil, ErrInvalidCurrency
    }
    
    return s.repo.Create(tx)
}
```

**Investment Service:**

Файл: `server/investment/internal/domain/service/service.go`
```go
func (s *InvestmentService) CreateTrade(...) (*model.Trade, error) {
    // Существующие валидации уже есть, но добавить:
    if fee.LessThan(decimal.Zero) {
        return nil, errors.New("fee cannot be negative")
    }
    // ...
}
```

---

## Android Tasks

### A0.1 Secure Token Storage

**Добавить зависимости:**

Файл: `app/android/app/build.gradle.kts`
```kotlin
dependencies {
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
```

**Создать TokenManager:**

Файл: `app/android/app/src/main/java/com/barghest/bux/data/local/TokenManager.kt`
```kotlin
package com.barghest.bux.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }
    
    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }
    
    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }
    
    fun isLoggedIn(): Boolean = getToken() != null
    
    companion object {
        private const val KEY_TOKEN = "jwt_token"
    }
}
```

---

### A0.2 Dynamic Authorization Header

**Обновить Api.kt:**

Файл: `app/android/app/src/main/java/com/barghest/bux/data/network/Api.kt`
```kotlin
class Api(private val tokenManager: TokenManager) {
    
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(DefaultRequest) {
            contentType(ContentType.Application.Json)
            // Динамический токен
            tokenManager.getToken()?.let { token ->
                header("Authorization", "Bearer $token")
            }
        }
    }
    
    // ... остальной код
}
```

---

### A0.3 Обновить Koin Module

Файл: `app/android/app/src/main/java/com/barghest/bux/di/appModule.kt`
```kotlin
val appModule = module {
    // Token Manager
    single { TokenManager(androidContext()) }
    
    // Api с TokenManager
    single { Api(get()) }
    
    // Repositories
    single<TransactionRepository> { TransactionRepository(get()) }
    single<AuthRepository> { AuthRepository(get(), get()) }  // + TokenManager
    
    // Services
    single { TransactionService(get()) }
    single { AuthService(get()) }
    
    // ViewModels
    viewModel { MainViewModel(get()) }
    viewModel { LoginViewModel(get(), get()) }  // + TokenManager
    viewModel { AddTransactionViewModel(get()) }
}
```

---

### A0.4 Обновить AuthRepository

Файл: `app/android/app/src/main/java/com/barghest/bux/data/repository/AuthRepository.kt`
```kotlin
class AuthRepository(
    private val api: Api,
    private val tokenManager: TokenManager
) {
    suspend fun login(username: String, password: String): Result<User> {
        return try {
            val response = api.login(LoginRequest(username, password))
            response.map { dto ->
                // Сохраняем токен
                tokenManager.saveToken(dto.token)
                dto.user.toDomain()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun logout() {
        tokenManager.clearToken()
    }
    
    fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()
}
```

---

### A0.5 Network Security Config

Файл: `app/android/app/src/main/res/xml/network_security_config.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Production: no cleartext -->
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
    
    <!-- Debug only: allow localhost -->
    <debug-overrides>
        <trust-anchors>
            <certificates src="user" />
        </trust-anchors>
    </debug-overrides>
    
    <!-- Emulator localhost exception -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">localhost</domain>
    </domain-config>
</network-security-config>
```

---

### A0.6 Fix Error Handling

**TransactionRepository:**

Файл: `app/android/app/src/main/java/com/barghest/bux/data/repository/TransactionRepository.kt`
```kotlin
class TransactionRepository(private val api: Api) {
    
    // До: возвращал List с silent failure
    // После: возвращает Result
    suspend fun getAll(): Result<List<Transaction>> {
        return api.fetchTransactions().map { list ->
            list.map { it.toDomain() }
        }
    }
    
    suspend fun add(transaction: NewTransaction): Result<Unit> {
        return api.postTransaction(transaction.toRequest())
    }
}
```

**TransactionService:**

Файл: `app/android/app/src/main/java/com/barghest/bux/domain/service/TransactionService.kt`
```kotlin
class TransactionService(private val repository: TransactionRepository) {
    
    suspend fun getTransactions(): Result<List<Transaction>> {
        return repository.getAll()
    }
    
    suspend fun addTransaction(transaction: NewTransaction): Result<Unit> {
        return repository.add(transaction)
    }
}
```

---

### A0.7 Fix MainViewModel

Файл: `app/android/app/src/main/java/com/barghest/bux/ui/screens/main/MainViewModel.kt`
```kotlin
class MainViewModel(private val service: TransactionService) : ViewModel() {
    
    private val _state = MutableStateFlow<TransactionListState>(TransactionListState.Loading)
    val state: StateFlow<TransactionListState> = _state.asStateFlow()
    
    init {
        refresh()
    }
    
    fun refresh() {
        viewModelScope.launch {
            _state.value = TransactionListState.Loading
            
            service.getTransactions()
                .onSuccess { transactions ->
                    _state.value = if (transactions.isEmpty()) {
                        TransactionListState.Empty
                    } else {
                        TransactionListState.Success(transactions)
                    }
                }
                .onFailure { error ->
                    _state.value = TransactionListState.Error(
                        error.message ?: "Unknown error"
                    )
                }
        }
    }
}

sealed interface TransactionListState {
    data object Loading : TransactionListState
    data class Success(val transactions: List<Transaction>) : TransactionListState
    data class Error(val message: String) : TransactionListState
    data object Empty : TransactionListState
}
```

---

### A0.8 Fix MainScreen

Файл: `app/android/app/src/main/java/com/barghest/bux/ui/screens/main/MainScreen.kt`
```kotlin
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: MainViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Мои финансы") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_transaction") }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val currentState = state) {
                is TransactionListState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is TransactionListState.Empty -> {
                    Text(
                        text = "Пока нет записей",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                is TransactionListState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentState.message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.refresh() }) {
                            Text("Повторить")
                        }
                    }
                }
                is TransactionListState.Success -> {
                    LazyColumn {
                        items(currentState.transactions) { transaction ->
                            TransactionItem(transaction = transaction)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${transaction.amount} RUB",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
```

---

### A0.9 Fix Login Navigation

Файл: `app/android/app/src/main/java/com/barghest/bux/ui/screens/auth/LoginScreen.kt`
```kotlin
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = koinViewModel()
) {
    val state = viewModel.uiState
    
    // Навигация после успешного входа
    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) {
            navController.navigate(Screen.Main.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }
    
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = state.username,
                onValueChange = viewModel::updateUsername,
                label = { Text("Логин") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::updatePassword,
                label = { Text("Пароль") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { viewModel.login() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.loading
            ) {
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Войти")
                }
            }
            
            if (state.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
```

**LoginViewModel:**
```kotlin
class LoginViewModel(
    private val authService: AuthService,
    private val tokenManager: TokenManager
) : ViewModel() {
    
    var uiState by mutableStateOf(LoginUiState())
        private set
    
    fun updateUsername(value: String) {
        uiState = uiState.copy(username = value)
    }
    
    fun updatePassword(value: String) {
        uiState = uiState.copy(password = value)
    }
    
    fun login() {
        viewModelScope.launch {
            uiState = uiState.copy(loading = true, error = null)
            
            authService.login(uiState.username, uiState.password)
                .onSuccess { user ->
                    uiState = uiState.copy(
                        loading = false,
                        isLoggedIn = true
                    )
                }
                .onFailure { e ->
                    uiState = uiState.copy(
                        loading = false,
                        error = e.message ?: "Ошибка входа"
                    )
                }
        }
    }
}

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false
)
```

---

## Checklist

### Backend
- [x] B0.1: JWT secret из environment
- [x] B0.2: Decimal для Amount/Price/Quantity
- [x] B0.3: UserID унификация (uint везде)
- [x] B0.4: Корректные HTTP status codes
- [x] B0.5: Базовая валидация

### Android
- [x] A0.1: TokenManager с EncryptedSharedPreferences
- [x] A0.2: Динамический Authorization header
- [x] A0.3: Обновить Koin module
- [x] A0.4: AuthRepository с сохранением токена
- [x] A0.5: Network security config
- [x] A0.6: Result<T> в repositories
- [x] A0.7: Sealed state в MainViewModel
- [x] A0.8: Loading/Error/Empty states в MainScreen
- [x] A0.9: Навигация после login

---

## Статус: ЗАВЕРШЕНО

**Дата завершения:** 2025-01-24

### Что было сделано:

**Backend:**
1. JWT secret теперь читается из environment variable `JWT_SECRET`
2. Все денежные значения (Amount, Price, Quantity, Fee) используют `decimal.Decimal`
3. UserID унифицирован как `uint` во всех сервисах
4. HTTP status codes исправлены (GET возвращает 200, POST — 201)
5. Добавлена валидация в Transaction и Investment services

**Android:**
1. Создан `TokenManager` с `EncryptedSharedPreferences` для безопасного хранения токенов
2. Authorization header добавляется динамически из TokenManager
3. Koin module обновлен с новыми зависимостями
4. AuthRepository сохраняет токен при успешном логине
5. Network security config запрещает cleartext кроме localhost для разработки
6. Все repositories возвращают `Result<T>` вместо nullable/empty значений
7. MainViewModel использует sealed interface для состояний
8. MainScreen отображает Loading/Error/Empty/Success состояния
9. LoginScreen автоматически переходит на MainScreen после успешного входа

---

## Тестирование Phase 0

### Backend
```bash
# Проверить, что без JWT_SECRET сервис не запускается
unset JWT_SECRET
cd server/user && go run cmd/main.go
# Ожидается: panic

# Проверить с JWT_SECRET
export JWT_SECRET="test-secret-key-minimum-32-chars"
cd server/user && go run cmd/main.go
# Ожидается: успешный запуск
```

### Android
```bash
# Собрать release APK и проверить, что cleartext не работает
./gradlew assembleRelease

# Проверить debug build с localhost
./gradlew assembleDebug
# Ожидается: работает с 10.0.2.2
```

---

## Следующие шаги

После завершения Phase 0 переходим к [Phase 1: Core Domain Model](./phase-1-tasks.md).
