# ICU Patient Chart — AI Agent Guide

## Current Session

**2026-08-19: Responsive UI Phase 4 — wizard steppers, sticky CTAs, mobile dialogs (issue #163)** — commit `f73bc58`, CI run `32286616343` all 6 jobs green (E2E 10m28s), issue #163 closed. Layout/interaction markup only; no Vitest spec updates were needed (all assertions are role/text-based; responsive classes are inert in jsdom). `SetupSteps.tsx` rewritten on the `Stepper` primitive (`size="md"`, `gap-1.5`): each step title is `hidden md:inline` except the active one → mobile shows compact dots + active label only (labels verified rendered exactly once in `SetupSteps.test.tsx`); App.tsx routes the `*Page.tsx` wrappers, the `*Step.tsx` components are Vitest-only. Setup pages (`PatientSearchPage`, `OrderSelectPage`, `OrderReviewPage`, `TemplateSelectPage`) + `WizardScreen`: sticky bottom action bars `sticky bottom-0 z-10 -mx-4 sm:-mx-6 border-t bg-background/95 backdrop-blur` with `pb-[max(0.75rem,env(safe-area-inset-bottom))]` (safe-area) and stacked `flex-col sm:flex-row` buttons (`w-full sm:w-auto`); `WizardScreen` also fixed the top-bar `-mx-6` overflow in the `p-4` mobile `ProcessLayout` main, progress row `flex-wrap`, stage chips `overflow-x-auto` + `shrink-0 whitespace-nowrap`. **Mobile fullscreen dialogs**: `DialogContent` gains `mobileFullscreen?: boolean` → `data-fullscreen="mobile"`; new `index.css` rules under `@media (max-width: 639.98px)` — `inset: 0`, full width/height, `border-radius: 0`, `translate: none` (Tailwind v4 translate utilities use the `translate` property, not `transform`), overlay `backdrop-filter: none` + dark tint; existing `[data-fullscreen]` modalMorph animations apply automatically. Used by `ClosePrescriptionDialog` + WizardScreen pause/fail dialogs; `HourlyGridDialog` passes `data-fullscreen="true"` via `{...props}` spread AFTER the new attribute → unaffected. Touch targets: all wizard secondary buttons + CTAs, `QualityGatePanel` criteria rows (`min-h-11`) and decision buttons (`flex-col sm:flex-row`, `w-full sm:w-auto`), `MedicineSearchInput`/`PrescriptionItemForm` dropdown rows + add buttons, `VitalSignForm` save, `DayPartPlanner` plan/complete buttons, `ScaleResultsPanel` select/input/Додати. ICU scale forms: `ApacheIiForm` grid → `grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4`, `SofaForm`/`BradenForm` → `grid-cols-1 sm:grid-cols-2 md:grid-cols-3`; all four `h-7 text-xs` inputs replaced with `pointer-coarse:min-h-11` (touch-only sizing); `CamIcuForm` labels + `RassSelector` trigger likewise. Verified already-compliant (no edits): `PrescriptionSpreadsheet` horizontal `overflow-auto` + sticky first column (Task 7), `MeasurementForms` `inputMode="decimal"` + `flex-col md:flex-row` (Task 4).

**2026-08-18: Distinct `PRESCRIPTION_LIST_CREATE` permission (create a prescription LIST, separate from `PRESCRIPTION_CREATE` planning)** — new RBAC code `PRESCRIPTION_LIST_CREATE` «Створення листка лікарських призначень» (catalog 24→25, `PermissionCatalog` — new constant + `Def` + DOCTOR/HOD default-grant rows; `PRESCRIPTION_CREATE` «Створення призначень» label kept, its description narrowed to "Планування призначень: додавання позицій у листок, планування та скасування доз"). `POST /api/prescriptions` (`PrescriptionController:70`) now gated on `PRESCRIPTION_LIST_CREATE`; item/plan/execute endpoints keep `PRESCRIPTION_CREATE`. Seeding: definition row added as a NEW Liquibase changeset `core/005-role-permissions-add-prescription-list-create.sql` (registered in `db.changelog-master-core.yaml`; `003` left untouched to preserve applied-changeset checksums); DEFAULT GRANTS are Java-seeded by `PermissionService.seedIfEmpty()` from `PermissionCatalog.defaultMatrix()` (which now includes `PRESCRIPTION_LIST_CREATE` for DOCTOR + HOD, not NURSE) — so `005` intentionally adds only the definition row, never grants (pre-inserting grants would flip `rolePermissionRepository.count()` non-zero and suppress the full default-matrix seed on a fresh install). Frontend: `PrescriptionPage.tsx` gates the drawer «Створити листок» button on `hasPermission('PRESCRIPTION_LIST_CREATE')` (destructure `hasPermission` from `useAuth`). `PermissionServiceTest` updated: catalog `hasSize(25)`, `PRESCRIPTION_LIST_CREATE` in `allCodes()`. Pre-flight green (mvn compile, oxlint 0 errors, tsc clean).

**2026-08-18: Phase 8 — documentation update (issues #157/#158 milestone complete)** — AGENTS.md + README.md rewritten (commit `0f015b6`) to reflect the post-refactor reality: backend Maven module layout with dependency direction (`common` leaf ← `icu-chart`/`medication-sheet`/`prosthesis-manufacturing` ← `app` shell), real frontend layout (`pages/`, per-feature `components/`, `api/` + `types/` modules, isolated `prosthetics/`), new «Module Boundaries» section (ArchUnit allowlist + oxlint `no-restricted-imports`), route prefixes `/icu/doctor/*`/`/icu/nurse/*`, Playwright 9 projects / 6 roles, UserRole 7 values, services grouped by module (common 4, icu-chart 16, med 11, prosth 11), Liquibase 15 changesets (core 4, icu 6, med 1, prosth 4), file counts (Java 348 main/112 test; TS 127 sources/69 test files; E2E 55 specs/~228 tests). README: MUI/Emotion → Base UI + Tailwind CSS (badges, Tech Stack, Project Structure tree), JDK 25, stale test counts replaced with file-based numbers, Module Boundaries paragraph. UseManual.md unchanged (no user-facing change). CI run `32119777622` all 6 jobs green (Code Quality 49s, E2E 11m55s); issue #158 closed. Docs-only commits — Phase 7 (import boundaries, `e7aab6f` + `cf874b2`, run `32116832684` green, #157 closed) and Phase 8 close the Module Separation milestone; only the historical removal notes mention `PRESCRIBER`/`user.permissions`/`src/medication-sheet` — no live references remain.

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

**2026-08-18: Phase 7 — import boundaries enforced with oxlint `no-restricted-imports`** — `.oxlintrc.json` `overrides` (per-directory rule sets, oxlint 1.73): `pages/prescription` → forbid `components/icu` + `components/monitoring`; `pages/prosthetics` → forbid `components/icu` + `components/prescription`; `components/icu` → forbid `components/prescription` + `components/monitoring`; `components/prescription` → forbid `components/icu` + `components/monitoring`; `components/common` → forbid all feature components (`icu`, `monitoring`, `prescription`, `prosthetics`). Patterns are `regex: "(^|/)icu/"`-style — they match BOTH relative specifiers (`../monitoring/criticalRanges`) and the `@/` alias form (`@/components/icu/...`) at any depth. Only straggler fixed: `clinicalRanges.ts` (shared validation bounds, single importer `components/icu/VitalSignsForm.tsx`) moved `components/monitoring/` → `lib/` (its doc comment still warns against merging with `components/monitoring/criticalRanges.ts` — alarm thresholds, distinct concept). Acceptance verified locally: deliberate cross-feature imports (relative + `@/` alias) fail `npm run lint`; full lint 0 errors; build green. CI `format-check` runs the same `npm run lint` with the project config, so the rules are enforced automatically.

**2026-08-18: Phase 6 — ICU-only frontend components/helpers moved out of shared** — commits `a0e654f` (docs) + `1238830` (refactor: 16 components + 6 scales + 3 helpers + 18 tests moved from `components/common/`, `hooks/`, `utils/`, `constants/` into `components/icu/`, `components/prescription/`, `components/monitoring/`; `common/` keeps only PatientSearch/ThemeToggle/AuditLogTable; empty `src/medication-sheet/` deleted). **E2E flake #2 root cause (run `32109411749`, nurse tests at 07:06Z) — NOT a Phase 6 regression**: the med-day is 08:00→07:59, and `HourlyGrid.isPastMedDay(h, realClockHour)` renders every hour before the current real hour as `✓` (non-clickable — `clickable` false in `TherapyCell`). In the 07:00–07:59 UTC window the ONLY clickable hour is the current one (7:00). `nurse-day-flow.spec.ts` planned `realHour+1+retry` → 8/9/10 → all past → click opened nothing → `planInput` never appeared (line 114). The snapshot also showed hour 7:00 already planned with `1000` — that's `modal-therapy.spec.ts`'s leftover (it plans the CURRENT real hour on the same day/order with DOSE `1000` and restores the plan; a doctor can re-plan a planned cell, so `order-execution.spec.ts` — also current-hour — passed). Fix: both nurse specs plan at the CURRENT real hour (never past); `nurse-day-flow` scans up to 24 cells from the current hour for the first non-`✓`/`✕` cell (free `➚` or already-planned dose — re-plannable); `order-execution` switched from the contended `Glucose 5%` (`d3333001`) to the runtime-created `Dobutamine` order (doctor-day-flow creates it earlier in the same run; free in every hour). Verified green for all 24 real hours; residual edge (accepted): a retry after a completed attempt in the 07:00–07:59Z window finds no free Glucose cell.

**2026-08-18: Phase 5 — frontend API clients & DTO types split into per-feature modules**

`frontend/src/api/endpoints.ts` + `frontend/src/types/index.ts` deleted (no barrels). New modules: `api/platform.ts` (authApi, patientApi, userApi, settingsApi, auditApi, adminApi), `api/icu.ts` (episodeApi, clinicalDayApi, hourlyRecordApi, medicalOrderApi, orderExecutionApi, medicalNoteApi, clinicalScaleApi, fluidBalanceApi, pdfApi, patientStateApi, ventilationApi, labResultApi, departmentApi), `api/medication.ts` (prescriptionApi, vitalSignApi); types likewise split into `types/{core,icu,medication}.ts` (core holds shared `PatientDto` + auth/RBAC/audit DTOs). `patientApi`/`PatientDto` live in platform/core because medication-sheet pages consume them (mirrors backend MIS-in-common). `api/client.ts` + `api/prosthetics.ts` + `prosthetics/types.ts` unchanged (prosthetics was already isolated). Every importer migrated to direct relative imports (`AuthContext.tsx` now imports `../api/platform`); no feature file imports another feature's API/types module; `endpoints.test.ts` and all test `vi.mock` factories retargeted (PatientDayPage.test.tsx mocks icu + platform separately). Automated via temp script `migrate_imports.py` (statement-aware, symbol→module map; prosthetics `./types`/`@/prosthetics/types` untouched).

Phase 5 shipped: commits `871951f` (migration + barrel deletion, 78 files) + `6116206` (E2E fix), CI run `32106534596` all 6 jobs green, issue #155 closed. **E2E flake root cause (found during Phase 5 CI runs `32104710009`/`32105901291`, 05:54/06:11Z) — NOT a Phase 5 regression**: `security-rules.spec.ts` planned a FIXED hour 6 on seed order `d3333001`, but `tests/specs/nurse/order-execution.spec.ts:31` plans+completes the REAL current hour (`new Date().getHours()`) on the same order — at CI times 05:00–06:59 UTC real hour = 6 → `OrderExecutionService.plan` throws `DocumentLockedException` → 422 `DOCUMENT_LOCKED` "Execution for this hour is already completed" → `expect(doctorPlan.ok()).toBeTruthy()` false. Confirmed from the trace artifact (retry1 `trace.zip`, response body sha1 `16c436ce…`). Fix (`6116206`): plan hour is now dynamic `new Date(Date.now() + 2 * 3600_000).getHours()` — offset from the real hour, so it never collides at any CI time (also distinct from `nurse-day-flow`'s `realHour+1`).

**2026-08-17: Phase 4 — legacy `users.permissions` CSV removed from the auth flow**

- `LoginResponse` no longer carries the legacy CSV permissions field; `AuthService` no longer populates it; `AuthController` mints JWTs via the 3-arg `JwtTokenProvider.generateToken(login, role, userId)`; the JWT `permissions` claim, the 4-arg `generateToken` overload and `getPermissionsFromToken` are deleted (no consumers existed — enforcement is matrix-based via `PermissionService`). `AuthContext.tsx` + TS `User`/`LoginResponse` types drop the legacy field; effective permissions come solely from `GET /api/users/me/permissions`. The `users.permissions` DB column, entity field and `002-user-permissions.sql` changeset remain untouched (schema never edited); `User.hasPermission/addPermission/removePermission` stay as entity utilities.

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
- The legacy per-user `PRESCRIBER` CSV (`user.permissions` column) is no longer part of the auth flow (removed 2026-08-17, see Current Session); the column and entity field remain for schema/data compatibility; `ScaleAuthorizationService` is now permission-driven (DOCTOR may create Браден per matrix).

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
frontend/  (React 19 + TypeScript 6 + Vite 8 + Tailwind CSS 4 + Base UI, single app — no feature subfolder roots; pages/components live at `src/` root)
  src/pages/               ← route pages: LoginPage, AppSelectorPage, doctor/, nurse/, prescription/, prosthetics/, admin/
  src/components/          ← per-feature components: icu/, monitoring/, prescription/, common/, prosthetics/, navigation/, ui/ (shadcn-style Base UI components)
  src/api/                 ← per-feature API clients: client.ts (axios instance), platform.ts, icu.ts, medication.ts, prosthetics.ts
  src/types/               ← shared DTO types: core.ts, icu.ts, medication.ts (prosthetics types live in src/prosthetics/types.ts)
  src/prosthetics/         ← prosthetics feature root: ProstheticsContext, types, validation, failureCategories
  src/services/, src/layouts/, src/lib/, src/utils/  ← AuthContext, Doctor/Nurse/Global layouts, shared helpers
backend/   (Spring Boot 4.1.0 + Java 25 + Maven, multi-module; dependency direction: common ← feature modules ← app)
  pom.xml                   ← parent POM (pom packaging, 5 modules)
  common/                   ← shared platform leaf (no internal deps; 119 main sources): `@SpringBootApplication` main class `com.superhumans.IcuPatientChartApplication` (mainClass of the runnable JAR), platform controllers (auth/user/patient/admin/audit/settings/mock-MIS), `entity/base` (BaseEntity) + `entity/core` (User, UserRole, Permission, RolePermission, AuditLog, SystemSettings, ReferenceValue), `repository/core`, auth (JWT), config (security, CORS, multi-DB wiring, SpringContext), exception, mapper, mis, service (AuthService, AuditService, PermissionService, PermissionCatalog), util
  icu-chart/                ← ICU chart feature (84 main sources): `com.superhumans.icu.*` (entities + repositories) + ICU domain root packages (controller ×13, service ×20, dto, mapper); depends on common
  medication-sheet/         ← medication sheet feature (61 main sources): `com.superhumans.medicationsheet.*` (entity/dto/repository/service/controller/mapper/config); depends on common
  prosthesis-manufacturing/ ← prosthetics manufacturing feature (84 main sources): `com.superhumans.prosthesismanufacturing.*` (entity/dto/repository/service/controller/mapper/config); depends on common
  app/                      ← deployable shell (no production code): depends on common + 3 features; the spring-boot plugin repackages the runnable JAR (mainClass in common); hosts the ArchUnit boundary test (`app/src/test/java/com/superhumans/architecture/ModuleBoundaryTest.java`)
tests/     (Playwright 1.61)
```

After login, user lands on `/select` (AppSelectorPage) and picks a sub-app. Routes are prefixed per sub-app:
- `/icu/doctor/*`, `/icu/nurse/*` → ICU chart
- `/prescriptions/doctor/*`, `/prescriptions/nurse/*` → Medication sheet
- `/prosthetics/*` → Prosthetics manufacturing
- `/admin/*` → Admin

- JWT auth stored in `localStorage`.
- Backend port: **8085** (`application.yml`).
- **Databases (PostgreSQL 16, one per module)** — 4 physical DBs, `ddl-auto: none`, schema per DB managed by its own Liquibase changelog (15 SQL changesets total: core 4, icu 6, med 1, prosth 4 — all in `common/src/main/resources/db/changelog/{core,icu,med,prosth}/`):

  | Database | Module | Purpose / contents |
  |---|---|---|
  | `my_fullstack_core` | COMMON (single-deployment core) | Users & authentication, dynamic RBAC (`permissions` + `role_permissions` matrix), audit log (`audit_logs`), system settings and reference values |
  | `my_fullstack_icu` | ICU Chart | Episodes, clinical days, hourly records, medical orders & executions, notes, clinical scale results, signatures, generated PDFs, labs, ventilation, patient state, fluid balance |
  | `my_fullstack_med` | Medication Sheet | Prescription lists/items/days/parts/executions/signatures, vital sign lists, medicine/allergy/drug-interaction caches, telegram subscriptions |
  | `my_fullstack_prosth` | Prosthetics Manufacturing | Patients, orders, flow templates, flow instances & step executions, quality gates & decisions, failure snapshots, evidence files |
  | `my_fullstack_db` | — (bootstrap only, **not used by the app**) | Default database auto-created by the PostgreSQL Docker service container in CI (`POSTGRES_DB` env var, required by the image); the application never connects to it — all CI DB-using jobs create the 4 real databases above inside that container (`CREATE DATABASE` ×4) |

  - Datasources configured in `application.yml` under `app.datasource.{core,icu,med,prosth}.{url,username,password}` (env override: `APP_DATASOURCE_*_URL/USERNAME/PASSWORD`); multi-DB bootstrap in `com.superhumans.config.multidb` (per-DB `DataSource`/EMF/`SpringLiquibase`/`JpaTransactionManager`, chained `transactionManager`).
- Seed data: `SeedDataInitializer` (COMMON) runs `data-{core,icu,med,prosth}.sql` on the matching datasource at boot (gated by `app.seed-data.enabled: true`; tests disable it). Counts: 9 users (6 core roles + 3 prosthetics), 50 episodes, 90 clinical days, 360 prescription lists, 90 vital sign lists, prosthetics 2 patients/2 orders/2 templates.
- CI: `.github/workflows/playwright.yml` — Postgres service, JDK 25, Node 22, Playwright chromium, 40min timeout. Every DB-using job creates the 4 DBs (`CREATE DATABASE` ×4) and passes the 12 `APP_DATASOURCE_*` env vars.
- Mock MIS: `MockMisServiceImpl` provides 5 test patients + department/user data.

## Module Boundaries (enforced)

**Backend — ArchUnit** (`app/src/test/java/com/superhumans/architecture/ModuleBoundaryTest.java`, runs in `backend-test`): the feature namespaces `com.superhumans.medicationsheet..` and `com.superhumans.prosthesismanufacturing..` may depend ONLY on:
- their own namespace,
- the shared platform allowlist: `entity.base`, `entity.core`, `repository.core`, `exception`, `mis`, `util` (com.superhumans),
- exact classes `AuditService`, `PermissionService`, `SpringContext`,
- third-party runtime packages (java/jakarta/lombok/org.springframework/org.mapstruct/org.hibernate/org.slf4j/com.fasterxml.jackson/com.itextpdf/io.swagger).

Everything else under `com.superhumans` — the ICU domain root packages (`controller`, `service`, `dto`, `entity`, `mapper`, `repository`) and `com.superhumans.icu.*` — is off-limits to features. Features must not depend on each other, and platform code must not depend on feature packages. The ICU feature is NOT subject to the allowlist: it lives in the platform packages by design (episodes, clinical days, orders, notes, scales, PDF, audit).

**Frontend — oxlint** (`frontend/.oxlintrc.json` `overrides` with `no-restricted-imports`, enforced by CI `format-check` via `npm run lint`): `pages/prescription` → forbid `components/icu` + `components/monitoring`; `pages/prosthetics` → forbid `components/icu` + `components/prescription`; `components/icu` → forbid `components/prescription` + `components/monitoring`; `components/prescription` → forbid `components/icu` + `components/monitoring`; `components/common` → forbid all feature components (`icu`, `monitoring`, `prescription`, `prosthetics`). Patterns are regex-based and match both relative specifiers and the `@/` alias form; shared code (api/, types/, lib/, utils/, ui/, navigation/) is importable from everywhere. `src/prosthetics/` is a fully isolated feature root (own API client, types, context).

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
| `e2e-test` | Playwright (55 spec files, chromium, 40-min timeout; `needs: backend-test, frontend-test`) | `playwright-report`, `playwright-test-results` |
| `build` | JAR + frontend dist artifacts (main push only; needs all 5 jobs) | — |

### Exit criteria
All checks pass: `format-check`, `backend-test`, `backend-integration`, `frontend-test`, `e2e-test` (plus `build` on `main`). Green run = done; start the next issue at Phase 1.

## Commands

### Backend (`cd backend`)
| Command | Action |
|---|---|
| `mvn -pl app spring-boot:run` | Dev server on `:8085` |
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
| `npm t` or `npx vitest run` | Run Vitest tests (~583 across 69 files) |

### Playwright (`cd tests`)
| Command | Action |
|---|---|
| `npx playwright test` | Run all E2E tests (55 spec files) |
| `npx playwright test --list` | List tests without running |
| `npx playwright show-report` | View HTML report |

## Testing

- **Backend**: 348 main sources / 112 test files across the multi-module reactor (common 119/10, icu-chart 84/62, medication-sheet 61/17, prosthesis-manufacturing 84/22, app 0/1 — the app test is the ArchUnit `ModuleBoundaryTest`). JaCoCo 60% instruction / 50% branch minimum. Checkstyle Google checks.
- **Frontend**: 583 Vitest tests across 69 test files (127 TS/TSX sources). Run with `npm t`.
- **E2E**: 55 Playwright spec files (~228 tests) across 9 projects (setup, login, api-error-mode, doctor, nurse, hod, admin, api, prosthetics).

## Playwright Projects

| Project | Depends On | storageState | Tests |
|---|---|---|---|
| setup | — | — | Auth setup (6 roles) |
| login-chromium | — | none | Login/logout flow |
| api-error-mode-chromium | — | none | Mock MIS error scenarios |
| doctor-chromium | setup, api-error-mode | `.auth/doctor.json` | Dashboard, create card, prescriptions, notes, sign-off |
| nurse-chromium | setup, api-error-mode | `.auth/nurse.json` | Dashboard, vitals, fluid balance, order execution |
| hod-chromium | setup, api-error-mode | `.auth/hod.json` | Dashboard, clinical day reopen |
| admin-chromium | setup, api-error-mode | `.auth/admin.json` | User tables, RBAC matrix, audit log |
| api-chromium | api-error-mode | none | Patient search API, error handling, scales access control |
| prosthetics-chromium | setup | `.auth/prosthetist.json` | Prosthetics workflow, quality gates |

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

Prosthetics seed patients (demographics served by the MIS Integration Layer wiremock `__files/patients_52.json`; clinical fields in local tables):

| Patient | ID | Order | Template |
|---|---|---|---|
| Сніжко Іван Петрович | `900001` | ПВ-26-0413 (upper_limb) | TP-UL-01 (ACTIVE) |
| Гаврилюк Олена Миколаївна | `900002` | ПВ-26-0414 (lower_limb) | TP-LL-01 (DRAFT) |

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
| `Permission` | — | code(PK), label, description, category | Dictionary of the RBAC catalog (25 codes) |
| `RolePermission` | — | role(PK, UserRole), permissionCode(PK→Permission) | Default-deny grants; presence = granted |
| `GeneratedPdf` | BaseEntity | clinicalDay(M→1), fileName, fileVersion, generatedAt, generatedBy, checksum, fileData(byte[]), transferStatus(TransferStatus), transferError, transferredAt | TransferStatus: PENDING/SENT/FAILED |
| `SystemSettings` | BaseEntity | key(unique), value(TEXT), description(TEXT) | — |

### Enums

| Enum | Values |
|---|---|
| `UserRole` | DOCTOR, NURSE, HEAD_OF_DEPARTMENT, ADMINISTRATOR, AUDITOR, PROSTHETIST, PROSTHETICS_ADMINISTRATOR |
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

| Path | Component | Guard (roles / permissions) |
|---|---|---|
| `/login` | `LoginRoute` → `LoginPage` | Redirects to `/` if authenticated |
| `/` | `RoleRedirect` | Authenticated → `/select` |
| `/select` | `AppSelectorPage` | Any authenticated |
| `/icu/doctor` | `DoctorLayout` > `DashboardPage` | DOCTOR, HEAD_OF_DEPARTMENT (or `MODULE_ICU_ACCESS`, excl. NURSE) |
| `/icu/doctor/department` | `DepartmentDashboardPage` | HEAD_OF_DEPARTMENT |
| `/icu/doctor/create-card` | `CreateCardPage` | DOCTOR, HEAD_OF_DEPARTMENT (or module perm) |
| `/icu/doctor/episode/:episodeId` | `PatientDayPage` | DOCTOR, HEAD_OF_DEPARTMENT (or module perm) |
| `/icu/nurse` | `NurseLayout` > `NurseDashboardPage` | NURSE (or `MODULE_ICU_ACCESS`, excl. DOCTOR/HOD) |
| `/icu/nurse/episode/:episodeId` | `PatientDayPage` | NURSE (or module perm) |
| `/prescriptions/doctor` | `PrescriptionPage` | DOCTOR, HEAD_OF_DEPARTMENT (or `MODULE_MEDICATION_ACCESS`, excl. NURSE) |
| `/prescriptions/doctor/:id` | `PrescriptionDetailPage` | DOCTOR, HEAD_OF_DEPARTMENT (or module perm) |
| `/prescriptions/nurse` | `NursePrescriptionPage` | NURSE (or `MODULE_MEDICATION_ACCESS`, excl. DOCTOR/HOD) |
| `/prescriptions/nurse/:id` | `PrescriptionDetailPage` | NURSE (or module perm) |
| `/prosthetics` | `ProstheticsDashboard` | PROSTHETIST, PROSTHETICS_ADMINISTRATOR (or `MODULE_PROSTHETICS_ACCESS`) |
| `/prosthetics/new/select-patient` | `PatientSearchPage` | same |
| `/prosthetics/new/select-order` | `OrderSelectPage` | same |
| `/prosthetics/new/review-order` | `OrderReviewPage` | same |
| `/prosthetics/new/select-template` | `TemplateSelectPage` | same |
| `/prosthetics/process/:id` | `ProcessLayout` > `ProcessDetail` | same |
| `/prosthetics/process/:id/history` | `ProcessHistoryPage` | same |
| `/prosthetics/process/:id/wizard` | `WizardScreen` | same (backend enforces PROSTHETIST writes) |
| `/prosthetics/process/:id/done` | `DoneScreen` | same |
| `/prosthetics/process/:id/failed` | `FailedScreen` | same |
| `/admin` | `AdminPage` | ADMINISTRATOR, AUDITOR (or `MODULE_ADMIN_ACCESS`) |

## Frontend Components

### Pages (28)
| Area | Files |
|---|---|
| root | `LoginPage.tsx`, `AppSelectorPage.tsx` |
| `doctor/` | `DashboardPage.tsx`, `DepartmentDashboardPage.tsx` (HOD), `CreateCardPage.tsx`, `PatientDayPage.tsx` |
| `nurse/` | `NurseDashboardPage.tsx` |
| `prescription/` | `PrescriptionPage.tsx`, `PrescriptionDetailPage.tsx`, `NursePrescriptionPage.tsx` |
| `prosthetics/` | `ProstheticsDashboard.tsx`, `DashboardPage.tsx`; `setup/` — `PatientSearchPage`, `OrderSelectPage`, `OrderReviewPage`, `TemplateSelectPage` + steps (`OrderStep`, `PatientStep`, `ReviewStep`, `TemplateStep`); `process/` — `ProcessDetail`, `ProcessHistoryPage`, `ProcessLayout`, `ProcessOverview`, `WizardScreen`, `DoneScreen`, `FailedScreen`, `MeasurementForms` |
| `admin/` | `AdminPage.tsx` (users, RBAC matrix «Доступи та ролі», audit log gated by `AUDIT_ACCESS`, stats) |

### Components by feature
| Directory | Contents |
|---|---|
| `components/icu/` | ICU chart feature: `ClinicalDayTimeline`, `DepartmentPatientCard`, `DocumentHeader`, `EpisodeTable`, `FluidBalancePanel`, `HourlyRecordTable`, `HourSelector`, `LabResultsPanel`, `MedicalNotesPanel`, `MedicalOrdersPanel`, `PatientStatePanel`, `ScaleResultsPanel`, `SignDialog`, `VentilationPanel`, `VitalSignsForm`, `useAutoSave`; `scales/` — `ApacheIiForm` (20 parameters), `SofaForm`, `CamIcuForm`, `BradenForm`, `RassSelector`, `ScaleFormFactory` |
| `components/monitoring/` | `HourlyGrid` (24-h grid, therapy cells, plan/execute, critical flash), `HourlyGridDialog` (fullscreen modal with undo/status/critical chip), `IntensiveCareCard` (central ICU card), `DoctorDashboard`, `NurseDashboard`, `PatientSidebar`, `criticalRanges.ts` (alarm thresholds), `dashboardTypes.ts` |
| `components/prescription/` | `PrescriptionGrid`, `PrescriptionSpreadsheet`, `PrescriptionTable`, `PrescriptionItemTable`, `PrescriptionItemForm`, `PrescriptionExecutionPanel`, `VitalSignGrid`, `VitalSignForm`, `DayPartPlanner`, `MedicineSearchInput`, `AllergyWarning`, `ClosePrescriptionDialog`, `DeleteConfirmPopover`, `ExecuteDosePopover`, `prescriptionDayParts.ts` |
| `components/common/` | `PatientSearch.tsx`, `ThemeToggle.tsx`, `AuditLogTable.tsx` (shared, feature-free) |
| `components/prosthetics/` | `StatusBadge`, `SetupSteps`, `QualityGatePanel`, `ProcessStat` |
| `components/navigation/` | `AppSidebar.tsx`, `Breadcrumbs.tsx` |
| `components/ui/` | shadcn-style Base UI primitives: `button`, `input`, `card`, `dialog`, `table`, `select`, `tabs`, `switch`, `checkbox`, `radio-group`, `dropdown-menu`, `tooltip`, `progress`, `skeleton`, `sonner`, … |

### API Client (`frontend/src/api/`)
- **`client.ts`**: Axios instance → `http://localhost:8085/api`, JWT interceptor
- **Per-feature modules** (no barrel): `platform.ts` (auth, patient, user, settings, audit, admin), `icu.ts` (episode, clinicalDay, hourlyRecord, medicalOrder, orderExecution, medicalNote, clinicalScale, fluidBalance, pdf, patientState, ventilation, labResult, department), `medication.ts` (prescription, vitalSign); prosthetics APIs in `prosthetics.ts` (isolated). Shared DTO types live in `types/core.ts`, ICU types in `types/icu.ts`, medication types in `types/medication.ts`.

### Auth (`frontend/src/services/AuthContext.tsx`)
- `AuthProvider` with user/token state, login/logout, role checking
- Token persisted in `localStorage`
- Guards in `App.tsx` via `Guard` component and `LoginRoute`/`RoleRedirect`

## DTOs

### Request DTOs
`LoginRequest`, `EpisodeCreateRequest`, `EpisodePatchRequest`, `EpisodeCloseRequest`, `ClinicalDayCreateRequest`, `ClinicalDayPatchRequest`, `HourlyRecordCreateRequest`, `HourlyRecordPatchRequest`, `MedicalOrderCreateRequest`, `MedicalOrderPatchRequest`, `MedicalNoteCreateRequest`, `MedicalNotePatchRequest`, `ScaleResultCreateRequest`, `ScaleResultPatchRequest`, `ScaleResultCalculateRequest`, `OrderExecutionCreateRequest`, `OrderExecutionPatchRequest`, `SignRequest`, `ReopenRequest` (19 total)

### Response DTOs
`LoginResponse`, `EpisodeResponse`, `ClinicalDayResponse`, `HourlyRecordResponse`, `MedicalOrderResponse`, `OrderExecutionResponse`, `MedicalNoteResponse`, `ScaleResultResponse`, `FluidBalanceResponse`, `SignResponse`, `PdfResponse`, `UserResponse`, `AuditLogResponse`, `ErrorResponse` (14 total)

## Backend Services (by module)

### Platform (`common`, 4)
| Service | Responsibility |
|---|---|
| `AuthService` | Login with password verification + JWT generation |
| `AuditService` | Create/query audit log entries with pagination |
| `PermissionService` | Dynamic RBAC: `has/hasAny/hasForRole` (SpEL for `@PreAuthorize`), matrix read, grant/revoke with cache invalidation + audit, first-boot seeding of defaults |
| `PermissionCatalog` | RBAC catalog: 25 permission codes across 8 categories, role defaults for seeding |

### ICU chart (`icu-chart`, 16)
| Service | Responsibility |
|---|---|
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
| `VentilationSettingsService`, `LabResultService`, `PatientStateAssessmentService`, `DepartmentService`, `EmailService` | Ventilation settings, lab results, patient state, department data, e-mail notifications |

### Medication sheet (`medication-sheet`, 11)
| Service | Responsibility |
|---|---|
| `PrescriptionListService`, `PrescriptionItemService`, `PrescriptionExecutionService` | Prescription lists/items/day-part executions (21-day grid) |
| `PrescriptionSchedulerService` | Scheduled plan/complete processing for day parts |
| `VitalSignService` | Vital sign days/entries |
| `MedicineCatalogService`, `DrugInteractionService` | Medicine catalog + allergy/drug-interaction checks |
| `NotificationService`, `LogNotificationService`, `LogEmailService`, `EmailService` | Telegram/e-mail notifications (logging fallbacks in tests) |

### Prosthetics manufacturing (`prosthesis-manufacturing`, 11)
| Service | Responsibility |
|---|---|
| `ProstheticsPatientService`, `ProstheticsOrderService`, `ProstheticsPdfService` | Patient/order CRUD + PDF generation |
| `FlowTemplateService` | Template CRUD + stages/steps/elements |
| `FlowInstanceService` | Instance lifecycle (create, pause, resume, complete steps) |
| `QualityGateService` | Gate decisions (PASS/REWORK/FAIL), rework loops |
| `FailureSnapshotService` | Failure capture + PDF report |
| `EvidenceFileService` | File upload (images/PDFs, 10MB limit) |
| `MisOrderTemplateDataService`, `TemplateSnapshotParser`, `MisOrderTemplateData` | Template parsing from MIS order data |

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
| §— | Liquibase schema management | `ddl-auto: none`, schema per DB via `db/changelog/db.changelog-master-{core,icu,med,prosth}.yaml` (15 SQL changesets: core 4, icu 6, med 1, prosth 4); seed data via `SeedDataInitializer` (`data-{core,icu,med,prosth}.sql`, gated by `app.seed-data.enabled`) |

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

## Responsive UI

- **Breakpoint strategy**: mobile < 640px (base styles), tablet 640–1024px (`sm:`…`lg:`), desktop > 1024px (`lg:`+). Explicit tokens in `index.css` `@theme`: `--breakpoint-sm: 40rem`, `--breakpoint-md: 48rem`, `--breakpoint-lg: 64rem`, `--breakpoint-xl: 80rem`. CSS-first: base styles target mobile, `sm:`/`md:`/`lg:` variants progressively enhance for tablet/desktop.
- **Shared hooks** (`frontend/src/hooks/useMediaQuery.ts`): `useMediaQuery(query)` (SSR-safe, `useSyncExternalStore`, subscribes via `addEventListener('change')`) and `useIsMobile()` = `useMediaQuery('(max-width: 639.98px)')`. MatchMedia is not implemented in jsdom — `src/test/setup.ts` provides a static mock (always `matches: false`); tests that need real behavior must install their own mock and restore `window.matchMedia` afterwards.
- **Desktop/tablet detection**: the ICU layout treats viewports ≤ 1023.98px as mobile (`(max-width: 1023.98px)` via the shared `useMediaQuery`) — used by `SidebarProvider` (offcanvas behavior) and `IntensiveCareCard` (single-column layout). Never duplicate inline matchMedia `useState`/`useEffect` blocks — use the shared hook.
- **Primitives** (Base UI ports, no new runtime deps): `components/ui/sheet.tsx` (Drawer-based Sheet — `SheetContent` `side` prop `left|right|top|bottom`, default `right`; popup renders NO DOM element on `Sheet` Root so it is a plain function, `swipeDirection` default `"down"`; animations via `index.css` `sheet-*` keyframes gated on `[data-slot="sheet-content"][data-side=…][data-open|data-closed]` + `prefers-reduced-motion`), `components/ui/stepper.tsx` (custom shadcn-style stepper — `Stepper`/`Step`/`StepIndicator`/`StepSeparator`/`StepTitle`/`StepDescription`/`useStepper`; 1-based `step`, `orientation`, `size`, `nonLinear` + `onStepClick` makes indicators `role="button"`; completed steps render a `Check` icon, `loading` renders `Loader2`), `components/ui/scroll-area.tsx` (Base UI ScrollArea with `keepMounted` scrollbars so both rails render in jsdom tests).

## Conventions

- **Commits**: Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`, `chore:`)
- **TypeScript**: `erasableSyntaxOnly: true` — no enums, no namespaces
- **Roles**: Gate in backend (Spring Security `@PreAuthorize`) and frontend (`Guard` component)
- **Routing**: `/icu/doctor/*` for DOCTOR/HOD, `/icu/nurse/*` for NURSE, `/prescriptions/*` for medication sheet, `/prosthetics/*` for prosthetics, `/admin/*` for ADMINISTRATOR
- **DB**: `ddl-auto: none` — schema per DB managed by the Liquibase changelogs in `db/changelog/{core,icu,med,prosth}/` (master yamls + 15 SQL changesets); never write manual DDL
- **Data seeding**: Only via `SeedDataInitializer` — one script per module: `data-core.sql`, `data-icu.sql`, `data-med.sql`, `data-prosth.sql` (in `backend/common/src/main/resources/`), executed on the matching datasource; gated by `app.seed-data.enabled: true`. Never write manual seed DDL.
- **Test seed data**: Integration tests use `data-test-core.sql` / `data-test-icu.sql` / `data-test-med.sql` (in `backend/icu-chart/src/test/resources/`) with plain INSERTs, routed per-datasource via `@Sql` + `@SqlConfig(dataSource = ...)` (plus `data-prescription.sql` with `@SqlConfig(dataSource = "medDataSource", separator = "GO")`) on a fresh PostgreSQL database. The production seed files keep `ON CONFLICT (id) DO NOTHING` for local dev resilience (exception: `prescription_lists` uses `ON CONFLICT (id) DO UPDATE SET document_name = EXCLUDED.document_name` to auto-heal Cyrillic encoding corruption). Modified data may persist across restarts. Reset each DB with `DROP SCHEMA public CASCADE; CREATE SCHEMA public;` in PostgreSQL before the next run.

## Encoding Policy

**All SQL seed files and generated SQL must be UTF-8** — never UTF-16LE, never Windows-1251.

- **Generator scripts** (`scripts/*.cjs`): Use `fs.writeFileSync(path, content, 'utf8')` for file output and `process.stdout.write(content, 'utf8')` for stdout. **Never use `console.log()`** to generate file content — on Windows PowerShell, `console.log` pipes through `process.stdout` which defaults to UTF-16LE, producing a UTF-16LE BOM and null-byte interleaved ASCII that PostgreSQL cannot decode.
- **Verification commands**:
  - `file scripts/*.sql` should report "UTF-8 Unicode text", never "Little-endian UTF-16 Unicode text"
  - `hexdump -C scripts/*.sql | head -3` should show no BOM (`FF FE`) and single-byte (not zero-interleaved) ASCII
- **`data-*.sql`**: Must be UTF-8. Any seed SQL file must be explicitly written as UTF-8. If a corrupted file was already concatenated, convert it with `Set-Content -Encoding UTF8` or `iconv -f UTF-16 -t UTF-8` and re-insert.
- **Auto-heal**: If corrupted `document_name` values already exist in the database, the `ON CONFLICT (id) DO UPDATE SET document_name = EXCLUDED.document_name` clause on `prescription_lists` INSERTs will overwrite them with clean UTF-8 text on the next `data-med.sql` execution.
- **Seed splitting**: `data.sql`/`data-test.sql` are generated per module with the `split-seed.cjs` statement-aware splitter (temp tool, not in repo). Re-run it after changing any seed content — the split files carry "DO NOT EDIT BY HAND" headers.

## Project Files (kept in repo)

```
AGENTS.md              ← This file — agent guide
README.md              ← Project README with badges and usage
UseManual.md           ← User manual (Ukrainian)
.gitignore             ← Global ignore rules
backend/
  pom.xml              ← Maven build with JaCoCo, Checkstyle, surefire (5 modules: common, icu-chart, medication-sheet, prosthesis-manufacturing, app)
  src/main/java/       ← 348 Java source files
  src/main/resources/  ← application.yml, data-{core,icu,med,prosth}.sql, PDF template, db/changelog/ (Liquibase)
  src/test/java/       ← 112 test files
frontend/
  package.json         ← Dependencies
  vite.config.ts       ← Vite build config
  tsconfig*.json       ← TypeScript configs
  index.html           ← App entry HTML
  public/              ← Static assets
  src/                 ← 127 TS/TSX source + 69 test files
tests/
  playwright.config.ts ← Playwright config with 9 projects
  package.json         ← Test dependencies
  specs/               ← 55 spec files
  pages/               ← Page Object Model (7 files)
  fixtures/            ← Test fixtures
docs/
  Технічне завдання карта Інтенсивної терапії.md  ← Full technical specification (3026 lines)
.github/
  workflows/playwright.yml  ← CI pipeline (3 jobs: integration-tests, test, format-check)
```
