# ICU Patient Chart — AI Agent Guide

## CI-ONLY RULE (DO NOT VIOLATE)

**TESTS MUST NEVER BE RUN LOCALLY.** The only valid testing workflow is:

```
GitHub Issue → implement → commit → push to main → CI workflow runs → poll for results
```

CI runs ALL test suites: unit tests, integration tests, Playwright E2E.
Local `mvn test` is FORBIDDEN. Local `mvn compile` is permitted for verifying compilation only.
This rule is documented in AGENTS.md, README.md, and checked by CI pipeline.

---

## Current Session

**2026-07-31: ТЗ v1.2 — SOFA input parameters formalized (docs only)**

Updated `docs/Технічне завдання карта Інтенсивної терапії.md` to v1.2 (2839 → 3026 lines). The ТЗ was brought in line with the existing implementation — verified `SofaCalculator` (all 13 inputs incl. epinephrine), `SofaForm` (4 vasopressors, GCS, creatinine, 24h urine output), `HourlyRecord.meanArterialPressure`; no code changes needed:

- §29/§30: GCS (3–15) field added to general state + hourly monitoring; FiO₂ defined (%); MAP marked auto-calculated with formula `MAP = (2 × ДАТ + САТ) / 3`; new block «Вазопресорна та інотропна підтримка» (допамін, добутамін, норадреналін, адреналін у мкг/кг/хв); діурез мл/год + сумарний за 24 години для SOFA
- §36: одиниці вимірювання (тромбоцити ×10⁹/л; креатинін/білірубін мкмоль/л або мг/дл); `pO₂` → `PaO₂` (визначення); автозапис PaO₂/FiO₂
- §53.2: серцево-судинна оцінка SOFA доповнена адреналіном (≤0.1 → 3, >0.1 → 4), дози у мкг/кг/хв

### Previous sessions (condensed):
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
  src/shared/               ← shared types, API client, components, auth
backend/   (Spring Boot 3.2.5 + Java 17 + Maven, multi-module)
  pom.xml                   ← parent POM (pom packaging, 3 modules)
  common/                   ← shared entities, JWT/security, base classes
  icu-chart/                ← existing app (@SpringBootApplication, single-deployment JAR)
  medication-sheet/         ← new module (auto-scanned under com.superhumans)
tests/     (Playwright 1.61)
```

After login, user lands on `/select` (AppSelectorPage) and picks a sub-app. Routes are prefixed per sub-app:
- `/doctor/*`, `/nurse/*` → ICU chart
- `/prescriptions/doctor/*`, `/prescriptions/nurse/*` → Medication sheet
- `/admin/*` → Admin

- JWT auth stored in `localStorage`.
- Backend port: **8085** (`application.yml`).
- DB: PostgreSQL 16, `ddl-auto: none` — schema managed by Liquibase changelogs.
- Seed data: `backend/src/main/resources/data.sql` (6 users, 3 episodes + 3 open clinical days, `spring.sql.init.mode: always`).
- CI: `.github/workflows/playwright.yml` — Postgres service, JDK 17, Node 22, Playwright chromium, 40min timeout.
- Mock MIS: `MockMisServiceImpl` provides 5 test patients + department/user data.

## Repeatable CI Development Workflow (THE Loop)

**All tests run exclusively via GitHub Actions CI — never locally.** Local `mvn test` / `npm test` / Playwright are FORBIDDEN; `mvn compile`, `npm run lint`, `npx tsc --noEmit` are permitted for pre-flight only.

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

- **Backend**: 557 total tests (from multi-module reactor: common + medication-sheet + icu-chart). JaCoCo 60% instruction / 50% branch minimum. Checkstyle Google checks.
- **Frontend**: ~350 Vitest tests across 44 files (pages, components, AuthContext, endpoints). Run with `npm t`.
- **E2E**: 45 Playwright spec files across 7 projects (setup, login, doctor, nurse, hod, admin, api).

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
| *(backend-only)* | — | AUDITOR |

Mock MIS provides 5 test patients: Петренко, Коваленко, Сидоренко, Бондаренко, Ткачук.

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
| GET | `/api/audit` | Yes | List (paginated, filters: userId, entity, entityId, action, dateFrom, dateTo) |
| GET | `/api/audit/{id}` | Yes | Get single audit log entry |

### Mock MIS Controls
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/mis/error-mode?mode=timeout\|not_found\|unavailable\|none` | Yes | Set mock MIS error simulation |

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
| `/admin` | `AdminPage` | ADMINISTRATOR |

## Frontend Components

### Pages (7)
| File | Description |
|---|---|
| `LoginPage.tsx` | Login form with error handling |
| `doctor/DashboardPage.tsx` | Doctor episode list with patient search |
| `doctor/DepartmentDashboardPage.tsx` | HOD department dashboard with stats |
| `doctor/CreateCardPage.tsx` | Create new episode (select patient, set dates) |
| `doctor/PatientDayPage.tsx` | Doctor view of clinical day (orders, notes, scales, sign-off) |
| `nurse/NurseDashboardPage.tsx` | Nurse episode list with active patients |
| `admin/AdminPage.tsx` | User management tables + audit log viewer |

### Common Components
| File | Description |
|---|---|
| `AuditLogTable.tsx` | Audit log viewer with filters |
| `ClinicalDayTimeline.tsx` | Timeline of clinical days for an episode |
| `DoctorDashboard.tsx` | Doctor dashboard quick-view |
| `EpisodeTable.tsx` | Table of episodes with search/filter |
| `FluidBalancePanel.tsx` | Fluid balance display with intake/output chart |
| `HourSelector.tsx` | Hour picker for vital signs entry |
| `HourlyRecordTable.tsx` | Hourly vital signs grid |
| `IntensiveCareCard.tsx` | Central ICU card component |
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
  src/                 ← 88 TS/TSX source + 44 test files
tests/
  playwright.config.ts ← Playwright config with 7 projects
  package.json         ← Test dependencies
  specs/               ← 45 spec files
  pages/               ← Page Object Model (7 files)
  fixtures/            ← Test fixtures
docs/
  Технічне завдання карта Інтенсивної терапії.md  ← Full technical specification (3026 lines)
.github/
  workflows/playwright.yml  ← CI pipeline (3 jobs: integration-tests, test, format-check)
```
