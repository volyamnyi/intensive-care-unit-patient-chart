# ICU Patient Chart — AI Agent Guide

## Current Session

**Exploratory testing + Model QA audit — 5 bugs fixed, 5+ new gaps found (2026-07-25):**

### SBTM Exploratory Testing Session (2h)
- **5 bugs found via Tours**: Saboteur (negative values), Garbage Collector (missing validation), Landmark (scales), Failure (error handling), FedEx (data integrity)
- **Bug 1+2**: `urineOutput`/`drainOutput` accept negative values — added `@DecimalMin` annotations + `@PrePersist` runtime checks in `HourlyRecord.java`
- **Bug 3**: Duplicate `record_hour` — already fixed (service-layer check + `@UniqueConstraint` + 409 handler)
- **Bug 4**: Clinical scales `GET /api/scales` returns `[]` — added 4 scales (GCS, RASS, SOFA, APACHE II) to `data.sql`
- **Bug 5**: 9+ unhandled promise rejections on invalid episode UUID — added `.catch()` in `PatientDayPage.tsx`
- **Verification**: `mvn test` 291/291 pass, `npm run build` clean
- **Commit**: `3ec3672`

### Model QA Specialist Audit (Grade: B)
- **DTO-layer validation gap (Medium)**: `HourlyRecordCreateRequest`/`HourlyRecordPatchRequest` lack `@DecimalMin` on `urineOutput`/`drainOutput` — validation only fires at entity `@PrePersist` level, giving worse error UX (DTO fields need annotations)
- **Unvalidated numeric fields (Medium)**: `painScore` (no 0–10 bounds), `etco2`, `fio2`, `cvp` have no validation annotations
- **No automated tests (Medium)**: 0 tests added alongside the 5 fixes — urineOutput/drainOutput validation, scale API, UUID error handling all lack regression tests
- **Bug 5 `.catch(() => {})` swallows errors silently** — should log with `console.warn`
- **Imbalance risk**: `setLoading(true)` called before `if (!episodeId) return;` — could leave spinner hanging

### Follow-up ET Session (Live API Testing)
- **Bug 1/2 confirmed NOT FIRING on running server** — negative values `-50.0`/`-30.0` accepted because DTOs lack validation annotations and server may be stale
- **Bug 3 ✅ PASS** — 409 Conflict on duplicate hour
- **Bug 4 ⚠️ Root cause discovered**: `clinical_scales` table was empty despite `data.sql` — likely UTF-8 encoding issue with Ukrainian text (`"Шкала коми Глазго"`) on Windows PostgreSQL `spring.sql.init` connection
- **Bug 5** — not yet tested
- **Pre-existing anomaly**: HourlyRecord `79782047-...` has `urineOutput: -999999.0` in DB

### Fixes Applied (This Session)
- **DTO validation gap fixed**: Added `@DecimalMin("0.0")` on `urineOutput`/`drainOutput` + all entity-consistent annotations (`@DecimalMin`/`@DecimalMax` for temperature, spo2; `@Min`/`@Max` for heartRate, respiratoryRate, systolicBP, diastolicBP, painScore) in both `HourlyRecordCreateRequest.java` and `HourlyRecordPatchRequest.java`
- **Unvalidated fields fixed**: Added `ClinicalConstants` for `PAIN_SCORE` (0–10), `ETCO2` (0–100), `FIO2` (0–1.0), `CVP` (0–30). Added `@Min`/`@Max`/`@DecimalMin`/`@DecimalMax` on entity fields + runtime checks in `validateClinicalRanges()`. Same annotations added to both DTOs.
- **PatientDayPage.tsx fixed**: `.catch(() => {})` → `.catch((err) => { console.warn('Failed to load episode:', err); })`
- **`setLoading(true)` imbalance** — determined NOT a bug: guard is at the top of the effect, `setLoading(true)` runs only when `episodeId` is truthy, so no spinner-hanging issue.
- **3 failing PatientDayPage tests fixed**: Test mock `useAuth()` returned a new `user` object ref on every render, causing `useEffect([episodeId, user])` to refire with `setLoading(true)` — moved to stable `TEST_USER` constant with `as const`.
- **5 GitHub issues created** (#8–#12) tracking DTO gap, unvalidated fields, missing tests, silent catch, seed encoding

### Still Pending
- Test Bug 5 on running server (invalid UUID console errors)

### Validation Tests Added
- **27 new tests in `HourlyRecordValidationTest.java`** covering all 6 newly-validated fields:
  - `urineOutput`: below-min (-0.1) → throws, at-min (0.0) → OK, positive (150.0) → OK
  - `drainOutput`: below-min (-0.1) → throws, at-min (0.0) → OK, positive (75.0) → OK
  - `painScore`: below-min (-1) → throws, above-max (11) → throws, at-boundaries (0, 10) → OK
  - `etco2`: below-min (-0.1) → throws, above-max (100.1) → throws, at-boundaries (0.0, 100.0) → OK, valid (38.0) → OK
  - `fio2`: below-min (-0.1) → throws, above-max (1.1) → throws, at-boundaries (0.0, 1.0) → OK, valid (0.4) → OK
  - `cvp`: below-min (-0.1) → throws, above-max (30.1) → throws, at-boundaries (0.0, 30.0) → OK, valid (8.0) → OK
- Total backend tests: 312 (was 291, +27 with 0 failures)
- Follows exact same pattern as existing tests (AssertJ `assertThatThrownBy`/`assertThatCode`)

**Previous sessions (condensed):**
- 2026-07-23: Frontend error display — `getErrorMessage()` helper (extracts `err.response?.data?.message`), used in all 8 API catch blocks. `OrderInlineForm` `onError` prop. `InvalidDataAccessApiUsageException` caught in `GlobalExceptionHandler` → 400.
- 2026-07-22: Controller tests fixed (106/106) — `@WebMvcTest` + JWT mocks. `GeneratedPdf` BYTEA column fix. Seed data day_number swap. Checkstyle `failsOnError: true`.
- 2026-07-21: PDF layout (003-15/о) corrected. Two-column frontend layout. Autosave §60. DayNumber ASC sorting.

## Architecture

```
frontend/  (React 19 + TS 6 + Vite 8 + MUI 9)
backend/   (Spring Boot 3.2.5 + Java 17 + Maven)
tests/     (Playwright 1.61)
```

- Monorepo with three independent packages (no workspace orchestration).
- JWT auth stored in `localStorage`. API base URL: `frontend/src/api/client.ts` → `http://localhost:8085/api`.
- Backend port: **8085** (`application.yml`).
- DB: PostgreSQL 16, `ddl-auto: update` — schema auto-created by Hibernate.
- Seed data: `backend/src/main/resources/data.sql` (6 users, 3 episodes + 3 open clinical days, `spring.sql.init.mode: always`).
- CI: `.github/workflows/playwright.yml` — Postgres service, JDK 17, Node 22, Playwright chromium, 40min timeout.
- Mock MIS: `MockMisServiceImpl` provides 5 test patients + department/user data.

## Main Test Scenario

**All tests run exclusively via GitHub Actions CI — never locally.**

| Test type | CI job | Trigger |
|---|---|---|
| Backend unit (151) | `test` → `mvn clean verify` | Push to `main` / `develop` or PR to `main` |
| Backend integration (79) | `integration-tests` → `mvn test -Pintegration-test` | Same |
| Frontend Vitest (~190) | `test` → `npm test` | Same |
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
| `mvn test` | Run 151 unit tests (excludes integration) |
| `mvn test -Pintegration-test` | Run 79 integration tests (requires Docker/PostgreSQL) |
| `mvn verify` | Run all + JaCoCo coverage check + Checkstyle |

### Frontend (`cd frontend`)
| Command | Action |
|---|---|
| `npm run dev` | Vite dev server on `:5173` |
| `npm run build` | `tsc -b && vite build` |
| `npm run lint` | Oxlint |
| `npx tsc --noEmit` | Type-check without build |
| `npm t` or `npx vitest run` | Run Vitest tests (~190 across 20 files) |

### Playwright (`cd tests`)
| Command | Action |
|---|---|
| `npx playwright test` | Run all E2E tests (38 spec files) |
| `npx playwright test --list` | List tests without running |
| `npx playwright show-report` | View HTML report |

## Testing

- **Backend**: 151 unit tests (14 classes) + 79 integration tests (13 classes, Testcontainers PostgreSQL) = 35 total test files. JaCoCo 60% instruction / 50% branch minimum. Checkstyle Google checks.
- **Frontend**: ~190 Vitest tests across 22 files (pages, components, AuthContext, endpoints). Run with `npm t`.
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
- **Test seed data**: Integration tests use `data-test.sql` (in `src/test/resources/`) with plain INSERTs on a fresh Testcontainers PostgreSQL database. The production `data.sql` keeps `ON CONFLICT (id) DO NOTHING` for local dev resilience but modified data may persist across restarts. Reset with `DROP SCHEMA public CASCADE; CREATE SCHEMA public;` in PostgreSQL before the next run.

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
