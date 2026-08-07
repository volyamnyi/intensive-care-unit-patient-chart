# ICU Patient Chart — AI Agent Guide

## CI RULE (EXECUTE TEST SUITE (UNIT, INTEGRATION, PLAYWRIGHT E2E TESTS) ONLY LOCALLY)

**TESTS RUNNING.** The only valid testing workflow is:

```
ASK USER RUN LOCALLY OR execute workflow: EXECUTE TEST SUITE (UNIT, INTEGRATION, PLAYWRIGHT E2E TESTS) -> FIX FAILURES -> RUN TESTS AGAIN IF FAILS -> COMMIT AND PUSH IF ALL TESTS ARE GREEN
```

ALL test suites: unit tests, integration tests, Playwright E2E.
Local `mvn test` is FORBIDDEN. Local `mvn compile` is permitted for verifying compilation only.
This rule is documented in AGENTS.md, README.md, and checked by CI pipeline.

---

## Current Session

**2026-08-07: Module-routing permissions (RBAC matrix)**

- **Module-routing RBAC**: 4 new permission codes in category «Модулі» — `MODULE_ICU_ACCESS`, `MODULE_MEDICATION_ACCESS`, `MODULE_PROSTHETICS_ACCESS`, `MODULE_ADMIN_ACCESS` (catalog 20 → 24 codes; SQL-seeded in `012-role-permissions.sql`). Defaults: DOCTOR/NURSE/HOD → ICU+MEDICATION, PROSTHETIST/PROSTHETICS_ADMIN → PROSTHETICS, ADMINISTRATOR/AUDITOR → ADMIN. `AppSidebar` + `AppSelectorPage` render a module only when the role holds its permission; `App.tsx` `Guard` accepts `permissions` (access = role **OR** permission — revoking a module permission never locks the role out). Prosthetics **read** endpoints widened to `hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')` — a doctor granted the module can navigate and view (read-only); writes still require the specific `PROSTHETICS_*` codes. E2E `permissions.spec.ts` grants DOCTOR `MODULE_PROSTHETICS_ACCESS` → sidebar link appears → navigation works.

**2026-08-07: Dynamic RBAC — role-permission matrix managed from the admin UI**

Full role-based access control with an admin-editable matrix. `permissions` + `role_permissions` tables (Liquibase `012-role-permissions.sql`), seeded by `PermissionService` on first boot when empty (definitions also SQL-seeded with `ON CONFLICT DO UPDATE`; grants are Java-seeded — edits persist across restarts since seeding only fires when the tables are empty).

- **Enforcement**: URL ceilings in `ClinicalSecurityRules` widened to `CLINICAL_ROLES` (write endpoints), precise enforcement via `@PreAuthorize("@permissionService.has('CODE')")` on controllers (icu-chart, medication-sheet, prosthesis-manufacturing). 403 (not 500) via the existing `AuthorizationDeniedException` handler.
- **Module path operations**: the `MODULE_*_ACCESS` checkboxes in the matrix grant the ability to VISIT a module end to end. Frontend: the `Guard` accepts `permissions` (role OR permission); role-scoped sibling sub-views stay exclusive via `excludeRoles` (a DOCTOR holding `MODULE_ICU_ACCESS` still cannot land on `/icu/nurse`; a NURSE cannot land on `/icu/doctor`; a non-clinical role granted `MODULE_ICU_ACCESS`/`MODULE_MEDICATION_ACCESS` enters via `/icu/doctor`/`/prescriptions/doctor`). Backend: `ClinicalSecurityRules` read rules accept the module permission for the module's GET paths (`access("hasAnyRole(CLINICAL_CORE) or @permissionService.has('MODULE_*_ACCESS')")`) — `CLINICAL_CORE` = DOCTOR/NURSE/HEAD_OF_DEPARTMENT/ADJACENT_SPECIALIST, so ADMINISTRATOR/PROSTHETIST/PROSTHETICS_ADMINISTRATOR read clinical modules only when the checkbox is checked; `GET /api/users/me/**` is `authenticated()` (AuthContext), `GET /api/users/**` needs clinical core or `MODULE_ADMIN_ACCESS`; writes stay ceiling+`@PreAuthorize` gated.
- **Matrix** (defaults): DOCTOR/HOD — episode, clinical day, prescriptions, sign doctor, APACHE II/SOFA + CAM-ICU/Браден/RASS, patient view, modules ICU + medication; NURSE — sign nurse, execute prescriptions, vitals, CAM-ICU/Браден/RASS, patient view, modules ICU + medication; HOD — reopen day (only); ADMIN — patient view + audit access + module «Адміністрування» only (clinical modules are NOT granted by default — the checkbox opens them); PROSTHETIST — dashboard/instance/step/pause + module prosthetics; PROSTHETICS_ADMIN — prosthetics + gate + templates + orders + module prosthetics. HOD removed from prosthetics guards (backend + `App.tsx` routes).
- **Admin UI**: `AdminPage` new tab «Доступи та ролі» — matrix editor (checkbox grid grouped by category, dirty tracking, «Зберегти зміни» diff-saves via `PUT /api/admin/permissions`). Audit tab «Переглянути» gated by `AUDIT_ACCESS` permission. Role dropdown extended with PROSTHETIST / PROSTHETICS_ADMINISTRATOR.
- **API**: `GET /api/admin/permissions` (matrix), `PUT /api/admin/permissions` (grant/revoke, body `{role, permissionCode, granted}`), `GET /api/users/me/permissions` (effective codes for current role). `AuthContext` loads effective permissions and `hasPermission` is matrix-based.
- **Tests**: `PermissionServiceTest` (common unit), `AdminPermissionsIntegrationTest` (grant → 403→400→403 enforcement cycle), E2E `tests/specs/admin/permissions.spec.ts` (UI matrix view + grant/revoke enforcement, serialized).
- Legacy per-user `PRESCRIBER` CSV (`user.permissions` column) remains untouched; `ScaleAuthorizationService` is now permission-driven (DOCTOR may create Браден per matrix).

### Previous sessions (condensed):

**2026-08-06: Prosthetics E2E verification — quality-gate flow fixed (gate guards stage entry)**

Full headed-browser E2E of the prosthetics flow verified end-to-end (prosthetist1 → patient Сніжко → order PR-2026-0001 → template TP-UL-01 → steps 1–4 → admin gate PASS → steps 5–6 → done → PDF → history), instance COMPLETED, 0 errors.

- **Quality gate semantics (backend bug)**: `FlowInstanceService.advance()` previously checked the CURRENT stage's gate (guards EXIT) — with the seed gate on the last stage (d0000004 «Контроль якості») the gate never fired before stage 4 and its steps auto-started; the wizard CTA («Контроль якості →», `WizardScreen.tsx:228–234`) and `QualityGateService.decide()` (gate must be on `currentStageId`) both expect the gate to guard ENTRY to its stage. Fixed `advance()`: completing the last step of a stage now looks at the NEXT stage — if it has a gate → `WAITING_REVIEW` + `currentStageId` = gated stage (no step auto-created); added `enterStage()`; `QualityGateService.pass()` now **enters** the gated stage's steps (was `moveToNextStage`, which skipped them); `rework()` also sets `currentStageId` to the rework stage. Gate flow now: stage 3 → WAITING_REVIEW → admin PASS → stage 4 steps → COMPLETED.
- **Order access for admins**: `ProstheticsOrderController` `GET /orders`, `/{id}`, `/{id}/document` were `hasAnyRole('PROSTHETIST')` — the admin wizard 403'd fetching the order. Widened to `hasAnyRole('PROSTHETIST', 'PROSTHETICS_ADMINISTRATOR')`.
- **403 vs 500**: `GlobalExceptionHandler` had no `AuthorizationDeniedException` handler → `@PreAuthorize` denials surfaced as 500; added a handler → 403 (`ErrorCode.FORBIDDEN`).
- **E2E spec aligned** (`tests/specs/prosthetics/prosthetics-workflow.spec.ts`): gate buttons are «Прийнято (Pass)»/«На доопрацювання»/«Брак (Fail)» (not «Схвалити|Пройдено»); wizard CTAs «Готово →»/«Завершити процес» (not «Завершити крок»); signature element is a toggle button «Область для електронного підпису»/«Підпис отримано» (clicked in `fillFields`). Since the gate requires an admin and the spec runs as prosthetist1, PASS is issued via API: `request` login as prosthetics_admin1 → GET snapshot → find gated stage → `POST /instances/{id}/gates/{gateId}/decision` with `{decision:'PASS', criteriaConfirmed:[...], comment:''}` → reload → continue steps 5–6 → `/done`.
- **UI labels (verified)**: wizard CTAs «Готово →» / «Контроль якості →» / «Завершити процес»; gate «Прийнято (Pass)» (disabled until all criteria checked), «На доопрацювання» (needs comment), «Брак (Fail)»; signature button «Область для електронного підпису» (signed: «Підпис отримано»); DoneScreen «Процес успішно завершено»; PDF `report_{id}.pdf`.

### Previous sessions (condensed):

**2026-08-04: Fullscreen Grid Modal Phase 8–9 — E2E tests + docs (Issues #141, #142, master #133)** — All 6 CI jobs green (run `30911073343`). Commit `35dac79`: modal-therapy E2E (locked day + open-day plan/cancel/restore), modal-grid E2E (nurse edit persistence, a11y, sticky panels), `getNextHourISO` timezone fix (`HourlyGrid.tsx:333–338`), plan-mode ✕ race fix (`onMouseDown preventDefault`). Vitest 419/419, lint 0, tsc clean.

- **Critical ranges extraction**: `CRITICAL_RANGES` + `isCritical()` moved from `HourlyGrid.tsx` into `frontend/src/components/monitoring/criticalRanges.ts` — single source of truth (8 inclusive ranges) shared by rail, chip and cell flash; `''`/NaN/unknown values non-critical; `countCriticalByHour`/`countCriticalTotal` + `pluralCritical` (1 → «критичне значення», 2–4 → «критичні значення», else «критичних значень»; 21/12 plural edges); unit tests in `criticalRanges.test.ts`.
- **Rail & chip**: OutlierRail (pin rail on mobile, highlight in main grid) shows violation cells from the same ranges as cell highlighting and updates after save; alarm chip counter matches critical cells, click focuses the first visible one; no animated/blinking elements in rail/chip (criteria 3); status span must NOT have `role="status"`/`aria-live`; dialog tests use fixture `realClockHour:10`.
- **E2E flake root cause (order-execution.spec.ts)** — NOT a parallel race (CI `workers: 1`): `HourlyGrid.isPastMedDay(h, realClockHour)` marks hours below the current real hour non-clickable (they render '✓'); CI runs ~05:00–08:00Z, so only hours {real, real+1} are ever clickable. The `+13` shift (`aad1568`) targeted hours 20–23 → always past → guaranteed failure (retry2 `Запланувати Glucose 5% 23:00`); fixed in `688398d` to `new Date().getHours()` (the real hour is never past; disjoint from `nurse-day-flow` at real+1; CI uses fresh ephemeral Postgres per run, so no cross-run depletion). Green on first attempt.

### Previous sessions (condensed):

**2026-08-04 (earlier): Fullscreen Grid Modal Phase 5 — edge-case hardening (Issue #138, master #133)** — 409 conflict banner («Оновити дані»/«Залишити мій варіант»), day-lock banner, print CSS for dialog overlay, rapid-toggle guards, `dayLoading` spinner, mobile 44px touch targets + `PatientSidebar` hidden, safe-area insets; 404 capture scoped to episode GET. Commits `6598efd`, `272d654`, `aab6700`. E2E 183 tests.

**2026-07-31: ТЗ v1.2 — SOFA input parameters formalized (docs only)**

Updated `docs/Технічне завдання карта Інтенсивної терапії.md` to v1.2 (2839 → 3026 lines). The ТЗ was brought in line with the existing implementation — verified `SofaCalculator` (all 13 inputs incl. epinephrine), `SofaForm` (4 vasopressors, GCS, creatinine, 24h urine output), `HourlyRecord.meanArterialPressure`; no code changes needed:

- §29/§30: GCS (3–15) field added to general state + hourly monitoring; FiO₂ defined (%); MAP marked auto-calculated with formula `MAP = (2 × ДАТ + САТ) / 3`; new block «Вазопресорна та інотропна підтримка» (допамін, добутамін, норадреналін, адреналін у мкг/кг/хв); діурез мл/год + сумарний за 24 години для SOFA
- §36: одиниці вимірювання (тромбоцити ×10⁹/л; креатинін/білірубін мкмоль/л або мг/дл); `pO₂` → `PaO₂` (визначення); автозапис PaO₂/FiO₂
- §53.2: серцево-судинна оцінка SOFA доповнена адреналіном (≤0.1 → 3, >0.1 → 4), дози у мкг/кг/хв

- 2026-07-30: Clinical scales — episode-level binding, calculator algorithms, E2E tests (Issues #1-#6). `ScaleResult` episodeId/rawData(jsonb); pure-static calculators (ApacheIi, Sofa, CamIcu, Braden); `ScaleAuthorizationService` per-scale roles (APACHE II/SOFA → DOCTOR); episode-level endpoints `GET/POST /episodes/{id}/scales` + `POST .../calculate`; `ScaleFormFactory` + forms (ApacheIiForm, SofaForm, CamIcuForm, BradenForm, RassSelector); PDF episode scales via `findByEpisodeId()`; 13 E2E tests (`scales-episode.spec.ts`, `scales-access.spec.ts`)
- 2026-07-29: 177/177 Playwright tests passing. Pattern A-D (20 fixes), Pattern E-H (additional fixes). DB reset script.
- 2026-07-29 (earlier): Issue #87 prescription list dropdown. Issue #84 GlobalLayout nav. Issue #83 nurse patient list. Cyrillic encoding fix (Issue #82). Global theme. PrescriptionGrid.
- 2026-07-26: Medication Sheet backend — Phase 3-6 complete (EmailService, PrescriptionSchedulerService, controllers + security, integration tests, docs). 422 backend tests.
- 2026-07-25: Exploratory testing — 5 bugs fixed. Model QA audit — grade B, all gaps fixed. 27 validation tests.

## MIS Data Policy (DO NOT VIOLATE)

**The ICU Chart module is a READ-ONLY client of MIS.** Only data retrieval from MIS is permitted. The sole exception is sending generated PDF documents to the patient's document repository.

| Operation | Status | MIS method |
|---|---|---|
| Search patients | ✅ ALLOWED (read) | `spzIBPatientSearch` |
| Get patient by ID | ✅ ALLOWED (read) | `spzIBPatientSearch` |
| Get hospitalization / schedule | ✅ ALLOWED (read) | `spzIBPatientScheduleList` |
| Get user profile | ✅ ALLOWED (read) | `spzIBUserDetails` |
| Get department users | ✅ ALLOWED (read) | `spzIBUserDetails` |
| Get departments | ✅ ALLOWED (read) | `spzIBCompanyDetails` |
| Get dictionaries | ✅ ALLOWED (read) | `spzIB*Dictionary` |
| Send PDF to MIS | ✅ ALLOWED (exception) | `sendPdf()` (transfers immutable PDF, no record modification) |
| Create patient | ❌ FORBIDDEN | `spzIBPatientCreate` — must never be called |
| Create schedule/appointment | ❌ FORBIDDEN | `spzIBScheduleCreate` — must never be called |
| Save agent/insurance | ❌ FORBIDDEN | `spzIBAgentSave` — must never be called |
| Save institution/venue | ❌ FORBIDDEN | `spzIBInstitutionSave` — must never be called |
| Any other MIS mutation | ❌ FORBIDDEN | All `*Save`, `*Create`, `*Update`, `*Delete` methods |

**Rule:** The `MisApiClient` only supports GET-style calls to `/api/run`. Any MIS write endpoint must never be implemented or called. Violating this policy will corrupt MIS data integrity.

## Architecture

```
frontend/  (React 19 + TS 6 + Vite 8 + MUI 9, single app)
  src/icu-chart/            ← ICU chart feature module
  src/medication-sheet/     ← Medication sheet (prescriptions) feature module
  src/prosthetics/          ← Prosthetics manufacturing feature module
  src/shared/               ← shared types, API client, components, auth
backend/   (Spring Boot 4.1.0 + Java 25 + Maven, multi-module)
  pom.xml                   ← parent POM (pom packaging, 4 modules)
  common/                   ← shared entities, JWT/security, base classes
  icu-chart/                ← existing app (@SpringBootApplication, single-deployment JAR)
  medication-sheet/         ← medication sheet module (auto-scanned under com.superhumans)
  prosthesis-manufacturing/ ← prosthetics manufacturing module (auto-scanned under com.superhumans)
tests/     (Playwright 1.61)
```

After login, user lands on `/select` (AppSelectorPage) and picks a sub-app. Routes are prefixed per sub-app:
- `/doctor/*`, `/nurse/*` → ICU chart
- `/prescriptions/doctor/*`, `/prescriptions/nurse/*` → Medication sheet
- `/prosthetics/*` → Prosthetics manufacturing
- `/admin/*` → Admin

- JWT auth stored in `localStorage`.
- Backend port: **8085** (`application.yml`).
- DB: PostgreSQL 16, `ddl-auto: none` — schema managed by Liquibase changelogs.
- Seed data: `backend/src/main/resources/data.sql` (6 users, 3 episodes + 3 open clinical days, `spring.sql.init.mode: always`).
- CI: `.github/workflows/playwright.yml` — Postgres service, JDK 17, Node 22, Playwright chromium, 40min timeout.
- Mock MIS: `MockMisServiceImpl` provides 5 test patients + department/user data.

## Repeatable CI Development Workflow (THE Loop)

**All tests run exclusively via GitHub Actions CI — never locally (may be violated if the user explicitly states so).** Local `mvn test` / `npm test` / Playwright are FORBIDDEN; `mvn compile`, `npm run lint`, `npx tsc --noEmit` are permitted for pre-flight only.

The complete development loop:

```
1. PRE-FLIGHT   → local checks only (fast feedback, no tests)
2. IMPLEMENT    → make code changes for the issue
3. STAGE/COMMIT → git add intended files only + Conventional Commits message
4. PUSH/TRIGGER → git push → GitHub Actions starts automatically
5. POLL         → gh run watch / periodic gh run list until all jobs finish
6. TRIAGE       → failing jobs: view logs, download artifacts, root-cause, fix code or tests
7. REPEAT       → steps 3–6 until every check passes
```

### Phase 0 — Pre-flight (local, test-free)
| Check | Command | Catches |
|---|---|---|
| Backend compiles | `mvn compile` (in `backend/`) | Compile errors |
| Frontend lint | `npm run lint` (in `frontend/`) | Oxlint violations |
| Frontend types | `npx tsc --noEmit` (in `frontend/`) | TypeScript errors |
| Frontend build | `npm run build` (in `frontend/`) | Production build breaks |

### Phase 1 — Implement
- Scope changes to the GitHub issue; do not touch unrelated files (medication-sheet module is off-limits except clinical scales).
- Fix defects in a NEW commit on failure — never amend or force-push.

### Phase 2 — Stage & commit
- `git status` / `git diff` first; stage ONLY intended files (never secrets, never `playwright-results/`).
- Conventional Commits: `feat:` / `fix:` / `refactor:` / `docs:` / `chore:` / `test:`.

### Phase 3 — Push & trigger
- `git push origin main` (or push a feature branch and open a PR to `main`).
- Workflow `.github/workflows/playwright.yml` triggers on push to `main`/`develop` and PR to `main`; the push output prints the run URL.

### Phase 4 — Poll
- `gh run list --limit 5` → find the run ID; `gh run watch <run-id>` blocks until completion, or poll with periodic `gh run list`.
- `gh run view <run-id>` → per-job status; `gh run view <run-id> --job <job-id> --log` → failed-job logs.

### Phase 5 — Triage failures
| Job (actual ID) | What it runs | Failure artifacts (`gh run download <run-id> -n <name> -D <dir>`) |
|---|---|---|
| `format-check` | Checkstyle + oxlint + `tsc --noEmit` | — |
| `backend-test` | `mvn clean test` (unit, PostgreSQL service) | `backend-test-results` (surefire-reports) |
| `backend-integration` | `mvn test -Pintegration-test` | `backend-integration-results` |
| `frontend-test` | Vitest + production build | `vitest-coverage` |
| `e2e-test` | Playwright (45 spec files, chromium, 40-min timeout; `needs: backend-test, frontend-test`) | `playwright-report`, `playwright-test-results` |
| `build` | JAR + frontend dist artifacts (main push only; needs all 5 jobs) | — |

### Exit criteria
All checks pass: `format-check`, `backend-test`, `backend-integration`, `frontend-test`, `e2e-test` (plus `build` on `main`). Green run = done; start the next issue at Phase 1.

## Commands

### Backend (`cd backend`)
| Command | Action |
|---|---|
| `mvn spring-boot:run` | Dev server on `:8085` |
| `mvn clean package -DskipTests` | Build JAR |
| `mvn compile` | Compile only |
| `mvn test` | Run unit tests (excludes integration) |
| `mvn test -Pintegration-test` | Run 79 integration tests (requires Docker/PostgreSQL) |
| `mvn verify` | Run all + JaCoCo coverage check + Checkstyle |

### Frontend (`cd frontend`)
| Command | Action |
|---|---|
| `npm run dev` | Vite dev server on `:5173` |
| `npm run build` | `tsc -b && vite build` |
| `npm run lint` | Oxlint |
| `npx tsc --noEmit` | Type-check without build |
| `npm t` or `npx vitest run` | Run Vitest tests (~350 across 44 files) |

### Playwright (`cd tests`)
| Command | Action |
|---|---|
| `npx playwright test` | Run all E2E tests (45 spec files) |
| `npx playwright test --list` | List tests without running |
| `npx playwright show-report` | View HTML report |

## Testing

- **Backend**: 557 total tests (from multi-module reactor: common + medication-sheet + icu-chart + prosthesis-manufacturing). JaCoCo 60% instruction / 50% branch minimum. Checkstyle Google checks.
- **Frontend**: 419 Vitest tests across 47 files (pages, components, AuthContext, endpoints, prosthetics). Run with `npm t`.
- **E2E**: 49 Playwright spec files (188 tests) across 7 projects (setup, login, doctor, nurse, hod, admin, api).

## Playwright Projects

| Project | Depends On | storageState | Tests |
|---|---|---|---|
| setup | — | — | Auth setup (4 roles) |
| login-chromium | — | none | Login flow |
| doctor-chromium | setup | `.auth/doctor.json` | Dashboard, create card, prescriptions, notes, sign-off |
| nurse-chromium | setup | `.auth/nurse.json` | Dashboard, vitals, fluid balance, order execution |
| hod-chromium | setup | `.auth/hod.json` | Dashboard, clinical day reopen |
| admin-chromium | setup | `.auth/admin.json` | User tables |
| api-chromium | — | none | Patient search API, error handling, scales access control |

## Seed Data

| Login | Password | Role |
|---|---|---|
| `doctor1` / `doctor2` | `doctor123` | DOCTOR |
| `nurse1` / `nurse2` | `nurse123` | NURSE |
| `head1` | `head123` | HEAD_OF_DEPARTMENT |
| `admin` | `admin123` | ADMINISTRATOR |
| `prosthetist1` / `prosthetist2` | `doctor123` | PROSTHETIST |
| `prosthetics_admin1` | `doctor123` | PROSTHETICS_ADMINISTRATOR |
| *(backend-only)* | — | AUDITOR |

Mock MIS provides 5 test patients: Петренко, Коваленко, Сидоренко, Бондаренко, Ткачук.

Prosthetics seed patients (local mock tables, not MIS):
| Patient | ID | Order | Template |
|---|---|---|---|
| Сніжко Оксана Володимирівна | `900001` | ПВ-26-0413 (upper_limb) | TP-UL-01 (ACTIVE) |
| Гаврилюк Тарас Олексійович | `900002` | ПВ-26-0414 (lower_limb) | TP-LL-01 (DRAFT) |

3 seed episodes with 4 clinical days:

| Episode | Clinical Days |
|---|---|
| `a1111111` (Петренко) | `b1111111` OPEN, `b1111112` NURSE_SIGNED |
| `a2222222` (Коваленко) | `b2222222` OPEN, `b4444444` NURSE_SIGNED |
| `a3333333` (Сидоренко) | `b3333333` OPEN |

**E2E test data isolation** (each spec targets a specific episode, no `.first()` race):
- `a1111111`: `signoff-full-chain` (signs `b1111111` + `b1111112`), `signoff`
- `a2222222`: `clinical-day-reopen` (reopens `b4444444`), `pdf-generation` (signs `b2222222`)
- `a3333333`: `notes`, `notes-full`, `prescriptions`, `prescription-cancel`, `scales-episode`

**Prosthetics E2E isolation** (separate mock tables, no cross-module interference):
- `prosthetist1` → owns `Сніжко` / `ПВ-26-0413` / instance from `TP-UL-01`
- `prosthetist2` → owns `Гаврилюк` / `ПВ-26-0414` / instance from `TP-LL-01`
- `prosthetics_admin1` → quality gate decisions, template admin
- Each spec uses fixed seed IDs (no `.first()`)

## Data Model

```
BaseEntity (abstract)
  ├── id: UUID (PK)
  ├── createdAt, createdBy, updatedAt, updatedBy
  └── version: Integer (@Version, optimistic locking)

Episode (1) ──── (N) ClinicalDay
                         │
               ┌────────┼────────┬────────┬────────┬────────┐
           Hourly  Medical  Medical  Scale   FluidBalance  Signature
           Record  Order    Note    Result

AuditLog (standalone, no BaseEntity)
  id, timestamp, userId, entity, entityId, action,
  oldValue, newValue, correlationId, details,
  ipAddress, userRole, isDeleted
```

### Entity Details

| Entity | Extends | Key Fields | Constraints |
|---|---|---|---|
| `User` | BaseEntity | login(unique), passwordHash, fullName, role(UserRole), email, specialityCode/Name, phone | Role: DOCTOR/NURSE/HEAD_OF_DEPARTMENT/ADMINISTRATOR/AUDITOR |
| `Episode` | BaseEntity | patientId, hospitalizationId, departmentId, admissionDate, dischargeDate, status(EpisodeStatus) | Status: DRAFT → ACTIVE → COMPLETED/ARCHIVED |
| `ClinicalDay` | BaseEntity | episode(M→1), dayNumber, startDateTime, endDateTime, status(ClinicalDayStatus), doctorSigned, nurseSigned, closedAt | Status: OPEN → NURSE_SIGNED → DOCTOR_SIGNED → CLOSED/REOPENED |
| `HourlyRecord` | BaseEntity | clinicalDay(M→1), recordTime, recordHour, consciousness, temperature(34-42), heartRate(0-300), respiratoryRate(0-60), systolicBP(50-250), diastolicBP(30-150), meanArterialPressure, spo2(50-100), glucose(1-30), etco2, fio2, cvp, urineOutput, drainOutput, stool, vomit, painScore, notes | UNIQUE(clinical_day_id, record_hour); ranges validated in @PrePersist/@PreUpdate |
| `MedicalOrder` | BaseEntity | clinicalDay(M→1), category, drugName, dose, unit, route, frequency, startTime, endTime, status(MedicalOrderStatus) | Status: DRAFT/ACTIVE/COMPLETED/CANCELLED |
| `OrderExecution` | BaseEntity | order(M→1), executedBy, executedAt, actualDose, status(OrderExecutionStatus), comment | Status: PLANNED/IN_PROGRESS/COMPLETED/PARTIALLY_COMPLETED/CANCELLED |
| `MedicalNote` | BaseEntity | clinicalDay(M→1), authorId, role, noteType, text(TEXT) | — |
| `ScaleResult` | BaseEntity | clinicalDay(M→1)(nullable), scale(M→1), result(text), episodeId(UUID), rawData(jsonb), calculatedAt, calculatedBy | Auto-calculates GCS/RASS from consciousness |
| `FluidBalance` | BaseEntity | clinicalDay(M→1), hour, intake, output, balance, cumulativeBalance | Recalculated on HourlyRecord changes |
| `Signature` | BaseEntity | clinicalDay(M→1), userId, role, signedAt, hash, status | — |
| `Permission` | — | code(PK), label, description, category | Dictionary of the RBAC catalog (24 codes) |
| `RolePermission` | — | role(PK, UserRole), permissionCode(PK→Permission) | Default-deny grants; presence = granted |
| `GeneratedPdf` | BaseEntity | clinicalDay(M→1), fileName, fileVersion, generatedAt, generatedBy, checksum, fileData(byte[]), transferStatus(TransferStatus), transferError, transferredAt | TransferStatus: PENDING/SENT/FAILED |
| `SystemSettings` | BaseEntity | key(unique), value(TEXT), description(TEXT) | — |

### Enums

| Enum | Values |
|---|---|
| `UserRole` | DOCTOR, NURSE, HEAD_OF_DEPARTMENT, ADMINISTRATOR, AUDITOR |
| `EpisodeStatus` | DRAFT, ACTIVE, COMPLETED, ARCHIVED |
| `ClinicalDayStatus` | OPEN, NURSE_SIGNED, DOCTOR_SIGNED, CLOSED, REOPENED |
| `MedicalOrderStatus` | DRAFT, ACTIVE, COMPLETED, CANCELLED |
| `OrderExecutionStatus` | PLANNED, IN_PROGRESS, COMPLETED, PARTIALLY_COMPLETED, CANCELLED |
| `TransferStatus` | PENDING, SENT, FAILED |

## API Endpoints

All endpoints prefixed with `/api`.

### Auth
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/login` | No | Authenticate, returns JWT |

### Episodes
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/episodes` | Yes | Search (patientId, status query params) |
| GET | `/api/episodes/{id}` | Yes | Get by ID |
| GET | `/api/episodes/{id}/clinical-days` | Yes | Get clinical days for episode |
| POST | `/api/episodes` | Yes | Create episode |
| PATCH | `/api/episodes/{id}` | Yes | Update episode fields (with version) |
| POST | `/api/episodes/{id}/close` | Yes | Close episode |
| PUT | `/api/episodes/{id}/archive` | Yes | Archive episode (→ 204) |

### Clinical Days
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/clinical-days/{id}` | Yes | Get by ID |
| POST | `/api/clinical-days` | Yes | Create (with episodeId) |
| PATCH | `/api/clinical-days/{id}` | Yes | Update endDateTime (with version) |
| POST | `/api/clinical-days/{id}/sign/nurse` | Yes | Nurse sign (→ 204 No Content) |
| POST | `/api/clinical-days/{id}/sign/doctor` | Yes | Doctor sign (→ 204 No Content) |
| POST | `/api/clinical-days/{id}/reopen` | Yes | Reopen (with version) |

### Hourly Records
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/clinical-days/{id}/hourly-records` | Yes | List for clinical day |
| POST | `/api/clinical-days/{id}/hourly-records` | Yes | Create (recordHour auto-set) |
| PATCH | `/api/hourly-records/{id}` | Yes | Update fields (with version) |

### Medical Orders
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/clinical-days/{id}/orders` | Yes | List for clinical day |
| POST | `/api/clinical-days/{id}/orders` | Yes | Create |
| PATCH | `/api/orders/{id}` | Yes | Update (with version) |
| POST | `/api/orders/{id}/cancel` | Yes | Cancel (with version) |

### Order Executions
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/orders/{id}/executions` | Yes | List for order |
| POST | `/api/orders/{id}/execute` | Yes | Create execution |
| PATCH | `/api/executions/{id}` | Yes | Update (with version) |

### Medical Notes
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/clinical-days/{id}/notes` | Yes | List for clinical day |
| POST | `/api/clinical-days/{id}/notes` | Yes | Create |
| PATCH | `/api/notes/{id}` | Yes | Update (with version) |

### Clinical Scales
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/scales` | Yes | List available scales |
| GET | `/api/clinical-days/{id}/scales` | Yes | Get scale results for clinical day |
| POST | `/api/clinical-days/{id}/scales` | Yes | Create scale result |
| PATCH | `/api/scales/{id}` | Yes | Update (with version) |
| GET | `/api/episodes/{episodeId}/scales` | Yes | Get episode-level scale results |
| POST | `/api/episodes/{episodeId}/scales` | Yes | Create episode-level scale result |
| POST | `/api/episodes/{episodeId}/scales/calculate` | Yes | Calculate and save scale from raw data |

### Fluid Balance
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/clinical-days/{id}/fluid-balance` | Yes | Get fluid balance entries |
| POST | `/api/clinical-days/{id}/fluid-balance/recalculate` | Yes | Recalculate from scratch |

### Patients (Mock MIS)
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/patients` | Yes | Search (query param) |
| GET | `/api/patients/{id}` | Yes | Get by ID |

### Users
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/users/me` | Yes | Current user profile |
| GET | `/api/users/doctors` | Yes | List all doctors |
| GET | `/api/users/nurses` | Yes | List all nurses |

### PDF
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/clinical-days/{id}/pdf` | Yes | Get latest generated PDF |
| POST | `/api/clinical-days/{id}/pdf` | Yes | Generate PDF for clinical day |
| GET | `/api/clinical-days/{id}/pdf/status` | Yes | Get PDF transfer status |

### Audit
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/audit` | Yes | List (paginated, filters: userId, entity, entityId, action, dateFrom, dateTo). Requires `AUDIT_ACCESS` or AUDITOR |
| GET | `/api/audit/{id}` | Yes | Get single audit log entry. Requires `AUDIT_ACCESS` or AUDITOR |

### RBAC (Admin role-permission matrix)
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/admin/permissions` | ADMINISTRATOR | Full matrix: roles, permission catalog, grants |
| PUT | `/api/admin/permissions` | ADMINISTRATOR | Grant/revoke: `{role, permissionCode, granted}` |
| GET | `/api/users/me/permissions` | Any authenticated | Effective permission codes of the current user's role |

### Mock MIS Controls
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/mis/error-mode?mode=timeout\|not_found\|unavailable\|none` | Yes | Set mock MIS error simulation |

### Prosthetics Manufacturing
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/prosthesis-manufacturing/patients` | Yes (PROSTHETIST, PROSTHETICS_ADMINISTRATOR) | Search patients |
| GET | `/api/prosthesis-manufacturing/patients/{id}` | Yes | Get patient by ID |
| POST | `/api/prosthesis-manufacturing/patients` | Yes (PROSTHETICS_ADMINISTRATOR) | Create patient |
| GET | `/api/prosthesis-manufacturing/orders` | Yes | List orders |
| GET | `/api/prosthesis-manufacturing/orders/{id}` | Yes | Get order by ID |
| POST | `/api/prosthesis-manufacturing/orders` | Yes (PROSTHETICS_ADMINISTRATOR) | Create order |
| GET | `/api/prosthesis-manufacturing/templates` | Yes | List flow templates |
| GET | `/api/prosthesis-manufacturing/templates/{id}` | Yes | Get template by ID |
| POST | `/api/prosthesis-manufacturing/templates` | Yes (PROSTHETICS_ADMINISTRATOR) | Create template |
| PATCH | `/api/prosthesis-manufacturing/templates/{id}` | Yes (PROSTHETICS_ADMINISTRATOR) | Update template |
| GET | `/api/prosthesis-manufacturing/instances` | Yes | List flow instances |
| GET | `/api/prosthesis-manufacturing/instances/{id}` | Yes | Get instance by ID |
| POST | `/api/prosthesis-manufacturing/instances` | Yes (PROSTHETIST) | Create instance from order + template |
| GET | `/api/prosthesis-manufacturing/instances/{id}/step-executions` | Yes | Get step executions for instance |
| POST | `/api/prosthesis-manufacturing/step-executions/{id}/complete` | Yes (PROSTHETIST) | Complete step execution |
| GET | `/api/prosthesis-manufacturing/instances/{id}/quality-gates` | Yes | Get quality gates for instance |
| POST | `/api/prosthesis-manufacturing/gate-decisions` | Yes (PROSTHETICS_ADMINISTRATOR) | Make gate decision (PASS/REWORK/FAIL) |
| POST | `/api/prosthesis-manufacturing/instances/{id}/pause` | Yes (PROSTHETIST) | Pause instance |
| POST | `/api/prosthesis-manufacturing/instances/{id}/resume` | Yes (PROSTHETIST) | Resume instance |
| POST | `/api/prosthesis-manufacturing/instances/{id}/replacement` | Yes (PROSTHETIST) | Create replacement after FAIL |
| GET | `/api/prosthesis-manufacturing/instances/{id}/failure-snapshot` | Yes | Get failure snapshot |
| GET | `/api/prosthesis-manufacturing/instances/{id}/pdf` | Yes | Generate PDF report for instance |
| POST | `/api/prosthesis-manufacturing/evidence-files` | Yes (PROSTHETIST) | Upload evidence file |

## Frontend Routes

| Path | Component | Guard (roles) |
|---|---|---|
| `/login` | `LoginRoute` → `LoginPage` | Redirects to `/` if authenticated |
| `/` | `RoleRedirect` | NURSE → `/nurse`, ADMIN → `/admin`, others → `/doctor` |
| `/doctor` | `DoctorLayout` > `DashboardPage` | DOCTOR, HEAD_OF_DEPARTMENT |
| `/doctor/create-card` | `DoctorLayout` > `CreateCardPage` | DOCTOR, HEAD_OF_DEPARTMENT |
| `/doctor/episode/:episodeId` | `DoctorLayout` > `PatientDayPage` | DOCTOR, HEAD_OF_DEPARTMENT |
| `/nurse` | `NurseLayout` > `NurseDashboardPage` | NURSE |
| `/nurse/episode/:episodeId` | `NurseLayout` > `PatientDayPage` | NURSE |
| `/prosthetics` | `ProstheticsLayout` > `DashboardPage` | PROSTHETIST, PROSTHETICS_ADMINISTRATOR |
| `/prosthetics/orders` | `ProstheticsLayout` > `OrderSelectPage` | PROSTHETIST, PROSTHETICS_ADMINISTRATOR |
| `/prosthetics/review` | `ProstheticsLayout` > `OrderReviewPage` | PROSTHETIST, PROSTHETICS_ADMINISTRATOR |
| `/prosthetics/template` | `ProstheticsLayout` > `TemplateSelectPage` | PROSTHETIST, PROSTHETICS_ADMINISTRATOR |
| `/prosthetics/wizard` | `ProstheticsLayout` > `WizardScreen` | PROSTHETIST |
| `/prosthetics/process/:instanceId` | `ProstheticsLayout` > `ProcessDetail` | PROSTHETIST, PROSTHETICS_ADMINISTRATOR |
| `/prosthetics/failed/:instanceId` | `ProstheticsLayout` > `FailedScreen` | PROSTHETIST, PROSTHETICS_ADMINISTRATOR |
| `/prosthetics/done/:instanceId` | `ProstheticsLayout` > `DoneScreen` | PROSTHETIST, PROSTHETICS_ADMINISTRATOR |
| `/admin` | `AdminPage` | ADMINISTRATOR |

## Frontend Components

### Pages (16)
| File | Description |
|---|---|
| `LoginPage.tsx` | Login form with error handling |
| `doctor/DashboardPage.tsx` | Doctor episode list with patient search |
| `doctor/DepartmentDashboardPage.tsx` | HOD department dashboard with stats |
| `doctor/CreateCardPage.tsx` | Create new episode (select patient, set dates) |
| `doctor/PatientDayPage.tsx` | Doctor view of clinical day (orders, notes, scales, sign-off) |
| `nurse/NurseDashboardPage.tsx` | Nurse episode list with active patients |
| `admin/AdminPage.tsx` | Admin panel: user management (roles incl. PROSTHETIST/PROSTHETICS_ADMIN, PRESCRIBER badge), RBAC matrix editor («Доступи та ролі»), audit log (gated by `AUDIT_ACCESS`), stats |
| `prosthetics/DashboardPage.tsx` | Prosthetist dashboard with instances, filters |
| `prosthetics/setup/OrderSelectPage.tsx` | Patient search + order selection |
| `prosthetics/setup/OrderReviewPage.tsx` | Order review with recipe PDF |
| `prosthetics/setup/TemplateSelectPage.tsx` | Template selection for new instance |
| `prosthetics/process/WizardScreen.tsx` | Step-by-step wizard with validation |
| `prosthetics/process/ProcessDetail.tsx` | Process overview with stages/steps |
| `prosthetics/process/ProcessOverview.tsx` | Compact process status view |
| `prosthetics/process/FailedScreen.tsx` | Failure snapshot + replacement |
| `prosthetics/process/DoneScreen.tsx` | Completed instance with PDF export |

### Common Components
| File | Description |
|---|---|
| `AuditLogTable.tsx` | Audit log viewer with filters |
| `ClinicalDayTimeline.tsx` | Timeline of clinical days for an episode |
| `DoctorDashboard.tsx` | Doctor dashboard quick-view |
| `EpisodeTable.tsx` | Table of episodes with search/filter |
| `FluidBalancePanel.tsx` | Fluid balance display with intake/output chart |
| `HourSelector.tsx` | Hour picker for vital signs entry |
| `HourlyGrid.tsx` | Hourly grid with monitoring data, therapy cells, plan/execute |
| `HourlyGridDialog.tsx` | Fullscreen modal dialog wrapping HourlyGrid with undo, status, critical chip |
| `HourlyRecordTable.tsx` | Hourly vital signs grid |
| `IntensiveCareCard.tsx` | Central ICU card component |
| `criticalRanges.ts` | Critical range definitions + `isCritical`, `countCriticalByHour`, `pluralCritical` |
| `LabResultsPanel.tsx` | Lab results entry and display |
| `MedicalNotesPanel.tsx` | Medical notes list and create/edit |
| `MedicalOrdersPanel.tsx` | Medical orders list and create/edit/cancel |
| `NurseDashboard.tsx` | Nurse dashboard quick-view |
| `PatientSearch.tsx` | Patient search autocomplete (from mock MIS) |
| `PatientStatePanel.tsx` | Patient state assessment panel |
| `ScaleResultsPanel.tsx` | Clinical scale results display with form integration |
| `scales/ApacheIiForm.tsx` | APACHE II calculator form (20 parameters) |
| `scales/SofaForm.tsx` | SOFA calculator form (6 organ systems) |
| `scales/CamIcuForm.tsx` | CAM-ICU delirium assessment form |
| `scales/BradenForm.tsx` | Braden pressure injury risk form |
| `scales/RassSelector.tsx` | RASS sedation level dropdown |
| `scales/ScaleFormFactory.tsx` | Routes scale names to form components |
| `SignDialog.tsx` | Sign dialog with hash confirmation |
| `VentilationPanel.tsx` | Ventilation settings panel |
| `VitalSignsForm.tsx` | Vital signs entry form |
| `prosthetics/StatusBadge.tsx` | Status badge with color coding |
| `prosthetics/SetupSteps.tsx` | Step indicator for setup wizard |
| `prosthetics/QualityGatePanel.tsx` | Quality gate checklist and decision UI |

### API Client (`frontend/src/api/`)
- **`client.ts`**: Axios instance → `http://localhost:8085/api`, JWT interceptor
- **`endpoints.ts`**: 12 API modules (auth, patient, episode, clinicalDay, hourlyRecord, medicalOrder, orderExecution, medicalNote, clinicalScale, fluidBalance, pdf, user, audit)

### Auth (`frontend/src/services/AuthContext.tsx`)
- `AuthProvider` with user/token state, login/logout, role checking
- Token persisted in `localStorage`
- Guards in `App.tsx` via `Guard` component and `LoginRoute`/`RoleRedirect`

## DTOs

### Request DTOs
`LoginRequest`, `EpisodeCreateRequest`, `EpisodePatchRequest`, `EpisodeCloseRequest`, `ClinicalDayCreateRequest`, `ClinicalDayPatchRequest`, `HourlyRecordCreateRequest`, `HourlyRecordPatchRequest`, `MedicalOrderCreateRequest`, `MedicalOrderPatchRequest`, `MedicalNoteCreateRequest`, `MedicalNotePatchRequest`, `ScaleResultCreateRequest`, `ScaleResultPatchRequest`, `ScaleResultCalculateRequest`, `OrderExecutionCreateRequest`, `OrderExecutionPatchRequest`, `SignRequest`, `ReopenRequest` (19 total)

### Response DTOs
`LoginResponse`, `EpisodeResponse`, `ClinicalDayResponse`, `HourlyRecordResponse`, `MedicalOrderResponse`, `OrderExecutionResponse`, `MedicalNoteResponse`, `ScaleResultResponse`, `FluidBalanceResponse`, `SignResponse`, `PdfResponse`, `UserResponse`, `AuditLogResponse`, `ErrorResponse` (14 total)

## Backend Services (13)

| Service | Responsibility |
|---|---|
| `AuthService` | Login with password verification + JWT generation |
| `EpisodeService` | CRUD + search + close/archive with optimistic locking |
| `ClinicalDayService` | CRUD + signing workflow + reopen with signature revocation + next-day gating |
| `HourlyRecordService` | CRUD with clinical day lock checking |
| `MedicalOrderService` | CRUD + cancel with status validation |
| `OrderExecutionService` | CRUD with order status validation |
| `MedicalNoteService` | CRUD with author role assignment |
| `ClinicalScaleService` | Scale results + automatic GCS/RASS from consciousness + episode-level results + algorithm-based calculation (APACHE II, SOFA, CAM-ICU, Braden) |
| `FluidBalanceService` | Recalculation from HourlyRecord + OrderExecution |
| `ScaleAuthorizationService` | Per-scale role-based access control (APACHE II/SOFA → DOCTOR, others → NURSE) |
| `SignatureService` | Create/revoke signatures, check existing signatures |
| `PdfGeneratorService` | Generate PDF (iText) with all clinical day sections |
| `AuditService` | Create/query audit log entries with pagination |
| `PermissionService` | Dynamic RBAC: `has/hasAny/hasForRole` (SpEL for `@PreAuthorize`), matrix read, grant/revoke with cache invalidation + audit, first-boot seeding of defaults |

Prosthetics module adds 7 additional services in `prosthesis-manufacturing` module:
- `ProstheticsPatientService` — patient CRUD
- `ProstheticsOrderService` — order CRUD + PDF generation
- `FlowTemplateService` — template CRUD + stages/steps/elements
- `FlowInstanceService` — instance lifecycle (create, pause, resume, complete steps)
- `QualityGateService` — gate decisions (PASS/REWORK/FAIL), rework loops
- `FailureSnapshotService` — failure capture + PDF report
- `EvidenceFileService` — file upload (images/PDFs, 10MB limit)

## Compliance Fixes Applied

| § | Description | Implementation |
|---|---|---|
| §46 | Unique constraint on (clinical_day_id, record_hour) | `recordHour` field + `@UniqueConstraint` on `HourlyRecord` |
| §47 | Next clinical day gating | `canAdvanceToNextDay()` checks status + hourly record count |
| §49 | Clinical range validation | JSR-380 annotations + `@PrePersist/@PreUpdate` validation |
| §52 | Auto fluid balance recalculation | `@PostPersist/@PostUpdate` triggers `FluidBalanceService.recalculate()` |
| §53 | Archive episode endpoint | `PUT /api/episodes/{id}/archive` → 204 No Content |
| §79 | Audit log IP address + user role + login events | `ipAddress`, `userRole` fields; `JwtAuthenticationFilter` logs login |
| §80 | HTTP 204 for sign endpoints | Sign endpoints return `ResponseEntity.noContent()` |
| §81 | Soft delete on AuditLog | `isDeleted` field + `findAllActive()` JPQL query |
| §84 | AUDITOR role | Added to `UserRole` enum |
| §86 | Integration tests in CI | New `integration-tests` job with PostgreSQL 16 service |
| §87 | Mock MIS error scenarios | Error modes: timeout, not_found, unavailable via `POST /api/mis/error-mode` |
| §88 | JaCoCo coverage | 60% instruction / 50% branch minimums |
| §89 | Checkstyle analysis | Google checks with console output |
| §94 | PDF transfer status tracking | `GeneratedPdf.transferStatus` + `TransferStatus` enum (PENDING/SENT/FAILED) + `GET /clinical-days/{id}/pdf/status` |
| §98 | MIS calls audited | All `MockMisServiceImpl` methods call `auditService.logAction()` including `sendPdf()` |
| §— | Liquibase schema management | `ddl-auto: none`, schema via `db/changelog/db.changelog-master.yaml` (6 changesets); `spring.sql.init.mode: always` for seed data |

## Key Patterns

- **Locators** (Playwright): prefer `getByRole`, `getByLabel`, row-specific filters over `.first()`
- **Seed data references**: 3 episodes — use `filter({ hasText })` with patient names (Петренко, Коваленко, Сидоренко)
- **Auth**: storageState per role, projects depend on `setup`
- **Parallelism**: `fullyParallel: true` — tests can race; use specific locators, not `.first()`, for shared data
- **CI retries**: 2 retries per test
- **Backend tests**: `@SpringBootTest` are `@Transactional` (rollback); `@DataJpaTest` uses `@AutoConfigureTestDatabase(replace = NONE)` (real PostgreSQL)
- **IDs**: All UUID strings (`string` type in TS, `UUID` type in Java)
- **Optimistic Locking**: `@Version version` on all entities → `VersionConflictException` → HTTP 409
- **Audit**: Every create/update/delete operation creates an `AuditLog` entry
- **Error response**: `ErrorResponse` DTO with `code`, `message`, `correlationId`
- **ClinicalDay locking**: Signed/closed days cannot be modified (throws `DocumentLockedException`)
- **Signing flow**: Nurse must sign before doctor; signatures can be revoked on reopen
- **Frontend error display**: All API catch blocks in `IntensiveCareCard.tsx` use `getErrorMessage(err, fallback)` which extracts `err.response?.data?.message` from Axios errors (shows the backend validation message instead of generic "Request failed with status code 400")
- **Backend validation exceptions**: `ConstraintViolationException` (JSR-380) and `InvalidDataAccessApiUsageException` (wraps `IllegalArgumentException` from `@PrePersist`) are both caught in `GlobalExceptionHandler` and return 400 with the validation message
- **Validation runs before method security**: `@Valid @RequestBody` argument binding happens *before* the `@PreAuthorize` interceptor — a request with an invalid body returns 400 (validation) even for roles lacking the permission. Tests that assert 403 must send a **valid** request body; an invalid body only proves the role *passed* security when it yields 201/200 (or 400 with a valid body in the denied case). Same applies to E2E specs (`security-rules.spec.ts`, `permissions.spec.ts`)
- **Fullscreen modal keyboard model** (`HourlyGridDialog.tsx`): initial focus on the close button (✕), not the first editable cell, to prevent accidental data change before reviewing patient state (WCAG 2.4.3). `Tab`/`Shift+Tab` cycles within the dialog (focus trap). `Esc` closes; `Alt+Enter` toggles. `Escape` in a dirty cell reverts the draft; `Enter` commits.
- **Custom keyframes** (`frontend/src/index.css`): non-Tailwind keyframes (`scale-in`, `fade-in`, `slide-in-from-left`, `content-show`) live in `index.css` under `@layer base`, scoped via `[data-fullscreen="true"]` so they don't affect non-modal use. `animate-in` utilities (`duration-…`, `fade-in`, `slide-in-from-…`, `zoom-in-…`) from a motion library are inert — the dialog overrides them with its own `style={{ animation: 'none' }}` on initial render to avoid double-animation. On `prefers-reduced-motion`, all animations are gated by a media-query guard.

## Conventions

- **Commits**: Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`, `chore:`)
- **TypeScript**: `erasableSyntaxOnly: true` — no enums, no namespaces
- **Roles**: Gate in backend (Spring Security `@PreAuthorize`) and frontend (`Guard` component)
- **Routing**: `/doctor/*` for DOCTOR/HOD, `/nurse/*` for NURSE, `/admin/*` for ADMINISTRATOR
- **DB**: `ddl-auto: none` — schema managed by Liquibase changelogs in `db/changelog/changesets/`; never write manual DDL
- **Data seeding**: Only via `data.sql` (`spring.sql.init.mode: always`)
- **Test seed data**: Integration tests use `data-test.sql` (in `src/test/resources/`) with plain INSERTs on a fresh Testcontainers PostgreSQL database. The production `data.sql` keeps `ON CONFLICT (id) DO NOTHING` for local dev resilience (exception: `prescription_lists` uses `ON CONFLICT (id) DO UPDATE SET document_name = EXCLUDED.document_name` to auto-heal Cyrillic encoding corruption). Modified data may persist across restarts. Reset with `DROP SCHEMA public CASCADE; CREATE SCHEMA public;` in PostgreSQL before the next run.

## Encoding Policy

**All SQL seed files and generated SQL must be UTF-8** — never UTF-16LE, never Windows-1251.

- **Generator scripts** (`scripts/*.cjs`): Use `fs.writeFileSync(path, content, 'utf8')` for file output and `process.stdout.write(content, 'utf8')` for stdout. **Never use `console.log()`** to generate file content — on Windows PowerShell, `console.log` pipes through `process.stdout` which defaults to UTF-16LE, producing a UTF-16LE BOM and null-byte interleaved ASCII that PostgreSQL cannot decode.
- **Verification commands**:
  - `file scripts/*.sql` should report "UTF-8 Unicode text", never "Little-endian UTF-16 Unicode text"
  - `hexdump -C scripts/*.sql | head -3` should show no BOM (`FF FE`) and single-byte (not zero-interleaved) ASCII
- **`data.sql`**: Must be UTF-8. Any seed SQL file concatenated into `data.sql` must be explicitly written as UTF-8. If a corrupted file was already concatenated, convert it with `Set-Content -Encoding UTF8` or `iconv -f UTF-16 -t UTF-8` and re-insert.
- **Auto-heal**: If corrupted `document_name` values already exist in the database, the `ON CONFLICT (id) DO UPDATE SET document_name = EXCLUDED.document_name` clause on `prescription_lists` INSERTs will overwrite them with clean UTF-8 text on the next `data.sql` execution.

## Project Files (kept in repo)

```
AGENTS.md              ← This file — agent guide
README.md              ← Project README with badges and usage
UseManual.md           ← User manual (Ukrainian)
.gitignore             ← Global ignore rules
backend/
  pom.xml              ← Maven build with JaCoCo, Checkstyle, surefire
  src/main/java/       ← 163 Java source files
  src/main/resources/  ← application.yml, data.sql, PDF template, db/changelog/ (Liquibase)
  src/test/java/       ← 62 test files (32 unit + 13 integration + 1 abstract + 16 more)
frontend/
  package.json         ← Dependencies
  vite.config.ts       ← Vite build config
  tsconfig*.json       ← TypeScript configs
  index.html           ← App entry HTML
  public/              ← Static assets
  src/                 ← 90 TS/TSX source + 47 test files
tests/
  playwright.config.ts ← Playwright config with 9 projects
  package.json         ← Test dependencies
  specs/               ← 47 spec files
  pages/               ← Page Object Model (7 files)
  fixtures/            ← Test fixtures
docs/
  Технічне завдання карта Інтенсивної терапії.md  ← Full technical specification (3026 lines)
.github/
  workflows/playwright.yml  ← CI pipeline (3 jobs: integration-tests, test, format-check)
```
