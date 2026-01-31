# Phase 4: UX Polish, Smart Features & Data — Plan

10 steps, each independently deployable. Follows existing clean architecture patterns.

## Steps

### Step 1: Settings & Profile Screen
- **Backend (User):** Add `PUT /users/profile`, `PUT /users/password`, email field to User model
- **Android:** SettingsScreen (theme toggle dark/light/system, logout), ProfileEditScreen, PreferencesManager (SharedPreferences)
- Modify: `model.go`, `service.go`, `repository.go` (user service); `Theme.kt`, `MainActivity.kt`, `NavigationGraph.kt`, `Api.kt`, `appModule.kt`

### Step 2: Recurring Transactions
- **Backend (Transaction):** RecurringTransaction model (frequency, next_date, end_date, is_active), CRUD endpoints, scheduler goroutine (auto-creates transactions when due)
- **Android:** RecurringTransactionsScreen, AddRecurringTransactionScreen, Room entity+DAO
- New endpoints: `GET/POST/PUT/DELETE /recurring-transactions`, `POST /recurring-transactions/:id/execute`
- Backend dep: `github.com/robfig/cron/v3`

### Step 3: Transaction Search & Filtering (Android-only)
- Search bar + filter dialog on MainScreen
- Filter by: description text, category, type, date range, amount range
- Local filtering on Room data in MainViewModel

### Step 4: Category Editing (fix TODO)
- **Android:** EditCategoryScreen + EditCategoryViewModel, wire `onCategoryClick` in CategoriesScreen (line 183)
- Backend PUT /categories/:id already exists — just add Android UI
- Prevent editing system categories

### Step 5: Biometric Auth (Android-only)
- BiometricPrompt wrapper service, BiometricAuthScreen
- Settings toggle to enable/disable
- Check on app launch if enabled + logged in
- Dep: `androidx.biometric:biometric-ktx`

### Step 6: Push Notifications
- **Backend:** FCM integration, DeviceToken model, notification triggers (budget overspend, recurring reminder)
- **Android:** FirebaseMessagingService, token registration on login
- New endpoints: `POST/DELETE /notifications/register`
- Deps: Firebase BOM, google-services plugin
- Requires `google-services.json` setup

### Step 7: Spending Insights
- **Backend:** `GET /insights/trends` (month-over-month), `GET /insights/top-categories`
- **Android:** InsightsScreen with charts, InsightsWidget on dashboard
- Aggregation queries in transaction repository

### Step 8: Goal Tracking
- **Backend:** Goal model (target_amount, deadline, linked account_id), CRUD, progress = account balance / target
- **Android:** GoalsScreen, AddGoalScreen, GoalDetailScreen with progress bar
- New endpoints: `GET/POST/PUT/DELETE /goals`

### Step 9: Multi-Currency Support
- **Backend:** Exchange rate API client, ExchangeRate cache model, CurrencyService, daily rate refresh in scheduler
- **Android:** Base currency selector in Settings, converted amounts in analytics
- New endpoints: `GET /currencies/rates`, `GET /currencies/convert`

### Step 10: Data Backup & Import
- **Backend:** `GET /backup/export` (full JSON), `POST /backup/import` (JSON restore), `POST /import/csv`
- **Android:** BackupScreen in Settings, file picker for import, share sheet for export

## Verification
Per step:
1. `docker-compose up -d --build` — backend compiles and starts
2. Test new endpoints with curl
3. `./gradlew assembleDebug` — Android builds
4. Manual UI testing on emulator
