# Test Codebase Improvement Plan

> **Note (QG-Removal, issues #229–#234):** `QualityGateServiceTest` / gate-endpoint rows below are historical — deleted with the whole Quality Gate subsystem.

**Status:** Implemented — 2026-08-20 (all H/M changes applied; pre-flight green; tests themselves un-run per CI-only rule)
**Scope:** Backend unit/integration tests (JUnit 5, Mockito, AssertJ — 112 files) and frontend Vitest tests (76 files). Playwright E2E is out of scope.
**Method:** Full inventory of both test suites (per-module/per-directory counts), representative reading of every backend module and all major frontend suites, byte-level verification of the suspected seed-file encoding issue, cross-referencing of recently shipped features (dynamic RBAC, `PRESCRIPTION_LIST_CREATE`, responsive UI phases) against their test coverage.
**Constraint:** No test execution locally — CI-only (`mvn test` / Vitest / Playwright forbidden). Only `mvn compile`, `npm run lint`, `npx tsc --noEmit`, `npm run build` are run for pre-flight.

---

## 1. Summary

The test codebase is in **good overall health**: the scale calculators, `PermissionService`, `OrderExecutionService`, `QualityGateService`, `FlowInstanceService`, `AuthIntegrationTest`, `AppSidebar.test.tsx`, `HourlyGridDialog.test.tsx` and `AdminPermissionsIntegrationTest` are genuinely strong. However the audit found:

- **1 fake test** (asserts nothing about the behavior it claims to verify).
- **1 missing UI-gate test** for a feature shipped 2026-08-18 (`PRESCRIPTION_LIST_CREATE`).
- **~14 missing validation branches** in the clinical-ranges entity test (gastric output, 3 vasopressors).
- **1 missing service-level guard test** (duplicate hourly-record hour, §46).
- **4 conditional/weakened assertions** in the audit integration test.
- **1 stale dead mock** (MUI theme object) left from the MUI → Base UI migration.
- **4 clear coverage gaps** with high-value, low-risk new test files (parser, order service, department service, error-message util).

## 2. Findings by priority

### HIGH — real test defects (fix)

| # | Location | Problem |
|---|---|---|
| H1 | `frontend/src/test/api/client.test.ts:23-38` | `dispatches auth:unauthorized custom event on 401` is a **fake test**: it never triggers the response interceptor (its own comment admits "we can't easily trigger interceptors"), only re-asserts `mod.default` is defined, and stubs/replaces `window.dispatchEvent` without ever calling it. The interceptor rejection handler IS directly invocable: `mod.default.interceptors.response.handlers[0].rejected(err)` with an AxiosError carrying `response.status === 401` |
| H2 | `frontend/src/test/pages/PrescriptionPage.test.tsx` | The mock returns `hasPermission: () => true` unconditionally. The 2026-08-18 feature `PRESCRIPTION_LIST_CREATE` gates «Створити листок» (`PrescriptionPage.tsx:374-385`) — **no test asserts the button is hidden** when the permission is absent. Integration coverage exists (`PrescriptionIntegrationTest.create_asNurse_returnsForbidden`) but the UI gate is untested |
| H3 | `backend/icu-chart/.../icu/entity/HourlyRecordValidationTest.java` | Validation matrix gaps vs. the entity's `validateClinicalRanges()` (`HourlyRecord.java:152-171`): **gastric output has zero tests**; dobutamine/norepinephrine/epinephrine have only `*_valid_doesNotThrow` tests — missing `*_belowMin_throws` and `*_aboveMax_throws` (dopamine is fully covered) |
| H4 | `backend/icu-chart/.../service/HourlyRecordServiceTest.java` | Missing `createHourlyRecord_whenHourAlreadyExists_throws` — the service-level duplicate guard for `UNIQUE(clinical_day_id, record_hour)` (§46, `HourlyRecordService.java:59-61`, throws `com.superhumans.exception.DuplicateHourlyRecordException`) |

### MEDIUM — weakened assertions / dead test code (fix)

| # | Location | Problem |
|---|---|---|
| M1 | `backend/icu-chart/.../integration/AuditIntegrationTest.java:52-54,67-69` | `if (!logs.isEmpty()) { allMatch(...) }` — the filter tests pass silently on empty results. A LOGIN audit row (entity `AUTH`, action `LOGIN`) is guaranteed per test: every test logs in via `getAdminToken()` and `AuthService.logAuth("LOGIN", ...)` writes entity `AUTH` (`AuditService.java:95-97`). Make strict: `isNotEmpty().allMatch(...)` |
| M2 | `backend/icu-chart/.../integration/AuditIntegrationTest.java:39,86` | `parseContent(...)` asserted only `isNotNull()` — paginated content is guaranteed non-empty (see M1); assert `isNotEmpty()` |
| M3 | `frontend/src/test/App.test.tsx:26-74` | ~50-line **stale MUI `mockTheme`** (palette/typography/breakpoints/shadows/zIndex from the pre-Base-UI era) — dead config; `useThemeMode` mock only ever uses `mode`/`toggleTheme`. Also `vi.mock('../pages/AppSelectorPage')` is **duplicated** (lines 81-83 and 127-129) |

### MEDIUM — coverage gaps with new test files (implement)

| # | Location | Gap | Effort / risk |
|---|---|---|---|
| M4 | `backend/prosthesis-manufacturing/.../TemplateSnapshotParser.java` | Snapshot (de)serialization round-trip, corrupted-JSON → `IllegalArgumentException`, nested structure (stages/gates/rework loops/elements) | Pure Jackson parser — trivially unit-testable, no mocks |
| M5 | `backend/prosthesis-manufacturing/.../ProstheticsOrderService.java` | list filtering (4 branches incl. `OrderStatus.valueOf`), `get` NotFound, `generateRecipe` lazy generation + audit, `getRecipePdf` skips regeneration when data exists | Standard Mockito service test, module-boundary safe (mocks only) |
| M6 | `backend/icu-chart/.../service/DepartmentService.java` | stats aggregation (per-status day counts, doctor/nurse counts, department-scoped vs global), patient row mapping (MIS name lookup, latest-day status, doctor-name map, days-since-admission) | Mockito service test — 4 mocks, pure aggregation logic |
| M7 | `frontend/src/utils/errorMessage.ts` | `getErrorMessage(err, fallback)` used in 15+ components, zero direct tests | Tiny pure function — Axios-error / plain-error / unknown-error branches |

### LOW — documented, not changed (deferred)

| # | Location | Note |
|---|---|---|
| L1 | `backend/medication-sheet/.../PrescriptionControllerTest.java:435-439`, `VitalSignControllerTest.java:135-143` | Commented-out nurse-forbidden tests with stale TODO ("re-enable when SecurityConfig moved to common"). SecurityConfig IS in common today, but these slices use `addFilters=false` (no security filter chain) and would need method-security wiring + `PermissionService` bean — the role matrix is already covered end-to-end by `PrescriptionIntegrationTest` (6 nurse/doctor pairs). Re-enabling inside `@WebMvcTest` adds maintenance risk with no new coverage |
| L2 | `backend/common/.../SecurityConfigHttpsTest` `@Disabled` | Disabled without documented reason; likely TLS-simulation setup burden. Deferred — verify need before enabling |
| L3 | Untested frontend components: `Breadcrumbs`, `AuditLogTable`, `ProcessStat`, `DocumentHeader`, `HourSelector`, `lib/utils.ts` (`cn`) | Low-value, presentation-heavy components; `cn` is a 1-line `clsx` wrapper. Candidates for a later phase |
| L4 | `ModuleBoundaryTest` uses `ImportOption.DoNotIncludeTests` | Tests are not subject to ArchUnit boundary checks. Grep found no cross-feature imports in test files today; a follow-up could add a second ArchUnit rule with `IncludeTests` |
| L5 | 92 `.isNotNull()` assertions in backend integration tests | Audited: nearly all precede real value assertions (e.g. `PdfGeneratorIntegrationTest` checks `fileVersion == 2`, `AuditIntegrationTest` checks `entityId` match). Legitimate pattern, not churn-worthy |
| L6 | `AdminPermissionsIntegrationTest` `@Order`-dependent sequence | Intentional: revoke-first, idempotent grants, cache invalidation verification. Do not "fix" |

### Verified non-issues (no action)

- `data-test-icu.sql` Cyrillic "corruption" — **false alarm**: Windows console codepage renders UTF-8 as mojibake; hex dump shows clean UTF-8 (`D0 91` = «Б», `E2 80 94` = em-dash).
- `PrescriptionIntegrationTest` already covers the `PRESCRIPTION_LIST_CREATE` backend gate (`create_asNurse_returnsForbidden`) and the whole med-sheet RBAC surface (6 doctor/nurse pairs).
- `OrderExecutionServiceTest` already covers the plan-hour race that historically flaked E2E (`plan_whenAlreadyCompleted_throws`, `execute_logsBackEntry_whenPastHour`).
- Scale calculators, `PermissionServiceTest` (25-code catalog incl. `PRESCRIPTION_LIST_CREATE`), `QualityGateServiceTest`, `FlowInstanceServiceTest`, `AuthIntegrationTest`, `HourlyGridDialog.test.tsx` — high quality, no action.

## 3. Changes to implement

All changes are **test-file only**; no production code is modified.

### Backend
1. `backend/icu-chart/src/test/java/com/superhumans/icu/entity/HourlyRecordValidationTest.java` — add: `gastricOutput_belowMin_throws` / `gastricOutput_atMin_doesNotThrow` / `gastricOutput_positive_doesNotThrow`; `dobutamine_belowMin_throws` / `dobutamine_aboveMax_throws`; `norepinephrine_belowMin_throws` / `norepinephrine_aboveMax_throws`; `epinephrine_belowMin_throws` / `epinephrine_aboveMax_throws`. (Messages: "Gastric output must be at least 0", "Dobutamine/Norepinephrine/Epinephrine must be between 0 and 100 мкг/кг/хв".)
2. `backend/icu-chart/src/test/java/com/superhumans/service/HourlyRecordServiceTest.java` — add `createHourlyRecord_whenHourAlreadyExists_throwsDuplicate` (stub `findByClinicalDayIdAndRecordHour` → present; assert `DuplicateHourlyRecordException`, verify `save` never called).
3. `backend/icu-chart/src/test/java/com/superhumans/integration/AuditIntegrationTest.java` — drop the `if (!isEmpty())` guards (strict `isNotEmpty().allMatch(...)`), make pagination asserts `isNotEmpty()`.
4. NEW `backend/prosthesis-manufacturing/src/test/java/com/superhumans/prosthesismanufacturing/service/TemplateSnapshotParserTest.java` — round-trip, corruption, nested-snapshot fidelity (stages/gates/rework/step elements).
5. NEW `backend/prosthesis-manufacturing/src/test/java/com/superhumans/prosthesismanufacturing/service/ProstheticsOrderServiceTest.java` — list branches, NotFound, recipe generation/lazy PDF, audit logging.
6. NEW `backend/icu-chart/src/test/java/com/superhumans/service/DepartmentServiceTest.java` — stats aggregation + patient mapping.

### Frontend
7. `frontend/src/test/api/client.test.ts` — rewrite the third test to invoke the registered response-interceptor rejection handler with a 401 AxiosError and assert `auth:unauthorized` was dispatched; add a non-401 passthrough case.
8. `frontend/src/test/pages/PrescriptionPage.test.tsx` — make `hasPermission` configurable; add test: with `PRESCRIPTION_LIST_CREATE` absent, the drawer renders «Немає листків призначень» and **no** «Створити листок» button.
9. `frontend/src/test/App.test.tsx` — delete the MUI `mockTheme`; collapse `useThemeMode` to `mode`/`toggleTheme`; remove the duplicated `AppSelectorPage` mock.
10. NEW `frontend/src/utils/errorMessage.test.ts` — Axios error with `response.data.message`, plain `Error`, unknown/null, fallback usage.

## 4. Pre-flight validation

After the edits (no test execution):
- `mvn compile` in `backend/` (new test files must compile — verify via `mvn test-compile` semantics; `compile` covers main, but test sources are validated by `mvn test-compile` which is also test-free and permitted as a compile-only step).
- `npm run lint` in `frontend/` (oxlint, incl. restricted-import rules for `src/test/components/prescription/**`).
- `npx tsc --noEmit` in `frontend/` (new/edited TS test files).
- `npm run build` in `frontend/`.

## 5. Out of scope / follow-ups

- Playwright E2E: no changes (responsive QA phase closed 2026-08-19).
- Production code: none touched; no product bugs surfaced by the audit.
- Follow-up candidates: L1-L4 (deferred), plus a future ArchUnit `IncludeTests` rule to boundary-check test imports.