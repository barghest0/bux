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

#### Android ⚠️ ЧАСТИЧНО

**Готово:**
- `data/dto/Budget.kt` — BudgetResponse, CreateBudgetRequest, UpdateBudgetRequest, BudgetStatusResponse
- `domain/model/Budget.kt` — Budget, BudgetStatus, BudgetPeriod
- `data/mapper/BudgetMapper.kt` — маппинг DTO → Domain
- `data/network/Api.kt` — fetchBudgets(), fetchBudgetStatus(), createBudget(), deleteBudget()
- `data/repository/BudgetRepository.kt` — BudgetRepository
- `ui/screens/budgets/BudgetsViewModel.kt` — BudgetsViewModel
- `ui/screens/budgets/BudgetsScreen.kt` — список бюджетов с progress bar (потрачено/лимит)

**НЕ СДЕЛАНО:**
- `ui/screens/budgets/AddBudgetScreen.kt` — экран создания бюджета (выбор категории, ввод суммы, период)
- `ui/screens/budgets/AddBudgetViewModel.kt`
- Регистрация BudgetRepository и BudgetsViewModel в `di/appModule.kt`
- Добавление `Screen.Budgets` и `Screen.AddBudget` в `NavigationGraph.kt`
- Навигация к бюджетам с Analytics Dashboard

---

### Step 7: Backend — Pagination ❌ НЕ НАЧАТО

**Описание:** Добавить `page` и `page_size` query params ко всем list endpoint'ам во всех 3 сервисах.

**Что нужно сделать:**
- Создать generic `PaginatedResponse[T]` struct
- Обновить все repository Get* методы — добавить LIMIT/OFFSET
- Обновить все list handlers — парсить query params
- Defaults: page=1, page_size=50. Обратно совместимо.

**Файлы (все 3 сервиса):**
- `repository.go` / `account_repository.go` / `category_repository.go` / `budget_repository.go`
- Все HTTP handlers с GET list

---

### Step 8: Backend + Android — CSV Export ❌ НЕ НАЧАТО

**Описание:** Экспорт транзакций в CSV файл.

**Backend:**
- `server/services/transaction/internal/presentation/http/export.go`
- `GET /transactions/export?format=csv&from=&to=` → Content-Type: text/csv

**Android:**
- Кнопка "Export" на экране транзакций
- Скачивание файла + Android share sheet

---

## Сводка

| Шаг | Описание | Статус |
|-----|----------|--------|
| 1 | Backend: Transaction Summary endpoint | ✅ DONE |
| 2 | Android: Vico charts + domain models | ✅ DONE |
| 3 | Android: AnalyticsRepository + API | ✅ DONE |
| 4 | Android: Analytics Dashboard Screen | ✅ DONE |
| 5 | Android: Net Worth Screen | ✅ DONE |
| 6 | Backend + Android: Budget Tracking | ⚠️ PARTIAL (backend done, Android needs AddBudget screen + DI/Nav wiring) |
| 7 | Backend: Pagination | ❌ NOT STARTED |
| 8 | Backend + Android: CSV Export | ❌ NOT STARTED |

---

## Что осталось доделать (в порядке приоритета)

### 1. Довести Step 6 до конца
- Создать `AddBudgetScreen.kt` и `AddBudgetViewModel.kt`
- Зарегистрировать `BudgetRepository`, `BudgetsViewModel`, `AddBudgetViewModel` в `appModule.kt`
- Добавить `Screen.Budgets`, `Screen.AddBudget` в `NavigationGraph.kt`
- Добавить кнопку навигации к бюджетам на Analytics Dashboard

### 2. Step 7: Pagination
- Generic PaginatedResponse
- Обновить все list endpoints (3 сервиса)
- Обновить Android для поддержки pagination (опционально для MVP)

### 3. Step 8: CSV Export
- Backend endpoint
- Android: download + share

---

## Тестирование

1. `cd server/services/transaction && go build ./...` — ✅ компилируется
2. `docker-compose up -d --build` — проверить что backend стартует
3. `GET /analytics/summary` — проверить корректные суммы
4. `POST /budgets` + `GET /budgets/status` — проверить бюджеты
5. Android: после логина открывается Analytics Dashboard
6. Android: Net Worth screen показывает breakdown
7. `./gradlew build` — проверить что Android собирается (требуется JDK)
