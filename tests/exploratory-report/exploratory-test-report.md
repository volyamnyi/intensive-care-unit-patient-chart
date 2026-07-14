# Exploratory Functional Testing Report

**Date:** 2026-07-14T11:08:21.014Z
**App:** ICU Patient Chart

## Summary

| Status | Count |
|--------|-------|
| ✅ PASS | 41 |
| ❌ FAIL | 0 |
| ℹ️ INFO | 8 |
| ⚠️ WARN | 0 |
| **Total** | **49** |

## Login

| # | Step | Status | Detail |
|---|---|---|---|
| 1 | Login page title | ✅ | Title visible |
| 2 | Login field | ✅ |  |
| 3 | Password field | ✅ |  |
| 4 | Submit button | ✅ |  |
| 5 | Invalid credentials error | ✅ | MUI Alert with error text visible |
| 6 | Doctor login | ✅ | Redirected to /doctor |

## Dashboard

| # | Step | Status | Detail |
|---|---|---|---|
| 1 | Active patients title | ✅ |  |
| 2 | Patients table visible | ✅ |  |
| 3 | Table has 3 rows | ✅ | Петренко Іван Сергійович12.07.2026-АктивнийВідкрити | Коваленко Олена Вікторівна13.07.2026-АктивнийВідкрити | Сидоренко  |
| 4 | Search Коваленко | ✅ | Rows: 1 |
| 5 | Navigate to /doctor/create-card | ✅ | Reached create-card page |

## CreateCard

| # | Step | Status | Detail |
|---|---|---|---|
| 1 | Short search (<2 chars) | ✅ |  |
| 2 | Search Бондаренко | ✅ | MIS returned Бондаренко |

## Episode

| # | Step | Status | Detail |
|---|---|---|---|
| 1 | Open episode | ✅ | Opened: http://localhost:5173/doctor/episode/a1111111-1111-1111-1111-111111111 |

## Episode Tabs

| # | Step | Status | Detail |
|---|---|---|---|
| 1 | Found 5 tabs | ✅ |  |
| 2 | Tab "Вітальні показники" clicked | ✅ |  |
| 3 | Tab "Призначення" clicked | ✅ |  |
| 4 | Tab "Шкали" clicked | ✅ |  |
| 5 | Tab "Нотатки" clicked | ✅ |  |
| 6 | Tab "Баланс рідини" clicked | ✅ |  |

## SignOff

| # | Step | Status | Detail |
|---|---|---|---|
| 1 | Sign dialog | ✅ |  |
| 2 | Cancel | ✅ |  |

## Navigation

| # | Step | Status | Detail |
|---|---|---|---|
| 1 | Back button | ✅ |  |

## Nurse

| # | Step | Status | Detail |
|---|---|---|---|
| 1 | Login | ✅ | Redirected to /nurse |
| 2 | Dashboard title | ✅ |  |
| 3 | Search Сидоренко | ✅ | Rows: 1 |
| 4 | Open episode | ✅ |  |
| 5 | Episode tabs: 5 | ✅ |  |
| 6 | Vitals form labels: 14 | ✅ |  |
| 7 |   Label: "АТ сист (мм.рт.ст)" | ℹ️ |  |
| 8 |   Label: "АТ діас (мм.рт.ст)" | ℹ️ |  |
| 9 |   Label: "ЧСС (в 1 хв)" | ℹ️ |  |
| 10 |   Label: "SpO2 (%)" | ℹ️ |  |
| 11 |   Label: "Темп. тіла (°С)" | ℹ️ |  |
| 12 |   Label: "ЦВТ (мм.вод.ст)" | ℹ️ |  |
| 13 |   Label: "ЧД (в 1 хв)" | ℹ️ |  |
| 14 |   Label: "Свідомість" | ℹ️ |  |
| 15 | Save vitals | ✅ | Vitals filled and saved |
| 16 | Fluid balance tab clicked | ✅ |  |

## HOD

| # | Step | Status | Detail |
|---|---|---|---|
| 1 | Login | ✅ | Shared /doctor route |
| 2 | Can see "Нова карта" | ✅ |  |
| 3 | Open episode | ✅ |  |

## Admin

| # | Step | Status | Detail |
|---|---|---|---|
| 1 | Login | ✅ | Redirected to /admin |
| 2 | Title "Користувачі системи" | ✅ |  |
| 3 | Doctors section | ✅ |  |
| 4 | Nurses section | ✅ |  |

## AccessCtrl

| # | Step | Status | Detail |
|---|---|---|---|
| 1 | Admin accessing /nurse | ✅ | Redirected to /admin |
| 2 | Admin accessing /doctor | ✅ | Redirected to /admin |

## MIS Search

| # | Step | Status | Detail |
|---|---|---|---|
| 1 | Search Ткачук | ✅ | Found in MIS results |

---

# Comprehensive Functional Test Plan

## Use Case 1: Authentication

| ID | Test Case | Steps | Expected | Priority |
|---|---|---|---|---|
| TC-01 | Login as doctor | /login → doctor1/doctor123 → submit | → /doctor | Critical |
| TC-02 | Login as nurse | /login → nurse1/nurse123 → submit | → /nurse | Critical |
| TC-03 | Login as HOD | /login → head1/head123 → submit | → /doctor | Critical |
| TC-04 | Login as admin | /login → admin/admin123 → submit | → /admin | Critical |
| TC-05 | Invalid credentials | wrong/wrong → submit | Error "Невірний логін або пароль" | Critical |
| TC-06 | Unauthenticated redirect | /doctor without token | → /login | Critical |
| TC-07 | Logout | user menu → Вийти | → /login | High |
| TC-08 | Access: admin→/nurse | admin navigates to /nurse | Redirected to /admin | High |
| TC-09 | Access: admin→/doctor | admin navigates to /doctor | Redirected to /admin | High |
| TC-10 | Access: nurse→/doctor | nurse navigates to /doctor | Redirected or 403 | High |

## Use Case 2: Doctor Dashboard

| ID | Test Case | Expected | Priority |
|---|---|---|---|
| TC-11 | Dashboard loads with episodes | Table with Петренко, Коваленко, Сидоренко | Critical |
| TC-12 | Search filters table | Type "Коваленко" → only matching row | High |
| TC-13 | Clear search restores list | Clear field → all rows back | Medium |
| TC-14 | Search non-existent | "ZZZ" → empty state | Medium |
| TC-15 | "Нова карта" navigates | → /doctor/create-card | High |
| TC-16 | "Відкрити" opens episode | → /doctor/episode/:id | Critical |
| TC-17 | Page title | "ВАІТ — Лікар" | Low |

## Use Case 3: Create Card

| ID | Test Case | Expected | Priority |
|---|---|---|---|
| TC-18 | Create card for Бондаренко | Search → select → create → episode page | Critical |
| TC-19 | Short search validates | 1 char → "Введіть мінімум 2 символи" | High |
| TC-20 | Cancel returns to dashboard | Click "Скасувати" → /doctor | Medium |
| TC-21 | Patient details shown | Name, sex, blood group | High |

## Use Case 4: Episode Page

| ID | Test Case | Expected | Priority |
|---|---|---|---|
| TC-22 | All tabs render | Показники, Призначення, Шкали, Нотатки, Баланс рідини | Critical |
| TC-23 | Tab switching | Click each tab → content changes | High |
| TC-24 | Back to dashboard | Click "Назад" → dashboard | High |

## Use Case 5: Vitals

| ID | Test Case | Expected | Priority |
|---|---|---|---|
| TC-25 | Enter & save vitals | Fill SYS/DIA/HR/SpO2/Temp → save → confirmation | Critical |
| TC-26 | Numeric validation | type=number, min/max attributes | High |
| TC-27 | Out-of-range | SYS=500 → validation | Medium |
| TC-28 | Empty submission | Click save empty → validation | Medium |
| TC-29 | Different hour slot | Select hour → save independently | High |

## Use Case 6: Prescriptions

| ID | Test Case | Expected | Priority |
|---|---|---|---|
| TC-30 | Create prescription | Fill drug/dose/route/unit/freq/start → save → "Активне" | Critical |
| TC-31 | Cancel creation | Click cancel → form closed | Medium |
| TC-32 | Empty form validation | Save empty → errors | High |

## Use Case 7: Notes

| ID | Test Case | Expected | Priority |
|---|---|---|---|
| TC-33 | Add note | Type text → "Додати нотатку" → note in list | Critical |
| TC-34 | Empty note | Click add without text → error | Medium |

## Use Case 8: Fluid Balance

| ID | Test Case | Expected | Priority |
|---|---|---|---|
| TC-35 | Tab components | "Баланс рідини" heading + intake/output/balance | High |
| TC-36 | Recalculate | Click recalculate → balance updated | High |
| TC-37 | Balance = intake - output | Enter vitals → recalculate → correct | Medium |

## Use Case 9: Scales

| ID | Test Case | Expected | Priority |
|---|---|---|---|
| TC-38 | Scales tab | List of available scales | High |
| TC-39 | APACHE II result | Fill params → save → score | High |
| TC-40 | SOFA result | Same → score saved | High |

## Use Case 10: Sign-off

| ID | Test Case | Expected | Priority |
|---|---|---|---|
| TC-41 | Nurse signs | Click sign → NURSE_SIGNED | Critical |
| TC-42 | Doctor signs after nurse | Sign NURSE_SIGNED day → DOCTOR_SIGNED | Critical |
| TC-43 | Cancel dialog | Click cancel → day unchanged | Medium |
| TC-44 | Doctor signs before nurse | Sign OPEN day → warning | High |
| TC-45 | Read-only warning | Dialog text about read-only after sign | High |

## Use Case 11: Order Execution

| ID | Test Case | Expected | Priority |
|---|---|---|---|
| TC-46 | View active orders | Prescriptions tab → list | High |
| TC-47 | Execute order | Click "Виконати" → status updated | High |

## Use Case 12: Nurse Dashboard

| ID | Test Case | Expected | Priority |
|---|---|---|---|
| TC-48 | Dashboard loads | "Активні пацієнти" + table | Critical |
| TC-49 | Search | Type → filtered | High |
| TC-50 | Open episode | → /nurse/episode/:id | Critical |
| TC-51 | Title | "ВАІТ — Медсестра" | Low |

## Use Case 13: Admin Dashboard

| ID | Test Case | Expected | Priority |
|---|---|---|---|
| TC-52 | Page loads | "Користувачі системи" | Critical |
| TC-53 | Doctors table | doctor1, doctor2 visible | High |
| TC-54 | Nurses table | nurse1, nurse2 visible | High |
| TC-55 | Title | "ВАІТ — Адміністратор" | Low |

## Use Case 14: Routing

| ID | Test Case | Expected | Priority |
|---|---|---|---|
| TC-56 | / redirect by role | Doctor→/doctor, Nurse→/nurse, Admin→/admin | High |
| TC-57 | Direct /doctor/create-card | Page loads correctly | High |
| TC-58 | Direct /nurse/episode/:id | Episode page loads | High |

## Use Case 15: Error Handling

| ID | Test Case | Expected | Priority |
|---|---|---|---|
| TC-59 | Optimistic locking conflict | Two tabs save same episode → 409 | Medium |
| TC-60 | Invalid episode ID | /doctor/episode/bad-id → error/redirect | Medium |
