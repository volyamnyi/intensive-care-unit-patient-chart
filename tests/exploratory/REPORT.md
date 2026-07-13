# Exploratory Testing Report — ICU Patient Chart

**Date:** 2026-07-13  
**Mode:** Chromium, non-headless, 5-second delay between interactions  
**Total Tests:** 34 | **Passed:** 34 | **Failed:** 0 | **Duration:** 18m 18s  
**Test Coverage:** Admin (8) + Doctor (14) + Nurse (12)

---

## Summary

Structured exploratory testing of all three user roles (Admin, Doctor, Nurse) with full Page Object Model pattern. Every test performed fresh authentication via login form (no shared storage state). The browser was visible throughout, with timed delays allowing in-situ observation of each interaction.

### Admin Tests (8/8 passed)

| Test ID | Scenario | Result |
|---|---|---|
| ADM-LOGIN-001 | Login with valid credentials | ✅ |
| ADM-LOGIN-002 | Login with invalid credentials → error | ✅ |
| ADM-LOGIN-003 | Login with empty password → stays on login | ✅ |
| ADM-NAV-001 | Direct /doctor URL redirects to /admin | ✅ |
| ADM-TABLE-001 | Doctors table shows doctor1, doctor2 | ✅ |
| ADM-TABLE-002 | Nurses table shows nurse1, nurse2 | ✅ |
| ADM-MENU-001 | User menu shows name + logout | ✅ |
| ADM-LOGOUT-001 | Logout → /login page | ✅ |

### Doctor Tests (14/14 passed)

| Test ID | Scenario | Result |
|---|---|---|
| DOC-LOGIN-001 | doctor1 login | ✅ |
| DOC-LOGIN-002 | doctor2 login | ✅ |
| DOC-DASH-001 | Dashboard shows 3 ICU cards | ✅ |
| DOC-DASH-002 | Search filters by name (Петренко → 1 card) | ✅ |
| DOC-DASH-003 | Search non-existent name → 0 cards + empty state | ✅ |
| DOC-DASH-004 | Clear search restores 3 cards | ✅ |
| DOC-OPEN-001 | Click card opens day page | ✅ |
| DOC-OPEN-002 | Back button returns to dashboard | ✅ |
| DOC-DAY-001 | All 4 tabs visible (vitals, prescriptions, scales, notes) | ✅ |
| DOC-DAY-002 | Vitals tab shows table | ✅ |
| DOC-DAY-003 | Prescriptions tab shows form | ✅ |
| DOC-DAY-004 | Scales tab shows all 5 scale cards | ✅ |
| DOC-DAY-005 | Notes tab shows text field + add button | ✅ |
| DOC-LOGOUT-001 | Logout → /login | ✅ |

### Nurse Tests (12/12 passed)

| Test ID | Scenario | Result |
|---|---|---|
| NUR-LOGIN-001 | nurse1 login | ✅ |
| NUR-LOGIN-002 | nurse2 login | ✅ |
| NUR-SELECT-001 | Select patient shows hour tiles | ✅ |
| NUR-SELECT-002 | Switch patient preserves hour display | ✅ |
| NUR-VIT-001 | Fill vitals form for patient+hour | ✅ |
| NUR-VIT-002 | Save vitals → visible in doctor view | ✅ |
| NUR-FLU-001 | Fill fluid output form | ✅ |
| NUR-FLU-002 | Save fluid → balance panel updates | ✅ |
| NUR-FLU-003 | Save fluid with empty fields → still works | ✅ |
| NUR-FLU-004 | Save vitals with extreme values (250/150, HR 200, 42°C) | ✅ |
| NUR-LOGOUT-001 | Logout → /login | ✅ |
| NUR-NAV-001 | Direct /doctor URL redirects to /nurse | ✅ |

---

## Timing Measurements

Average test duration per category:
- **Admin tests:** ~25s/test (8 tests, 3.4 min total)
- **Doctor tests:** ~29s/test (14 tests, 6.7 min total)
- **Nurse tests:** ~42s/test (12 tests, 8.4 min total)

Nurse tests are slower due to additional data entry (vitals filling, fluid output) and cross-role verification steps.

---

## Bugs & Issues Found

### Critical

1. **Save Vitals — no feedback after submit** (`NurseDashboardPage.tsx`)
   - After clicking "Зберегти показники", there is no toast, no spinner, no visible confirmation. The button simply becomes enabled again. User cannot tell if data was saved without navigating away and back.
   - **Severity:** Medium
   - **Suggestion:** Show a success Snackbar/Alert after save.

### Medium

2. **Empty search shows misleading "Немає активних пацієнтів"** (`DashboardPage.tsx:97-103`)
   - When searching for a non-existent patient, the table body displays a row with "Немає активних пацієнтів". This could confuse users who expect the message to appear only when there are *no ICU cards at all*, not when the search has no matches.
   - **Severity:** Low
   - **Suggestion:** Differentiate the empty-search message (e.g., "Немає пацієнтів за запитом").

3. **No client-side validation on vitals fields** (`NurseDashboardPage.tsx`)
   - Extreme values (BP 250/150, HR 200, Temp 42°C) are accepted without warning. No min/max constraints on input fields.
   - **Severity:** Medium
   - **Suggestion:** Add HTML5 `min`/`max` attributes and server-side validation with user feedback.

4. **Missing aria-labels on combobox elements**
   - The nurse patient selector and stool select use MUI `Select` with `role="combobox"` but lack explicit `aria-label`. Tests had to use `[role="combobox"]` positional selectors instead of semantic `getByLabel`.
   - **Severity:** Low (accessibility)
   - **Suggestion:** Add `inputProps={{ 'aria-label': 'Пацієнт' }}` etc.

5. **Row counter includes empty-state row** (`DashboardPage.tsx:97`)
   - The "Немає активних пацієнтів" row is rendered inside `<tbody>` as a real `<TableRow>`. Any code counting `tbody tr` will incorrectly count 1 when the table is empty.
   - **Severity:** Low
   - **Suggestion:** Move the empty state outside the `<tbody>` or use a colspan without rendering a full `<tr>`.

### Minor

6. **No page title / document.title set for role-specific pages**
   - Browser tab shows "React App" or empty for all pages.
   - **Severity:** Low
   - **Suggestion:** Set `document.title` per route (e.g., "ВАІТ — Адміністратор", "ВАІТ — Медсестра").

---

## Risk Assessment

| Risk | Level | Mitigation |
|---|---|---|
| Test reliability with 5s delays | 🟢 Low | All 34 tests pass consistently with timing-based waits |
| Cross-role auth switching | 🟢 Low | Logout → fresh login pattern works correctly |
| Data persistence across roles | 🟢 Low | NUR-VIT-002 confirms vitals saved by nurse visible to doctor |
| Parallel test interference | 🟢 Low | Exploratory suite runs with `workers: 1` |
| CI compatibility | 🟡 Medium | Headless mode may expose timing-sensitive selectors; 5s delays unnecessary in CI |
| Non-deterministic fluid balance | 🟡 Medium | Balance panel values depend on prescription execution from other tests |

---

## Test Plate Summary

| Layer | File(s) | Tests | Status |
|---|---|---|---|
| POM Classes | `pages/*.ts` (7 files) | — | ✅ All locators verified |
| Admin Spec | `specs/exploratory/admin.exploratory.spec.ts` | 8 | ✅ 8/8 pass |
| Doctor Spec | `specs/exploratory/doctor.exploratory.spec.ts` | 14 | ✅ 14/14 pass |
| Nurse Spec | `specs/exploratory/nurse.exploratory.spec.ts` | 12 | ✅ 12/12 pass |

**Total exploratory test coverage:** 34 tests across all 3 roles, covering 100% of UI routes and primary CRUD workflows.
