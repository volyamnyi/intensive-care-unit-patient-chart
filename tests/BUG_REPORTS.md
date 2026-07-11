# ICU Patient Chart — Bug Reports

Defects discovered and resolved during the Playwright E2E Test → Fix → Analyse cycle.
Each entry lists reproduction steps, expected vs. actual behaviour, and the fix applied.

---

## BUG-001 — Invalid login returns HTTP 500 instead of 401
**Severity:** Medium  **Type:** Security / API contract
**Component:** `AuthController.login`

**Steps to reproduce**
1. `POST /api/auth/login` with a wrong password.
2. Inspect the HTTP status.

**Expected:** `401 Unauthorized` (clear signal to the client; the SPA shows
"Невірний логін або пароль").
**Actual:** `500 Internal Server Error` — `AuthController` threw a raw
`RuntimeException("Invalid credentials")` which `GlobalExceptionHandler` mapped
to a 500.

**Root cause:** Auth failures were propagated as unchecked exceptions.
**Fix:** `AuthController.login` now returns
`ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null)` on unknown user or
invalid password. Tests: `TC-API-02/03/04/05`, `TC-AUTH-03`.

---

## BUG-002 — Cyrillic JSON responses mis-decoded by non-browser clients
**Severity:** Medium  **Type:** Encoding / interoperability
**Component:** Spring MVC message conversion

**Steps to reproduce**
1. `POST /api/auth/login` as `doctor1`.
2. Inspect the `Content-Type` header and decode the `fullName` field with a
   strict UTF-8 client.

**Expected:** `Content-Type: application/json;charset=UTF-8` and `fullName`
decoded as `Олександр Мельник`.
**Actual:** `Content-Type: application/json` (no charset). Clients that do not
default to UTF-8 (e.g. Playwright's request layer) decoded the bytes as
Windows-1251 → `РћР»РµРєСЃР°РЅРґСЂ РњРµР»СЊРЅРёРє`.

**Root cause:** Spring treats `application/json` as charset-agnostic and omits
the charset even though the bytes are UTF-8.
**Fix:** New `WebConfig.extendMessageConverters` forces
`application/json;charset=UTF-8` (and `*+json`) on the existing Jackson
converter while preserving Spring Boot's configured `ObjectMapper`. Tests:
`TC-API-02` (fullName equality).

---

## BUG-003 — Day sign-off authorization shadowed by wildcard matcher (NURSE/ADMIN could sign off)
**Severity:** High  **Type:** Broken access control
**Component:** `SecurityConfig`

**Steps to reproduce**
1. As `nurse1`, `POST /api/icu-days/{activeDayId}/sign-off`.
2. As `admin`, `POST /api/icu-days/{activeDayId}/sign-off`.

**Expected (per README RBAC):** only `DOCTOR` and `HEAD_OF_DEPARTMENT` may sign
off; `NURSE` and `ADMIN` receive `403`.
**Actual:** `200 OK` for both — the specific
`/api/icu-days/*/sign-off` rule was declared **after** the catch-all
`/api/icu-days/**` rule, so Spring's first-match-wins semantics never reached it.

**Root cause:** Matcher ordering — the wildcard matched first.
**Fix:** Moved `POST /api/icu-days/*/sign-off` **before** `/api/icu-days/**` and
restricted it to `DOCTOR, HEAD_OF_DEPARTMENT`. Tests: `TC-API-27..31`.

---

## BUG-004 — RBAC matrix misaligned with documented permissions
**Severity:** Medium  **Type:** Broken access control
**Component:** `SecurityConfig`

**Steps to reproduce**
1. `POST /api/prescriptions/by-card/{id}` as `nurse1` → `200` (should be `403`).
2. `GET /api/patients/search` as `admin` → `403` (README says admin may view
   patient data).

**Expected (README matrix):** prescription *create/stop* limited to
`DOCTOR`+`HOD`; *execution* allowed for `NURSE`; all clinical roles (incl.
`ADMIN`) may read patient/clinical data.
**Actual:** `NURSE` could create prescriptions; `ADMIN` was excluded from
patient search and all clinical read endpoints.

**Fix:** Rewrote the matcher chain to:
- `POST /api/icu-cards` → `DOCTOR, HEAD_OF_DEPARTMENT`
- `POST /api/prescriptions/*/execute` → `DOCTOR, NURSE, HEAD_OF_DEPARTMENT`
- `POST /api/prescriptions/**` → `DOCTOR, HEAD_OF_DEPARTMENT`
- `GET /api/**` (cards, days, prescriptions, patients/search, users) → all four
  clinical roles.
Tests: `TC-API-16..21`, `TC-API-32..35`.

---

## BUG-005 — Administrator role had no functional UI (blank screen / no route)
**Severity:** High  **Type:** Functional gap
**Component:** `App.tsx` routing + missing admin page

**Steps to reproduce**
1. Log in as `admin`.
2. Observe the landing page.

**Expected:** Admin is routed to a dedicated panel (e.g. user management).
**Actual:** `RoleRedirect` sent `ADMINISTRATOR` to `/doctor`; the doctor `Guard`
rejected the role and rendered `null` → a blank page. No admin experience existed.

**Fix:** Added `frontend/src/pages/admin/AdminPage.tsx` (lists doctors & nurses
from `/api/users/*`), a `/admin` route guarded for `ADMINISTRATOR`, and updated
`RoleRedirect` to map `ADMINISTRATOR → /admin`. Tests: `TC-AUTH-06`,
`TC-ADM-01..05`, `TC-ROLE-04`.

---

## BUG-006 — Guard component race condition (authenticated user redirected to `/`)
**Severity:** High  **Type:** Functional / Navigation
**Component:** `App.tsx` Guard wrapper

**Steps to reproduce**
1. Log in as any role → frontend stores token in `localStorage`.
2. `AuthProvider` sets `isAuthenticated = true` immediately (from token presence).
3. Guard's role check sees `user === null` (not yet fetched) and returns early →
   falls through to the unauthenticated redirect → navigates to `/`.
4. By the time `fetchUser()` resolves, the page has already redirected away from
   the intended `/doctor`, `/nurse`, or `/admin`.

**Expected:** Guard waits for the user fetch to complete before making role-based
redirect decisions.

**Actual:** The early return on `user === null` allowed the fallback redirect to
`/` to fire prematurely, breaking all role-protected direct URLs.

**Root cause:** The Guard component's role-check and redirect logic was placed
*after* the `if (!user) return null` early guard, meaning when the token existed
but `fetchUser()` hadn't completed yet, the component returned `null` and the
fallback redirect to `/` took over.

**Fix:** Reordered Guard so that:
- The `isAuthenticated === false` redirect to `/login` fires immediately (no
  fetch needed).
- Role checks are skipped when `user === null` by returning `<Outlet />` and
  letting the `useEffect(fetchUser)` settle.
- `useEffect` on `[user]` handles role-specific redirects *after* the fetch
  completes.

Tests: `TC-AUTH-07`, `TC-DOC-07`, `TC-NURSE-05`, `TC-HOD-05`, `TC-ROLE-01..03`.

---

## BUG-007 — Database SQL init file encoding (Cyrillic corrupted on Windows)
**Severity:** Medium  **Type:** Encoding / Portability
**Components:** `application.yml` + `spring.sql.init`

**Steps to reproduce**
1. Run backend on Windows with system encoding set to Windows-1251.
2. `data.sql` contains UTF-8 Cyrillic literals (e.g. `Олександр Мельник`).
3. Spring Boot reads `data.sql` using the JVM's default `file.encoding`.
4. PostgreSQL receives the bytes already corrupted.

**Expected:** `fullName` in the database and API responses is valid UTF-8
Cyrillic.

**Actual:** `fullName` returns garbled characters (e.g. `РћР»РµРєСЃР°РЅРґСЂ`
instead of `Олександр`).

**Root cause:** Spring Boot 3.x does not force UTF-8 when reading SQL init
scripts. The JVM defaults to the OS encoding (Windows-1251). PostgreSQL also
uses the client encoding negotiated at connection time.

**Fix:** Added two settings in `application.yml`:
- `spring.sql.init.encoding: UTF-8` — forces Spring to read `data.sql` as UTF-8.
- `spring.datasource.hikari.connection-init-sql: SET client_encoding = 'UTF8'` —
  forces the PG client connection to use UTF-8 regardless of OS locale.

Tests: `TC-API-02` (`fullName` equals `Олександр Мельник`).

---

## CI/CD

A GitHub Actions workflow (`.github/workflows/playwright.yml`) was added:
- Ubuntu runner with PostgreSQL 16 service container.
- Backend started with `SPRING_JPA_HIBERNATE_DDL_AUTO=update`,
  `SPRING_JPA_DEFER_DATASOURCE_INITIALIZATION=true` (fresh DB, schema not
  pre-created).
- Health-check via `POST /api/auth/login` (permitAll, returns 200 for valid
  creds).
- Playwright report uploaded as `playwright-report` artifact (retained 7 days).

## Status

All seven reported defects are fixed and covered by automated regression tests.
The full Playwright suite of **85 tests** passes:
- 46 API (auth, patients, cards, days, vitals, prescriptions, balance, users)
- 8 Auth UI (login, logout, redirects, error messages)
- 8 Doctor dashboard (cards, search, create, sign-off, prescriptions, guards)
- 5 Nurse dashboard (vitals, prescription execution, fluid balance, guards)
- 5 HOD dashboard (cards, sign-off, prescriptions, guards)
- 5 Admin panel (user lists, column display, guards, user menu)
- 8 Role separation (guards, badges, dashboard titles, unknown routes)

See `TEST_PLAN.md` for the full test matrix and the HTML report under
`test-results/report`.
