# Test Plan Based on Exploratory Testing

**Project:** ICU Patient Chart (Карта інтенсивної терапії)
**Author:** Exploratory testing session (headed Playwright, full stack on localhost)
**Date:** 2026-07-20
**Scope:** Functional exploration of the React frontend (`localhost:5173`) against the live
Spring Boot backend (`localhost:8085`) and PostgreSQL, covering all four roles
(DOCTOR, NURSE, HEAD_OF_DEPARTMENT, ADMINISTRATOR).

This document converts the findings of an exploratory session into a structured,
repeatable test plan. It catalogs **exploratory charters**, **observed findings
(including defects)**, **comprehensive use cases**, and the **minimum set of unique
test cases** to add at the unit, integration, and E2E layers. It builds *on top of*
the existing 230+ tests (151 backend unit, 79 backend integration, ~190 Vitest, 35
Playwright specs) and fills the gaps the exploration exposed.

---

## 1. How the exploration was run

| Aspect | Detail |
|---|---|
| Tooling | `playwright-core` driving the already-downloaded Chromium, headed, `--start-fullscreen`, `1920×1080`, `locale: 'uk'` |
| Harness | `tests/exploratory/explore.cjs` (inventory + failed-request capture), `tests/exploratory/interact.cjs` (flow exercise) |
| Artifacts | `tests/exploratory/exploratory-report.json`, `tests/exploratory/exploratory-interactions.json` |
| Login | Fresh login per role via `#root input` × 2 + submit button (regex `/вхід|увійти|login|укв|submit/i`) |
| Wait strategy | `domcontentloaded` + fixed `DELAY` (700–800 ms) + extra settle time for async data loads |
| Roles / seeds | `doctor1/doctor123` → `/doctor`, `nurse1/nurse123` → `/nurse`, `head1/head123` → `/doctor`, `admin/admin123` → `/admin` |

### Critical correction discovered during exploration
The `AGENTS.md` "Seed Data" section abbreviates episode IDs as `a1111111`,
`a2222222`, `a3333333`. **These are not valid UUIDs.** The real seed rows use full
UUIDs with recognizable prefixes:

| Logical | Real episode UUID | Patient | Status |
|---|---|---|---|
| `a1111111` | `a1111111-1111-1111-1111-111111111111` | Петренко (1001) | ACTIVE |
| `a2222222` | `a2222222-2222-2222-2222-222222222222` | Коваленко (1002) | ACTIVE |
| `a3333333` | `a3333333-3333-3333-3333-333333333333` | Сидоренко (1003) | ACTIVE |

Calling `/api/episodes/a1111111` (short form) returns **HTTP 400** (`Invalid UUID
string`). The existing Playwright specs already use the *correct* full UUIDs; only the
exploratory harness initially used the short form and therefore produced false 400s.
**Lesson:** always author tests against full UUID seeds.

---

## 2. Exploratory charters (session notes)

| # | Charter | Covered routes | Outcome |
|---|---|---|---|
| C1 | Doctor navigates dashboards & opens an episode | `/doctor`, `/doctor/episode/a1111111-…` | OK; episode page renders ~300 elements, 0 console errors |
| C2 | Doctor exercises episode tabs & panels | notes, scales, ШВЛ, lab results, patient state | Tab buttons present & clickable; panels render |
| C3 | Doctor edits vitals on a signed day | `/doctor/episode/a1111111-…` (day 2 NURSE_SIGNED) | **Vitals inputs `disabled`** — read-only by design (locked day) |
| C4 | Doctor signs a clinical day | `Підписати добу` | Gated: enabled only when day is `NURSE_SIGNED` for doctor |
| C5 | Nurse views & edits an OPEN day | `/nurse/episode/a1111111-…` (day 1 OPEN) | OK; vitals inputs present |
| C6 | Nurse signs an OPEN day | `Підписати добу` | Gated: enabled only when day is `OPEN` for nurse |
| C7 | HOD reopens a signed day | `/doctor/episode/a1111111-…` | **`Перевідкрити` (Reopen)** button present, requires reason |
| C8 | Admin views users & audit log | `/admin` | **DEFECT:** clicking "Переглянути журнал аудиту" throws `logs.map is not a function` |
| C9 | Create new ICU card | `/doctor/create-card` | Patient search by ПІБ/телефон/№ медкарти; dropdown needs async verification |
| C10 | Theme & user-menu controls | all pages | Present on every page; theme toggle observed |

---

## 3. Findings & defects

### 🔴 F1 — Admin audit log crashes (`logs.map is not a function`) — CONFIRMED DEFECT
- **Severity:** High (admin-only feature is completely broken)
- **Repro:** Login as `admin` → `/admin` → click **"Переглянути журнал аудиту"**.
- **Observed:** `PAGEERROR: TypeError: logs.map is not a function` (0 other errors on the page).
- **Root cause (verified via API):** `GET /api/audit?page=0&size=10` returns a Spring
  Data paginated envelope `{ "content": [ … ], "pageable": …, "totalElements": … }`.
  `frontend/src/pages/admin/AdminPage.tsx:44` does `setAuditLogs(res.data)` — assigning
  the **envelope object**, not the array. `AuditLogTable` then calls `.map` on an object.
- **Fix:** use `res.data.content` (with defensive fallback `?? []`).
- **Test impact:** new E2E + unit test (see §5 T-Admin-*).

### 🟡 F2 — Vitals inputs are disabled on locked (signed/closed) days — BY DESIGN, needs tests
- `PatientDayPage.tsx:120` → `isLocked = status !== 'OPEN' && status !== 'REOPENED'`.
- On a `NURSE_SIGNED` day (doctor's auto-selected day for `a1111111` is day 2 = NURSE_SIGNED),
  every `input[type=number]` is `disabled: true` → `.fill()` times out (observed).
- **Not a bug**, but a behavioral contract that must be asserted: editing is only possible
  on `OPEN`/`REOPENED` days. Exploration could not exercise an *edit* of vitals because the
  seeded OPEN day for the doctor role was not auto-selected.
- **Test impact:** E2E asserting disabled state on signed days + enabled state on OPEN day.

### 🟡 F3 — Silent error swallowing reduces observability — TESTABILITY GAP
- `PatientDayPage.tsx` `loadDayData`, `handleSignOff`, `handleReopen`, `handleGeneratePDF`
  all use empty `catch {}` blocks. Network/auth failures are invisible to the user and to
  tests (no error banner, no console error).
- Per agent-testing best practice (validate *error observability*), these paths should
  surface a message. Tests should assert that a forced backend error yields a visible error.
- **Test impact:** integration test asserting a 500 from an upstream call is surfaced;
  recommend a follow-up code change to render an `Alert`.

### 🟢 F4 — No console/page errors on normal flows
- All role dashboards, episode pages, tabs, and the create-card page rendered with **0 console
  errors and 0 API ≥400 responses** when using correct UUID seeds. The earlier 400s were a
  harness artifact (short IDs), now corrected.

### 🟢 F5 — Signing workflow correctly gated
- Nurse may sign only `OPEN` days; doctor may sign only `NURSE_SIGNED` days; HOD may reopen
  `NURSE_SIGNED`/`DOCTOR_SIGNED` days and only with a non-empty reason. Buttons appear/disappear
  exactly per these rules. Confirms backend `+ frontend` authorization symmetry.

### 🟡 F6 — Create-card patient search dropdown not verified
- Typing "Петренко" into the search field did not surface a dropdown in the text snapshot
  within the settle window. The mock MIS (`MockMisServiceImpl`) provides 5 patients, so the
  autocomplete likely works but is debounced/async. **Needs a dedicated E2E** (see T-CC-*).

---

## 4. Comprehensive use-case catalog

Each use case (UC) is a unique behavior worth at least one test. "Layer" indicates where the
primary assertion lives; most UCs are covered at multiple layers.

| UC | Use case | Primary layer | Existing coverage |
|---|---|---|---|
| UC-01 | Login with valid credentials redirects by role | E2E | `auth/login.spec.ts` |
| UC-02 | Login rejected on wrong password | E2E/Unit | `auth/login.spec.ts`, `AuthServiceTest` |
| UC-03 | Role-based route guard (doctor/nurse/admin/HOD) | E2E | `auth/access-control.spec.ts` |
| UC-04 | Doctor dashboard lists active episodes from mock MIS | E2E | `doctor/dashboard.spec.ts` |
| UC-05 | Open an episode → PatientDayPage renders | E2E | `doctor/episode.spec.ts` |
| UC-06 | **Doctor cannot edit vitals on a signed (locked) day** | E2E | *(missing)* |
| UC-07 | **Nurse can edit vitals on an OPEN day** | E2E | `nurse/vitals.spec.ts` (exists) |
| UC-08 | Episode tabs (notes/scales/ШВЛ/lab/state) switch panels | E2E | `doctor/notes`, `scales`, `ventilation`, `lab-results`, `patient-state` |
| UC-09 | Add a medical note (doctor & nurse) | E2E/Unit | `doctor/notes.spec.ts`, `MedicalNoteServiceTest` |
| UC-10 | Create & cancel a medical order (prescription) | E2E/Unit | `doctor/prescriptions*.spec.ts`, `MedicalOrderServiceTest` |
| UC-11 | Nurse signs an OPEN day (→ NURSE_SIGNED) | E2E/Unit | `nurse/*`, `SignatureIntegrationTest` |
| UC-12 | Doctor signs a NURSE_SIGNED day (→ DOCTOR_SIGNED) | E2E/Unit | `doctor/signoff*.spec.ts` |
| UC-13 | **HOD reopens a signed day with a reason** | E2E/Unit | `hod/clinical-day-reopen.spec.ts` |
| UC-14 | **HOD reopen blocked without a reason** | E2E/Unit | *(thin)* |
| UC-15 | Create new ICU card (patient search → create episode) | E2E/Unit | `doctor/create-card.spec.ts`, `EpisodeServiceTest` |
| UC-16 | **Create-card patient autocomplete from mock MIS** | E2E | *(missing)* |
| UC-17 | **Admin user tables (doctors/nurses) render** | E2E/Unit | `admin/admin.spec.ts`, `AdminPage.test.tsx` |
| UC-18 | **Admin audit log renders without crashing** | E2E/Unit | *(broken — F1)* |
| UC-19 | Admin audit log filter by entity | E2E | *(missing)* |
| UC-20 | Fluid balance recalculation | E2E/Unit | `nurse/fluid-balance*.spec.ts`, `FluidBalanceServiceTest` |
| UC-21 | Order execution (nurse) | E2E/Unit | `nurse/order-execution*.spec.ts` |
| UC-22 | PDF generation for a CLOSED day | E2E/Unit | `api/pdf-generation.spec.ts`, `PdfGeneratorIntegrationTest` |
| UC-23 | Optimistic-locking conflict (version) → 409 | API/Unit | `api/optimistic-locking.spec.ts` |
| UC-24 | Hourly-record clinical-range validation | Unit/Integration | `HourlyRecordValidationTest`, `HourlyRecordServiceTest` |
| UC-25 | Theme toggle persists across navigation | E2E | *(missing)* |
| UC-26 | **Error observability when backend returns 5xx** | E2E/Integration | *(gap — F3)* |
| UC-27 | Mock MIS error modes (timeout/not_found/unavailable) | API | `api/mis-error-scenarios.spec.ts` |

---

## 5. Minimum set of unique test cases to ADD

The following are **new** tests that close the gaps above. They are deliberately minimal and
non-duplicative — each targets a finding or an untested branch.

### 5.1 Playwright E2E (`tests/specs/…`)

| ID | Spec file | Test | UC | Validates |
|---|---|---|---|---|
| T-Admin-1 | `admin/audit-log.spec.ts` | admin opens audit log → table renders rows | UC-18 | **F1 regression:** no `logs.map` crash; rows visible |
| T-Admin-2 | `admin/audit-log.spec.ts` | admin filters audit by entity=AUTH → filtered rows | UC-19 | filter wired to `params.entity` |
| T-CC-1 | `doctor/create-card.spec.ts` (extend) | type "Петренко" → autocomplete shows patient | UC-16 | mock MIS dropdown |
| T-Lock-1 | `doctor/episode-locked.spec.ts` | doctor opens `a1111111` (day 2 NURSE_SIGNED) → vitals inputs `disabled` | UC-06 | **F2** locked-day contract |
| T-Lock-2 | `nurse/episode-open.spec.ts` | nurse opens `a1111111` day 1 OPEN → vitals inputs enabled | UC-07 | editable on OPEN |
| T-Reopen-1 | `hod/clinical-day-reopen.spec.ts` (extend) | HOD opens reopen dialog, submits empty reason → button disabled / no reopen | UC-14 | reason required |
| T-Err-1 | `doctor/episode.spec.ts` (extend) | force `/api/episodes/{id}` to 500 (route abort) → visible error Alert, no white screen | UC-26 | **F3** observability |
| T-Theme-1 | `doctor/dashboard.spec.ts` (extend) | toggle theme → persists after navigation to episode and back | UC-25 | theme state |

### 5.2 Frontend unit (Vitest, `frontend/src/test/…`)

| ID | Test file | Test | UC | Validates |
|---|---|---|---|---|
| U-Admin-1 | `pages/AdminPage.test.tsx` (extend) | `loadAudit` with paginated `{content:[…]}` → `auditLogs` is the array, `AuditLogTable` renders | UC-18 | **F1 fix** (`res.data.content`) |
| U-Admin-2 | `pages/AdminPage.test.tsx` (extend) | `loadAudit` with empty `content:[]` → no crash, empty state | UC-18 | defensive default |
| U-Lock-1 | `pages/PatientDayPage.test.tsx` (extend) | `isLocked` true for NURSE_SIGNED/DOCTOR_SIGNED/CLOSED, false for OPEN/REOPENED | UC-06/07 | locking logic |
| U-Lock-2 | `components/HourlyRecordTable.test.tsx` (extend) | inputs `disabled` when `isLocked` prop true | UC-06 | table respects lock |
| U-Sign-1 | `pages/PatientDayPage.test.tsx` (extend) | `canSign` false for doctor on OPEN day, true on NURSE_SIGNED | UC-11/12 | sign gating |
| U-Reopen-1 | `pages/PatientDayPage.test.tsx` (extend) | `canReopen` true only for HOD on signed days; reopen reason required | UC-13/14 | reopen gating |

### 5.3 Backend integration (`backend/src/test/java/.../integration/`)

| ID | Test file | Test | UC | Validates |
|---|---|---|---|---|
| I-Audit-1 | `AuditIntegrationTest.java` (extend) | `GET /api/audit` returns `content` array + pageable metadata | UC-18 | API contract matches frontend expectation |
| I-Audit-2 | `AuditIntegrationTest.java` (extend) | `GET /api/audit?entity=AUTH` filters by entity | UC-19 | filter implemented |
| I-Episode-1 | `EpisodeIntegrationTest.java` (extend) | `GET /api/episodes/{shortId}` → 400 with clear message (non-UUID) | — | **F1-corollary:** input validation |
| I-Reopen-1 | `ClinicalDayIntegrationTest.java` (extend) | reopen without reason → 400; with reason → status REOPENED + signature revoked | UC-13/14 | reopen contract |
| I-Lock-1 | `HourlyRecordIntegrationTest.java` (extend) | PATCH hourly record on signed day → 423/DocumentLocked | UC-06 | backend lock enforcement |

### 5.4 Backend unit (`backend/src/test/java/.../service/`)

| ID | Test file | Test | UC | Validates |
|---|---|---|---|---|
| S-Admin-1 | `AuditServiceTest.java` (extend) | `list()` returns `Page<AuditLog>` (content + pageable) — documents the shape the frontend must unwrap | UC-18 | contract clarity |
| S-Lock-1 | `ClinicalDayServiceTest.java` (extend) | `isLocked`/`canEdit` logic parity with frontend `isLocked` | UC-06/07 | shared rule |

---

## 6. Priority & execution order

1. **P0 (fix + regression):** F1 — implement `res.data.content` fix, then T-Admin-1 / U-Admin-1 / I-Audit-1.
2. **P1 (behavioral contracts):** F2 — T-Lock-1, T-Lock-2, U-Lock-1/2, I-Lock-1.
3. **P1 (gating):** T-Reopen-1, U-Reopen-1, I-Reopen-1.
4. **P2 (coverage gaps):** T-CC-1, T-Admin-2, T-Theme-1.
5. **P3 (observability):** T-Err-1 + recommend F3 code change to surface errors.

---

## 7. "Definition of done" for this plan

- [ ] F1 fixed in `AdminPage.tsx` and covered by T-Admin-1, U-Admin-1, I-Audit-1.
- [ ] Locked-day contract (F2) covered at E2E + unit + integration.
- [ ] Reopen reason-required path covered.
- [ ] Create-card autocomplete E2E added.
- [ ] Error-observability E2E added (and F3 code fix recommended in PR).
- [ ] All new tests green in CI (`mvn verify`, `npm t`, `npx playwright test`).
- [ ] Exploratory harness artifacts retained under `tests/exploratory/` for re-runs.

---

## 8. Reproduce the exploration

```powershell
# full stack already running: backend :8085, frontend :5173 (Vite), Postgres :5432
cd tests/exploratory
node explore.cjs      # inventory + failed-request capture -> exploratory-report.json
node interact.cjs     # flow exercise -> exploratory-interactions.json
```

Open `exploratory-report.json` for the full per-role, per-route element inventory and
`exploratory-interactions.json` for the click/type outcomes and captured errors.
