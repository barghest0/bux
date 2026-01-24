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
Phase 1: Core Domain Model         ← СЛЕДУЮЩАЯ
    ↓
Phase 2: Investments & Assets
    ↓
Phase 3: Analytics & Scale
```

## Быстрый старт

1. Прочитать [current-state-analysis.md](./current-state-analysis.md) для понимания текущего состояния
2. Изучить [architecture-evolution-plan.md](./architecture-evolution-plan.md) для общей картины
3. Phase 0 завершена — следующий этап Phase 1

## Phase 0: Критические проблемы (ИСПРАВЛЕНЫ)

| Проблема | Severity | Статус |
|----------|----------|--------|
| Hardcoded JWT secret (backend) | CRITICAL | DONE |
| Hardcoded JWT token (Android) | CRITICAL | DONE |
| float64 для денег | CRITICAL | DONE |
| Cleartext traffic (Android) | HIGH | DONE |
| Silent failures в repositories | MEDIUM | DONE |

## Phase 1: Следующие задачи

| Задача | Приоритет |
|--------|-----------|
| Account модель (центр финансов) | HIGH |
| Расширенная Transaction модель | HIGH |
| Room Database для offline-first | HIGH |
| Sync mechanism | MEDIUM |

## Контакты

Вопросы по архитектуре — создать issue в репозитории.
