# ICU Patient Chart — Revised Test Plan

## 1. Bug Inventory & Fixes

| ID | Bug Description | Severity | File Fixed | Fix Summary |
|---|---|---|---|---|
| B1 | No feedback after saving vitals | Medium | `NurseDashboardPage.tsx` | Added MUI Snackbar + Alert "Показники збережено" on save; also added missing onClick handler for "Зберегти втрати" button |
| B2 | Empty search shows misleading message | Low | `DashboardPage.tsx` | Differentiated: `cards.length === 0` → "Немає активних пацієнтів" (outside table); search has no matches → "Немає пацієнтів за запитом" (inside tbody) |
| B3 | No client-side validation on vitals | Medium | `NurseDashboardPage.tsx` | Added `type="number"` + `slotProps.htmlInput.min/max` on all 7 vitals fields (SYS 60-300, DIA 30-200, HR 20-300, SpO2 0-100, Temp 30-45, CVP 0-50, RR 4-80) |
| B4 | Missing aria-labels on combobox | Low | `NurseDashboardPage.tsx` | Added `slotProps.select.inputProps['aria-label']` on patient Select and stool Select |
| B5 | Empty-state row inside tbody inflates tr count | Low | `DashboardPage.tsx` | Moved empty-state <Alert> outside <Table> when no cards; search-empty still uses <TableRow> with different text |
| B6 | No page title / document.title set | Low | All pages | Added `useEffect(() => { document.title = 'ВАІТ — <Role>' }, [])` in each page |

## 2. Test Pyramid

```
         ╱  E2E (Playwright)  ╲         ← 12 new tests + 43 existing = 55 total
        ╱   Integration (Backend) ╲      ← 3 @SpringBootTest tests (existing)
       ╱    Unit (Frontend Vitest)  ╲   ← 12 new tests + 23 existing = 35 total
      ╱     Unit (Backend JUnit 5)    ╲  ← ~150 tests (existing)
     ╱══════════════════════════════════╲
```

### 2.1 Frontend Unit Tests (Vitest) — 35 tests

| File | Tests | What it validates |
|---|---|---|
| `LoginPage.test.tsx` | 3 | Login form renders, error on failure, credentials passed |
| `AuthContext.test.tsx` | 6 | Auth state, token persistence, login/logout, role checks, provider guard |
| `endpoints.test.ts` | 14 | All API endpoint paths + params |
| **`DashboardPage.test.tsx`** | **6** | Loading spinner, 3 cards render, search filters, empty-vs-search-empty messages, clear search restores |
| **`NurseDashboardPage.test.tsx`** | **6** | Loading spinner, empty state, vitals form renders, validation attributes (type, min, max), document.title, Snackbar after save |

### 2.2 Backend Unit Tests (JUnit 5) — ~150 tests

Existing 27 test files covering services (12), controllers (6), repositories (3), integration (3), auth/security (3). No changes needed.

### 2.3 Playwright E2E Tests — 55 tests (43 existing + 12 new)

| Project | Spec File | Tests (new) | What validates |
|---|---|---|---|
| `login-chromium` | `auth/login.spec.ts` | +1 | Login page title is "ВАІТ — Вхід" |
| `doctor-chromium` | `doctor/dashboard.spec.ts` | +3 | Search shows "Немає пацієнтів за запитом"; clear search restores cards; page title "ВАІТ — Лікар" |
| `nurse-chromium` | `nurse/dashboard.spec.ts` | +2 | Patient select has aria-label="Пацієнт"; page title "ВАІТ — Медсестра" |
| `nurse-chromium` | `nurse/vitals.spec.ts` | +2 | Snackbar "Показники збережено" after save; vitals fields have `type=number`, `min`, `max` |
| `admin-chromium` | `admin/admin.spec.ts` | +1 | Page title "ВАІТ — Адміністратор" |
| `api-chromium` | `api/patients.spec.ts` | — | (no change) |
| `exploratory-chromium` | `exploratory/*` | — | (no change, config made CI-compatible) |

### 2.4 New E2E Test Coverage Details

#### Vitals save feedback (nurse/vitals.spec.ts)

```
save vitals → Snackbar("Показники збережено") appears
```

**Validates B1 fix:** User now sees confirmation after saving.

#### HTML5 validation attributes (nurse/vitals.spec.ts)

```
vitals fields → have type="number", min, max attributes
```

**Validates B3 fix:** Browser enforces range constraints.

#### Aria-label on combobox (nurse/dashboard.spec.ts)

```
patient select → aria-label == "Пацієнт"
```

**Validates B4 fix:** Screen readers can identify the combobox.

#### Search message differentiation (doctor/dashboard.spec.ts)

```
search "NonExistent" → "Немає пацієнтів за запитом"
clear search → all 3 cards visible
```

**Validates B2 fix:** User knows search returned no results vs no cards exist.

#### Document titles (all role specs)

```
/doctor → title "ВАІТ — Лікар"
/nurse → title "ВАІТ — Медсестра"
/admin → title "ВАІТ — Адміністратор"
/login → title "ВАІТ — Вхід"
```

**Validates B6 fix:** Each page sets descriptive tab title.

## 3. CI/CD Pipeline

### Workflow: `.github/workflows/playwright.yml`

```
Push → Checkout → Setup JDK 17 → Setup Node 22
     → mvn clean package (backend build + tests)
     → Start backend JAR (:8085)
     → npm ci (frontend deps)
     → npm run lint (frontend lint — NEW)
     → npm test (frontend unit tests — NEW)
     → npm run build (frontend production)
     → npx vite preview --port 5173
     → npx playwright install chromium
     → npx playwright test
     → Upload artifacts (playwright-report, test-results)
```

**Added steps:** Frontend lint (fast fail) and frontend Vitest unit tests execute before frontend build, catching regressions earlier.

### Project Dependencies (Playwright config)

```
setup (authenticates 4 roles)
  ├── doctor-chromium ← depends on setup
  ├── nurse-chromium  ← depends on setup
  ├── hod-chromium    ← depends on setup
  └── admin-chromium  ← depends on setup
login-chromium (no auth needed)
api-chromium (no auth needed)
exploratory-chromium (no auth, CI-compatible config)
```

## 4. Risk Register

| Risk | Level | Mitigation |
|---|---|---|
| Parallel test interference | 🟢 Low | `fullyParallel: true` but specific role-based locators (getByRole, getByLabel, row filters) avoid `.first()` collisions |
| Snackbar auto-hide (3s) | 🟡 Medium | Playwright assertions use `{ timeout: 5000 }` and `waitFor` which wait for element visibility |
| Vitals persistence in CI | 🟢 Low | Seed data creates fresh DB per workflow run; @Transactional rollback on integration tests |
| Fluid balance non-determinism | 🟡 Medium | Balance-dependent tests use seed data (Петренко); no cross-test state dependencies |
| CI timeout (40 min) | 🟢 Low | E2E suite ~8 min; backend tests ~3 min; frontend tests ~30s |
| Exploratory tests in CI | 🟢 Low | Changed `headless` from `false` to default (headless in CI); retries: 2; timeout: 300s |

## 5. Expected Outcomes

### After all fixes and tests pass in CI:

1. **Nurse saves vitals** → hears "Показники збережено" via Snackbar ✅
2. **Doctor searches non-existent patient** → sees "Немає пацієнтів за запитом" (not the generic empty message) ✅
3. **Nurse enters BP 300/200** → browser rejects with HTML5 validation (max=300, max=200) ✅
4. **Screen reader focuses patient select** → hears "Пацієнт" via aria-label ✅
5. **`tbody tr` count when table is empty** → 0 (empty-state rendered outside `<table>`) ✅
6. **Browser tabs show meaningful titles** → "ВАІТ — Лікар", "ВАІТ — Медсестра", etc. ✅
7. **CI fails fast** → frontend lint + unit tests before build, catching type errors and logic regressions early ✅

### Test Count Summary

| Layer | Before | After | New |
|---|---|---|---|
| Frontend unit (Vitest) | 23 | 35 | 12 |
| Backend (JUnit 5) | ~150 | ~150 | 0 |
| Playwright E2E | 43 | 55 | 12 |
| **Total** | **~216** | **~240** | **24** |
