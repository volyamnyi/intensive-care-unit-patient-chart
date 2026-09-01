# QA Test Plan: Prosthetics Process Control System
## Система управління технологічним процесом виготовлення протезів

---

## Executive Summary

This test plan covers the complete workflow for creating a prosthetist's technical chart in the Process Control System for the Manufacture of Prosthetics. The system enforces strict step-by-step compliance with technology cards (ТТП) through a wizard-based interface with validation at each step.

**System Under Test:** http://localhost:5173/prosthetics
**Reference MVP:** https://prosthetics-process-management-6utl7jt7u-volodymyr-sh.vercel.app/ (for UI reference only)
**Login:** prosthetist1 / **Password:** doctor123
**Documentation Reference:** docs/Process Control System for the Manufacture of Prosthetics.md

---

## Test Scope

### In Scope
- Complete workflow from login to process creation and execution
- All screens (Screens 1-15) as documented
- Session context management across Phase 3 screens
- Hard Block validation mechanisms
- Quality Gate decision logic
- Rework loop functionality
- Failure handling and snapshot creation

### Out of Scope
- Doctor Eleks integration (mocked)
- PDF generation backend
- Actual manufacturing hardware integration

---

## Detailed Step-by-Step Walkthrough

### Phase 1: Authorization (Screen 1)

| Step | Action | Data to Enter | Expected Result | Validation |
|------|--------|---------------|-----------------|------------|
| 1.1 | Navigate to login page | N/A | Login screen displays with fields: Логін, Пароль, button "Увійти" | Page loads within 3 seconds |
| 1.2 | Click "Увійти" with empty fields | N/A | Error: "Невірний логін або пароль. Спробуйте ще раз" | Both fields highlighted |
| 1.3 | Enter invalid credentials | Login: "wrong", Password: "wrong" | Error message displayed, button remains active | Error visible, no redirect |
| 1.4 | Enter valid credentials | Login: "demo", Password: "demo" | Redirect to Dashboard (Screen 2) | URL changes, dashboard visible |
| 1.5 | Verify session persistence | Refresh page (F5) | Remain logged in or redirected to Dashboard | No logout on refresh |

**Alternative Path - Session Expiration:**
- Wait for session timeout (if configured) → Redirect to login with message

---

### Phase 2: Dashboard (Screen 2)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 2.1 | Verify Header elements | Logo, system name, user name "demo", "Вийти" button, notification indicator visible |
| 2.2 | Verify Statistics Cards | Four cards: Активні / Призупинені / Завершені / Провалені with numeric values |
| 2.3 | Click "Новий процес" button | Redirect to Screen 3 (Patient Selection) |
| 2.4 | Click "Мої процеси" button | Filters table to show only current user's processes |
| 2.5 | Verify Process Table Columns | ID, Пацієнт, Замовлення, Шаблон процесу, Поточний етап, Поточний крок, Статус, Оновлено |
| 2.6 | Click on a process row (if exists) | Navigate to process detail (Screen 5 or Screen 8) |

---

### Phase 3: Creating a New Process (Screens 3-6)

#### Screen 3: Patient Selection (Крок 1 з 4)

| Step | Action | Data to Enter | Expected Result | Validation |
|------|--------|---------------|-----------------|------------|
| 3.1 | Verify page title | N/A | "Вибір пацієнта" / "Крок 1 з 4" | Title visible |
| 3.2 | Verify initial state | N/A | Search field empty, table not visible, "Далі" button disabled | All elements in correct initial state |
| 3.3 | Click "Знайти" with empty search | N/A | No search triggered OR message "Введіть критерії пошуку" | Appropriate response |
| 3.4 | Search for patient | Search: "Петренко" | Table displays matching patients with columns: ID, ПІБ, Дата народження, Статус, Дія | Results match search criteria |
| 3.5 | Search with no results | Search: "ZZZZZZZZ" | Message: "За вказаним запитом пацієнтів не знайдено. Сробуйте змінити критерії пошуку." | Message displayed |
| 3.6 | Click "Обрати" on first patient | N/A | Row highlighted, patient saved to session context, "Далі" becomes active | Visual feedback, button enabled |
| 3.7 | Click "Далі" | N/A | Navigate to Screen 4 (Order Selection) | URL changes, Screen 4 loads |
| 3.8 | Click "Назад" | N/A | Navigate to Dashboard | Return to Screen 2 |

**Edge Cases:**
- 3.9 | Search by patient ID | Search valid ID | Correct patient displayed | ID search works
- 3.10 | Search by date of birth | Search "01.01.1990" | Patients with that DOB shown | DOB search works

#### Screen 4: Order Selection (Крок 2 з 4)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 4.1 | Verify sticky header | Patient PIB and ID displayed at top |
| 4.2 | Verify page title | "Вибір замовлення" / "Крок 2 з 4" |
| 4.3 | Verify orders table | Columns: № замовлення, Тип протеза, Лікар, Дата призначення, Статус, Дія |
| 4.4 | Verify only "Активне" or "Призначене" orders shown | No completed/cancelled orders visible |
| 4.5 | Click "Обрати" on an order | Row highlighted, order saved to session, "Далі" enabled |
| 4.6 | Click "Далі" | Navigate to Screen 5 (Order Review) |
| 4.7 | Click "Назад" | Return to Screen 3, previously selected patient remains selected |

**Session Context Verification:**
- 4.8 | Navigate back and forth between screens 3-4 | Selected patient persists | Session context maintained

#### Screen 5: Order Review & Confirmation (Крок 3 з 4)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 5.1 | Verify left panel sections | Section 1: Особисті відомості, Section 2: Причина та рівень порушень, Section 3: Замовлення |
| 5.2 | Verify patient data fields | ПІБ, Дата народження, Стать, Зріст/вага, Соціальний статус |
| 5.3 | Verify medical data | Причина ураження, дата ампутації, Уражена кінцівка, рівень ампутації, Стан кукси |
| 5.4 | Verify order data | № замовлення, Дата призначення, Лікар |
| 5.5 | Verify PDF viewer in center | Embedded PDF viewer with zoom, scroll, fullscreen controls |
| 5.6 | Verify "Старт" button initial state | Disabled (until PDF fully loads) |
| 5.7 | Wait for PDF to load | "Старт" button becomes enabled |
| 5.8 | Click "Старт" | Duplicate check performed → Navigate to Screen 6 (Template Selection) |
| 5.9 | Click "До головного меню" | Confirmation dialog → Clear session context → Redirect to Dashboard |
| 5.10 | Click "Назад" | Return to Screen 4, previously selected order remains |

**Hard Block Verification:**
- 5.11 | Attempt to click "Старт" before PDF loads | Button disabled, cannot proceed | Hard block working

#### Screen 6: Flow Template Selection (Крок 4 з 4)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 6.1 | Verify page title | "Вибір технологічного маршруту" / "Крок 4 з 4" |
| 6.2 | Verify context info | "Пацієнт: [ПІБ]" displayed |
| 6.3 | Verify templates filtered | Only templates matching ProductType, AmputationLevel, LimbSide shown |
| 6.4 | Verify template card content | Назва шаблону, Опис, Версія, Орієнтовна тривалість, Кількість етапів, Індикатор, Кнопка "Обрати" |
| 6.5 | Click "Обрати" on a template | Instance created → Navigate to Screen 7 (Tech Card Overview) |
| 6.6 | Click "Скасувати" | Confirmation → Clear session → Return to Dashboard |
| 6.7 | Click "Назад" | Return to Screen 5, all selections preserved |

**Template Filtering Verification:**
- 6.8 | Verify inappropriate templates hidden | No lower limb templates for upper limb prescription | Filtering works correctly

---

### Phase 4: Process Execution (Screens 7-8)

#### Screen 7: Technology Card Overview

| Step | Action | Expected Result |
|------|--------|-----------------|
| 7.1 | Verify left panel - Structure Tree | Stage 1 → Step 1.1, 1.2; Stage 2 → ... displayed as expandable tree |
| 7.2 | Verify center panel - BPMN Diagram | Stages as rectangles, Quality Gates as diamonds, arrows showing transitions, rework loops as reverse arrows |
| 7.3 | Verify right panel - Metadata | Process name, patient, order, assignee, progress indicator |
| 7.4 | Click "Розпочати процес" | Process status → In Progress → Navigate to Screen 8 (first step) |
| 7.5 | Click "Назад" | Return to Screen 6 (Template Selection) |

#### Screen 8: Wizard Execution (Key Screen)

**Header Verification:**
| Step | Action | Expected Result |
|------|--------|-----------------|
| 8.1.1 | Verify sticky header content | Process name, Patient PIB, Order №, Status, Live Timer |
| 8.1.2 | Verify timer starts automatically | Timer counting up from 00:00:00 | Timer functional |
| 8.1.3 | Verify progress bar | Shows "X/Y" (stage/step number) with visual indicator | Progress accurate |

**Main Content Area:**
| Step | Action | Expected Result |
|------|--------|-----------------|
| 8.2.1 | Verify instruction text | Technology card text for current step displayed |
| 8.2.2 | Verify web elements rendered | Correct control types for step type (checkbox, numeric, dropdown, etc.) |
| 8.2.3 | Verify field order | Fields displayed top-to-bottom as configured in Order Index |
| 8.2.4 | Verify "Готово" button initial state | Disabled until all required elements completed | Hard Block active |

**Step Type: information**
| Step | Action | Expected Result |
|------|--------|-----------------|
| 8.3.1 | Verify "Ознайомлено" checkbox present | Checkbox visible |
| 8.3.2 | Check the checkbox | Checkbox marked, "Готово" becomes enabled |

**Step Type: measurement**
| Step | Action | Data | Expected Result |
|------|--------|------|-----------------|
| 8.4.1 | Enter valid measurement | Value within min/max | Field normal/green |
| 8.4.2 | Enter measurement below min | Value < min | Field turns red, tooltip: "Значення має бути в межах X-Y мм" |
| 8.4.3 | Enter measurement above max | Value > max | Field turns red, tooltip shown |
| 8.4.4 | Enter non-numeric value | "abc" | Field rejects input or shows validation error |

**Step Type: checklist**
| Step | Action | Expected Result |
|------|--------|-----------------|
| 8.5.1 | Verify multiple checkboxes | All required items visible |
| 8.5.2 | Check some but not all required | "Готово" remains disabled |
| 8.5.3 | Check all required items | "Готово" becomes enabled |

**Step Type: media**
| Step | Action | Expected Result |
|------|--------|-----------------|
| 8.6.1 | Verify upload control | File upload button/area present |
| 8.6.2 | Upload file with invalid type | Error: file type not accepted |
| 8.6.3 | Upload file exceeding size limit | Error: file too large |
| 8.6.4 | Upload valid file | File appears in list, preview if image |
| 8.6.5 | Upload minimum required files | "Готово" becomes enabled (if other requirements met) |

**Step Type: selection**
| Step | Action | Expected Result |
|------|--------|-----------------|
| 8.7.1 | Verify dropdown/radio options | Options loaded from dictionary |
| 8.7.2 | Make a selection | Selection highlighted, validation passes |
| 8.7.3 | Verify required selection blocks completion without selection | "Готово" disabled until selection made |

**Resources Panel:**
| Step | Action | Expected Result |
|------|--------|-----------------|
| 8.8.1 | Verify materials table | Columns: Матеріали, Кількість, Одиниця виміру, Компоненти, Витрачений час |
| 8.8.2 | Verify materials autocomplete | Type-ahead from dictionary works |

**Bottom Panel Controls:**
| Step | Action | Expected Result |
|------|--------|-----------------|
| 8.9.2 | Click "Пауза" | Reason dialog opens |
| 8.9.2a | Select reason "Очікування пацієнта" | Timer stops, status → Paused |
| 8.9.2b | Select reason "Відсутні матеріали" | Timer stops, status → Paused: Material |
| 8.9.2c | Select reason "Технологічний простій" | Timer stops, status → Paused |
| 8.9.3 | Click "До головного меню" | Confirmation dialog → Exit without completing → Dashboard |
| 8.9.4 | Click "Готово" (when enabled) | Validation runs → Step status → Completed → Next step loads |
| 8.9.5 | Click "Назад" (when enabled) | Return to previous step (if allowed by template) |

**Backward Navigation Rules:**
- 8.9.6 | Attempt "Назад" on first step | Button disabled (no previous step)
- 8.9.7 | Attempt "Назад" after completing step | Button disabled (backward not allowed after completion)

---

### Phase 5: Quality Control & Branching (Screens 9-10)

#### Screen 9: Quality Gate

| Step | Action | Expected Result |
|------|--------|-----------------|
| 9.1 | Verify Quality Gate displays after stage completion | Screen 9 loads with stage-specific gate |
| 9.2 | Verify gate name and description | Name and criteria description displayed |
| 9.3 | Verify checklist items | All criteria listed as checkboxes |
| 9.4 | Verify attachments section | File upload area for photos/scans |
| 9.5 | Attempt "Схвалити" with unchecked items | Button disabled or error |
| 9.6 | Check all required items | "Схвалити" becomes enabled |
| 9.7 | Click "Схвалити" (Pass) | Gate passed → Timestamp recorded → Next stage loads |
| 9.8 | Click "Відхилити" (Fail) | Dialog opens with options |

#### Screen 10: Rejection Dialog

| Step | Action | Expected Result |
|------|--------|-----------------|
| 10.1 | Verify dialog options | "Повернути на доопрацювання" and "Позначити процес як провалений" |
| 10.2 | Verify "Причина відхилення" field | Required text field present |
| 10.3 | Click "Повернути на доопрацювання" without reason | Validation error |
| 10.4 | Enter reason, click "Повернути на доопрацювання" | Rework loop created → Step status → Rework → Return to target step |
| 10.5 | Click "Позначити процес як провалений" with reason | Failure Snapshot created → Process status → Failed → Screen 13 |

**Rework Loop Verification:**
| Step | Action | Expected Result |
|------|--------|-----------------|
| 10.6 | Complete rework step | Status → In Progress |
| 10.7 | Re-enter Quality Gate | Gate displays again |
| 10.8 | Exceed max rework attempts | Auto-fail: "Перевищено ліміт доопрацювань" |

---

### Phase 6: Completion & Archiving (Screens 11-15)

#### Screen 15: Successful Completion

| Step | Action | Expected Result |
|------|--------|-----------------|
| 15.1 | Verify completion screen displays | After final Quality Gate Pass |
| 15.2 | Verify full process information | Patient, order, process details shown |
| 15.3 | Verify completed stages list | Each stage with timestamp |
| 15.4 | Verify Quality Gate results | All gates shown with Pass status |
| 15.5 | Verify summary resources | Materials + labor totals |
| 15.6 | Click "Експорт PDF" | PDF report downloads |
| 15.7 | Click "Повернутися до панелі управління" | Redirect to Dashboard, process removed from active list |

#### Screens 13-14: Failed Process Handling

| Step | Action | Expected Result |
|------|--------|-----------------|
| 13.1 | Verify Failure Snapshot displays | Process info, date, cause, completed stages, audit log, resources |
| 13.2 | Click "Експорт звіту (PDF)" | Failure report downloads |
| 13.3 | Click "Створити Замінювальний процес" | New process pre-filled with same order data |
| 13.4 | Click "Повернутися на Dashboard" | Redirect to Dashboard, process in "Провалені" filter |

---

## Validation Rules Summary

### Hard Block Rules
| Rule | Implementation | Verification |
|------|----------------|--------------|
| Cannot skip steps | "Готово" disabled until all required elements complete | Attempt completion with incomplete fields |
| Cannot skip stages | Progression only after Quality Gate Pass | Verify forward navigation blocked |
| Cannot proceed without selection | "Далі" disabled on Screens 3-6 until selection made | Verify button states |
| Cannot complete step with invalid data | Field-level validation blocks completion | Enter out-of-range values |

### Field Validation Rules
| Field Type | Validation | Error Message |
|------------|------------|---------------|
| numeric_input | min_value ≤ x ≤ max_value | "Значення має бути в межах X-Y [unit]" |
| text_input | regex_pattern match | Format-specific message |
| text_input | max_length | Character limit exceeded |
| file_upload | mime_types | "Недопустимий формат файлу" |
| file_upload | max_size | "Розмір файлу перевищує ліміт" |
| image_upload | min_count | "Мінімум X фото необхідно" |
| selection | required | "Оберіть значення зі списку" |

### Status Color Coding
| Status | Color |
|--------|-------|
| New | Gray |
| In Progress | Blue |
| Paused | Yellow |
| Blocked: Patient | Orange |
| Blocked: Material | Orange |
| Waiting for Review | Violet |
| Correction | Red |
| Failed QC | Red |
| Completed | Green |
| Failed | Dark Red |

---

## Edge Cases & Error Scenarios

### Session Management
| Scenario | Expected Behavior |
|----------|-------------------|
| Refresh page during Phase 3 (F5) | Session context lost → Redirect to Dashboard with message "Переривання сесії" |
| Browser back button | Navigate back, context preserved |
| Multiple tabs | Each tab independent or shared session (define behavior) |
| Direct URL access without session | Redirect to appropriate screen |

### Concurrent Access
| Scenario | Expected Behavior |
|----------|-------------------|
| Two users edit same process | Optimistic locking: second save gets 409 Conflict |
| Process signed by nurse during editing | Error: "Документ заблокований підписом" |

### Network Failures
| Scenario | Expected Behavior |
|----------|-------------------|
| Connection lost during save | Draft saved locally, sync when restored |
| Timeout on API call | Error message, retry option |

---

## Bug/Discrepancy Report Template

| Field | Description |
|-------|-------------|
| Bug ID | Unique identifier (BUG-XXX) |
| Severity | Critical / Major / Minor |
| Screen | Screen number and name |
| Step | Step number from this plan |
| Expected Behavior | From specification |
| Actual Behavior | Observed behavior |
| Screenshot | Attach at moment of bug |
| Console Errors | Network/JS errors |
| Reproducibility | Steps to reproduce |

---

## Documentation Issues Found

| Issue | Description | Location |
|-------|-------------|----------|
| DOC-001 | Screen numbering inconsistent (Screens 3-6 referenced as Phase 3 but Screen 5 not numbered in text) | Section 2.3 |
| DOC-002 | "Скасувати" button behavior on Screen 6 not fully specified | Section 2.3.4 |
| DOC-003 | Session storage mechanism not specified (sessionStorage vs server) | Section 2.3 |
| DOC-004 | Exact fields for Failure Snapshot not enumerated | Section 2.6.2 |
| DOC-005 | "Старт" button enabled condition - "PDF loaded" vs other conditions | Section 2.3.3 |

---

## Test Execution Checklist

- [ ] Phase 1: Authorization (5 steps + 1 alt)
- [ ] Phase 2: Dashboard (6 steps)
- [ ] Phase 3: Patient Selection (10 steps)
- [ ] Phase 3: Order Selection (8 steps)
- [ ] Phase 3: Order Review (11 steps)
- [ ] Phase 3: Template Selection (8 steps)
- [ ] Phase 4: Tech Card Overview (5 steps)
- [ ] Phase 4: Wizard Execution (30+ steps)
- [ ] Phase 5: Quality Gate (8 steps)
- [ ] Phase 5: Rejection Dialog (8 steps)
- [ ] Phase 6: Completion (7 steps)
- [ ] Phase 6: Failure Handling (4 steps)
- [ ] Edge Cases (10 scenarios)
- [ ] Validation Rules (15 rules)

**Total Steps:** 150+

---

*Document Version: 1.0*
*Created: 2026-08-06*
*Specification Reference: Process Control System for the Manufacture of Prosthetics v1.0*
