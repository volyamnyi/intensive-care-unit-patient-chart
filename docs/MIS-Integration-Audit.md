# MIS Integration Audit — дві реалізації `MisService` (issue #191)

**Дата:** 2026-08-24 · **Статус:** ЗАВЕРШЕНО (фази #191–#194). WireMockMisServiceImpl — єдина реалізація MisService.

`MisService` має дві реалізації: `MockMisServiceImpl` (in-memory, дефолт у dev/CI через
`app.mis.mock-enabled: true`) та `WireMockMisServiceImpl` (HTTP → WireMock-фікстури,
активується через `app.mis.wiremock-enabled=true`, `matchIfMissing=true`). Фікстури в
`backend/common/src/main/resources/mis-wiremock/__files/` — єдине джерело правди щодо даних.

## Таблиця розбіжностей

| Метод | Mock | WireMock | Ризик / план |
|---|---|---|---|
| `searchPatients` | 1001–1050 + 2001–2040 (хірургія/реабілітація з room/bed/doctor) | `patients_52.json`: 1001–1050 + 900001/900002; фільтр fullName/externalId1/phone | Пресcription dashboard втратить 40 пацієнтів при перемиканні → #192 (розширити фікстуру) або #193 |
| `getPatient` | map lookup | patientList + фільтр по id | Паритетний ✓ |
| `getHospitalization` | knownHospitalizationIds set; дані з patients/departments | scheduleList[0]; **departmentId hardcoded 1L**, diagnosis/room/bed константи; silent-fallback empty на будь-який виняток клієнта | Маскує помилки інтеграції; hardcoded departmentId розійдеться з dept-id від hashCode → #192 |
| `getUser` | точний матч по id у users-map | запит `UserLogin="user"+userId`, потім фільтр по userID — залежить від фікстур userLogin | Працює лише якщо fixture login = "user"+id; зараз у фікстурі logins doctor1/nurse1… → **не знаходить** для id 11–16. → #192 (виправити запит на UserID) |
| `getDepartmentUsers` | фільтрує за departmentId | повертає всіх (ігнорує параметр) | Протезування (`MisOrderTemplateDataService`) отримає зайвих юзерів → #192 додати departmentId до фікстур і фільтр |
| `getDepartments` | стабільні id 1/2 | `hashCode(companyGUID)` — нестабільні між JVM-ранами (рядковий хеш стабільний у Java, але семантика інша) | Послуги/відділення в протезуванні прив'язані до id → #192 узгодити |
| `getDictionary` orderCategories/noteTypes/consciousness | `MisDictionaries.*` | `MisDictionaries.*` | Ідентично ✓ (винесено в спільні константи, parity-test) |
| `getDictionary` bookingStatus/paymentStatus/scheduleStatus | `List.of()` (немає даних) | парсинг відповідних фікстур | Mock бідніший → некритично (протезні шляхи під WireMock) |
| `sendPdf` | true + аудит | true + аудит | Ідентично ✓ |
| `searchMedicineCatalog` | 20 препаратів in-memory | `List.of()` + лог «Use MockMIS mode» | Медпошук мертвий під WireMock → **#192** (parity-test `@Disabled` активується тоді) |
| `getPatientAllergies` | 1001: Penicillin/Aspirin; 1002: Iodine | `List.of()` | Алергічні попередження мертві під WireMock → **#192** |
| `getServices` / `getPatientBookings` / `getPatientDocuments` / `getPatientInfo` | `List.of()`/empty | парсинг фікстур service_list/booking_list/document_list/patient_info | Mock бідніший → протезування працює тільки під WireMock; ок |
| `setErrorMode` / `checkErrors` | дубльований код | дубльований код | Винесено в `MisErrorSimulator` (ця фаза) |

## Конфігураційні ризики

- Обидва прапорці `true` → `NoUniqueBeanDefinitionException` без fail-fast валідації → #193.
- `matchIfMissing = true` на wiremock-enabled: якщо вимкнути mock без явного `wiremock-enabled=true`,
  WireMock активується мовчки → #193 зробити явним.
- CI (`.github/workflows/playwright.yml`) не піднімає WireMock-сервер — E2E працює на Mock;
  протезні E2E отримують демографію 900001/900002 через local-fallback `ProstheticsPatientService`.

## Покриття фікстур ↔ тестів (ця фаза)

| Фікстура (`__files/`) | Юніт-тест WireMock | Parity-тест |
|---|---|---|
| `patients_52.json` | searchPatients/getPatient | кожен спільний пацієнт 1001–1050: id/name/birthDate/sexCode/extId1/2/phone/email |
| `user_details.json` | getUser/getDepartmentUsers | ids 11–16: login + specialityCode |
| `company_details.json` | getDepartments (hashCode-гілка) | — (id-семантика задокументована як розбіжність) |
| `*_status_dictionary.json` ×3 | getDictionary booking/payment/schedule | — |
| `service_list.json` | getServices | — |
| `booking_list.json` | getPatientBookings (дати, quantity) | — |
| `document_list.json` | getPatientDocuments | — |
| `patient_info.json` | getPatientInfo (account/bookings/debt, null-поля) | — |
| `patient_schedule.json` | getHospitalization hit / empty / exception-fallback | — |
| medicine/allergies | — (WireMock повертає порожньо) | `@Disabled` parity до #192 |

## Фази виконання

- **#192** — закрити розриви: медицина/алергії у WireMock, departmentId-фільтр, getUser-запит,
  розширити patients_52.json або звузити mock.
- **#193** — ✅ WireMock дефолт; fail-fast; embedded-режим; matchIfMissing=false.
- **#194** — ✅ MockMisServiceImpl видалено; тести мігровано; pom-гігієна виконана.
