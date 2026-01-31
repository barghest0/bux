# BUX Architecture Plans

Документация по архитектуре и планам развития проекта.

## Документы

| Файл | Описание |
|------|----------|
| [current-state-analysis.md](./current-state-analysis.md) | Анализ текущего состояния кодовой базы |
| [architecture-evolution-plan.md](./architecture-evolution-plan.md) | Полный план эволюционного развития |
| [phase-0-tasks.md](./phase-0-tasks.md) | Детальные задачи Phase 0 (Security & Foundation) |

## Фазы развития

```
Phase 0: Security & Foundation     ✅ ЗАВЕРШЕНО (2025-01-24)
    ↓
Phase 1: Core Domain Model         ✅ ЗАВЕРШЕНО (2026-01-24)
    ↓
Phase 2: Investments & Assets      ✅ ЗАВЕРШЕНО (2026-01-24)
    ↓
Phase 3: Analytics & Scale         ✅ ЗАВЕРШЕНО (2026-01-31)
```

## Быстрый старт

1. Прочитать [current-state-analysis.md](./current-state-analysis.md) для понимания текущего состояния
2. Изучить [architecture-evolution-plan.md](./architecture-evolution-plan.md) для общей картины
3. Phase 0 и Phase 1 завершены — следующий этап Phase 2

## Phase 0: Критические проблемы (ИСПРАВЛЕНЫ)

| Проблема | Severity | Статус |
|----------|----------|--------|
| Hardcoded JWT secret (backend) | CRITICAL | DONE |
| Hardcoded JWT token (Android) | CRITICAL | DONE |
| float64 для денег | CRITICAL | DONE |
| Cleartext traffic (Android) | HIGH | DONE |
| Silent failures в repositories | MEDIUM | DONE |

## Phase 1: Core Domain Model (ЗАВЕРШЕНО)

| Задача | Статус |
|--------|--------|
| Account модель (backend) | DONE |
| Account repository, service, handlers | DONE |
| Расширенная Transaction модель (Type, Status, AccountID) | DONE |
| Transaction с обновлением балансов | DONE |
| Room Database (Android) | DONE |
| Account/Transaction entities и DAOs | DONE |
| Accounts list screen | DONE |
| Add Account screen | DONE |
| Обновленный MainScreen с счетами | DONE |
| AddTransaction с выбором счета | DONE |

## Phase 2: Categories, Investments & Sync (ЗАВЕРШЕНО)

| Задача | Статус |
|--------|--------|
| Category модель (backend) | DONE |
| Category repository, service, handlers | DONE |
| Default categories seeder | DONE |
| Holding model с P&L расчетами | DONE |
| PriceHistory model | DONE |
| Portfolio value endpoint | DONE |
| CategoryEntity и CategoryDao (Android) | DONE |
| CategoriesScreen и AddCategoryScreen | DONE |
| Update AddTransactionScreen с категориями | DONE |
| SyncManager и SyncWorker | DONE |
| Pull-to-refresh на экранах | DONE |
| PortfoliosScreen | DONE |
| PortfolioDetailScreen с holdings | DONE |
| AddTradeScreen | DONE |

## Phase 3: Analytics & Scale (ЗАВЕРШЕНО)

| Задача | Статус |
|--------|--------|
| Backend: Transaction Summary endpoint | DONE |
| Android: Vico charts + domain models | DONE |
| Android: AnalyticsRepository + API | DONE |
| Android: Analytics Dashboard Screen | DONE |
| Android: Net Worth Screen | DONE |
| Backend + Android: Budget Tracking | DONE |
| Backend: Pagination (transactions, trades) | DONE |
| Backend + Android: CSV Export | DONE |

## Контакты

Вопросы по архитектуре — создать issue в репозитории.
