# E2E Test Suite Improvement Plan

Date: 2026-08-20
Scope: `tests/` — Playwright 1.61 suite (59 spec files, 268 tests, 11 projects).
Bar: deterministic waits (no `waitForTimeout`, no `networkidle`), every test owns its
data, role-based selectors, failures debuggable from artifacts (trace/screenshot are
already configured: `trace: on-first-retry`, `screenshot: only-on-failure`).

---

## 1. Audit findings

### 1.1 Orphaned spec — `auth/access-control.spec.ts` never runs (P0)

The `login-chromium` project only matches `**/auth/login.spec.ts` and
`**/auth/logout.spec.ts`. `access-control.spec.ts` matches **no project**, so its 4 RBAC
tests are silently skipped in CI. Verified by reading `playwright.config.ts` — no project
has a `testMatch`/`testIgnore` that collects the file.

The tests are also broken as written:

| Test | Why it would fail if collected |
|---|---|
| `admin redirected from /nurse` | `goto('/icu/nurse')` without auth → `Guard` sends unauthenticated visitors to `/login` (App.tsx:70). Even with auth, the guard redirects to `/` → `RoleRedirect` → `/select` (App.tsx:76 + 116), **not** `/admin` as asserted |
| `admin redirected from /doctor` | Same as above |
| `doctor can access /create-card` | No storageState → redirected to `/login`, «Нова карта інтенсивної терапії» never renders |
| `nurse can access /episode/:id` | No storageState → `/login`; «Відкрити» button cannot exist; also clicked `.first()` on a shared dashboard table |

**Fix (implemented)**: rewrite the spec as a per-role storageState RBAC matrix test
(admin/doctor/nurse × allowed route + denied route), wire it into `login-chromium` and
give that project `dependencies: ['setup']` so `.auth/*.json` files exist before it runs.

### 1.2 Dead core Page Objects — zero imports (P2)

`tests/pages/BasePage.ts`, `LoginPage.ts`, `DoctorDashboardPage.ts`,
`NurseDashboardPage.ts`, `PatientDayPage.ts`, `CreateCardPage.ts`, `AdminPage.ts` are
imported by **no spec** (grep across `tests/` — only cross-imports between themselves).
They also carry stale selectors (e.g. tabs «Вітальні»/«Призначення» that no longer exist
in the unified `IntensiveCareCard`), which is a trap for future writers.

**Fix (implemented)**: delete the 7 dead files. The living POM convention is the
prosthetics one (`tests/pages/prosthetics/*`, used by `prosthetics-e2e.spec.ts` +
`responsive/mobile-wizard-smoke.spec.ts`).

### 1.3 `waitForTimeout` hotspots (P1)

74 matches across `tests/`. Hotspots:

| Location | Count | Purpose | Fix (implemented) |
|---|---|---|---|
| `pages/prosthetics/WizardExecutionPage.ts` | ~15 | timer tick, step-complete round-trip, dropdown popup, signature toggle, pause dialog close, resource row, back-step, draft toast | condition waits: `expect.poll` on timer text; `waitForResponse` on `POST …/steps/…/complete`; popup row visible; «Підпис отримано» label; dialog hidden; resource row visible; progress text change |
| `pages/prosthetics/SetupWizardPage.ts` | 6 | patient-search debounce, order-list refetch, PDF load, template card/select | `waitForResponse` on `/patients` GET; `waitForResponse` on `/orders` GET in the retry loop; «Старт» enabled (PDF-gated); template card visible + «Обрати» enabled |
| `helpers/prosthetics-flow.ts:133` | 1 | post-«Розпочати процес» settle | `expect(startButton).toBeHidden()` — the start screen unmounts when the instance starts |
| `pages/prosthetics/QualityGatePage.ts` | — | _deleted in QG-Removal Phase 5 (no Quality Gate)_ | — |
| `specs/prosthetics/prosthetics-e2e.spec.ts` | 27 | legacy verification harness (dashboard filters/search, template load, wizard navigation, URL-state checks) | surgical: URL waits (`/process/`, `/done|failed`), response waiters (step-complete POST), tolerant row/heading waits, removed redundant sleeps before auto-waiting assertions; Quality Gate phase removed in QG-Removal Phase 5 |
| `specs/prosthetics/prosthetics-workflow.spec.ts` | ~8 | legacy verification harness | surgical: URL waits (`/process/`, `/done|/failed`), results-row wait, remove throttles inside bounded poll loops |
| `specs/prosthetics/prosthetics-spec-verification.spec.ts` | ~7 | legacy verification harness | surgical: response-waiter + `countCompleted` hybrid in the step-completion retry loop, remove throttles inside bounded checks |
| `specs/doctor/dashboard-table.spec.ts` | 9 | client-side search filter re-render; post-Tab focus | auto-waiting assertions (`toContainText` on first row, `not.toBeVisible` for hidden table), `expect.poll` on row count after clear; Tab focus is synchronous |
| `specs/admin/audit-log.spec.ts` | 2 | audit filter refetch after «Пошук» | `waitForResponse` on `GET …/api/audit` registered before the click |
| `specs/doctor/prescription-add-drug.spec.ts` | 5 | catalog suggestion render, drawer rows, close-list round-trip | auto-waiting `toBeVisible` on suggestion/rows; `waitForResponse` on the close POST |

The verification specs remain **self-reporting** (they log bugs instead of failing) —
full rewrite to hard-assertion tests is a follow-up (see §3). Only fixed sleeps that gate
a known, observable event were removed; the bounded `isVisible(timeout)` checks inside
their loops are the wait mechanism and stay.

### 1.4 `networkidle` misuse (P1)

| Location | Problem | Fix (implemented) |
|---|---|---|
| `nurse/nurse-day-flow.spec.ts:106` | Waits for executions to load before scanning ✓/✕ cells — `networkidle` can fire while the async executions GET is still in flight, or hang on long-polling | Register `waitForResponse` for `GET …/executions` **before** `doctorPage.goto`, await it after the row is visible |
| `responsive/no-horizontal-scroll.spec.ts:31` | Layout audit measured immediately after `networkidle` — late async content can change `scrollWidth` after the measurement | `goto(waitUntil: 'domcontentloaded')` + `expect.poll` on `document.documentElement.scrollWidth` until **two consecutive samples agree** (layout settled), then run the offender report |

### 1.5 Weak scale coverage (P2, deferred)

`scales-episode.spec.ts` fully exercises APACHE II (form fill → calculated score) but
only asserts SOFA/CAM-ICU/Браден *render*. The SOFA calculator (13 inputs incl.
vasopressors) and CAM-ICU decision algorithm are covered by backend unit tests; adding
UI form-fill + result assertions is valuable but medium-priority (see §3).

### 1.6 What is already healthy (keep)

- Hour-model discipline: `nurse-day-flow`, `order-execution`, `modal-therapy`,
  `validation-edge-cases` all plan at the current real hour / scan from it — verified
  against `HourlyGrid.tsx` `HOURS = (i+8)%24`, `medDayPos`, `isPastMedDay` (at 07:00Z only
  hour 7 is clickable — the specs already handle it).
- Data isolation: fixed seed episodes per spec (`a1111111`/`a2222222`/`a3333333`),
  prosthetics isolated by role (`prosthetist1` → Сніжко/ПВ-26-0413, `prosthetist2` →
  Гаврилюк/ПВ-26-0414), no `.first()` on shared ICU tables (filtered rows).
- Cross-role fixtures (`fixtures/index.ts` + `doctorPage`/`nursePage`) used correctly
  (contexts inherit `use` options via `browser.newContext`).
- `episode-error.spec.ts` (route abort + root-not-empty + URL preserved) is a model
  error-path test.

---

## 2. Implemented changes

1. `playwright.config.ts` — `login-chromium` collects `**/auth/access-control.spec.ts`,
   `dependencies: ['setup']`.
2. `specs/auth/access-control.spec.ts` — rewritten RBAC matrix (7 tests):
   - ADMINISTRATOR: `/admin` opens; `/icu/nurse` and `/icu/doctor` → `/select`.
   - DOCTOR: `/icu/doctor/create-card` opens; `/icu/nurse` → `/select` (module permission
     does not defeat the role-scoped sibling exclusion).
   - NURSE: `/icu/nurse/episode/a3333333-…` opens («Показник / година» renders);
     `/icu/doctor` → `/select`.
3. `pages/prosthetics/SetupWizardPage.ts` — sleep-free (`searchPatient`,
   `selectOrder`, `waitForPdfToLoad`, `selectTemplate`, `selectFirstTemplate`).
4. `pages/prosthetics/WizardExecutionPage.ts` — sleep-free (`verifyTimerIsRunning`,
   `completeStep`, `selectAllDropdowns`, `interactWithSignature`, `executeCurrentStep`,
   `pauseProcess`, `addResource`, `goBack`).
5. `helpers/prosthetics-flow.ts` — `startProcessIfNeeded` waits for the start screen to
   unmount.
6. `specs/nurse/nurse-day-flow.spec.ts` — executions-GET response wait replaces
   `networkidle`.
7. `specs/responsive/no-horizontal-scroll.spec.ts` — settled-layout poll replaces
   `networkidle`.
8. Deleted dead POMs: `pages/BasePage.ts`, `LoginPage.ts`, `DoctorDashboardPage.ts`,
   `NurseDashboardPage.ts`, `PatientDayPage.ts`, `CreateCardPage.ts`, `AdminPage.ts`.
9. `specs/prosthetics/prosthetics-workflow.spec.ts` +
   `specs/prosthetics/prosthetics-spec-verification.spec.ts` — surgical sleep removal
   (URL waits, row waits, response-waiter retry loop), throttles removed inside bounded
   poll loops.
10. `specs/prosthetics/prosthetics-e2e.spec.ts` — all 27 `waitForTimeout` sites converted
    (URL waits, step-complete response waiters, tolerant row/heading waits).
11. ~~`pages/prosthetics/QualityGatePage.ts` — gate-decision response waiters~~ — file deleted in QG-Removal Phase 5 (no Quality Gate).
12. `specs/doctor/dashboard-table.spec.ts`, `specs/admin/audit-log.spec.ts`,
    `specs/doctor/prescription-add-drug.spec.ts` — modern hard-assertion specs: sleeps
    replaced with auto-waiting assertions, `expect.poll`, and `waitForResponse` (search
    filters are client-side — verified in `DashboardPage.tsx:26-27`).
13. Final sweep: `grep waitForTimeout|networkidle|waitForLoadState` across `tests/` —
    zero matches in specs/pages/helpers (the only remaining `waitForLoadState` is
    `domcontentloaded` — a navigation event, not a network wait; `diag-patient.cjs` is a
    manual diagnostic script outside the suite).

## 3. Follow-ups (not implemented)

| Priority | Item | Rationale |
|---|---|---|
| P1 | Rewrite the two prosthetics verification specs as hard-assertion tests (or quarantine them) | They are self-reporting («log bugs, never fail») and each is a single 5-minute test — a regression in them is invisible to CI. Replace with the deterministic `prosthetics-e2e.spec.ts` patterns; keep a slim console/network-error assertion spec if the monitoring value is wanted |
| P1 | Add `tests/docs` selector & wait conventions section (or a `docs/` README) | Lock in the rules: no `waitForTimeout`, `waitForResponse` before triggering action, `expect.poll` for stability checks, role selectors, per-test data |
| P2 | SOFA/CAM-ICU UI assertions in `scales-episode.spec.ts` | Deepen form-fill + calculated-result coverage for the two remaining calculators |
| P2 | Selective execution by test impact | 268 tests ≈ 14 min; a docs-only change re-runs everything. `--grep` per changed area is a stopgap; a dependency map is the real fix |
| P2 | Quarantine lane | The suite has retries=2; as flakes are fixed, ratchet to 1 then 0. Track pass-on-retry rate per spec |

## 4. Verification

- `npx tsc --noEmit` in `tests/` (type-level pre-flight, no test execution).
- `npx playwright test --list` — collection sanity check (all 11 projects, incl. the new
  access-control tests).
- Full validation runs in CI on the next push (per AGENTS.md the suite runs only in
  GitHub Actions; local execution is forbidden).
- Watch CI run: `login-chromium` (now 9 tests: login/logout/access-control),
  `nurse-chromium` (executions-wait change), `responsive-tablet-chromium`
  (settled-layout audit), `prosthetics-chromium` (POM waits) — and confirm E2E duration
  did not regress (baseline ≈ 14 min).