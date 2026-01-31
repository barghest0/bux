# Phase 3: Analytics & Scale — Детальные задачи

> Приоритет: HIGH  
> Цель: Аналитика, Net Worth, бюджеты, пагинация, экспорт

---

## Обзор

Phase 3 фокусируется на:

1. **Analytics Dashboard** — главный экран с net worth, доходами/расходами, графиками
2. **Net Worth** — агрегация всех активов пользователя
3. **Budget Tracking** — бюджеты по категориям с отслеживанием расходов
4. **Pagination** — пагинация в списковых endpoint'ах
5. **CSV Export** — экспорт транзакций

---

## Архитектурные решения

1. **Net Worth считается на Android** — агрегируем данные из существующих API (account balances + portfolio summaries). Новый backend-сервис не нужен.
2. **Transaction Summary — новый endpoint** на backend (`GET /analytics/summary`) для агрегации income/expense по категориям и месяцам.
3. **Charts: Vico 2.1.2** — Compose-native библиотека для графиков (`com.patrykandpatrick.vico:compose-m3`).
4. **Валюты**: MVP без конвертации — показываем суммы сгруппированные по валютам.
5. **Budget** — новая модель в transaction service.

---

## Статус задач

### Step 1: Backend — Transaction Summary endpoint ✅ DONE

**Новые файлы:**
- `server/services/transaction/internal/presentation/http/analytics.go` — HTTP handler для `GET /analytics/summary`
- `server/services/transaction/internal/presentation/http/dto/analytics.go` — DTO (CategorySummary, MonthlySummary, TransactionSummaryResponse)
- `server/services/transaction/internal/domain/service/analytics_service.go` — AnalyticsService с агрегацией

**Изменённые файлы:**
- `server/services/transaction/internal/data/repository/repository.go` — добавлены `GetSummaryByCategory()` и `GetSummaryByMonth()` (SQL агрегации с GROUP BY)
- `server/services/transaction/cmd/main.go` — зарегистрирован AnalyticsService и route

**API:**
```
GET /analytics/summary?from=2025-01-01&to=2025-12-31
→ {
    "total_income": "150000",
    "total_expense": "80000", 
    "net": "70000",
    "by_category": [{ category_id, category_name, category_icon, category_color, type, total, count }],
    "by_month": [{ year, month, income, expense, net }]
  }
```

---

### Step 2: Android — Vico charts + domain models ✅ DONE

**Изменённые файлы:**
- `app/android/gradle/libs.versions.toml` — добавлен `vico = "2.1.2"` и `vico-compose-m3` library
- `app/android/app/build.gradle.kts` — добавлен `implementation(libs.vico.compose.m3)`

**Новые файлы:**
- `domain/model/Analytics.kt` — TransactionSummary, CategorySummary, MonthlySummary, AssetGroup, NetWorthData
- `data/dto/Analytics.kt` — CategorySummaryResponse, MonthlySummaryResponse, TransactionSummaryResponse
- `data/mapper/AnalyticsMapper.kt` — маппинг DTO → Domain

---

### Step 3: Android — AnalyticsRepository + API calls ✅ DONE

**Изменённые файлы:**
- `data/network/Api.kt` — добавлен `fetchTransactionSummary(from, to)`
- `di/appModule.kt` — зарегистрирован AnalyticsRepository и новые ViewModels

**Новые файлы:**
- `data/repository/AnalyticsRepository.kt` — агрегирует net worth из AccountRepository + InvestmentRepository, получает summary с backend

---

### Step 4: Android — Analytics Dashboard Screen ✅ DONE

**Новые файлы:**
- `ui/screens/analytics/AnalyticsDashboardScreen.kt` — главный экран после логина
- `ui/screens/analytics/AnalyticsDashboardViewModel.kt`

**Изменённые файлы:**
- `ui/application/navigation/NavigationGraph.kt` — добавлены `Screen.Analytics` и `Screen.NetWorth`
- `ui/screens/auth/LoginScreen.kt` — после логина переход на `Screen.Analytics` вместо `Screen.Main`

**Содержимое экрана:**
- Карточка Net Worth (сумма по валютам, инвестиции отдельно)
- Income/Expense карточка (доходы, расходы, баланс)
- Vico bar chart по месяцам (income vs expense)
- Quick actions (категории, портфели)
- Горизонтальная лента счетов
- Последние 5 транзакций

---

### Step 5: Android — Net Worth Screen ✅ DONE

**Новые файлы:**
- `ui/screens/analytics/NetWorthScreen.kt` — детальный breakdown чистых активов
- `ui/screens/analytics/NetWorthViewModel.kt`

**Содержимое:**
- Общая стоимость по валютам
- Группировка по типам активов (банковские счета, карты, наличные, крипто, инвестиции, недвижимость)
- Каждая группа: иконка, название, количество счетов, сумма, процент от общего
- Внутри группы — список отдельных счетов (если > 1)
- Инвестиционные портфели (рыночная стоимость)
- Pull-to-refresh

---

### Step 6: Backend + Android — Budget Tracking ⚠️ В ПРОЦЕССЕ

#### Backend ✅ DONE

**Новые файлы:**
- `server/services/transaction/internal/domain/model/budget.go` — Budget модель (UserID, CategoryID, Amount, Currency, Period)
- `server/services/transaction/internal/data/repository/budget_repository.go` — CRUD + `GetBudgetStatus()` (SQL JOIN с transactions для расчёта потраченного)
- `server/services/transaction/internal/domain/service/budget_service.go` — BudgetService с валидацией и расчётом статуса
- `server/services/transaction/internal/presentation/http/budgets.go` — HTTP handlers
- `server/services/transaction/internal/presentation/http/dto/budget.go` — DTO

**Изменённые файлы:**
- `server/services/transaction/cmd/main.go` — зарегистрированы BudgetRepository, BudgetService, BudgetHTTP
- `server/services/transaction/internal/infra/db/migration.go` — добавлена миграция Budget

**API:**
```
POST   /budgets           — создать бюджет
GET    /budgets           — список бюджетов пользователя
PUT    /budgets/:id       — обновить бюджет
DELETE /budgets/:id       — удалить бюджет
GET    /budgets/status    — бюджеты с расчётом потраченного (budget_amount, spent_amount, remaining, spent_percent)
```

#### Android ✅ DONE

**Файлы:**
- `data/dto/Budget.kt` — BudgetResponse, CreateBudgetRequest, UpdateBudgetRequest, BudgetStatusResponse
- `domain/model/Budget.kt` — Budget, BudgetStatus, BudgetPeriod
- `data/mapper/BudgetMapper.kt` — маппинг DTO → Domain
- `data/network/Api.kt` — fetchBudgets(), fetchBudgetStatus(), createBudget(), deleteBudget()
- `data/repository/BudgetRepository.kt` — BudgetRepository
- `ui/screens/budgets/BudgetsViewModel.kt` — BudgetsViewModel
- `ui/screens/budgets/BudgetsScreen.kt` — список бюджетов с progress bar (потрачено/лимит)
- `ui/screens/budgets/AddBudgetScreen.kt` — экран создания бюджета (выбор категории, ввод суммы, период, валюта)
- `ui/screens/budgets/AddBudgetViewModel.kt` — ViewModel с валидацией
- `di/appModule.kt` — зарегистрированы BudgetRepository, BudgetsViewModel, AddBudgetViewModel
- `NavigationGraph.kt` — добавлены Screen.Budgets и Screen.AddBudget
- `AnalyticsDashboardScreen.kt` — кнопка навигации к бюджетам в Quick Actions

---

### Step 7: Backend — Pagination ✅ DONE

**Описание:** Добавлена пагинация для ключевых list endpoints (transactions, trades). Обратно совместимо — без `page` параметра возвращается полный список.

**Новые файлы:**
- `server/services/transaction/internal/presentation/http/dto/pagination.go` — PaginationParams, PaginatedResponse[T], ParsePagination()
- `server/services/investment/internal/presentation/http/dto/pagination.go` — аналогично

**Изменённые файлы:**
- `server/services/transaction/internal/data/repository/repository.go` — GetByUserIDPaginated(), GetByAccountIDPaginated()
- `server/services/transaction/internal/domain/service/service.go` — GetTransactionsByUserPaginated(), GetTransactionsByAccountPaginated()
- `server/services/transaction/internal/presentation/http/transactions.go` — поддержка `?page=&page_size=`
- `server/services/investment/internal/data/repository/repository.go` — GetTradesByPortfolioIDPaginated()
- `server/services/investment/internal/domain/service/service.go` — GetTradesPaginated()
- `server/services/investment/internal/presentation/http/http.go` — поддержка пагинации в GetTrades

**API:**
```
GET /transactions?page=1&page_size=50 → { data: [...], page, page_size, total_count, total_pages }
GET /api/portfolios/:id/trades?page=1&page_size=50 → аналогично
```
Defaults: page=1, page_size=50, max=200.

---

### Step 8: Backend + Android — CSV Export ✅ DONE

**Backend:**

**Новые файлы:**
- `server/services/transaction/internal/presentation/http/export.go` — ExportHTTP handler

**Изменённые файлы:**
- `server/services/transaction/cmd/main.go` — зарегистрирован ExportHTTP

**API:**
```
GET /transactions/export?from=2025-01-01&to=2025-12-31 → Content-Type: text/csv
```

**Android:**

**Изменённые файлы:**
- `data/network/Api.kt` — exportTransactionsCSV()
- `data/repository/TransactionRepository.kt` — exportCSV()
- `domain/service/TransactionService.kt` — exportCSV()
- `ui/screens/main/MainViewModel.kt` — exportCSV(context), ExportState
- `ui/screens/main/MainScreen.kt` — кнопка экспорта в top bar + Android share sheet
- `AndroidManifest.xml` — FileProvider для share

**Новые файлы:**
- `res/xml/file_paths.xml` — конфигурация FileProvider

---

## Сводка

| Шаг | Описание | Статус |
|-----|----------|--------|
| 1 | Backend: Transaction Summary endpoint | ✅ DONE |
| 2 | Android: Vico charts + domain models | ✅ DONE |
| 3 | Android: AnalyticsRepository + API | ✅ DONE |
| 4 | Android: Analytics Dashboard Screen | ✅ DONE |
| 5 | Android: Net Worth Screen | ✅ DONE |
| 6 | Backend + Android: Budget Tracking | ✅ DONE |
| 7 | Backend: Pagination | ✅ DONE |
| 8 | Backend + Android: CSV Export | ✅ DONE |

---

## Что осталось доделать

Phase 3 полностью завершена. Все 8 шагов реализованы.

---

## Тестирование

1. `cd server/services/transaction && go build ./...` — ✅ компилируется
2. `docker-compose up -d --build` — проверить что backend стартует
3. `GET /analytics/summary` — проверить корректные суммы
4. `POST /budgets` + `GET /budgets/status` — проверить бюджеты
5. Android: после логина открывается Analytics Dashboard
6. Android: Net Worth screen показывает breakdown
7. `./gradlew build` — проверить что Android собирается (требуется JDK)
