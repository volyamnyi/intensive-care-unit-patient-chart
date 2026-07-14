# ICU Patient Chart — Exploratory Test Plan (v2)

## Session Charter

**Objective:** Discover functional defects, usability issues, and coverage gaps in the ICU Patient Chart web application through session-based exploratory testing across all roles and core workflows.

**Methods:** Session-based exploratory testing (see testomat.io guide — process: classify errors → create charter → time box → assess → debrief)

**Time Box:** 90 minutes per session

**Scope:** Frontend UI (http://localhost:5173) — login, dashboards, create card, episode page (5 tabs), sign-off, navigation, role-based access control, error states, user menu, session handling.

**Out of Scope:** Backend API (covered by unit/integration tests), PDF generation, performance/load testing, mobile responsiveness.

**Environment:** Playwright chromium, non-headless, fullscreen, slowMo=400ms

---

## Use Cases & Test Cases

### UC-01: Authentication & Authorization
| ID | Test Case | Precondition | Steps | Expected Result |
|----|-----------|-------------|-------|-----------------|
| TC-01.01 | Login as DOCTOR | No active session | 1. Go to /login<br>2. Enter doctor1 / doctor123<br>3. Click Увійти | Redirected to /doctor, user name visible in AppBar |
| TC-01.02 | Login as NURSE | No active session | 1. Go to /login<br>2. Enter nurse1 / nurse123<br>3. Click Увійти | Redirected to /nurse, "медсестра" in AppBar title |
| TC-01.03 | Login as HOD | No active session | 1. Go to /login<br>2. Enter head1 / head123<br>3. Click Увійти | Redirected to /doctor (same layout as doctor) |
| TC-01.04 | Login as ADMIN | No active session | 1. Go to /login<br>2. Enter admin / admin123<br>3. Click Увійти | Redirected to /admin, "Користувачі системи" visible |
| TC-01.05 | Login with invalid credentials | No active session | 1. Go to /login<br>2. Enter wrong login/password<br>3. Click Увійти | Error message displayed, stays on /login |
| TC-01.06 | Empty login fields | No active session | 1. Go to /login<br>2. Click Увійти without filling | HTML5 validation blocks, no network call |
| TC-01.07 | Route restriction — NURSE→/doctor | Auth as NURSE | 1. Navigate to /doctor | Redirected to /nurse |
| TC-01.08 | Route restriction — DOCTOR→/nurse | Auth as DOCTOR | 1. Navigate to /nurse | Redirected to /doctor |
| TC-01.09 | Route restriction — ADMIN→/doctor | Auth as ADMIN | 1. Navigate to /doctor | Redirected to /admin |
| TC-01.10 | Route restriction — ADMIN→/nurse | Auth as ADMIN | 1. Navigate to /nurse | Redirected to /admin |
| TC-01.11 | Logout from DOCTOR | Auth as DOCTOR | 1. Click user menu icon<br>2. Click "Вийти" | Redirected to /login, /doctor blocked after |
| TC-01.12 | Logout from NURSE | Auth as NURSE | 1. Click user menu icon<br>2. Click "Вийти" | Redirected to /login, /nurse blocked after |
| TC-01.13 | Logout from ADMIN | Auth as ADMIN | 1. Click user menu icon<br>2. Click "Вийти" | Redirected to /login, /admin blocked after |
| TC-01.14 | Login as doctor2 | No active session | 1. Enter doctor2 / doctor123 | Redirected to /doctor |
| TC-01.15 | Login as nurse2 | No active session | 1. Enter nurse2 / nurse123 | Redirected to /nurse |

### UC-02: Doctor Dashboard
| ID | Test Case | Precondition | Steps | Expected Result |
|----|-----------|-------------|-------|-----------------|
| TC-02.01 | Dashboard loads with active episodes | Auth as DOCTOR/HOD | 1. Go to /doctor | Table shows episodes with name, date, open button |
| TC-02.02 | Search filters episode list | Auth as DOCTOR/HOD | 1. Go to /doctor<br>2. Type patient name in search | Table filtered to matching rows |
| TC-02.03 | "Нова карта" navigates to create-card | Auth as DOCTOR/HOD | 1. Click `button[name="Нова карта"]` | URL → /doctor/create-card |
| TC-02.04 | "Відкрити" opens episode | Auth as DOCTOR/HOD | 1. Click `button[name="Відкрити"]` first() | URL → /doctor/episode/{id} |
| TC-02.05 | Page title | Auth as DOCTOR/HOD | 1. Check document.title | Contains "Карта інтенсивної терапії" |
| TC-02.06 | Empty search for nonexistent patient | Auth as DOCTOR/HOD | 1. Search for "ZZZ_NONEXISTENT" | Table empty or "нічого не знайдено" |
| TC-02.07 | Dashboard patient name links to episode | Auth as DOCTOR/HOD | 1. Click patient name in table (not button) | URL → /doctor/episode/{id} |
| TC-02.08 | AppBar "Пацієнти" link returns to dashboard | On /doctor/create-card | 1. Click "Пацієнти" link in AppBar | URL → /doctor |

### UC-03: Create Card (Episode)
| ID | Test Case | Precondition | Steps | Expected Result |
|----|-----------|-------------|-------|-----------------|
| TC-03.01 | Search patient from MIS | Auth as DOCTOR/HOD | 1. Go to /doctor/create-card<br>2. Type name (>=2 chars) into ПІБ autocomplete | Dropdown shows matching patients |
| TC-03.02 | Create episode successfully | Auth as DOCTOR/HOD, no ACTIVE episode for patient | 1. Search + select patient<br>2. Verify patient data displayed<br>3. Click "Створити карту" | URL → /doctor/episode/{id} |
| TC-03.03 | Cancel returns to dashboard | Auth as DOCTOR/HOD | 1. Search + select patient<br>2. Click "Скасувати" | URL → /doctor |
| TC-03.04 | Short query shows hint | Auth as DOCTOR/HOD | 1. Type single character | "Введіть мінімум 2 символи" visible |
| TC-03.05 | Error when patient has ACTIVE episode | Patient already has ACTIVE episode | 1. Search + select patient with existing episode<br>2. Click "Створити карту" | Alert "Помилка створення карти" |
| TC-03.06 | Full patient data after selection | Auth as DOCTOR/HOD | 1. Select patient from search | Fields: ПІП, Дата народження, Стать, Зріст, Маса, Група крові, Rezus, № медкарти |
| TC-03.07 | Page heading visible | Auth as DOCTOR/HOD | 1. Go to /doctor/create-card | h5 "Нова карта інтенсивної терапії" |

### UC-04: Episode Page — Tab Navigation
| ID | Test Case | Precondition | Steps | Expected Result |
|----|-----------|-------------|-------|-----------------|
| TC-04.01 | All 5 tabs visible | On episode page (any role) | 1. Check tab bar | Тabs: Вітальні показники, Призначення, Шкали, Нотатки, Баланс рідини |
| TC-04.02 | Tab switching renders different content | On episode page | 1. Click each tab sequentially | Each shows distinct panel content |
| TC-04.03 | Tab re-renders after switch-away | On episode page | 1. Click tab A, then B, then A again | Tab A content still renders correctly |
| TC-04.04 | ClinicalDayTimeline visible | On episode page | 1. Check above tab bar | Timeline with clinical day chips/badges |
| TC-04.05 | HourSelector in vitals tab | On episode page | 1. Click Вітальні показники tab | 24 hour buttons in grid (8:00–7:00) |
| TC-04.06 | Back button returns to dashboard | Auth as DOCTOR/HOD | 1. Click AppBar back arrow or logo link | URL → /doctor |
| TC-04.07 | Episode page heading | On episode page | 1. Check h6 heading | "Карта інтенсивної терапії" (link /doctor) |

### UC-05: Hourly Records (Vitals)
| ID | Test Case | Precondition | Steps | Expected Result |
|----|-----------|-------------|-------|-----------------|
| TC-05.01 | Enter vitals for an unfilled hour (NURSE) | Auth as NURSE, on episode vitals tab | 1. Click unfilled hour button<br>2. Fill: АТ (сист), АТ (діаст), ЧСС, SpO₂, t° тіла<br>3. Click "Зберегти" | Vitals saved, hour button shows filled state |
| TC-05.02 | Hour grid filled/unfilled state | Auth as NURSE, after saving vitals | 1. Check hour button after save | Filled hour has different visual style |
| TC-05.03 | Vitals form HTML5 validation | Auth as NURSE | 1. Click hour, submit empty form | Required field errors shown |
| TC-05.04 | View existing vitals (DOCTOR, read-only) | Auth as DOCTOR, vitals exist | 1. Click filled hour | Values shown, fields disabled/read-only |
| TC-05.05 | CVP and additional vitals fields | Auth as NURSE | 1. Open vitals form for any hour | Fields: ЦВТ, Сатурація, ЧД, Доза вазопресорів visible |

### UC-06: Medical Orders (Prescriptions)
| ID | Test Case | Precondition | Steps | Expected Result |
|----|-----------|-------------|-------|-----------------|
| TC-06.01 | Create prescription (DOCTOR/HOD) | Auth as DOCTOR/HOD, on Призначення tab | 1. Click "+ Нове призначення"<br>2. Fill: Препарат, Доза, Од., Шлях, Частота, Початок<br>3. Click "Створити" | Prescription appears in list with "Активне" status |
| TC-06.02 | Cancel prescription (DOCTOR/HOD) | Has ACTIVE prescription | 1. Find active prescription<br>2. Click "Скасувати"<br>3. Confirm if dialog shown | Status changes to "Скасовано" |
| TC-06.03 | Execute order (NURSE) | Auth as NURSE, ACTIVE order exists | 1. Click "Призначення" tab<br>2. Find active order<br>3. Click "Виконати"<br>4. Fill execution details (час, параметри)<br>5. Confirm | Execution recorded, order status updated |
| TC-06.04 | Empty prescriptions state | No orders for clinical day | 1. Open Призначення tab | "Немає призначень" or equivalent empty state |
| TC-06.05 | Order form validation | Auth as DOCTOR/HOD | 1. Click "+ Нове призначення"<br>2. Submit empty form | Validation errors on required fields |
| TC-06.06 | Doctor sees execute button vs nurse | Compare DOCTOR and NURSE views | 1. Login as doctor → see no "Виконати" on own orders<br>2. Login as nurse → see "Виконати" on active orders | Correct role-based UI |

### UC-07: Clinical Notes
| ID | Test Case | Precondition | Steps | Expected Result |
|----|-----------|-------------|-------|-----------------|
| TC-07.01 | Add note (DOCTOR) | Auth as DOCTOR, on Нотатки tab | 1. Type in "Нова нотатка" textarea<br>2. Click "Додати нотатку" | Note appears in notes list |
| TC-07.02 | Add note (NURSE) | Auth as NURSE | 1. Same flow as doctor | Note appears (both can write) |
| TC-07.03 | Empty note shows error | Auth as DOCTOR or NURSE | 1. Click "Додати нотатку" without text | Error visible, note not added |
| TC-07.04 | Multiple notes ordering | After adding >=2 notes | 1. Add two notes<br>2. Check order | Newest note at top (reverse chronological) |
| TC-07.05 | Notes persist after tab switch | Note added | 1. Switch away from Нотатки tab<br>2. Switch back | Note still visible |

### UC-08: Scale Results
| ID | Test Case | Precondition | Steps | Expected Result |
|----|-----------|-------------|-------|-----------------|
| TC-08.01 | View scales tab | On episode page | 1. Click "Шкали" tab | Scale panel displayed |
| TC-08.02 | Empty scales state | No scale results exist | 1. Open Шкали tab | "Немає даних шкал" or empty state |
| TC-08.03 | Doctor can create scale (if data seeded) | Auth as DOCTOR/HOD, scales seeded | 1. Open Шкали tab<br>2. Select scale type<br>3. Fill score<br>4. Submit | Result appears in list |
| TC-08.04 | Nurse cannot create scales | Auth as NURSE | 1. Open Шкали tab | Scale form not shown (view-only) |

### UC-09: Fluid Balance
| ID | Test Case | Precondition | Steps | Expected Result |
|----|-----------|-------------|-------|-----------------|
| TC-09.01 | View fluid balance tab | On episode page | 1. Click "Баланс рідини" tab | Balance table displayed |
| TC-09.02 | Empty fluid balance state | No data | 1. Open tab | Empty table or "немає даних" |
| TC-09.03 | "Перерахувати" button | Any role | 1. Open tab | Button visible, click recalculates totals |
| TC-09.04 | Fluid balance table columns | Data present | 1. Check table headers | Columns: hour, intake (total/iv/enteral/etc), output (total/diuresis/etc), balance |

### UC-10: Sign-off Workflow
| ID | Test Case | Precondition | Steps | Expected Result |
|----|-----------|-------------|-------|-----------------|
| TC-10.01 | Nurse signs day (OPEN→NURSE_SIGNED) | Auth as NURSE, day in OPEN | 1. Click "Підписати"<br>2. Confirm in dialog | Day status → NURSE_SIGNED, toast/feedback shown |
| TC-10.02 | Doctor signs day (NURSE_SIGNED→DOCTOR_SIGNED) | Auth as DOCTOR/HOD, day in NURSE_SIGNED | 1. Click "Підписати"<br>2. Confirm | Day status → DOCTOR_SIGNED |
| TC-10.03 | Sign dialog cancel | Eligible to sign | 1. Click "Підписати"<br>2. Click "Скасувати" in dialog | Dialog closes, status unchanged |
| TC-10.04 | Sign button hidden when already signed | Day is DOCTOR_SIGNED or CLOSED | 1. Check page | No "Підписати" button |
| TC-10.05 | Sign dialog shows confirmation message | Auth, eligible | 1. Open sign dialog | Dialog has title/description confirming the action |

### UC-11: Nurse Dashboard
| ID | Test Case | Precondition | Steps | Expected Result |
|----|-----------|-------------|-------|-----------------|
| TC-11.01 | Dashboard loads patient list | Auth as NURSE | 1. Go to /nurse | Table shows patients, title "медсестра" |
| TC-11.02 | Search filters patients | Auth as NURSE | 1. Type in search field | Table rows filtered |
| TC-11.03 | Open episode by "Відкрити" | Auth as NURSE | 1. Click "Відкрити" on a row | URL → /nurse/episode/{id} |
| TC-11.04 | No "Нова карта" button | Auth as NURSE | 1. Check dashboard toolbar | Create button absent |
| TC-11.05 | Nurse AppBar title | Auth as NURSE | 1. Check AppBar | "Карта інтенсивної терапії — медсестра" |
| TC-11.06 | Nurse role label in user menu | Auth as NURSE | 1. Open user menu | Role shown as "Медсестра" |

### UC-12: Admin Page
| ID | Test Case | Precondition | Steps | Expected Result |
|----|-----------|-------------|-------|-----------------|
| TC-12.01 | View users tables | Auth as ADMIN | 1. Go to /admin | Two tables: "Лікарі" and "Медсестри" |
| TC-12.02 | Doctor table contents | Auth as ADMIN | 1. Check Лікарі table | Columns: login, full name, email — shows doctor1, doctor2 |
| TC-12.03 | Nurse table contents | Auth as ADMIN | 1. Check Медсестри table | Columns: login, full name, email — shows nurse1, nurse2 |
| TC-12.04 | Admin user menu | Auth as ADMIN | 1. Click user icon | Menu shows admin name + "Вийти" |
| TC-12.05 | Admin logout | Auth as ADMIN | 1. User menu → Вийти | Redirected to /login |
| TC-12.06 | Admin page heading | Auth as ADMIN | 1. Check page | "Користувачі системи" heading visible |

### UC-13: Cross-cutting / Edge Cases
| ID | Test Case | Precondition | Steps | Expected Result |
|----|-----------|-------------|-------|-----------------|
| TC-13.01 | 404 unknown route | Any | 1. Navigate to /nonexistent-route | Custom 404 page or redirect to valid route |
| TC-13.02 | Direct URL to episode /doctor/episode/{id} | Auth as DOCTOR/HOD | 1. Navigate directly to /doctor/episode/{id} | Episode page loads with data |
| TC-13.03 | Direct URL to /doctor/create-card | Auth as DOCTOR | 1. Navigate directly to URL | Create card page loads |
| TC-13.04 | Refresh on episode page | On episode page | 1. Press F5 / reload | Page reloads, tab content preserved |
| TC-13.05 | Browser back navigation | After dashboard → episode | 1. Click browser back | Returns to dashboard |
| TC-13.06 | Browser forward after back | After back navigation | 1. Click browser forward | Returns to episode page |
| TC-13.07 | AppBar title link navigates | On any doctor page | 1. Click "Карта інтенсивної терапії" title | Navigates to /doctor |

### UC-14: Clinical Day Timeline
| ID | Test Case | Precondition | Steps | Expected Result |
|----|-----------|-------------|-------|-----------------|
| TC-14.01 | Multiple clinical days shown | Episode with >1 clinical day | 1. Open episode, check above tabs | Timeline shows all days with status badges |
| TC-14.02 | Switch clinical day via timeline | Episode with >1 clinical day | 1. Click different day in timeline | Content (tabs) updates to selected day |
| TC-14.03 | Current day highlighted | Episode with multiple days | 1. Check timeline | Currently selected day visually distinct |
| TC-14.04 | Status badges on days | Days have various statuses | 1. Check each badge | Labels: OPEN, NURSE_SIGNED, DOCTOR_SIGNED, CLOSED or Ukrainian equivalents |

### UC-15: Optimistic Locking & Error Resilience
| ID | Test Case | Precondition | Steps | Expected Result |
|----|-----------|-------------|-------|-----------------|
| TC-15.01 | API 409 conflict handled gracefully | Two sessions editing same entity | 1. Session A modifies episode<br>2. Session B (stale version) tries to modify | Error message "версія конфлікту" or similar, no crash |
| TC-15.02 | Network error shows message | API unreachable | 1. Block API (e.g., stop backend)<br>2. Perform any data action | Error boundary/notification, no white screen |
| TC-15.03 | Token expiry redirects to login | Token expired | 1. Wait for token expiry (or manipulate)<br>2. Try any API call | Redirected to /login, cannot access protected routes |

### UC-16: User Menu & Role-based UI Differences
| ID | Test Case | Precondition | Steps | Expected Result |
|----|-----------|-------------|-------|-----------------|
| TC-16.01 | Doctor user menu shows doctor role | Auth as DOCTOR | 1. Click user icon | Menu: full name, "Лікар" label, "Вийти" |
| TC-16.02 | Nurse user menu shows nurse role | Auth as NURSE | 1. Click user icon | Menu: full name, "Медсестра" label, "Вийти" |
| TC-16.03 | HOD user menu | Auth as HOD | 1. Click user icon | Same as doctor (HOD sees doctor layout) |
| TC-16.04 | Admin user menu | Auth as ADMIN | 1. Click user icon | Menu: admin name, "Вийти" (no role label) |
| TC-16.05 | Doctor sees "Пацієнти" nav link | Auth as DOCTOR/HOD | 1. Check AppBar | "Пацієнти" button visible |
| TC-16.06 | Nurse has NO nav links | Auth as NURSE | 1. Check AppBar | No navigation links except user menu |

### UC-17: Concurrent Sessions & Data Consistency
| ID | Test Case | Precondition | Steps | Expected Result |
|----|-----------|-------------|-------|-----------------|
| TC-17.01 | Two browser tabs, same episode | Both auth as same role | 1. Open episode in tab A<br>2. Open same episode in tab B<br>3. Add note in tab A<br>4. Check tab B | Tab B shows new note after refresh or polling |
| TC-17.02 | Doctor and nurse on same episode concurrently | Doctor in one tab, Nurse in another | 1. Doctor creates prescription<br>2. Nurse sees it in orders tab | Nurse can see and execute the new order |

---

## Session Plan (Improved — 6 sessions × 15 min)

### Session 1: All-Role Authentication (15 min)
- TC-01.01 through TC-01.15
- Login every user, test route restrictions, test logout from each role, test invalid credentials
- **New vs v1:** Added empty field validation (TC-01.06), route restriction for ADMIN (TC-01.09/01.10), logout from each role (TC-01.11/01.12/01.13)

### Session 2: Doctor Dashboard + Create Card (15 min)
- TC-02.01 through TC-03.07
- Dashboard data display, search, new card navigation, episode open, MIS search, create episode, cancel, short query, error handling
- **New vs v1:** Patient name link (TC-02.07), AppBar link (TC-02.08), page heading (TC-03.07), episode alias scenario

### Session 3: Episode Navigation + Vitals (15 min)
- TC-04.01 through TC-05.05
- All tabs, tab switching, re-rendering, timeline, hour selector, vitals CRUD, validation, read-only view
- **New vs v1:** Tab re-render (TC-04.03), page heading (TC-04.07), CVP field (TC-05.05), hour filled state (TC-05.02)

### Session 4: Orders + Notes Deep Dive (15 min)
- TC-06.01 through TC-07.05
- Create, cancel, execute, empty states, validation, role-based UI differences, note ordering, tab persistence
- **New vs v1:** Cancel prescription (TC-06.02), order form validation (TC-06.05), role UI comparison (TC-06.06), note ordering (TC-07.04), tab persistence (TC-07.05)

### Session 5: Scales + Fluid Balance + Sign-off + Timeline (15 min)
- TC-08.01 through TC-10.05 + TC-14.01 through TC-14.04
- Empty/with-data states, role differences, sign chain (nurse→doctor→complete), timeline interaction
- **New vs v1:** Nurse view-only (TC-08.04), empty balance (TC-09.02), table columns (TC-09.04), sign hidden when done (TC-10.04), sign dialog message (TC-10.05), timeline interaction (UC-14)

### Session 6: Admin + Edge Cases + Concurrent Sessions (15 min)
- TC-12.01 through TC-17.02
- Admin tables, admin logout, direct URLs, browser navigation, refresh, 404, user menu differences, concurrent sessions
- **New vs v1:** All of UC-16 (user menu), UC-17 (concurrent sessions), forward nav (TC-13.06), AppBar link (TC-13.07)

---

## Traceability Matrix

| \# | Area | Existing E2E | Existing Tests Cover | Exploratory Tests |
|----|------|-------------|---------------------|-------------------|
| 1 | Login | 5 | doctor+nurse+invalid+title+redirect | all 6 users, empty fields, route restrictions, logout all roles |
| 2 | Doctor Dashboard | 5 | display+new card+search+open+title | empty search, patient name link, AppBar link |
| 3 | Create Card | 3 | create+cancel+short query | error on ACTIVE episode, full patient data, heading |
| 4 | Episode Tabs | 3 | all tabs+switching+back | timeline, hour selector, tab re-render |
| 5 | Vitals | 2 | enter+HTML5 validation | filled state, doctor read-only, CVP field |
| 6 | Orders | 2 | create+status | cancel, execute, validation, empty state, role UI |
| 7 | Notes | 2 | create+empty validation | note ordering, tab persistence |
| 8 | Scales | 1 | tab visibility | empty state, role differences, CREATION (gap) |
| 9 | Fluid Balance | 1 | tab visibility | empty state, recalculate, table columns |
| 10 | Sign-off | 1 | dialog visibility | full nurse→doctor chain, hidden when done |
| 11 | Nurse Dashboard | 4 | display+open+search+title | nav links absent, role label |
| 12 | Admin | 4 | tables+details+title | user menu, logout |
| 13 | Edge Cases | 0 | none | 404, direct URLs, refresh, back/forward, AppBar link |
| 14 | Timeline | 0 | none | day switching, highlight, status badges |
| 15 | Locking/Errors | 0 | none | 409, network error, token expiry |
| 16 | User Menu | 0 | none | role labels, nav links per role |
| 17 | Concurrent | 0 | none | multi-tab, doctor+nurse collaboration |

---

## Bug Reporting Template

```
### [BUG] Short Description
**Severity:** Critical / Major / Minor / Trivial
**Environment:** http://localhost:5173
**Role:** DOCTOR / NURSE / HOD / ADMIN
**Preconditions:**
**Steps to Reproduce:**
1.
2.
3.
**Expected:**
**Actual:**
**Screenshot:** [path]
```

---

## Key Locators Reference

| Element | Locator Strategy | Notes |
|---------|-----------------|-------|
| Login button | `getByRole('button', { name: 'Увійти' })` | |
| New card button | `getByRole('button', { name: 'Нова карта' })` | **NOT** a link (role=button) |
| Cancel (create card) | `getByRole('button', { name: 'Скасувати' })` | |
| Open episode | `getByRole('button', { name: 'Відкрити' }).first()` | Always .first() for determinism |
| MIS search | `getByLabel('ПІБ, телефон або № медкарти')` | Autocomplete input |
| User menu | `getByRole('button', { name: /Меню користувача/i })` | IconButton with AccountCircle |
| Logout | `getByText('Вийти')` | Inside opened menu |
| Tabs | `getByRole('tab', { name: '...' })` | MUI Tab role |
| Add note | `getByRole('button', { name: 'Додати нотатку' })` | |
| New prescription | `getByRole('button', { name: '+ Нове призначення' })` | |
| Create prescription | `getByRole('button', { name: 'Створити' })` | Inside dialog |
| Sign day | `getByRole('button', { name: 'Підписати' })` | |
| Patient search placeholder | `getByPlaceholder(/Пошук/i)` | On dashboards |
