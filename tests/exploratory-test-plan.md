# ICU Patient Chart — Exploratory Test Plan (v3)

## Session Charter

**Objective:** Discover functional defects, usability issues, and coverage gaps in the ICU Patient Chart web application through session-based exploratory testing across all roles and core workflows.

**Methods:** Session-based exploratory testing (testomat.io methodology — classify errors → create charter → time box → assess → debrief). Heuristics: CRUD-SAP (Create, Read, Update, Delete, Search, Authorize, Prescribe), FEW HICCUPS (Functionality, Errors, Workflow, Hierarchy, Input, Consistency, Configuration, Usability, Performance, Security).

**Time Box:** 15 minutes per session (6+ sessions)

**Scope:** Frontend UI (http://localhost:5173) — login, dashboards, create card, episode page (5 tabs), sign-off, navigation, role-based access control, error states, user menu, session handling, PDF, audit, episode lifecycle.

**Out of Scope:** Backend API (covered by unit/integration tests), PDF generation, performance/load testing, mobile responsiveness.

**Environment:** Playwright chromium, non-headless, fullscreen, slowMo=400ms

**Severity Classification:**
- **P1 — Critical:** Workflow block, data loss, wrong clinical data display
- **P2 — Major:** Feature broken, role-based access violation, incorrect state transition
- **P3 — Minor:** UI glitch, missing validation, edge case not handled
- **P4 — Trivial:** Cosmetic, locator/timing issue in test only

---

## Use Cases & Test Cases (50 UCs)

### UC-01: Authentication
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-01.01 | Login as DOCTOR | No active session | 1. Go to /login<br>2. Enter doctor1 / doctor123<br>3. Click Увійти | Redirected to /doctor, user name visible in AppBar | P1 |
| TC-01.02 | Login as NURSE | No active session | 1. Go to /login<br>2. Enter nurse1 / nurse123<br>3. Click Увійти | Redirected to /nurse, "медсестра" in AppBar title | P1 |
| TC-01.03 | Login as HOD | No active session | 1. Go to /login<br>2. Enter head1 / head123<br>3. Click Увійти | Redirected to /doctor (same layout as doctor) | P1 |
| TC-01.04 | Login as ADMIN | No active session | 1. Go to /login<br>2. Enter admin / admin123<br>3. Click Увійти | Redirected to /admin, "Користувачі системи" visible | P1 |
| TC-01.05 | Invalid credentials | No active session | 1. Enter wrong login/password<br>2. Click Увійти | Error Alert "Невірний логін або пароль", stays on /login | P2 |
| TC-01.06 | Empty fields client validation | No active session | 1. Click Увійти with empty fields | HTML5 validation blocks submission | P3 |

### UC-02: Route Restrictions
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-02.01 | NURSE blocked from /doctor | Auth as NURSE | 1. Navigate to /doctor | Redirected to /nurse | P1 |
| TC-02.02 | DOCTOR blocked from /nurse | Auth as DOCTOR | 1. Navigate to /nurse | Redirected to /doctor | P1 |
| TC-02.03 | ADMIN blocked from /doctor | Auth as ADMIN | 1. Navigate to /doctor | Redirected to /admin | P1 |
| TC-02.04 | ADMIN blocked from /nurse | Auth as ADMIN | 1. Navigate to /nurse | Redirected to /admin | P1 |
| TC-02.05 | Unauthenticated blocked from /doctor | No auth | 1. Navigate to /doctor | Redirected to /login | P1 |

### UC-03: Logout
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-03.01 | Logout from DOCTOR | Auth as DOCTOR | 1. User menu → "Вийти" | Redirected to /login, /doctor blocked | P1 |
| TC-03.02 | Logout from NURSE | Auth as NURSE | 1. User menu → "Вийти" | Redirected to /login, /nurse blocked | P1 |
| TC-03.03 | Logout from ADMIN | Auth as ADMIN | 1. User menu → "Вийти" | Redirected to /login, /admin blocked | P1 |
| TC-03.04 | Logout clears localStorage token | Auth as any | 1. Logout<br>2. Check localStorage | Token removed, auth state cleared | P2 |

### UC-04: Role-based Redirect (Root `/`)
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-04.01 | DOCTOR redirected to /doctor | Auth as DOCTOR | 1. Navigate to / | Redirected to /doctor | P1 |
| TC-04.02 | NURSE redirected to /nurse | Auth as NURSE | 1. Navigate to / | Redirected to /nurse | P1 |
| TC-04.03 | ADMIN redirected to /admin | Auth as ADMIN | 1. Navigate to / | Redirected to /admin | P1 |

### UC-05: Doctor Dashboard — Data Display
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-05.01 | Active episodes table loads | Auth as DOCTOR, episodes exist | 1. Go to /doctor | Table shows episodes with Пацієнт, Дата госпіталізації, Статус, Дії columns | P1 |
| TC-05.02 | Episodes have ACTIVE status chips | Auth as DOCTOR | 1. Check status column | Chip shows "Активний" (green), not DRAFT/COMPLETED | P2 |
| TC-05.03 | Page heading visible | Auth as DOCTOR | 1. Check document title | Contains "Карта інтенсивної терапії" | P3 |
| TC-05.04 | Dashboard title shows | Auth as DOCTOR | 1. Check h4/h5 | "Активні пацієнти ВАІТ" | P3 |

### UC-06: Doctor Dashboard — Search & Filter
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-06.01 | Search filters by patient name | Auth as DOCTOR | 1. Type "Петренко" in search | Table filtered to matching rows only | P1 |
| TC-06.02 | Empty search for nonexistent | Auth as DOCTOR | 1. Search "ZZZ_NONEXISTENT" | Alert "Немає пацієнтів за запитом" or empty table | P2 |
| TC-06.03 | Clear search restores full list | Auth as DOCTOR | 1. Search → filter<br>2. Clear search field | All episodes visible again | P2 |
| TC-06.04 | Search placeholder text | Auth as DOCTOR | 1. Check search field | Placeholder "Пошук пацієнта за ПІБ..." | P4 |

### UC-07: Doctor Dashboard — Navigation Actions
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-07.01 | "Нова карта" button navigates | Auth as DOCTOR | 1. Click "Нова карта" | URL → /doctor/create-card | P1 |
| TC-07.02 | "Відкрити" link opens episode | Auth as DOCTOR | 1. Click "Відкрити" on a row | URL → /doctor/episode/{id} | P1 |
| TC-07.03 | Patient name click opens episode | Auth as DOCTOR | 1. Click patient name link in table | URL → /doctor/episode/{id} | P2 |
| TC-07.04 | "Пацієнти" nav returns to dashboard | On /doctor/create-card | 1. Click "Пацієнти" in AppBar | URL → /doctor | P2 |

### UC-08: Create Card — Patient Search
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-08.01 | Search patient from MIS | Auth as DOCTOR | 1. Go to /doctor/create-card<br>2. Type >=2 chars into autocomplete | Dropdown shows matching patients | P1 |
| TC-08.02 | Short query hint | Auth as DOCTOR | 1. Type 1 character | "Введіть мінімум 2 символи" or equivalent | P3 |
| TC-08.03 | No results message | Auth as DOCTOR | 1. Search "ZZZ" (>=2 chars, no match) | "Немає результатів" in dropdown | P3 |
| TC-08.04 | Debounced search (300ms) | Auth as DOCTOR | 1. Type rapidly, observe network calls | Only one API call after typing stops | P3 |

### UC-09: Create Card — Patient Data Display
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-09.01 | All patient fields render after selection | Auth as DOCTOR | 1. Search + select patient from dropdown | Fields: ПІП, Дата народження, Стать, Зріст, Маса, Група крові, Rezus, № медкарти | P1 |
| TC-09.02 | Birth date format | Auth as DOCTOR | 1. Select patient, check date | Format: DD.MM.YYYY or YYYY-MM-DD | P3 |
| TC-09.03 | Sex code display | Auth as DOCTOR | 1. Select patient, check Стать | "Чол" or "Жін" displayed | P3 |
| TC-09.04 | Height and weight with units | Auth as DOCTOR | 1. Select patient | Height (см), Weight (кг) units shown | P3 |

### UC-10: Create Card — Create & Cancel
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-10.01 | Create episode successfully | Patient has no ACTIVE episode | 1. Search+select patient<br>2. Click "Створити карту" | URL → /doctor/episode/{id}, episode created | P1 |
| TC-10.02 | Cancel returns to dashboard | On create-card page | 1. Click "Скасувати" | URL → /doctor | P2 |
| TC-10.03 | Heading visible | Auth as DOCTOR | 1. Go to /doctor/create-card | h5 "Нова карта інтенсивної терапії" | P3 |
| TC-10.04 | Error on duplicate ACTIVE episode | Patient already has ACTIVE episode | 1. Try creating for patient with existing episode | Error Alert shown, stays on page | P2 |

### UC-11: Nurse Dashboard
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-11.01 | Patient list loads | Auth as NURSE | 1. Go to /nurse | Table shows patients with columns | P1 |
| TC-11.02 | Search filters patients | Auth as NURSE | 1. Type in search field | Rows filtered | P2 |
| TC-11.03 | "Відкрити" opens episode | Auth as NURSE | 1. Click "Відкрити" on a row | URL → /nurse/episode/{id} | P1 |
| TC-11.04 | No "Нова карта" button | Auth as NURSE | 1. Check toolbar | Create button absent | P2 |
| TC-11.05 | Title shows "медсестра" | Auth as NURSE | 1. Check page | Title "Активні пацієнти" | P3 |

### UC-12: Episode Page — Layout & Navigation
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-12.01 | All 5 tabs visible | On episode page | 1. Check tab bar | Вітальні показники, Призначення, Шкали, Нотатки, Баланс рідини | P1 |
| TC-12.02 | Tab switching renders content | On episode page | 1. Click each tab | Each shows distinct panel | P1 |
| TC-12.03 | Tab content persists after switch | On episode page | 1. Tab A → B → A | Tab A content still renders | P2 |
| TC-12.04 | Back button returns to dashboard | Auth as DOCTOR | 1. Click "Назад" | URL → /doctor | P1 |
| TC-12.05 | Nurse back returns to /nurse | Auth as NURSE | 1. Click "Назад" | URL → /nurse | P1 |
| TC-12.06 | Patient name displayed as heading | On episode page | 1. Check h5/h6 | Patient full name visible | P2 |

### UC-13: Clinical Day Timeline
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-13.01 | Clinical days shown in timeline | Episode with >=1 clinical day | 1. Check above tabs | Horizontal scrollable day tiles "Доба N" | P1 |
| TC-13.02 | Switch day via timeline | Episode with >=2 clinical days | 1. Click different day tile | Tab content updates to selected day | P1 |
| TC-13.03 | Selected day highlighted | Episode with multiple days | 1. Observe current day | Visually distinct (background color) | P2 |
| TC-13.04 | Day status color coding | Days with various statuses | 1. Check each day chip | OPEN/NURSE_SIGNED pink, DOCTOR_SIGNED/CLOSED green, REOPENED yellow | P2 |
| TC-13.05 | Timeline horizontal scroll | Many clinical days | 1. Check scroll behavior | Horizontal scrollbar or drag available | P3 |

### UC-14: Hourly Records (Vitals) — Entry
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-14.01 | Hour selector renders 24 hours | On vitals tab | 1. Click Вітальні показники | 24 hour pills (8:00–7:00) | P1 |
| TC-14.02 | Select unfilled hour | Auth as NURSE | 1. Click unfilled hour pill | Vitals form appears for that hour | P1 |
| TC-14.03 | Enter all vitals fields | Auth as NURSE | 1. Fill АТ сист, АТ діас, ЧСС, SpO₂, t° тіла, ЦВТ, ЧД, etCO₂, FiO₂, Діурез, Дренаж, Біль, Свідомість, Нотатки<br>2. Click "Зберегти показники" | Vitals saved, hour pill shows filled state | P1 |
| TC-14.04 | Filled hour visual indicator | After saving | 1. Check hour pill after save | Checkmark (✓) or green color | P2 |
| TC-14.05 | Past vs future hour styling | Current time known | 1. Compare filled/missing hours | Past-filled=green, past-missing=red, current=teal | P3 |

### UC-15: Hourly Records — Update
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-15.01 | Nurse updates existing vitals | Prev vitals exist, auth as NURSE | 1. Select filled hour<br>2. Modify ЧСС value<br>3. Save | Updated value persisted | P2 |
| TC-15.02 | Version conflict on stale update | Two sessions editing same hour | 1. Session A saves vitals<br>2. Session B tries to save old version | Conflict error, no data loss | P2 |

### UC-16: Hourly Records — Read-only View
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-16.01 | Doctor sees vitals as read-only | Auth as DOCTOR, vitals exist | 1. Click filled hour pill | Values shown, save button absent or disabled | P1 |
| TC-16.02 | HourlyRecordTable shows summary | Vitals exist | 1. Check table below form | Table: Година, АТ, ЧСС, SpO₂, Темп, ЦВТ, ЧД | P2 |
| TC-16.03 | Table row color coding | Past filled+missing hours | 1. Check row colors | Green for filled, pink for missing | P3 |

### UC-17: Hourly Records — Validation
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-17.01 | HTML5 validation on empty form | Auth as NURSE | 1. Open vitals form, submit empty | Browser validation or UI errors | P2 |
| TC-17.02 | Range validation on numeric fields | Auth as NURSE | 1. Enter ЧСС=500 (above 300) | Validation error or field rejects | P3 |
| TC-17.03 | Negative values rejected | Auth as NURSE | 1. Enter АТ=-10 | Validation error | P3 |

### UC-18: Medical Orders — Creation
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-18.01 | Create prescription (DOCTOR) | Auth as DOCTOR/HOD | 1. Click "+ Нове призначення"<br>2. Fill Категорія, Препарат, Доза, Од., Шлях, Частота, Початок<br>3. Click "Створити" | Order appears in list with "Активне" status | P1 |
| TC-18.02 | Order with end time | Auth as DOCTOR | 1. Create with Кінець datetime | Order shows duration, scheduled end | P2 |
| TC-18.03 | Toggle form visibility | Auth as DOCTOR | 1. Click "+ Нове призначення" to open<br>2. Click again to close | Form shows/hides | P3 |
| TC-18.04 | Empty orders state | No orders exist | 1. Open tab | "Немає призначень" or empty state | P2 |

### UC-19: Medical Orders — Status Lifecycle
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-19.01 | Cancel active order (DOCTOR) | Has ACTIVE order | 1. Click "Скасувати" on order | Status changes to "Скасовано" | P1 |
| TC-19.02 | Cancel then verify status chip | After cancel | 1. Check order row | Chip shows "Скасовано" with appropriate color | P2 |
| TC-19.03 | Status chip colors | Orders with various statuses | 1. Check each status | Чернетка/DRAFT, Активне/ACTIVE, Виконано/COMPLETED, Скасовано/CANCELLED — distinct colors | P2 |

### UC-20: Medical Orders — Execution (Nurse)
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-20.01 | Nurse sees execute button on active orders | Auth as NURSE, ACTIVE order exists | 1. Open Призначення tab | "Виконати" button visible on each active order | P1 |
| TC-20.02 | Execute order with actual dose | Auth as NURSE | 1. Click "Виконати"<br>2. Enter actualDose, comment<br>3. Confirm | Execution recorded, order status → "Виконано" | P1 |
| TC-20.03 | Doctor does NOT see execute button | Auth as DOCTOR | 1. Check orders tab | No "Виконати" button on own orders | P2 |

### UC-21: Medical Orders — Validation
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-21.01 | Empty order form validation | Auth as DOCTOR | 1. Open form, click "Створити" empty | Required field errors | P2 |
| TC-21.02 | Order form fields | Auth as DOCTOR | 1. Check all fields | Категорія, Препарат, Доза, Од., Шлях, Частота, Початок, Кінець | P3 |

### UC-22: Clinical Notes — Creation
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-22.01 | Doctor adds note | Auth as DOCTOR | 1. Type in textarea<br>2. Click "Додати нотатку" | Note appears in list | P1 |
| TC-22.02 | Nurse adds note | Auth as NURSE | 1. Same flow | Note appears | P1 |
| TC-22.03 | Empty note rejected | Auth as any | 1. Click "Додати нотатку" without text | Error shown, no note added | P2 |
| TC-22.04 | Note with all fields visible | After adding note | 1. Check note card | Shows author (role), timestamp, text | P2 |

### UC-23: Clinical Notes — Ordering & Persistence
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-23.01 | Reverse chronological order | >=2 notes exist | 1. Check note list | Newest note at top | P2 |
| TC-23.02 | Notes persist after tab switch | Note added | 1. Switch tabs<br>2. Return to Нотатки | Note still visible | P2 |
| TC-23.03 | Multiple notes scrollable | Many notes | 1. Add enough notes to overflow | Scrollable list | P3 |

### UC-24: Scale Results — Display
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-24.01 | Scales tab renders | On episode page | 1. Click "Шкали" tab | Scale panel with available scales list | P1 |
| TC-24.02 | Empty scales state | No results exist | 1. Open tab | "Не заповнено" or empty state | P2 |
| TC-24.03 | Scale result cards | Results exist | 1. Check card | Scale name, result value, calculation timestamp | P2 |

### UC-25: Scale Results — Creation
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-25.01 | Doctor can create scale | Auth as DOCTOR/HOD | 1. Select scale from dropdown<br>2. Enter result value<br>3. Click "Додати" | Result card appears | P2 |
| TC-25.02 | Nurse cannot create scale | Auth as NURSE | 1. Open tab | Scale creation controls absent or disabled | P2 |
| TC-25.03 | Scale dropdown lists available scales | Auth as DOCTOR | 1. Open scale creation | Dropdown with scale names | P2 |

### UC-26: Fluid Balance — Display
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-26.01 | Fluid balance tab renders | On episode page | 1. Click "Баланс рідини" | Summary panel with intake/output values | P1 |
| TC-26.02 | Надійшло (intake) display | Data exists | 1. Check intake section | Total intake in ml | P2 |
| TC-26.03 | Виділено (output) display | Data exists | 1. Check output section | Total output in ml | P2 |
| TC-26.04 | Добовий баланс color coding | Data exists | 1. Check daily balance | Red if negative, green if positive | P2 |
| TC-26.05 | Кумулятивний баланс | Data exists | 1. Check cumulative | Running total balance | P2 |

### UC-27: Fluid Balance — Actions
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-27.01 | "Перерахувати" button visible | On tab | 1. Open tab | Button visible in panel | P2 |
| TC-27.02 | Recalculate updates values | Data present | 1. Click "Перерахувати" | Values refresh, loading state shown | P2 |
| TC-27.03 | Loading spinner while recalculating | Slow response | 1. Click "Перерахувати" | Progress indicator visible | P3 |

### UC-28: Sign-off — Nurse
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-28.01 | Nurse signs OPEN day | Auth as NURSE, day in OPEN | 1. Click "Підписати добу"<br>2. Confirm dialog | Day status → NURSE_SIGNED, success feedback | P1 |
| TC-28.02 | Sign dialog cancel | Eligible to sign | 1. Click "Підписати"<br>2. Click "Скасувати" | Dialog closes, status unchanged | P2 |
| TC-28.03 | Sign day disabled after nurse signed | Day is NURSE_SIGNED | 1. Check sign button | Button absent or disabled for nurse | P2 |

### UC-29: Sign-off — Doctor
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-29.01 | Doctor signs NURSE_SIGNED day | Auth as DOCTOR, day NURSE_SIGNED | 1. Click "Підписати"<br>2. Confirm | Day status → DOCTOR_SIGNED | P1 |
| TC-29.02 | Sign hidden when DOCTOR_SIGNED | Day already signed | 1. Check page | No sign button | P2 |
| TC-29.03 | Sign dialog role-specific text | Auth as DOCTOR | 1. Open sign dialog | Text mentions PDF generation & MIS | P3 |

### UC-30: Sign-off — Dialog
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-30.01 | Dialog title shows day number | Eligible to sign | 1. Click "Підписати" | Title "Підписання доби №N" | P2 |
| TC-30.02 | Dialog confirm disables button | After confirm click | 1. Click "Підписати" in dialog | Button shows loading, double-submit prevented | P2 |

### UC-31: Admin Page
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-31.01 | Two user tables render | Auth as ADMIN | 1. Go to /admin | "Лікарі" and "Медсестри" Paper sections | P1 |
| TC-31.02 | Doctors table content | Auth as ADMIN | 1. Check Лікарі table | Columns: ПІБ, Логін, Роль, Email — doctor1, doctor2 shown | P1 |
| TC-31.03 | Nurses table content | Auth as ADMIN | 1. Check Медсестри table | Columns: ПІБ, Логін, Роль, Email — nurse1, nurse2 shown | P1 |
| TC-31.04 | HOD appears in doctors table | Auth as ADMIN | 1. Check Лікарі table | head1 shown with role "Завідувач відділення" | P2 |
| TC-31.05 | Page heading | Auth as ADMIN | 1. Check page | "Користувачі системи" visible | P3 |

### UC-32: Admin User Menu
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-32.01 | User menu shows admin name | Auth as ADMIN | 1. Click AccountCircle icon | Disabled item with admin fullName | P2 |
| TC-32.02 | Admin logout works | Auth as ADMIN | 1. Menu → "Вийти" | Redirected to /login | P1 |

### UC-33: Doctor Layout — AppBar
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-33.01 | AppBar title links to /doctor | On any doctor page | 1. Click "Карта інтенсивної терапії" | URL → /doctor | P2 |
| TC-33.02 | "Пацієнти" nav link visible | Auth as DOCTOR/HOD | 1. Check AppBar | "Пацієнти" link visible | P2 |
| TC-33.03 | "Пацієнти" nav navigates | Auth as DOCTOR/HOD | 1. Click "Пацієнти" | URL → /doctor | P2 |
| TC-33.04 | Nurse has no nav links | Auth as NURSE | 1. Check AppBar | Only title + user menu, no nav | P2 |

### UC-34: User Menu — Role Labels
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-34.01 | Doctor menu shows "Лікар" | Auth as DOCTOR | 1. Open user menu | "Лікар" label visible | P2 |
| TC-34.02 | Nurse menu shows "Медсестра" | Auth as NURSE | 1. Open user menu | "Медсестра" label visible | P2 |
| TC-34.03 | HOD menu | Auth as HOD | 1. Open user menu | Same as doctor (HOD sees doctor layout) | P2 |
| TC-34.04 | User full name in menu | Any role | 1. Open user menu | Full name shown (disabled menu item) | P2 |
| TC-34.05 | Menu animation (MUI Menu) | Any role | 1. Click user icon | Menu animates in (wait for transition) | P4 |

### UC-35: Episode Status Color Coding
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-35.01 | Status chip on episode page | On episode | 1. Check status chip | Color-coded status: Відкрита (green), Підписана медсестрою (pink), etc. | P2 |
| TC-35.02 | Episode ID chip visible | On episode | 1. Check near status | Chip with first 8 chars of episode ID | P3 |
| TC-35.03 | "Доба №N" subtitle | On episode | 1. Check below patient name | "Доба №N" with current day number | P3 |

### UC-36: 404 / Unknown Route
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-36.01 | Unknown route shows no crash | Any auth | 1. Navigate to /nonexistent | No white screen, no error boundary crash | P2 |
| TC-36.02 | Invalid episode ID | Any auth | 1. Navigate to /doctor/episode/invalid-id | Error state or empty data (no crash) | P2 |

### UC-37: Browser Navigation
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-37.01 | Browser back from episode | On episode page | 1. Click browser back | Returns to dashboard | P2 |
| TC-37.02 | Browser forward after back | After back | 1. Click browser forward | Returns to episode page | P2 |
| TC-37.03 | Page refresh on episode | On episode page | 1. Press F5/reload | Page reloads, data re-fetched | P2 |

### UC-38: Direct URL Access
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-38.01 | Direct to /doctor/episode/{id} | Auth as DOCTOR | 1. Navigate directly to episode URL | Episode page loads with data | P1 |
| TC-38.02 | Direct to /doctor/create-card | Auth as DOCTOR | 1. Navigate directly | Create card page loads | P2 |
| TC-38.03 | Direct to /nurse/episode/{id} | Auth as NURSE | 1. Navigate directly | Episode page loads in nurse layout | P1 |

### UC-39: Loading States
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-39.01 | Dashboard loading spinner | Slow API | 1. Navigate to /doctor | CircularProgress while loading | P3 |
| TC-39.02 | Episode page loading | Slow API | 1. Navigate to episode | Loading state while data fetches | P3 |
| TC-39.03 | Create card button loading | During creation | 1. Click "Створити карту" | Button shows loading state, disabled | P3 |

### UC-40: Empty States
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-40.01 | Dashboard empty state | No active episodes | 1. Check page when no data | Alert "Немає активних пацієнтів" | P2 |
| TC-40.02 | Notes empty state | No notes | 1. Open Нотатки tab | Empty textarea + "Додати нотатку" button only | P2 |
| TC-40.03 | Orders empty state | No orders | 1. Open Призначення tab | Empty list message | P2 |
| TC-40.04 | Scales empty state | No scales | 1. Open Шкали tab | "Не заповнено" or empty state | P2 |
| TC-40.05 | Fluid balance empty state | No data | 1. Open Баланс рідини | 0 values shown | P2 |

### UC-41: Error Handling
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-41.01 | API 500 error shows error state | API returns 500 | 1. Trigger server error | Error Alert or fallback UI, no crash | P2 |
| TC-41.02 | Network failure notification | API unreachable | 1. Stop backend<br>2. Perform any action | Error notification or boundary | P2 |
| TC-41.03 | Token expiry redirects to login | Token expired | 1. Wait for expiry (or manipulate localStorage)<br>2. Make API call | Redirected to /login | P1 |

### UC-42: Version Conflict (Optimistic Locking)
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-42.01 | Episode 409 conflict handled | Two sessions | 1. Session A: save clinical day<br>2. Session B (old version): try save | Conflict error message, no crash | P2 |
| TC-42.02 | Vitals 409 conflict handled | Two sessions editing same hour | 1. Concurrent save attempts | Graceful error handling | P2 |

### UC-43: Concurrent Sessions
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-43.01 | Multi-tab same episode | Same role, two tabs | 1. Tab A: add note<br>2. Tab B: refresh or switch tabs | Tab B sees new note | P2 |
| TC-43.02 | Doctor+nurse collaboration | Doctor in one tab, nurse another | 1. Doctor creates prescription<br>2. Nurse checks orders | Nurse can see and execute new order | P2 |
| TC-43.03 | Same-user multi-session | Login as same user twice | 1. Both sessions active | Both work without interference | P3 |

### UC-44: Episode Lifecycle — Close
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-44.01 | Close episode (discharge) | Auth as DOCTOR, ACTIVE episode | 1. Trigger close via API | Episode status → COMPLETED | P2 |
| TC-44.02 | Closed episode not in active list | Episode COMPLETED | 1. Go to dashboard | Completed episode not shown | P2 |

### UC-45: PDF Generation
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-45.01 | PDF generation triggered on sign | Day signed by doctor | 1. Sign day as doctor | PDF metadata created (generatedAt, fileName) | P2 |
| TC-45.02 | PDF metadata accessible | PDF exists | 1. Check PDF endpoint | File name, version, checksum available | P3 |

### UC-46: Clinical Day — Reopen
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-46.01 | Reopen signed clinical day | Day is NURSE_SIGNED or DOCTOR_SIGNED | 1. Trigger reopen | Status → REOPENED, version incremented | P2 |
| TC-46.02 | Reopened day shows yellow chip | Day REOPENED | 1. Check status after reopen | Yellow status chip | P3 |

### UC-47: Patient Data — International / Edge Cases
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-47.01 | Patient with null height/weight | Patient has missing data | 1. Search + select | Height/Weight show empty or "—" | P3 |
| TC-47.02 | Birth date formatting | Any patient | 1. Check birth date | Consistent format across all views | P3 |
| TC-47.03 | Foreign patient name | Mock MIS provides test patients | 1. Search/select various patients | Non-Cyrillic characters render correctly | P3 |

### UC-48: Vitals — Special Fields
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-48.01 | Pain score field (0-10) | Auth as NURSE, vitals form open | 1. Check Біль 0-10 field | Numeric input with range validation | P2 |
| TC-48.02 | Consciousness text field | Auth as NURSE | 1. Check Свідомість field | Free-text input for consciousness level | P2 |
| TC-48.03 | Vitals notes field | Auth as NURSE | 1. Check Нотатки field | Multiline text for vitals notes | P3 |

### UC-49: Order Execution Details
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-49.01 | Execution comment saved | Auth as NURSE | 1. Execute order with comment | Comment appears in execution record | P2 |
| TC-49.02 | Execution timestamp captured | Auth as NURSE | 1. Execute order | executedAt timestamp recorded | P2 |
| TC-49.03 | Execution status progression | After execution | 1. Check order status | Order status → COMPLETED | P2 |

### UC-50: Note Author Information
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-50.01 | Note shows author role | Note exists | 1. Check note card | Role badge (Лікар/Медсестра) visible | P2 |
| TC-50.02 | Note shows creation time | Note exists | 1. Check note card | Timestamp in readable format | P2 |
| TC-50.03 | Doctor vs nurse note distinction | Both have notes | 1. Compare note cards | Different visual indicators per role | P3 |

### UC-51: Scale — Available Scales List
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-51.01 | Available scales fetched on tab open | Auth as any | 1. Open Шкали tab | Scale names loaded from API | P2 |
| TC-51.02 | Scale description available | Scales exist | 1. Check scale info | Description or tooltip on scale name | P3 |

### UC-52: Fluid Balance — Per-Hour Breakdown
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-52.01 | Table columns present | Data exists | 1. Check table | Columns: Година, Надійшло, Виділено, Баланс | P2 |
| TC-52.02 | Hourly balance calculation | Individual hour data | 1. Check an hour row | Per-hour balance = intake - output | P2 |
| TC-52.03 | Cumulative balance increases | Multiple hours | 1. Check cumulative column | Running total across hours | P2 |

### UC-53: Auth Token Persistence
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-53.01 | Token stored in localStorage | After login | 1. Login as any user | localStorage has token | P2 |
| TC-53.02 | Page reload preserves auth | After login | 1. Reload page | User stays authenticated, correct layout | P1 |
| TC-53.03 | Token removed on logout | After logout | 1. Logout<br>2. Check localStorage | No token, auth state cleared | P2 |

### UC-54: Vitals Form — Mean Arterial Pressure
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-54.01 | MAP field auto-calculated | Systolic+Diastolic entered | 1. Enter АТ сист and АТ діас | MAP may auto-calculate or be editable | P3 |
| TC-54.02 | MAP in hourly record table | Vitals with MAP | 1. Check table | MAP column or derived from BP values | P3 |

### UC-55: Access Control — Episode Page per Role
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-55.01 | Doctor sees all 5 tabs with full controls | Auth as DOCTOR | 1. Open episode | Can create orders, notes, scales; vitals read-only | P1 |
| TC-55.02 | Nurse sees clinical data entry controls | Auth as NURSE | 1. Open episode | Can edit vitals, execute orders, add notes; cannot create orders or scales | P1 |
| TC-55.03 | Doctor cannot execute orders | Auth as DOCTOR | 1. Check orders tab | Execute button absent on own orders | P2 |
| TC-55.04 | Nurse cannot create card | Auth as NURSE | 1. Check dashboard | "Нова карта" absent | P2 |

---

## Session Plan (6 sessions × 15 min)

### Session 1: Authentication & Access Control (15 min)
- TC-01.01 through TC-04.03
- Login all 6 users, route restrictions, logout each role, root redirect
- **Heuristics:** Authorize (all roles × all routes), Consistency (redirect behavior)

### Session 2: Doctor Dashboard, Create Card, Nurse Dashboard (15 min)
- TC-05.01 through TC-11.05
- Dashboard data+search+navigation, create card patient search+data+create+cancel, nurse dashboard
- **Heuristics:** CRUD (search, create, cancel), Input (autocomplete, validation)

### Session 3: Episode Navigation, Timeline, Vitals (15 min)
- TC-12.01 through TC-17.02
- Tabs, back navigation, clinical day timeline, hour selector, vitals entry, update, read-only, validation
- **Heuristics:** FEW HICCUPS (Workflow, Input), Config (per-role views)

### Session 4: Orders, Notes, Scales (15 min)
- TC-18.01 through TC-25.03
- Create/cancel orders, nurse execution, notes CRUD + ordering, scales display + creation
- **Heuristics:** CRUD-SAP, Hierarchy (role gating), Config (doctor vs nurse views)

### Session 5: Fluid Balance, Sign-off, Admin (15 min)
- TC-26.01 through TC-32.02
- Fluid balance display+recalculate, nurse sign→doctor sign chain, admin tables+logout
- **Heuristics:** Workflow (sign chain), Update (recalculate), Authorize (admin)

### Session 6: Layout, User Menu, Edge Cases, Concurrent (15 min)
- TC-33.01 through TC-55.04
- AppBar nav, user menu per role, 404, browser nav, direct URLs, loading/empty states, error handling, concurrent sessions, version conflict
- **Heuristics:** Errors (API failures), Performance (loading states), Security (token expiry, concurrent access)

---

## Heuristics Reference

| Heuristic | Application |
|-----------|-------------|
| **CRUD-SAP** | Create, Read, Update, Delete, Search, Authorize, Prescribe — test every operation on every entity |
| **FEW HICCUPS** | Functionality, Errors, Workflow, Hierarchy, Input, Consistency, Configuration, Usability, Performance, Security — checklist for session coverage |
| **Mnemonics for Input** | Boundary (min/max), Range (0-10 pain, 20-300 HR), Format (date, time), Empty, Special chars (Cyrillic, UTF-8) |
| **Role Matrix** | DOCTOR, NURSE, HOD, ADMIN — every feature × every role, including what should be hidden |
| **State Transitions** | Episode: DRAFT→ACTIVE→COMPLETED→ARCHIVED; ClinicalDay: OPEN→NURSE_SIGNED→DOCTOR_SIGNED→CLOSED/REOPENED; Order: ACTIVE→COMPLETED/CANCELLED |

---

## Traceability Matrix

| Area | Existing E2E | New E2E Coverage | Exploratory UCs |
|------|-------------|------------------|-----------------|
| Auth & Access | 5 | Login+invalid+redirect | UC-01–04 (login, routes, logout, redirect) |
| Doctor Dashboard | 5 | Display+search+new+open+title | UC-05–07 (data, search, nav) |
| Create Card | 3 | Create+cancel+short query | UC-08–10 (search, selection, data, errors) |
| Nurse Dashboard | 4 | Display+open+search+title | UC-11 |
| Episode Tabs | 3 | All tabs+back | UC-12 (tabs, nav) |
| Timeline | 0 | — | UC-13 (days, switch, colors) |
| Vitals | 2 | Enter+HTML5 validation | UC-14–17 (entry, update, read-only, validation) |
| Orders | 2 | Create+status | UC-18–21 (create, lifecycle, execute, validation) |
| Notes | 2 | Create+empty | UC-22–23 (create, order, persistence) |
| Scales | 1 | Tab visibility | UC-24–25 (display, creation, roles) |
| Fluid Balance | 1 | Tab visibility | UC-26–27 (display, recalculate) |
| Sign-off | 1 | Dialog | UC-28–30 (nurse→doctor chain) |
| Admin | 4 | Tables+title | UC-31–32 (tables, logout) |
| Layout & Menu | 0 | — | UC-33–35 (AppBar, user menu, status chips) |
| Edge Cases | 0 | — | UC-36–38 (404, browser nav, direct URLs) |
| States & Errors | 0 | — | UC-39–42 (loading, empty, error, version conflict) |
| Concurrency | 0 | — | UC-43 (multi-tab, collaboration) |
| Episode Lifecycle | 0 | — | UC-44–46 (close, PDF, reopen) |
| Special Fields | 0 | — | UC-47–54 (pain, conscious, MAP, author info, etc.) |
| Role Gating | 0 | — | UC-55 (episode controls per role) |

---

## Bug Reporting Template

```
### [BUG] Short Description
**Severity:** Critical / Major / Minor / Trivial
**Environment:** http://localhost:5173
**Role:** DOCTOR / NURSE / HOD / ADMIN
**UC/TC:** UC-NN / TC-NN.NN
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
| New card button | `getByRole('button', { name: 'Нова карта' })` | role=button |
| Cancel (create card) | `getByRole('button', { name: 'Скасувати' })` | |
| Open episode | `getByRole('link', { name: 'Відкрити' })` | link, not button |
| MIS search | `getByLabel('ПІБ, телефон або № медкарти')` | Autocomplete input |
| User menu | `getByRole('button', { name: /Меню користувача/i })` | IconButton with AccountCircle |
| Logout | `getByText('Вийти')` | Inside opened menu |
| Tabs | `getByRole('tab', { name: '...' })` | MUI Tab role |
| Add note | `getByRole('button', { name: 'Додати нотатку' })` | |
| New prescription | `getByRole('button', { name: '+ Нове призначення' })` | |
| Create prescription | `getByRole('button', { name: 'Створити' })` | Inside dialog |
| Sign day | `getByRole('button', { name: 'Підписати добу' })` | |
| Patient search (dashboard) | `getByPlaceholder(/Пошук/i)` | |
| Save vitals | `getByRole('button', { name: 'Зберегти показники' })` | |
| Back button | `getByRole('button', { name: 'Назад' })` | On episode page |
| Clinical day tile | `getByText(/Доба \d+/)` | Timeline chips |
| Fluid recalculate | `getByRole('button', { name: 'Перерахувати' })` | |
| Scale create | `getByRole('button', { name: 'Додати' })` | In scales tab |
| Execute order | `getByRole('button', { name: 'Виконати' })` | Nurse only |
| Cancel order | `getByRole('button', { name: 'Скасувати' })` | On order row |
| Пацієнти nav | `getByRole('link', { name: 'Пацієнти' })` | Role=link not button |
| Hour pill (vitals) | `getByText(/:00/)` | May need parent context |
