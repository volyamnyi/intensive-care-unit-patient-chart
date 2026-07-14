# ICU Patient Chart — Exploratory Test Plan (v4)

## Session Charter

**Objective:** Discover functional defects, usability issues, and coverage gaps in the ICU Patient Chart web application through session-based exploratory testing across all roles and core workflows.

**Methods:** Session-based exploratory testing (testomat.io methodology — classify errors → create charter → time box → assess → debrief). Heuristics: CRUD-SAP (Create, Read, Update, Delete, Search, Authorize, Prescribe), FEW HICCUPS (Functionality, Errors, Workflow, Hierarchy, Input, Consistency, Configuration, Usability, Performance, Security), SFDPOT (Structure, Function, Data, Platform, Operations, Time).

**Time Box:** 15 minutes per session (8+ sessions)

**Scope:** Frontend UI (http://localhost:5173) — login (6 users × 3 roles), dashboards (doctor/nurse/admin), create card, episode page (5 tabs × 2 roles), clinical day timeline, vitals entry, orders lifecycle, notes, scales, fluid balance, sign-off chain, navigation, role-based access control, error states, user menu, session handling, PDF audit, episode lifecycle, concurrent sessions.

**Out of Scope:** Backend API (covered by unit/integration tests), PDF binary generation, performance/load testing, mobile responsiveness.

**Environment:** Playwright chromium, non-headless, fullscreen, slowMo=400ms

**Severity Classification:**
- **P1 — Critical:** Workflow block, data loss, wrong clinical data display
- **P2 — Major:** Feature broken, role-based access violation, incorrect state transition
- **P3 — Minor:** UI glitch, missing validation, edge case not handled
- **P4 — Trivial:** Cosmetic, locator/timing issue in test only

---

## Use Cases & Test Cases (75 UCs, 280+ TCs)

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
| TC-55.04 | Nurse cannot create card | Auth as NURSE | 1. Check dashboard | "Нова карта" absent | P2 | TC-55.04 | Nurse cannot create card | Auth as NURSE | 1. Check dashboard | "Нова карта" absent | P2 |

### UC-56: Clinical Day Timeline — Two-Day View
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-56.01 | Two clinical days shown | Episode has >=2 days | 1. Open episode page | Both "Доба 1" and "Доба 2" visible in timeline | P1 |
| TC-56.02 | Day tile shows date | Days exist | 1. Check each tile | Date label below day number (e.g. "14 лип.") | P2 |
| TC-56.03 | Doctor opens Доба 2 (NURSE_SIGNED) | Day is signed by nurse | 1. Click Доба 2 tile | Tab content loads for day 2, doctor sees Підписати добу | P2 |
| TC-56.04 | Nurse opens Доба 1 (OPEN) | Day is OPEN | 1. Click Доба 1 tile | Tab content loads for day 1, nurse sees Підписати добу | P2 |
| TC-56.05 | Switch day loads different vitals | Days have different data | 1. Day 1 → tab data → Day 2 → tab data | Each day shows its own vitals/orders/notes/balance | P2 |
| TC-56.06 | Timeline horizontal scroll | Many clinical days created | 1. Scroll timeline | Horizontal scroll allows navigating all days | P3 |

### UC-57: Vitals Tab — Doctor Read-Only vs Nurse Editable
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-57.01 | Doctor sees Зберегти показники visible | Auth as DOCTOR, day not signed by doctor | 1. Open Vitals tab | Save button present (note: may be enable/disable issue to verify) | P2 |
| TC-57.02 | Nurse sees Зберегти показники with editable fields | Auth as NURSE, OPEN day | 1. Open Vitals tab | All numeric fields editable, save button enabled | P1 |
| TC-57.03 | All 14 vitals fields rendered | Any role, vitals tab open | 1. Check form fields | АТ сист, АТ діас, ЧСС, SpO2, Темп. тіла, ЦВТ, ЧД, Свідомість, etCO2, FiO2, Діурез, Дренаж, Біль (0-10), Нотатки | P1 |
| TC-57.04 | Vitals form field units displayed | Any role | 1. Check each label | Units: мм.рт.ст, в 1 хв, %, °С, мм.вод.ст, мл/год, мл | P2 |
| TC-57.05 | HourlyRecordTable shows 24 rows | Vitals tab open | 1. Scroll table | 24 rows: 8:00 through 7:00, columns: Година, АТ сист, АТ діас, ЧСС, SpO2, Темп, ЦВТ, ЧД | P1 |

### UC-58: Vitals Hour Pills — Visual States
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-58.01 | 24 hour pills visible (8:00–7:00) | Vitals tab open | 1. Check pill bar | 24 pills labeled 8:00 through 7:00 | P1 |
| TC-58.02 | Select hour opens form for that hour | Any role | 1. Click pill "9:00" | Form title shows "Показники — 9:00", fields for that hour | P1 |
| TC-58.03 | Currently selected hour highlighted | Pill selected | 1. Click pill | Selected hour visually distinct from others | P2 |
| TC-58.04 | ▶ icon on current hour pill | Based on system time | 1. Check pill bar | Play icon (▶) on the current hour pill | P3 |
| TC-58.05 | Past unfilled hours show visual indicator | Day has elapsed hours | 1. Check past hours | Past unfilled hours visually distinct (missing data indication) | P2 |

### UC-59: Medical Orders — Doctor Creates Order
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-59.01 | "+ Нове призначення" expands form | Auth as DOCTOR | 1. Click "+ Нове призначення" | Order creation form slides open with all fields | P1 |
| TC-59.02 | Order form has all required fields | Form expanded | 1. Check form fields | Категорія, Препарат, Доза, Од., Шлях, Частота, Початок, Кінець | P1 |
| TC-59.03 | Category dropdown populates | Form expanded | 1. Click Категорія | Dropdown with medical categories (e.g., Антибіотики, Інфузія) | P2 |
| TC-59.04 | Route dropdown populates | Form expanded | 1. Click Шлях | Dropdown with routes (e.g., В/в, В/м, П/ш, Перорально) | P2 |
| TC-59.05 | Frequency field accepts schedule text | Form expanded | 1. Enter "2 рази на добу" | Free text accepted | P2 |
| TC-59.06 | Start datetime picker works | Form expanded | 1. Click Початок field | Date/time picker or text input for ISO datetime | P2 |
| TC-59.07 | Toggle form collapses | Form open | 1. Click "+ Нове призначення" again | Form collapses, button text unchanged | P3 |
| TC-59.08 | Nurse cannot see "+ Нове призначення" | Auth as NURSE | 1. Open Orders tab | Button absent; orders have "Виконання" column instead | P1 |

### UC-60: Orders Tab — Nurse Execution
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-60.01 | Nurse sees "Виконання" column | Auth as NURSE, orders tab | 1. Check table header | Column "Виконання" with execute action per order | P1 |
| TC-60.02 | Nurse executes active order | Auth as NURSE, ACTIVE order exists | 1. Click "Виконати"<br>2. Enter actual dose<br>3. Enter comment (optional)<br>4. Confirm | Order status → COMPLETED, execution timestamp recorded | P1 |
| TC-60.03 | Doctor does not see "Виконання" column | Auth as DOCTOR | 1. Check orders tab header | No "Виконання" column or execute buttons | P2 |
| TC-60.04 | Order table columns differ per role | Compare doctor vs nurse | 1. Doctor: Препарат, Доза, Шлях, Статус<br>2. Nurse adds: Виконання column | Doctor gets Статус column, nurse gets Статус + Виконання | P2 |

### UC-61: Scales Tab — Doctor Creates Assessment
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-61.01 | Scales tab shows "Немає даних шкал" | No scales recorded | 1. Open Шкали tab | Text "Немає даних шкал" displayed | P2 |
| TC-61.02 | Doctor sees scale creation form | Auth as DOCTOR | 1. Open Шкали tab | Dropdown with available scale names + add button visible | P2 |
| TC-61.03 | Nurse cannot create scale | Auth as NURSE | 1. Open Шкали tab | No scale creation controls; only empty state or read-only list | P2 |
| TC-61.04 | Select scale shows its input fields | Auth as DOCTOR | 1. Select a scale from dropdown | Scale-specific input fields appear (e.g., score selectors) | P2 |
| TC-61.05 | Scale result saved after creation | Auth as DOCTOR | 1. Fill scale inputs<br>2. Click "Додати" | Scale result card appears with name, value, timestamp | P2 |

### UC-62: Notes Tab — Full CRUD
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-62.01 | Textarea with "Нова нотатка" placeholder | Notes tab open | 1. Check textarea | Placeholder text "Нова нотатка" visible | P2 |
| TC-62.02 | Add note with text | Any role | 1. Type in textarea<br>2. Click "Додати нотатку" | Note appears in list with author name, role, timestamp | P1 |
| TC-62.03 | Note shows author full name | Note exists | 1. Check note card content | Author's full name displayed (e.g., "Олександр Мельник") | P2 |
| TC-62.04 | Note shows author role badge | Note exists | 1. Check note card | Role badge "Лікар" or "Медсестра" displayed | P2 |
| TC-62.05 | Note shows creation timestamp | Note exists | 1. Check note card | Readable timestamp format (e.g., "14.07.2026 10:30") | P2 |
| TC-62.06 | Notes display in reverse chronological order | >=2 notes exist | 1. Check list order | Newest note at top | P2 |
| TC-62.07 | Empty note rejected | Any role | 1. Click "Додати нотатку" without text | Note not added, validation shown | P2 |
| TC-62.08 | "Немає нотаток" empty state | No notes exist | 1. Open tab | Text "Немає нотаток" visible | P2 |

### UC-63: Fluid Balance Tab — Recalculation
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-63.01 | Fluid balance section headings | Tab open | 1. Check layout | Headings: Надійшло, Виділено, Добовий баланс, Кумулятивний баланс | P1 |
| TC-63.02 | Initial values show 0 мл | No fluid data | 1. Open tab | All values: "0 мл" | P2 |
| TC-63.03 | "Перерахувати" recalculates balance | Vitals with I/O data exist | 1. Enter intake/output vitals<br>2. Click "Перерахувати" | Values update from latest calculations | P1 |
| TC-63.04 | Recalculate loading state | During API call | 1. Click "Перерахувати" | Button shows loading/progress state | P3 |
| TC-63.05 | Balance color coding positive/negative | Balance != 0 | 1. Check after recalculation | Positive: green, Negative: red color applied | P2 |

### UC-64: Sign-off Chain — Nurse then Doctor
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-64.01 | Nurse signs OPEN day dialog | Auth as NURSE, OPEN day | 1. Click "Підписати добу" | Dialog opens with day number and confirmation text | P1 |
| TC-64.02 | Sign dialog title shows day number | Eligible to sign | 1. Check dialog title | "Підписання доби №N" | P2 |
| TC-64.03 | Confirm sign changes status | After nurse signs | 1. Click "Підписати" in dialog | Day status → NURSE_SIGNED, success feedback | P1 |
| TC-64.04 | Cancel sign dialog | Dialog open | 1. Click "Скасувати" | Dialog closes, status unchanged | P2 |
| TC-64.05 | Doctor signs NURSE_SIGNED day | Auth as DOCTOR, day NURSE_SIGNED | 1. Click "Підписати добу"<br>2. Confirm | Day status → DOCTOR_SIGNED, PDF metadata generated | P1 |
| TC-64.06 | Sign button hidden for CLOSED day | Day already fully signed | 1. Check page after doctor sign | No sign button visible for that day | P2 |
| TC-64.07 | Dual-role sign: nurse signs → doctor signs | Full workflow | 1. Nurse signs day<br>2. Doctor signs same day | Chain completes successfully | P1 |

### UC-65: Episode Status Display
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-65.01 | Status label on episode page header | On episode page | 1. Check below patient name | "Статус: {status translation}" visible | P1 |
| TC-65.02 | OPEN status displays as "Відкрита" | Day is OPEN | 1. Login as nurse, open episode | "Статус: Відкрита" displayed | P2 |
| TC-65.03 | NURSE_SIGNED status displays as "Підписана медсестрою" | Day signed by nurse | 1. Login as doctor, open episode | "Статус: Підписана медсестрою" displayed | P2 |
| TC-65.04 | Episode ID chip shows first 8 chars | On episode page | 1. Check next to status | "№ a1111111" format with truncated UUID | P2 |
| TC-65.05 | Current day number subtitle | On episode page | 1. Check below patient name | "Доба №N" format (e.g., "Доба №2") | P2 |
| TC-65.06 | Status color changes per status | Different roles view same episode | 1. Compare nurse (Відкрита) vs doctor (Підписана медсестрою) | Different status colors applied | P2 |

### UC-66: User Menu — Per-Role Content
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-66.01 | Doctor user menu: name + role + logout | Auth as DOCTOR | 1. Open menu | Items: "Олександр Мельник" (name), "Лікар" (role), "Вийти" (logout) | P1 |
| TC-66.02 | Nurse user menu: name + role + logout | Auth as NURSE | 1. Open menu | Items: "Олена Ткаченко", "Медсестра", "Вийти" | P1 |
| TC-66.03 | Admin user menu: name + role + logout | Auth as ADMIN | 1. Open menu | Items: "Адміністратор" or admin name, "Вийти" | P1 |
| TC-66.04 | Click "Вийти" logs out | Menu open | 1. Click "Вийти" | Redirected to /login, localStorage token removed | P1 |
| TC-66.05 | Menu closes on outside click | Menu open | 1. Click outside menu | Menu closes gracefully | P3 |

### UC-67: Episode Page — Back Navigation
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-67.01 | "Назад" button visible on episode page | On episode page | 1. Check episode header | "Назад" button present | P2 |
| TC-67.02 | Doctor "Назад" returns to /doctor | Auth as DOCTOR | 1. Click "Назад" | URL → /doctor (dashboard) | P1 |
| TC-67.03 | Nurse "Назад" returns to /nurse | Auth as NURSE | 1. Click "Назад" | URL → /nurse (dashboard) | P1 |
| TC-67.04 | Browser back from episode | On episode page | 1. Press browser back button | Returns to dashboard | P2 |

### UC-68: AppBar — Role-Specific Title
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-68.01 | Doctor AppBar title: "Карта інтенсивної терапії" | Auth as DOCTOR | 1. Check AppBar | Title without role suffix | P2 |
| TC-68.02 | Nurse AppBar title: "Карта інтенсивної терапії — медсестра" | Auth as NURSE | 1. Check AppBar | Title includes "— медсестра" suffix | P2 |
| TC-68.03 | AppBar title links to dashboard | Any role | 1. Click title | Redirects to appropriate dashboard (/doctor or /nurse) | P2 |
| TC-68.04 | Doctor has "Пацієнти" nav link | Auth as DOCTOR | 1. Check AppBar | "Пацієнти" link visible next to title | P2 |
| TC-68.05 | Nurse has no nav links | Auth as NURSE | 1. Check AppBar | Only title + user menu, no "Пацієнти" link | P2 |

### UC-69: Create Card — Full Workflow
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-69.01 | Autocomplete searches by ПІБ | On create card | 1. Type "Петренко" in search | Dropdown shows "Петренко Іван Сергійович" with card number, DOB, city | P1 |
| TC-69.02 | Patient result shows formatted details | Search returns patient | 1. Check option text | "Петренко Іван Сергійович МК-001234 · 1978-03-15 · м. Київ" | P2 |
| TC-69.03 | "ПІБ, телефон або № медкарти" label | On create card | 1. Check autocomplete label | Label "ПІБ, телефон або № медкарти" above input | P2 |
| TC-69.04 | Autocomplete has "Open" and "Clear" icons | Search field active | 1. Check input adornments | Open dropdown button (aria="Open") and Clear (aria="Clear") buttons | P2 |
| TC-69.05 | Clear icon resets search | After typing | 1. Click Clear icon | Search field cleared, dropdown closed | P2 |
| TC-69.06 | Patient data fields render after selection | Patient selected | 1. Scroll to data section | Все поля: ПІП, Дата народження, Стать, Зріст (см), Маса (кг), Група крові, Rezus, № медкарти | P1 |
| TC-69.07 | "Створити карту" button creates episode | Patient selected | 1. Click "Створити карту" | Episode created, redirected to /doctor/episode/{id} | P1 |
| TC-69.08 | "Скасувати" returns to dashboard | On create card page | 1. Click "Скасувати" | URL → /doctor | P2 |
| TC-69.09 | Short query (1 char) shows hint | In search field | 1. Type 1 character | "Введіть мінімум 2 символи" or equivalent hint | P3 |
| TC-69.10 | No results for nonexistent query | Search field | 1. Type "ZZZ" (>=2 chars) | "Немає результатів" in dropdown | P3 |

### UC-70: Admin Page — User Tables Detail
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-70.01 | Two table sections visible | Auth as ADMIN | 1. Go to /admin | "Лікарі" and "Медсестри" sections with column headers | P1 |
| TC-70.02 | Doctors table: ПІБ, Логін, Роль, Email | Table loaded | 1. Check columns | doctor1: Олександр Мельник / doctor1 / Лікар / melnyk@hospital.ua | P1 |
| TC-70.03 | Nurses table: ПІБ, Логін, Роль, Email | Table loaded | 1. Check columns | nurse1: Олена Ткаченко / nurse1 / Медсестра / tkachenko@hospital.ua | P1 |
| TC-70.04 | doctor2 visible in doctors table | Auth as ADMIN | 1. Scroll doctors table | doctor2: Наталія Бойко / doctor2 / Лікар / boyko@hospital.ua | P2 |
| TC-70.05 | nurse2 visible in nurses table | Auth as ADMIN | 1. Scroll nurses table | nurse2: Марія Кравчук / nurse2 / Медсестра / kravchuk@hospital.ua | P2 |
| TC-70.06 | HOD not listed separately | Admin check | 1. Verify head1 | If head1 shown, verify role label is correct (Завідувач відділення) | P2 |

### UC-71: Vitals Form — Pain (0-10) and Consciousness
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-71.01 | Pain field labeled "Біль (0-10)" | Vitals form open | 1. Check Біль field | Label "Біль (0-10)" with numeric input (0-10 range) | P2 |
| TC-71.02 | Pain accepts values 0-10 | Auth as NURSE | 1. Enter Біль = 7 | Value accepted | P2 |
| TC-71.03 | Pain rejects value > 10 | Auth as NURSE | 1. Enter Біль = 15 | Validation error or value clamped | P3 |
| TC-71.04 | Consciousness free-text field | Vitals form open | 1. Check Свідомість field | Text input labeled "Свідомість" (consciousness state) | P2 |
| TC-71.05 | Consciousness accepts Ukrainian text | Auth as NURSE | 1. Enter "Ясна, заспокоєна" | Text accepted and saved | P2 |
| TC-71.06 | Vitals notes textarea | Vitals form open | 1. Check Нотатки field | Multi-line textarea for vitals notes | P3 |

### UC-72: Vitals — Pressure, HR, SpO2, Temp Fields
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-72.01 | Systolic BP field (АТ сист) | Vitals form | 1. Check field | Numeric input, label "АТ сист (мм.рт.ст)" | P1 |
| TC-72.02 | Diastolic BP field (АТ діас) | Vitals form | 1. Check field | Numeric input, label "АТ діас (мм.рт.ст)" | P1 |
| TC-72.03 | Heart rate field (ЧСС) | Vitals form | 1. Check field | Numeric input, label "ЧСС (в 1 хв)" | P1 |
| TC-72.04 | SpO2 field | Vitals form | 1. Check field | Numeric input, label "SpO2 (%)" | P1 |
| TC-72.05 | Temperature field (Темп. тіла) | Vitals form | 1. Check field | Numeric (decimal), label "Темп. тіла (°С)" | P1 |
| TC-72.06 | CVP field (ЦВТ) | Vitals form | 1. Check field | Numeric, label "ЦВТ (мм.вод.ст)" | P2 |
| TC-72.07 | Respiratory rate (ЧД) | Vitals form | 1. Check field | Numeric, label "ЧД (в 1 хв)" | P2 |
| TC-72.08 | etCO2 field | Vitals form | 1. Check field | Numeric, label "etCO2 (мм.рт.ст)" | P2 |
| TC-72.09 | FiO2 field | Vitals form | 1. Check field | Numeric, label "FiO2 (%)" | P2 |
| TC-72.10 | Diuresis field (Діурез) | Vitals form | 1. Check field | Numeric, label "Діурез (мл/год)" | P2 |
| TC-72.11 | Drainage field (Дренаж) | Vitals form | 1. Check field | Numeric, label "Дренаж (мл)" | P2 |

### UC-73: Vitals — Form Validation
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-73.01 | Blood pressure range 0-300 | Auth as NURSE | 1. Enter АТ сист = 350 | Validation error or field clamp | P3 |
| TC-73.02 | Heart rate range 0-300 | Auth as NURSE | 1. Enter ЧСС = 350 | Validation rejects | P3 |
| TC-73.03 | SpO2 range 0-100 | Auth as NURSE | 1. Enter SpO2 = 120 | Validation rejects | P3 |
| TC-73.04 | Temperature range 30-45 | Auth as NURSE | 1. Enter temp = 50 | Validation rejects | P3 |
| TC-73.05 | Negative values rejected | Auth as NURSE | 1. Enter negative values in numeric fields | Validation rejects | P3 |

### UC-74: Empty States Consistency
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-74.01 | Orders empty: "Немає призначень" | No orders exist | 1. Open Призначення tab | "Немає призначень" text displayed | P2 |
| TC-74.02 | Scales empty: "Немає даних шкал" | No scales exist | 1. Open Шкали tab | "Немає даних шкал" text displayed | P2 |
| TC-74.03 | Notes empty: "Немає нотаток" | No notes exist | 1. Open Нотатки tab | "Немає нотаток" text displayed below textarea | P2 |
| TC-74.04 | Fluid balance empty: 0 ml all sections | No vitals data | 1. Open Баланс рідини | Все разделы показывают 0 мл | P2 |
| TC-74.05 | Dashboard empty: "Немає активних пацієнтів" | No active episodes | 1. Go to dashboard | Empty state alert visible | P2 |
| TC-74.06 | Hours table empty: all "-" | No vitals recorded | 1. Check vitals table | All 24 rows show "-" for every column | P2 |

### UC-75: Cross-Role Differences on Episode
| ID | Test Case | Precondition | Steps | Expected Result | Severity |
|----|-----------|-------------|-------|-----------------|----------|
| TC-75.01 | Doctor sees 5 tabs with orders+scales creation | Auth as DOCTOR | 1. Compare tab controls | Doctor: can create orders, scales, notes; vitals may show save button | P1 |
| TC-75.02 | Nurse sees 5 tabs without order/scale creation | Auth as NURSE | 1. Compare tab controls | Nurse: can save vitals, execute orders, add notes; no order/scale creation | P1 |
| TC-75.03 | Orders tab columns differ | Both roles check | 1. Doctor: header columns<br>2. Nurse: header columns | Doctor: Препарат, Доза, Шлях, Статус<br>Nurse: Препарат, Доза, Шлях, Статус, Виконання | P2 |
| TC-75.04 | Scales tab controls differ | Both roles check | 1. Doctor: creation controls visible<br>2. Nurse: no creation controls | Doctor sees scale dropdown + Add button; Nurse sees only empty state | P2 |
| TC-75.05 | AppBar title differs | Both roles check | 1. Doctor: title without suffix<br>2. Nurse: title with "— медсестра" | Role-appropriate title in header | P2 |

---

## Session Plan (8 sessions × 15 min)

### Session 1: Authentication & Access Control (15 min)
- TC-01.01 through TC-04.03
- Login all 6 users (doctor1/2, nurse1/2, head1, admin), route restrictions per role, logout each role, root redirect
- **Heuristics:** Authorize (all roles × all routes), Consistency (redirect behavior), SFDPOT (Platform — localStorage)

### Session 2: Doctor Dashboard, Create Card (15 min)
- TC-05.01 through TC-07.04, TC-69.01 through TC-69.10
- Dashboard data display, search & filter, navigation actions; create card full workflow — autocomplete, patient data, create, cancel, clear, search behavior
- **Heuristics:** CRUD (search, create, cancel), Input (autocomplete, validation, clear), SFDPOT (Data — MIS integration)

### Session 3: Nurse Dashboard & Episode Page Layout (15 min)
- TC-11.01 through TC-11.05, TC-12.01 through TC-12.06, TC-67.01 through TC-67.04, TC-68.01 through TC-68.05
- Nurse dashboard navigation, episode page layout (5 tabs, back, patient name), AppBar role-specific titles, back navigation per role
- **Heuristics:** FEW HICCUPS (Workflow), Hierarchy (role-based views), Config (doctor vs nurse AppBar)

### Session 4: Vitals & Hourly Records (15 min)
- TC-14.01 through TC-17.03, TC-57.01 through TC-58.05, TC-71.01 through TC-73.05
- Hour selector (24 pills), vitals entry for nurse, doctor read-only, all 14 field types, pain 0-10, consciousness, hour pill visual states, validation ranges, hourly table
- **Heuristics:** CRUD (create vitals), Input (range, boundary, format), FEW HICCUPS (Functionality), SFDPOT (Data integrity)

### Session 5: Medical Orders & Notes (15 min)
- TC-18.01 through TC-23.03, TC-59.01 through TC-62.08
- Order creation (doctor), form fields and dropdowns, order lifecycle, nurse execution with Виконати column, notes CRUD, author badges, timestamps, empty states
- **Heuristics:** CRUD-SAP (Створити, Виконати, Скасувати), Hierarchy (doctor vs nurse order controls), Consistency (empty states)

### Session 6: Scales & Fluid Balance (15 min)
- TC-24.01 through TC-27.03, TC-61.01 through TC-63.05
- Scales tab empty state, doctor scale creation, nurse read-only, fluid balance display (intake/output/balance/cumulative), recalculate button, color coding
- **Heuristics:** CRUD (scale create, fluid recalculate), Config (role-gated scale creation), Update (recalculate clears stale data)

### Session 7: Sign-off Chain, Admin, Timeline (15 min)
- TC-28.01 through TC-30.02, TC-56.01 through TC-56.06, TC-64.01 through TC-65.06, TC-70.01 through TC-70.06
- Nurse signs OPEN day → doctor signs NURSE_SIGNED day → CLOSED, clinical day timeline with two-day view, status display, admin user tables
- **Heuristics:** Workflow (sign chain: OPEN → NURSE_SIGNED → DOCTOR_SIGNED → CLOSED), State Transitions, SFDPOT (Operations — sequential workflow)

### Session 8: User Menu, Edge Cases, Concurrent, Error (15 min)
- TC-31.01 through TC-34.05, TC-36.01 through TC-43.03, TC-66.01 through TC-66.05, TC-74.01 through TC-75.05
- User menu per role (name/role/logout), 404 route, browser navigation, direct URL access, loading/empty/error states, version conflict, concurrent sessions, role differences
- **Heuristics:** Errors (API 500/network), Performance (loading), Security (token, access control, concurrent), Consistency (empty states across all tabs)

---

## Heuristics Reference

| Heuristic | Application |
|-----------|-------------|
| **CRUD-SAP** | Create, Read, Update, Delete, Search, Authorize, Prescribe — test every operation on every entity |
| **FEW HICCUPS** | Functionality, Errors, Workflow, Hierarchy, Input, Consistency, Configuration, Usability, Performance, Security — checklist for session coverage |
| **SFDPOT** | Structure (page layout, sections), Function (every button, link, input), Data (formats, units, display), Platform (browser nav, localStorage), Operations (workflow sequence), Time (timeline, historical data) |
| **Mnemonics for Input** | Boundary (min/max), Range (0-10 pain, 20-300 HR), Format (date, time), Empty, Special chars (Cyrillic, UTF-8) |
| **Role Matrix** | DOCTOR, NURSE, HOD, ADMIN — every feature × every role, including what should be hidden |
| **State Transitions** | Episode: DRAFT→ACTIVE→COMPLETED→ARCHIVED; ClinicalDay: OPEN→NURSE_SIGNED→DOCTOR_SIGNED→CLOSED/REOPENED; Order: ACTIVE→COMPLETED/CANCELLED |
| **Tab Matrix** | Вітальні показники, Призначення, Шкали, Нотатки, Баланс рідини — every tab × every role, check for hidden/shared controls |

---

## Traceability Matrix

---

## Detailed Element Inventory (from v4 Headed Exploration)

| Page / Tab | Interactive Elements | Read-Only Elements |
|------------|---------------------|--------------------|
| **Doctor Dashboard** (/doctor) | User menu (3 items), Нова карта button, search input, Відкрити role=button (×3), AppBar title link, Пацієнти link | Table (3 patients), Активний chips (×3), headings |
| **Nurse Dashboard** (/nurse) | User menu (3 items), search input, Відкрити (×3), AppBar title link | Table (3 patients), Активний chips, headings |
| **Create Card** (/doctor/create-card) | Autocomplete search, Clear icon, Open dropdown, patient selection option, Створити карту, Скасувати, patient data fields (8 read-only), User menu | Search label, data section |
| **Episode — Vitals tab** | 24 hour pills (8:00–7:00), 11 numeric inputs, 2 text inputs, 1 textarea, Зберегти показники, Clinical day timeline (Доба 1/2), Підписати добу, Назад | HourlyRecordTable (24 rows), status chip, episode ID chip |
| **Episode — Orders tab (DOCTOR)** | + Нове призначення (expand/collapse), order form (Категорія, Препарат, Доза, Од., Шлях, Частота, Початок, Кінець), Створити | Table header (Препарат, Доза, Шлях, Статус), empty state "Немає призначень" |
| **Episode — Orders tab (NURSE)** | Виконати per order, Підписати добу | Table header (adds Виконання column), empty state |
| **Episode — Scales tab** | [DOCTOR only] scale dropdown + Додати button; [NURSE] nothing | Empty state "Немає даних шкал" |
| **Episode — Notes tab** | Textarea "Нова нотатка", Додати нотатку button, note cards (author, role, timestamp) | Empty state "Немає нотаток" |
| **Episode — Fluid Balance tab** | Перерахувати button | 4 sections (Надійшло, Виділено, Добовий баланс, Кумулятивний баланс) all "0 мл" |
| **Admin** (/admin) | User menu (3 items) | 2 tables (Лікарі + Медсестри), 4 columns each |
| **Login page** | 2 text inputs, Увійти button | — |

---

| Area | Existing E2E | New E2E Coverage | Exploratory UCs |
|------|-------------|------------------|-----------------|
| Auth & Access | 5 | Login+invalid+redirect | UC-01–04 (login 6 users, routes, logout, redirect) |
| Doctor Dashboard | 5 | Display+search+new+open+title | UC-05–07 (data, search, nav) |
| Create Card | 3 | Create+cancel+short query | UC-08–10 + UC-69 (full workflow, autocomplete clear, patient data fields) |
| Nurse Dashboard | 4 | Display+open+search+title | UC-11 |
| Episode Tabs | 3 | All tabs+back | UC-12 + UC-68 (tabs, nav, role-specific AppBar) |
| Timeline | 2 | Day switch+status colors | UC-13 + UC-56 (two-day view, date labels, day switch) |
| Vitals | 2 | Enter+HTML5 validation | UC-14–17 + UC-57 + UC-58 + UC-71 + UC-72 + UC-73 (24 pills, all 14 fields, doctor vs nurse, visual states, pain/consciousness, validation) |
| Orders | 2 | Create+status | UC-18–21 + UC-59 + UC-60 (order form fields, dropdowns, nurse execution column, role differences) |
| Notes | 2 | Create+empty | UC-22–23 + UC-62 (author badge, role distinction, reverse chronological) |
| Scales | 1 | Tab visibility | UC-24–25 + UC-61 (creation for doctor, empty state, scale selection) |
| Fluid Balance | 1 | Tab visibility | UC-26–27 + UC-63 (4 sections, recalculate, color coding, loading) |
| Sign-off | 1 | Dialog | UC-28–30 + UC-64 (full chain: nurse→doctor, dialog, status progression) |
| Admin | 4 | Tables+title | UC-31–32 + UC-70 (detailed user data, all 6 users visible) |
| Layout & Menu | 0 | — | UC-33–35 + UC-66 + UC-67 + UC-68 (user menu 3 items per role, back nav, AppBar title per role) |
| Edge Cases | 0 | — | UC-36–38 (404, browser nav, direct URLs) |
| States & Errors | 1 | Empty states | UC-39–42 + UC-74 (all 6 empty states across tabs, loading, error) |
| Concurrency | 0 | — | UC-43 (multi-tab, collaboration) |
| Episode Lifecycle | 0 | — | UC-44–46 (close, PDF, reopen) |
| Special Fields | 0 | — | UC-47–54 + UC-71 (pain, conscious, MAP, author info, vitals field units) |
| Role Gating | 2 | Tab differences | UC-55 + UC-75 (episode controls per role, tab-by-tab comparison) |

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
| Cancel order | `getByRole('button', { name: 'Скасувати' })` | On order row, also in dialogs |
| Пацієнти nav | `getByRole('link', { name: 'Пацієнти' })` | Role=link, in AppBar |
| Hour pill (vitals) | `getByRole('button', { name: /^\d+:/ })` | 24 pills 8:00–7:00 |
| Autocomplete search (create card) | `getByLabel('ПІБ, телефон або № медкарти')` | MUI Autocomplete |
| Create card submit | `getByRole('button', { name: 'Створити карту' })` | After patient selected |
| Create card cancel | `getByRole('button', { name: 'Скасувати' })` | On create card page |
| Autocomplete clear | `getByRole('button', { name: 'Clear' })` | Icon button in autocomplete |
| Autocomplete open | `getByRole('button', { name: 'Open' })` | Dropdown toggle |
| Patient name (episode header) | `page.locator('#root h5')` | First h5 below AppBar |
| Episode status | `page.locator('text=/Статус:/*')` | Text with status translation |
| Episode ID chip | `page.locator('text=/№ [a-z0-9]{8}/')` | First 8 UUID chars |
| Day timeline tile | `getByRole('button', { name: /Доба \d+/ })` | May need exact text match |
| Current day indicator | `page.locator('[class*="Mui-selected"]')` | Selected day chip |
| Expand order form | `getByRole('button', { name: '+ Нове призначення' })` | Toggle visibility |
| Order category | `getByLabel('Категорія')` | Dropdown/autocomplete |
| Order drug | `getByLabel('Препарат')` | Text input |
| Order dose | `getByLabel('Доза')` | Numeric input |
| Order unit | `getByLabel('Од.')` | Dropdown (мг, мл, etc.) |
| Order route | `getByLabel('Шлях')` | Dropdown (В/в, В/м, П/ш) |
| Order frequency | `getByLabel('Частота')` | Text input |
| Order start | `getByLabel('Початок')` | DateTime input |
| Order end | `getByLabel('Кінець')` | DateTime input (optional) |
| Orders empty | `getByText('Немає призначень')` | Empty state text |
| Scales empty | `getByText('Немає даних шкал')` | Empty state text |
| Notes empty | `getByText('Немає нотаток')` | Empty state text |
| Notes textarea | `getByRole('textbox', { name: 'Нова нотатка' })` | Or placeholder match |
| Note author name | `page.locator('.MuiCardHeader-content')` | Card header with name |
| Note role badge | `page.locator('.MuiChip-root:has-text("Лікар"):visible, .MuiChip-root:has-text("Медсестра"):visible')` | MUI Chip in note |
| Fluid intake | `text=Надійшло:` | Followed by ml value |
| Fluid output | `text=Виділено:` | Followed by ml value |
| Daily balance | `text=Добовий баланс:` | Color-coded |
| Cumulative balance | `text=Кумулятивний баланс:` | Running total |
| Vitals systolic | `getByLabel('АТ сист (мм.рт.ст)')` | Numeric input |
| Vitals diastolic | `getByLabel('АТ діас (мм.рт.ст)')` | Numeric input |
| Vitals HR | `getByLabel('ЧСС (в 1 хв)')` | Numeric input |
| Vitals SpO2 | `getByLabel('SpO2 (%)')` | Numeric input |
| Vitals temp | `getByLabel('Темп. тіла (°С)')` | Numeric (decimal) input |
| Vitals CVP | `getByLabel('ЦВТ (мм.вод.ст)')` | Numeric input |
| Vitals RR | `getByLabel('ЧД (в 1 хв)')` | Numeric input |
| Vitals etCO2 | `getByLabel('etCO2 (мм.рт.ст)')` | Numeric input |
| Vitals FiO2 | `getByLabel('FiO2 (%)')` | Numeric input |
| Vitals diuresis | `getByLabel('Діурез (мл/год)')` | Numeric input |
| Vitals drainage | `getByLabel('Дренаж (мл)')` | Numeric input |
| Vitals pain | `getByLabel('Біль (0-10)')` | Numeric 0-10 |
| Vitals consciousness | `getByLabel('Свідомість')` | Text input |
| Vitals notes | `getByLabel('Нотатки')` | Textarea in vitals form |
| Save vitals | `getByRole('button', { name: 'Зберегти показники' })` | Nurse saves |
| Vitals table | `page.locator('table').last()` | HourlyRecordTable |
| Back button (episode) | `getByRole('button', { name: 'Назад' })` | Returns to dashboard |
| Sign day button | `getByRole('button', { name: 'Підписати добу' })` | On episode page |
| Sign dialog confirm | `getByRole('button', { name: 'Підписати' })` | Inside sign dialog |
| Doctor role in menu | `getByText('Лікар')` | Disabled menu item |
| Nurse role in menu | `getByText('Медсестра')` | Disabled menu item |
| AppBar title (doctor) | `getByRole('link', { name: 'Карта інтенсивної терапії' })` | Role=link |
| AppBar title (nurse) | `getByRole('link', { name: /Карта інтенсивної терапії — медсестра/ })` | With suffix |
| Admin header | `getByRole('heading', { name: 'Користувачі системи' })` | h5 |
| Doctors table header | `getByRole('heading', { name: 'Лікарі' })` | Section header |
| Nurses table header | `getByRole('heading', { name: 'Медсестри' })` | Section header |
