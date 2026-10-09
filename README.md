# SmartMess — Multi-Tenant SaaS & Offline-First Android System

> **Stack:** Android Native (Pure Java) + SQLite + WorkManager + OkHttp/Gson + Material Design 3  
> **Backend Integration:** Laravel 11+ & Inertia.js (`https://mess.e-bd.shop/api/v1`)  
> **Architecture:** Clean Layered Architecture (Domain Models, SQLite DAOs, Dual Pool Expense Engine, In-App SIM SMS Auto-Costing, Two-Way Background Sync Engine)

---

## 1. System Overview & Core Capabilities

SmartMess is an enterprise-grade, offline-first mess (hostel/shared bachelor living) meal and accounting management system. It solves complex shared-living financial friction with mathematically rigorous accounting engines, automated SIM SMS ledger impact, and two-way sync with Laravel cloud multi-tenancy.

### Key Highlights
1. **Multi-Tenancy Isolation:** Each mess is an isolated tenant identified by unique UUID / invite code (`messes` table).
2. **Dual Expense Engine (Dual Pool Financial Model):**
   - **Variable (Meal) Pool:** Fish, meat, vegetables, eggs factored strictly into live dynamic meal rates:
     $$\text{Live Meal Rate} = \frac{\text{Total Variable Meal Expenses}}{\text{Total Consumed Meals}}$$
   - **Shared (Fixed) Food Pool:** Oil, onion, salt, spices, cooking gas split equally across all active members.
   - **Asset & Utility Pool:** Room rent, Wi-Fi, maid/cook salary, cleaning assets split equally without skewing food meal rates.
   - **Individual Member Debits:** Direct charges (fines, personal guest costs, single-member SMS reminders) debited strictly to that member's ledger.
3. **In-App SIM SMS & Automatic Ledger Costing:**
   - Background SMS dispatched directly through the device's SIM card using Android's `SmsManager`.
   - **Single Due Reminder:** Automatically debits the recipient member's ledger (individual expense) with the configured `per_sms_cost` (e.g. ৳ 0.50) and credits the sender's account with a deposit reimbursement.
   - **Broadcast Notice:** Automatically splits the total SMS cost ($N \times \text{per\_sms\_cost}$) equally among all mess members.
4. **Offline-First SQLite Architecture:**
   - All CRUD operations occur locally in SQLite with zero latency and full offline resilience.
   - Every record is assigned a UUID (`UUID.randomUUID().toString()`) and tracked with `sync_status` (0 = pending, 1 = synced).
5. **Two-Way Background Sync Engine:**
   - **Push:** Automatically batches offline pending records and uploads to `POST /api/v1/sync/push`.
   - **Pull:** Fetches incremental delta updates since `last_sync_timestamp` from `GET /api/v1/sync/pull` and upserts them locally.
   - Periodic synchronization scheduled via AndroidX `WorkManager` (every 15 minutes when connected to network) + manual "Sync Now" trigger.
6. **Role-Based Access Control (RBAC):**
   - **Manager:** Full access (log costs, edit meals, add members, adjust cutoff times, trigger cycle settlements).
   - **Assistant Manager / Bazar Boy:** Can log market purchases and attach voucher receipts.
   - **General Member:** View-only dashboard and daily meal on/off toggles before the cutoff time.
7. **Meal Cut-Off Rule Enforcement:**
   - Configurable daily cutoff time (e.g., `22:00:00` / 10:00 PM).
   - Once the clock reaches cutoff time, tomorrow's meals are locked (`is_locked = 1`) and cannot be altered by general members.
8. **Voucher Media Engine:**
   - Voucher bills are downscaled to a maximum width of 1080px and compressed to WebP at 75% quality for ultra-compact local caching and cloud upload.
9. **SaaS Dynamic Plan & Feature Gating:**
   - Feature flags (`max_members`, `sms_sim`, `ocr_receipt`, `pdf_branding`, `ad_free`) enforced via `PlanGateManager`.

---

## 2. Mathematical Accounting Engine Formulas

Let $M$ be the set of active mess members, with count $|M|$.
Let $C$ be the active accounting cycle date range $[D_{\text{start}}, D_{\text{end}}]$.

1. **Total Mess Consumed Meals ($T_{\text{meals}}$):**
   $$T_{\text{meals}} = \sum_{u \in M} (\text{breakfast}_u + \text{lunch}_u + \text{dinner}_u + \text{guest\_meals}_u)$$

2. **Dynamic Meal Rate ($R_{\text{meal}}$):**
   $$R_{\text{meal}} = \begin{cases} \frac{\sum E_{\text{raw\_meal}}}{T_{\text{meals}}} & \text{if } T_{\text{meals}} > 0 \\ 0 & \text{otherwise} \end{cases}$$

3. **Per-Member Shared Fixed Cost ($S_{\text{fixed}}$):**
   $$S_{\text{fixed}} = \frac{\sum E_{\text{shared\_food}}}{|M|}$$

4. **Per-Member Utility & Shared SMS Cost ($S_{\text{util}}$):**
   $$S_{\text{util}} = \frac{\sum E_{\text{utility\_asset}} + \sum E_{\text{sms\_broadcast}}}{|M|}$$

5. **Individual Total Cost for Member $u$ ($C_u$):**
   $$C_u = (\text{meals}_u \times R_{\text{meal}}) + S_{\text{fixed}} + S_{\text{util}} + \sum E_{\text{individual}, u}$$

6. **Member Net Balance ($B_u$):**
   $$B_u = \sum \text{Deposits}_u - C_u$$
   - If $B_u \ge 0$: **Refundable / Credit Balance**
   - If $B_u < 0$: **Due / Payable Balance** ($|B_u|$)

7. **Mess Fund / Cash in Hand ($H$):**
   $$H = \sum_{\text{all}} \text{Deposits} - \sum_{\text{all}} \text{Expenses}$$

---

## 3. Project Structure

```
mess-android/
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/
│       │   ├── values/ (colors.xml, strings.xml, themes.xml)
│       │   ├── drawable/ (icons, badges, cards, gradients)
│       │   ├── layout/ (14 XML activities & list items)
│       │   └── menu/ (menu_dashboard.xml)
│       └── java/com/smartmess/android/
│           ├── SmartMessApp.java
│           ├── data/
│           │   ├── local/
│           │   │   ├── DatabaseHelper.java
│           │   │   ├── SQLiteContract.java
│           │   │   └── dao/ (MessDao, UserDao, MealDao, ExpenseDao, DepositDao, SmsLogDao, SaasPlanDao)
│           │   ├── remote/
│           │   │   ├── ApiConfig.java
│           │   │   ├── ApiClient.java
│           │   │   └── dto/ (Login, Register, SyncPush, SyncPull)
│           │   └── sync/
│           │       ├── SyncManager.java
│           │       └── SyncWorker.java
│           ├── engine/
│           │   ├── AccountingEngine.java
│           │   ├── CalculationModels.java
│           │   ├── PlanGateManager.java
│           │   └── VoucherManager.java
│           ├── sms/
│           │   ├── SmsDispatcher.java
│           │   └── SmsCostingManager.java
│           ├── model/
│           │   ├── Mess.java, User.java, Meal.java, Expense.java, Deposit.java, SmsLog.java, SaasPlan.java
│           ├── ui/
│           │   ├── auth/ (SplashActivity, LoginActivity, RegisterActivity)
│           │   ├── dashboard/ (DashboardActivity)
│           │   ├── meals/ (MealListActivity, AddMealActivity, DailyMealToggleActivity, MealAdapter)
│           │   ├── expenses/ (ExpenseListActivity, AddExpenseActivity, ExpenseAdapter)
│           │   ├── deposits/ (DepositListActivity, AddDepositActivity, DepositAdapter)
│           │   ├── members/ (MemberListActivity, AddMemberActivity, MemberAdapter)
│           │   ├── sms/ (DueReminderActivity, SmsBroadcastActivity)
│           │   ├── reports/ (SummaryReportActivity, MemberSummaryAdapter)
│           │   └── settings/ (SettingsActivity)
│           └── utils/
│               ├── SessionManager.java
│               ├── DateTimeUtils.java
│               ├── CurrencyUtils.java
│               └── PermissionHelper.java
├── build.gradle
├── settings.gradle
├── gradle.properties
└── gradlew.bat
```

---

## 4. How to Open and Run in Android Studio

1. **Launch Android Studio**.
2. Select **Open** and choose the `mess-android` directory (`c:\Users\Admin\Documents\codester\mess-android`).
3. Android Studio will automatically resolve Gradle dependencies:
   - `androidx.appcompat`, `material:1.11.0`, `okhttp:4.12.0`, `gson:2.10.1`, `work-runtime:2.9.0`, `glide:4.16.0`.
4. Connect an Android device (or launch an Emulator running Android API 21+).
5. Click **Run** (`Shift + F10`).
6. The app comes pre-seeded with sample offline demo data (Manager, Bazar Boy, Member, dual-pool expenses, and meal counts) so it is instantly operational without needing an initial cloud connection!
