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

## Session v3 (55 UC, 59 TC, 52 screenshots)
**Date:** 2026-07-14
**Script:** `tests/exploratory-session-v3.mjs`
**Result:** 55 PASS, 4 FAIL (93%)
**Screenshots:** 52 captured in `tests/exploratory-report-v3/`

### Improvements vs v2
- **55 use cases** covered (up from 17) — full testomat.io methodology with severity classification
- New areas: root redirect, search clear/restore, patient data fields verified OK (P1 fix from v2), duplicate episode error, back button per role, patient name heading, browser back/forward/refresh chain, direct URL access, all 5 tab empty states audit, role-gated episode controls (doctor vs nurse), doctor role label, status chips
- Fixed P1 v2 bug: patient data fields now populate correctly after autocomplete selection (TC-09.01 PASS)
- Used direct login flows instead of expired auth state files for reliability

### New Findings

#### P1 — ClinicalDayTimeline chips not found (TC-13.01)
**Detail:** Attempted to find timeline chips via `[class*="MuiChip"]` with text "Доба" — found 0 chips. The component may use a different DOM structure (Box-based tiles, not MUI Chip) or the episode doesn't render the timeline without clinical days.
**Screenshots:** `13.01-timeline.png`
**Recommendation:** Inspect ClinicalDayTimeline component rendering for the current episode seed data

#### P1 — Order creation Активне chip not found (TC-18.01)
**Detail:** Dopamine order was submitted via the form (all fields filled, "Створити" clicked) but the subsequent check for `[class*="MuiChip"]` with text "Активне" returned false. Either: (a) order creation failed silently, (b) chip uses different label ("ACTIVE" or "Активний"), or (c) timing issue after save.
**Screenshots:** `18.01-order-form.png`, `18.01-order-created.png`

#### P2 — Note timestamp not found (TC-22.04)
**Detail:** Note text "V3 exploratory test note" was added successfully but timestamp containing "2026" was not found. The note card may use relative time (e.g., "щойно" / "just now") instead of absolute timestamps.
**Screenshots:** `22.01-note-added.png`

#### P2 — Nurse role label "Медсестра" continues to fail (TC-34.02)
**Detail:** Same timing issue as v2 P3 — nurse role label not visible after clicking user menu with 1000ms wait. Doctor role label "Лікар" works consistently.
**Fix:** Increase wait after menu click to 1500ms, or wait for menu animation to complete via `waitForSelector('[role="menu"]')`

### v3 New Features Verified
- **Search + clear restores full list**: 5→1→5 rows (TC-06.03)
- **Duplicate episode error**: Alert shown when creating card for patient with ACTIVE episode (TC-10.04)
- **Back button per role**: Doctor→/doctor, Nurse→/nurse (TC-12.04/12.05)
- **Browser back/forward/refresh**: Full navigation chain works (TC-37.01–37.03)
- **Invalid episode ID**: No crash/error boundary (TC-36.02)
- **Doctor can create orders**: Nurse cannot (TC-55.01/55.02) — role gating confirmed
- **Direct URL /doctor/create-card**: Loads correctly (TC-38.01)
- **Fluid balance recalculate**: Button found + clicked (TC-27.01/27.02)
- **Nurse sign dialog**: Title "Підписання доби №N" confirmed (TC-30.01)
- **Status chip**: Episode status visible on page (TC-35.01)
- **Admin tables**: 2 tables with Лікарі/Медсестри headings (TC-31.01–31.03)
- **404 route**: No crash on unknown path (TC-36.01)

---

## Coverage Gaps (Updated — Post v3)

### Resolved in v3
| Gap | Status | Note |
|-----|--------|------|
| MIS patient data fields | ✅ RESOLVED | Fields ALL OK after using `page.evaluate()` click (TC-09.01) |
| Empty states audit | ✅ COVERED | All 5 tabs rendered for visual audit (TC-40.01) |
| User menu role labels | ⚠️ PARTIAL | Doctor "Лікар" PASS, Nurse "Медсестра" FAIL (timing) |
| Browser navigation | ✅ COVERED | Back/forward/refresh chain verified (TC-37.01–37.03) |
| 404 route | ✅ COVERED | No crash on unknown route (TC-36.01) |
| Fluid balance recalculate | ✅ COVERED | Button found + clicked (TC-27.01/27.02) |
| Nurse sign dialog | ✅ COVERED | Dialog opens with correct title (TC-30.01) |
| Role-gated order creation | ✅ COVERED | Doctor can create, nurse cannot (TC-55.01/55.02) |
| Duplicate episode error | ✅ COVERED | Alert shown (TC-10.04) |
| Search + clear | ✅ COVERED | 5→1→5 rows verified (TC-06.03) |
| New: auth root redirect | ✅ COVERED | All 3 roles redirect correctly (TC-04.01–04.03) |
| New: invalid episode ID | ✅ COVERED | No crash (TC-36.02) |

### Still Open
| Gap | Area | Priority | Reason |
|-----|------|----------|--------|
| Full sign-off chain (nurse→doctor) | Sign-off | P1 | Requires sequential nurse-sign then doctor-sign test |
| Order execution by nurse | Orders | P1 | Active order needed in seed data for nurse to execute |
| Clinical day timeline chips | Timeline | P1 | Component structure needs investigation (0 chips found) |
| Order creation confirmation | Orders | P1 | Активне chip not found after submission |
| Note timestamp display | Notes | P2 | Note card may use relative time format |
| API error handling | Resilience | P2 | HTTP 409, 500, network failure (needs backend manipulation) |
| Token expiry | Auth | P2 | Auto-redirect on expired JWT |
| Scale creation | Scales | P2 | Requires available scales in seed data |
| Hour selector with data | Vitals | P2 | 0 hour pills found — seed data may lack hourly records |
| Prescription cancel | Orders | P3 | Cancel active order |
| Concurrent sessions | Collaboration | P3 | Two tabs / doctor+nurse on same episode |
| PDF generation | PDF | P3 | Triggered after doctor signs |
| Clinical day reopen | Timeline | P3 | Reopen signed day |
| Episode close | Episode | P3 | Discharge workflow |

---

## Priority Recommendations for Additional E2E Coverage

| Priority | Area | Test Description | Reason |
|----------|------|-----------------|--------|
| P1 | Sign-off Workflow | Full nurse → doctor sign chain | Core clinical workflow |
| P1 | Order Execution | Nurse executes doctor's order (needs ACTIVE order in seed data) | Core clinical workflow |
| P1 | Clinical Day Timeline | Investigate component structure & Ukrainian labels | Data navigation |
| P1 | Order Creation Assertion | Verify Активне chip appears after order submit | Order management |
| P2 | Note Timestamp | Verify note card shows creation time (relative vs absolute) | UI completeness |
| P2 | HourSelector | Seed hourly records to verify pill rendering with data | Data entry workflow |
| P2 | Error Handling | HTTP 409, 500, network failure | Resilience |
| P2 | Token Expiry | Auto-redirect on expired JWT | Auth security |
| P2 | Scale Creation | Create scale result (needs scales in seed data) | Scale workflow |
| P3 | Prescription Cancel | Cancel active prescription | Order management |
| P3 | Concurrent Sessions | Two tabs / doctor+nurse on same episode | Collaboration |
| P3 | PDF Generation | Verify PDF generated after doctor signs | Document workflow |
| P3 | Clinical Day Reopen | Reopen signed day | Day management |
