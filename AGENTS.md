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

**2026-07-28: Bug fix round — issues #82, #83, #84, #87 resolved**

### Issue #84 — Remove nav buttons from GlobalLayout header
- Removed "Пацієнти" and "Призначення" buttons from `GlobalLayout.tsx` header
- Nav now only shows "Відділення" (HOD) and "Додатки" (all users)
- Updated GlobalLayout tests (14 pass): Пацієнти/Призначення verified as absent

### Issue #83 — Nurse prescription patient list
- Rewrote `NursePrescriptionPage.tsx` to display sortable department-based patient table (Хірургія/Реабілітація toggle)
- Search, sort, and open prescription list per patient — same UX as `PrescriptionPage`
- Previously showed only PatientSearch field; now shows full patient list by default

### PostgreSQL 18 — ICU locale configuration
- Windows `psql` displayed Ukrainian as garbage (`������`, `РџРћРњРР›РљРђ`)
- Root cause was **not** the database — `SERVER_ENCODING` was already UTF8
- Problem: PostgreSQL used `libc` locale with `Ukrainian_Ukraine.1251` — CP1251 collation in a UTF-8 console
- Fixed `lc_messages`: `ALTER SYSTEM SET lc_messages = 'English_United States.1252'` → clean English output
- Created new database with **ICU locale**: `LOCALE_PROVIDER icu`, `ICU_LOCALE 'uk-UA'`, `ENCODING 'UTF8'` (requires `TEMPLATE template0` since `template1` uses libc)
- Verified: `datlocprovider = i` (ICU), `datlocale = uk-UA`; Ukrainian text + emoji (🚀) store and display correctly
- `spring.datasource.url` stays standard — no `characterEncoding=UTF-8` (MySQL-specific)
- Migration path: `pg_dump` from libc DB → `psql` restore into new ICU DB

### Issue #82 — Cyrillic encoding fix

**Root cause:** `scripts/generate-prescription-seed.cjs` used `console.log()` to output SQL. On Windows PowerShell, `console.log` pipes through `process.stdout` which defaults to UTF-16LE encoding (the PowerShell console code page). The generated `prescription-seed.sql` and `prescription-seed-1000.sql` were therefore UTF-16LE files. When these files were concatenated into `data.sql` (a UTF-8 file), the UTF-16LE BOM (`FF FE`) and embedded null bytes between ASCII characters caused PostgreSQL to store `document_name` as garbled text.

**Fix (3 layers):**

1. **Generator script** (`scripts/generate-prescription-seed.cjs` lines 213-219): Replaced `console.log(output)` with `fs.writeFileSync(path, output, 'utf8')` for file output and `process.stdout.write(output, 'utf8')` for stdout. These use Node.js Buffer with explicit UTF-8 encoding instead of the console's code-page-dependent encoding.

2. **Seed files**: Converted `scripts/prescription-seed.sql` and `scripts/prescription-seed-1000.sql` from UTF-16LE to UTF-8 (stripped BOM, re-encoded). File sizes dropped ~50% due to null byte removal.

3. **Auto-heal existing DB** (`data.sql`): Changed 90 `prescription_lists` INSERTs from `ON CONFLICT (id) DO NOTHING` to `ON CONFLICT (id) DO UPDATE SET document_name = EXCLUDED.document_name`. This overwrites garbage Cyrillic in any database that was seeded with the corrupted files, without requiring a full `DROP SCHEMA`.

**Verification:**
- `file scripts/prescription-seed.sql` reports "UTF-8 Unicode text" (was "Little-endian UTF-16 Unicode text")
- `hexdump -C | head` shows no BOM and single-byte ASCII characters
- All 390 frontend Vitest tests pass, backend compiles clean
- Running this against a corrupted DB will auto-correct the 90 document names on next startup

### Issue #87 — Prescription list dropdown for patients with multiple lists
- 90 patients have prescription lists (1001-1025 surgery, 1026-1050 rehab, 2001-2020 surgery, 2021-2040 rehab) — existing seed in `data.sql` has one `Active` list each
- No backend changes needed: `GET /api/prescriptions?patientId=X` already returns all lists regardless of status
- Added `'Active'` to `PrescriptionListStatus` type in `frontend/src/types/index.ts`
- Rewrote `PrescriptionPage.tsx` and `NursePrescriptionPage.tsx`: `PatientRow.lists: PrescriptionList[]` replaces single `list`; Відкрити button and patient name link show MUI dropdown Menu when multiple lists exist
- Generated `scripts/generate-closed-prescriptions.cjs` → `scripts/closed-prescription-seed.sql` (270 `Finished` lists, 1-5 per patient); appended to `data.sql`
- `getStatusText`/`getRowStyle` evaluate across all lists: any non-Finished → "В ході", all Finished → "Завершено", none → "Заплановано"
- Frontend: 39 test files, 310 tests, 0 failures. Backend compile + frontend typecheck pass

### Frontend — global theme
- **Default theme changed to light mode** (`dark` → `light` in ThemeContext)
- **ThemeToggle** shared component (`components/common/ThemeToggle.tsx`) — DarkMode/LightMode icon + tooltip
- Added to: DoctorLayout, NurseLayout, PrescriptionPage, PrescriptionDetailPage, NursePrescriptionPage, AdminPage, AppSelectorPage
- Theme tests updated (11 tests pass)

### Frontend — GlobalLayout
- **GlobalLayout** (`layouts/GlobalLayout.tsx`) — unified AppBar header for all authenticated routes
- Dynamic title/subtitle based on route (`useAppInfo()`):
  - `/doctor/*`, `/nurse/*` → "ВАІТ" / "Карта інтенсивної терапії"
  - `/prescriptions/*` → "Призначення" / "Листок лікарських призначень"
  - `/admin/*` → "Адмін" / "Адміністративна панель"
  - `/select` → "Superhumans Lviv" / "Вибір додатку"
- Smart nav: Пацієнти, Призначення (doctor/nurse), Відділення (HOD only), Додатки
- DoctorLayout/NurseLayout simplified to outlet-only wrappers
- Login page outside GlobalLayout (still standalone)

### Frontend — PrescriptionGrid
- `PrescriptionGrid.tsx` — inline 21-day spreadsheet (510 lines)
- Doctor mode: click to plan dose, middle-click to cancel
- Nurse mode: click to execute dose with popover (dose + 2-factor auth)
- 7-day scroll window, color-coded cells (blue=planned, green=completed, purple=cancelled)
- Add/remove medicine with allergy check, delete confirmation popover
- Used by: PrescriptionDetailPage (doctor), NursePrescriptionPage (nurse)

### Frontend — PrescriptionPage (dashboard)
- Department toggle: Хірургія (2001-2020) / Реабілітація (2021-2040)
- Sortable patient table (name, document), search filter (name, ID, card number)
- Create/Open buttons per patient

### Exploratory testing
- 4 bugs found → Issues #71-#74 created
- #71 (HIGH): Cyrillic text in data.sql stored as Windows-1251 in UTF-8 file
- #72 (MEDIUM): MockMIS patient names have department prefix (e.g. "Хірург Бойко...")
- #73 (MEDIUM): Nurse detail view — missing route + grid does not render
- #74 (LOW): Ghost empty button in doctor dashboard table

### Test summary
| Module | Tests | Type |
|---|---|---|
| medication-sheet | 88 | Unit (service + controller) |
| medication-sheet | 22 | Integration (in icu-chart) |
| icu-chart | 312 | Unit |
| **Backend total** | **422** | |
| Frontend | 310 | Vitest (39 files, 0 failures) |
| E2E | 38 | Playwright specs |

### Previous sessions (condensed):
- 2026-07-26 (earlier): Medication Sheet backend — Phase 3-6 complete (EmailService, PrescriptionSchedulerService, controllers + security, integration tests, docs). 422 backend tests.
- 2026-07-25: Exploratory testing — 5 bugs fixed (entity/DTO validation, scales, UUID errors). Model QA audit — grade B, all gaps fixed. 27 validation tests added. Backend: 312 tests.
- 2026-07-23: Frontend error display — `getErrorMessage()` helper, used in all 8 API catch blocks. `InvalidDataAccessApiUsageException` → 400 handler.
- 2026-07-22: Controller tests fixed (106/106). BYTEA column fix. Seed data day_number swap. Checkstyle.
- 2026-07-21: PDF layout (003-15/о) corrected. Two-column layout. Autosave. DayNumber ASC sorting.

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
- DB: PostgreSQL 16, `ddl-auto: update` — schema auto-created by Hibernate.
- Seed data: `backend/src/main/resources/data.sql` (6 users, 3 episodes + 3 open clinical days, `spring.sql.init.mode: always`).
- CI: `.github/workflows/playwright.yml` — Postgres service, JDK 17, Node 22, Playwright chromium, 40min timeout.
- Mock MIS: `MockMisServiceImpl` provides 5 test patients + department/user data.

## Main Test Scenario

**All tests run exclusively via GitHub Actions CI — never locally.**

| Test type | CI job | Trigger |
|---|---|---|
| Backend unit + integration (419) | `test` → `mvn clean verify` | Push to `main` / `develop` or PR to `main` |
| Backend integration (79) | `integration-tests` → `mvn test -Pintegration-test` | Same |
| Frontend Vitest (~390) | `test` → `npm test` | Same |
| Playwright E2E (38 spec files) | `test` → `npx playwright test` | Same |
| Format / Checkstyle | `format-check` → `mvn compile checkstyle:check` | Same |

Push → CI runs all 3 jobs in parallel → if any fails, fix and repeat until green.

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
| `npm t` or `npx vitest run` | Run Vitest tests (~390 across 38 files) |

### Playwright (`cd tests`)
| Command | Action |
|---|---|
| `npx playwright test` | Run all E2E tests (38 spec files) |
| `npx playwright test --list` | List tests without running |
| `npx playwright show-report` | View HTML report |

## Testing

- **Backend**: 419 total tests (from multi-module reactor: common + medication-sheet + icu-chart). JaCoCo 60% instruction / 50% branch minimum. Checkstyle Google checks.
- **Frontend**: ~390 Vitest tests across 38 files (pages, components, AuthContext, endpoints). Run with `npm t`.
- **E2E**: 38 Playwright spec files across 7 projects (setup, login, doctor, nurse, hod, admin, api).

## Playwright Projects

| Project | Depends On | storageState | Tests |
|---|---|---|---|
| setup | — | — | Auth setup (4 roles) |
| login-chromium | — | none | Login flow |
| doctor-chromium | setup | `.auth/doctor.json` | Dashboard, create card, prescriptions, notes, sign-off |
| nurse-chromium | setup | `.auth/nurse.json` | Dashboard, vitals, fluid balance, order execution |
| hod-chromium | setup | `.auth/hod.json` | Dashboard, clinical day reopen |
| admin-chromium | setup | `.auth/admin.json` | User tables |
| api-chromium | — | none | Patient search API, error handling |

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
- `a3333333`: `notes`, `notes-full`, `prescriptions`, `prescription-cancel`

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
| `HourlyRecord` | BaseEntity | clinicalDay(M→1), recordTime, recordHour, consciousness, temperature(34-42), heartRate(0-300), respiratoryRate(0-60), systolicBP(50-250), diastolicBP(30-150), spo2(50-100), glucose(1-30), etco2, fio2, cvp, urineOutput, drainOutput, stool, vomit, painScore, notes | UNIQUE(clinical_day_id, record_hour); ranges validated in @PrePersist/@PreUpdate |
| `MedicalOrder` | BaseEntity | clinicalDay(M→1), category, drugName, dose, unit, route, frequency, startTime, endTime, status(MedicalOrderStatus) | Status: DRAFT/ACTIVE/COMPLETED/CANCELLED |
| `OrderExecution` | BaseEntity | order(M→1), executedBy, executedAt, actualDose, status(OrderExecutionStatus), comment | Status: PLANNED/IN_PROGRESS/COMPLETED/PARTIALLY_COMPLETED/CANCELLED |
| `MedicalNote` | BaseEntity | clinicalDay(M→1), authorId, role, noteType, text(TEXT) | — |
| `ScaleResult` | BaseEntity | clinicalDay(M→1), scale(M→1), result, calculatedAt, calculatedBy | Auto-calculates GCS/RASS from consciousness |
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

### Common Components (18)
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
| `ScaleResultsPanel.tsx` | Clinical scale results display |
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
`LoginRequest`, `EpisodeCreateRequest`, `EpisodePatchRequest`, `EpisodeCloseRequest`, `ClinicalDayCreateRequest`, `ClinicalDayPatchRequest`, `HourlyRecordCreateRequest`, `HourlyRecordPatchRequest`, `MedicalOrderCreateRequest`, `MedicalOrderPatchRequest`, `MedicalNoteCreateRequest`, `MedicalNotePatchRequest`, `ScaleResultCreateRequest`, `ScaleResultPatchRequest`, `OrderExecutionCreateRequest`, `OrderExecutionPatchRequest`, `SignRequest`, `ReopenRequest` (18 total)

### Response DTOs
`LoginResponse`, `EpisodeResponse`, `ClinicalDayResponse`, `HourlyRecordResponse`, `MedicalOrderResponse`, `OrderExecutionResponse`, `MedicalNoteResponse`, `ScaleResultResponse`, `FluidBalanceResponse`, `SignResponse`, `PdfResponse`, `UserResponse`, `AuditLogResponse`, `ErrorResponse` (14 total)

## Backend Services (12)

| Service | Responsibility |
|---|---|
| `AuthService` | Login with password verification + JWT generation |
| `EpisodeService` | CRUD + search + close/archive with optimistic locking |
| `ClinicalDayService` | CRUD + signing workflow + reopen with signature revocation + next-day gating |
| `HourlyRecordService` | CRUD with clinical day lock checking |
| `MedicalOrderService` | CRUD + cancel with status validation |
| `OrderExecutionService` | CRUD with order status validation |
| `MedicalNoteService` | CRUD with author role assignment |
| `ClinicalScaleService` | Scale results + automatic GCS/RASS from consciousness |
| `FluidBalanceService` | Recalculation from HourlyRecord + OrderExecution |
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
- **DB**: `ddl-auto: update` — never write manual DDL; schema auto-created by Hibernate from entity annotations
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
  src/main/java/       ← 161 Java source files
  src/main/resources/  ← application.yml, data.sql, PDF template
  src/test/java/       ← 54 test files (32 unit + 13 integration + 1 abstract + 8 more)
frontend/
  package.json         ← Dependencies
  vite.config.ts       ← Vite build config
  tsconfig*.json       ← TypeScript configs
  index.html           ← App entry HTML
  public/              ← Static assets
  src/                 ← 59 TS/TSX source + 22 test files
tests/
  playwright.config.ts ← Playwright config with 7 projects
  package.json         ← Test dependencies
  specs/               ← 38 spec files
  pages/               ← Page Object Model (7 files)
  fixtures/            ← Test fixtures
docs/
  Технічне завдання карта Інтенсивної терапії.md  ← Full technical specification (2839 lines)
.github/
  workflows/playwright.yml  ← CI pipeline (3 jobs: integration-tests, test, format-check)
```
