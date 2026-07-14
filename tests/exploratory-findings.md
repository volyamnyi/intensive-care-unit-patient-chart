# Exploratory Testing Findings

## Session v1 (Initial)
**Date:** 2026-07-14
**Duration:** ~35 min
**Screenshots:** 26 captured

## Session v2 (Fullscreen, Expanded Coverage)
**Date:** 2026-07-14
**Duration:** Full 6-session run (~90 min)
**Screenshots:** 54 captured in `tests/exploratory-report/`
**Improvements vs v1:** Fullscreen mode (`noViewport: true`, `--start-maximized`), fixed locators (button not link), 17 new use cases, 404 handling, sign-dialog cancel, user menu comparison, browser back/forward, AppBar link navigation

---

## Issues Found

### P1 — MIS Autocomplete selection doesn't populate patient data fields
**File:** `frontend/src/components/common/PatientSearch.tsx` + `frontend/src/pages/doctor/CreateCardPage.tsx`
**Detail:** After selecting "Коваленко" from the MIS autocomplete dropdown, all 7 patient data fields (ПІП, Дата народження, Стать, Зріст, Маса, Група крові, Rezus) showed as MISSING. Either:
1. The autocomplete selection (`onChange`) didn't fire properly via Playwright's option click
2. The state update didn't trigger re-render of the patient data Paper
3. `getByText` locator doesn't match MUI TextField labels (need to verify from screenshot)
**Severity:** Medium — either a test script issue or an app interaction bug
**Screenshot:** `03.06-patient-data.png`

### P2 — HourSelector not found
**Detail:** The vitals tab on the nurse episode page showed 0 hour buttons with `:00` text and only 9 total buttons. The episode may have no clinical days with hour data, or the hour selector renders differently than expected.
**Severity:** Minor — may be a seed-data issue where clinical days don't have hourly records
**Screenshots:** `04.05-vitals-tab.png`, `05.01-no-hour-btn.png`

### P3 — "Пацієнти" nav uses link role, not button
**File:** `frontend/src/layouts/DoctorLayout.tsx:24-29`
**Detail:** The "Пацієнти" navigation element is a `<Button component={RouterLink}>` which renders as an `<a>` (role=link), not role=button. Script used `getByRole('button', { name: 'Пацієнти' })` and correctly got `false`.
**Severity:** Trivial (script locator bug, not app bug)
**Fix:** Use `getByRole('link', { name: 'Пацієнти' })` or `getByText('Пацієнти')`

### P3 — Nurse role label may need longer wait after menu open
**File:** `tests/exploratory-session.mjs:634`
**Detail:** Nurse role label ("Медсестра") showed as `false` after clicking user menu and waiting 800ms. Doctor role label ("Лікар") was found correctly with same wait. May be a race condition with MUI Menu animation when opening in a fresh context.
**Severity:** Trivial (script timing issue)
**Screenshots:** `16.01-doctor-menu.png`, `16.02-nurse-menu.png`

### P4 — ClinicalDayTimeline chip locator doesn't match Ukrainian labels
**Detail:** The chip status filter uses English regex (`OPEN|NURSE|DOCTOR|CLOSED`) but the app likely uses Ukrainian status labels. Found 0 chips.
**Severity:** Trivial (script locator bug)
**Fix:** Use Ukrainian status labels (ВІДКРИТО, ПІДПИСАНО МЕДСЕСТРОЮ, ПІДПИСАНО ЛІКАРЕМ, ЗАКРИТО)

---

## Session v2 Results Summary

| Session | Topic | Status | Screenshots | Key Findings |
|---------|-------|--------|-------------|--------------|
| 1 | Authentication | ✅ All 9 TCs pass | 11 | All 6 users login, route restrictions work, logout redirects, invalid creds stay on /login |
| 2 | Doctor Dashboard + Create Card | ✅ 7/7 pass | 8 | Dashboard shows 5 table rows, search works, "Нова карта" navigates, episode opens, cancel returns |
| 3 | Episode Nav + Vitals | ⚠️ 4/5 pass | 6 | All 5 tabs visible & switchable, re-render OK; HourSelector: 0 hour buttons found |
| 4 | Orders + Notes | ✅ 5/5 pass | 6 | Prescription created, notes added with ordering, tab persistence OK |
| 5 | Scales + Balance + Sign + Timeline | ✅ 7/7 pass | 4 | Scales empty state visible, recalculate button present, sign dialog opens+cancels |
| 6 | Admin + Edge Cases + User Menu | ⚠️ 10/12 pass | 13 | Admin tables (2), logout OK, 404 route, back/forward, AppBar link all work; "Пацієнти" nav not found as button, nurse role label timing |

### New Features Verified in v2:
- **404 route**: `/nonexistent-route` stays on that URL (no redirect) — shows React Router's default unmatched route handling
- **Browser back/forward**: From episode → dashboard (back) → episode (forward), works correctly
- **AppBar title link**: "Карта інтенсивної терапії" navigates to `/doctor`
- **Sign dialog cancel**: Cancel button visible and clickable in sign dialog
- **User menu**: Doctor role label works; nurse role label needs investigation
- **Prescription created** with all fields: Norepinephrine, 4 mcg, IV, stat, 2026-07-14T10:00
- **Two notes added**: Check ordering via screenshots

---

## Coverage Gaps

### Critical (core clinical workflows)
| Gap | Area | Reason |
|-----|------|--------|
| Full sign-off chain | Sign-off | Nurse signs → Doctor signs → day status advances |
| Order execution by nurse | Orders | Nurse executes a doctor's prescription |
| MIS patient data fields | Create Card | Autocomplete selection may not populate fields |

### Important (UI completeness)
| Gap | Area | Reason |
|-----|------|--------|
| Empty states | All tabs | Dashboard, notes, orders, scales empty states |
| API error handling | Resilience | HTTP 409 (version conflict), 500, network failure |
| Clinical day timeline | Navigation | Select different clinical days via timeline chips |

### Nice-to-have (edge cases)
| Gap | Area | Reason |
|-----|------|--------|
| Token expiry | Auth | Auto-redirect to login when JWT expires |
| Scale creation | Scales | Requires seed data or conditional test |
| Fluid balance recalculate | Fluids | "Перерахувати" button functional test |
| Prescription cancel | Orders | Cancel active prescription |
| Hour selector on nurse | Vitals | Hour grid rendering investigation |
| Concurrent sessions | Collaboration | Two browser tabs, doctor+nurse on same episode |

---

## Priority Recommendations for Additional E2E Coverage

| Priority | Area | Test Description | Reason |
|----------|------|-----------------|--------|
| P1 | Sign-off Workflow | Full nurse → doctor sign chain | Core clinical workflow |
| P1 | Order Execution | Nurse executes doctor's order | Core clinical workflow |
| P1 | MIS Patient Data | Verify patient data renders after autocomplete selection | Core creation workflow |
| P2 | Empty States | Dashboard/notes/orders when no data | UI completeness |
| P2 | Error Handling | HTTP 409, 500, network failure | Resilience |
| P2 | Clinical Day Timeline | Switch between multiple clinical days | Data navigation |
| P3 | HourSelector | Select different hours in vitals tab | Data entry workflow |
| P3 | Scale Creation | Doctor creates scale result | Requires seed data |
| P3 | Fluid Balance | Recalculate button | Existing feature |
| P3 | Prescription Cancel | Cancel active prescription | Order management |
| P3 | User Menu | Role labels, nav links per role | Role-based UI verification |
| P3 | Browser Navigation | Back/forward, 404, AppBar link | UX edge cases |
| P3 | Token Expiry | Auto-redirect on expired JWT | Auth security |
