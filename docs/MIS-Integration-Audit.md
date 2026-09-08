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

## Real-API gap — перехід на справжній MIS API (епік, issues #254–#268)

**Дата:** 2026-09-07 · **Статус:** планування завершено, виконання йде (трекінг — issues #254–#268).

### Поточна архітектура (факт станом на старт епіка)

```text
Controllers (PatientController, PrescriptionController, Prosthetics*Controller, …)
   ↓
MisService (14 методів — спільний контракт, backend/common/.../mis/MisService.java)
   ↓
WireMockMisServiceImpl (ЄДИНА реалізація, 546 рядків)
   ↓
MisApiClient.callMethod() → POST {app.mis.wiremock-url}/api/run
   {name: "spzIB*", params: [... + {Login: "integration"}], installationId: "0000…"}
   ↓
Embedded WireMock :9090 (MisEmbeddedWireMockConfig, 14 стабів) або зовнішній URL
```

Автентифікації до MIS немає взагалі: статичні `app.mis.login=integration` +
`app.mis.installation-guid=00000000-0000-0000-0000-000000000000` у кожному запиті
(`MisApiClient.java:32-60`). Єдиний MIS env — `APP_MIS_WIREMOCK_URL`. `spi*`-процедур
у коді немає (лише FORBIDDEN-згадки в `stub_mapping.json`).

### Цільова архітектура

```text
MIS API (real)
   ↓  Bearer-токен (MisAuthService, APP_MIS_API_* — тільки імена env, без значень)
Patient Integration Service: getAllPatientsUnderTreatment() — ЄДИНЕ джерело patient data
   ↓
Application Use Cases
   ├── Prosthetics: departmentId ∈ {19,27,37} AND EXISTS document.templateId ∈ {120,121}
   ├── Doctor's Orders Sheet: departmentId ∈ {19,37} (27 — ніколи)
   └── Intensive Care: departmentId = 19
   ↓
React Frontend (repo Shadcn-примітиви, без нових runtime-залежностей)
   ↓
Unit + Integration + Playwright E2E → Green CI (6/6)
```

### Ключові розбіжності плану з реальністю (враховано в issues)

- `departmentId` 19/27/37 **немає ніде в репозиторії**: реальні companyID 1–6
  (`company_details.json`), локальний mock `patientDepartmentID` 1/2, `prescription_lists.department_id`
  1/2. Єдиний department-фільтр — клієнтський `p.departmentId === 2/1` у
  `PrescriptionPage.tsx:48-93` та `NursePrescriptionPage.tsx:43-69`. Мапінг 19/27/37 —
  блокер (питання (b)), відповідь очікується від власника MIS.
- `documentTemplateId` 120/121 — лише у `mis-wiremock/__files/document_list.json`
  (120 = «Замовлення на протези верхніх кінцівок», **121 = «Висновок лікаря», НЕ «нижні
  кінцівки»**). Frontend про 120/121 нічого не знає; REST їх не віддає. Фікстурну
  семантику в прод-код не копіювати до підтвердження (питання (c)).
- `documentUrl` — лише локальний `URL.createObjectURL(blob)` згенерованого сервером
  рецепта (`OrderReviewPage.tsx:42,223-230`); бекенд-поля немає.
- Замовлення протезування — локальна БД (`data-prosth.sql`: `PR-2026-0001/0002`,
  пацієнти 900001/900002); MIS дає лише збагачення PDF-рецепта
  (`MisOrderTemplateDataService.load()` — 6 викликів на пацієнта, прототип N+1).
- Читання каталогу ліків іде повз кеш (`PrescriptionController:285` напряму в MIS;
  `MedicineCatalogService` ніхто не читає); у `MedicineSearchInput.tsx:8-25` —
  hardcoded fallback-каталог (прод-мок).
- Allergy-флоу (`spzIBPatientAllergy`) не має `spi`-еквівалента в плані — рішення
  власника: **видалити без винятків** (Phase 5, #258; потрібен sign-off власника
  продукту — клінічна безпека).

### Таблиця «spz → рішення» (чинна; рішення: видалити всі `spz` без винятків)

| `spz`-метод | `MisService`-метод | Рішення | Фаза |
|---|---|---|---|
| `spzIBPatientSearch` | `searchPatients/getPatient` | Заміна → `spiPatientProsthesCheck` | #256 |
| `spzIBDocumentList` | `getPatientDocuments` | Заміна → `spiDocumentProsthesCheck` | #257 |
| `spzIBMedicineDictionary` | `searchMedicineCatalog` | Заміна → `spiMedicineItemKindDetails` | #258 |
| `spzIBUserDetails` | `getUser/getDepartmentUsers` | Видалення + споживачі (`GET /api/users/{id}`, `resolveUserName`, `UserMisDTO`) | #264 (підготовка #257) |
| `spzIBCompanyDetails` | `getDepartments` | Видалення + споживачі (`company[0]`, `DepartmentDTO`) | #264 |
| `spzIBPatientScheduleList` | `getHospitalization` | Видалення + споживачі (`HospitalizationDTO`) | #264 |
| `spzIBPatientAllergy` | `getPatientAllergies` | Видалення цілого allergy-флоу | #258 |
| `spzIBServiceList/BookingList/PatientInfo` | `getServices/getPatientBookings/getPatientInfo` | Видалення + збагачення рецепта | #259 |
| 3 словники статусів | `getDictionary(name)` | Видалення фікстурних гілок | #264 |
| `spzIBVenueDetails` | — (без споживача) | Видалення без заміни | #264 |

Підсумковий `MisService`: `getAllPatientsUnderTreatment/searchPatients/getPatient/
getPatientDocuments/searchMedicineCatalog/sendPdf` (`sendPdf` — не `spz`, лишається
як єдиний дозволений write). Критерій Phase 11: `grep -r "spzIB"` — 0 збігів у живому коді.

### Відкриті питання до власника MIS (блокери)

(a) спека real API: base URL, `/token`+`/run`, формати `spiPatientProsthesCheck` /
`spiDocumentProsthesCheck` / `spiMedicineItemKindDetails`, помилки, таймаути, rate limits;
(b) мапінг відділень 19/27/37 проти реальних ID; (c) семантика 120/121, окремий метод
для 120, URL-vs-байти; (d) dual-mode vs hard cutover + credentials у CI (раннери
корпоративний MIS не бачать — real-сюїти локально з `SKIPPED`, прецедент LDAP);
(e) кеш ліків — читати з TTL чи видалити — RESOLVED у #258 (кеш видалено: live-read + `med/002-drop-allergy-and-medicine-cache.sql`); (f) batch-метод документів / TTL кешу eligibility.

### Послідовність

```text
#254 (audit, docs) → #255 (dual-mode config+auth)
  → #256 (patients) ─┐
  → #257 (documents) ─┼──→ #259 (eligibility) ──→ #262 (prosthetics UI)
  → #258 (medicines) ─┘
  → #260 (orders-sheet UI) ─┐
  → #261 (ICU UI) ──────────┼──→ #264 (cutover/cleanup)
  → #262, #263 (medicine UI) ┘
  → #265 (unit) → #266 (integration) → #267 (E2E) → #268 (regression/CI)
```

### Phase 3 — patients (issue #256, DONE)

- `MisService.getAllPatientsUnderTreatment()` — єдине базове джерело; real-режим
  викликає `spiPatientProsthesCheck` (константа `SPI_PATIENT_PROCEDURE`), wiremock —
  `spzIBPatientSearch` до cutover.
- Адаптери (рішення): у real-режимі `searchPatients`/`getPatient` делегують
  `getAllPatientsUnderTreatment()` з тією самою клієнтською фільтрацією, що й раніше
  (семантика `query` незмінна); `spzIBPatientSearch` у real-гілці мертвий.
- Мапінг: усі 12 полів цілі; парсер добудовано (`patientHeight/Weight/BloodGroup/
  RhFactor` раніше губилися; толерантний fallback-масив `patients`).
- Контракт токен-ендпоїнта лишається припущенням (блокер (a) відкритий) — зміниться
  лише DTO-мапінг, шов готовий.

### Phase 4 — documents (issue #257, DONE)

- `getPatientDocuments(patientId)` у real-режимі викликає `spiDocumentProsthesCheck`
  (константа `SPI_DOCUMENT_PROCEDURE`) з параметром `PatientID`; wiremock —
  `spzIBDocumentList` до cutover.
- `DocumentMisDTO` розширено до цільової форми: `documentUrl`, `patientId`,
  `orderDate`, `patientFullName/Address`, `productCode/Name`, `mobilityLevel`,
  `patientGender`, `age/height/weight`, `note` (усі nullable; аліаси ключів і fallback-
  масив `documents` — толерантно, бо спека (a) відкрита).
- Рішення «верхні кінцівки»: окремого existing-методу для template 120 не знайдено —
  діє універсальний retrieval (обидва шаблони одним викликом); фільтр 120/121 — Phase 6.
- Рішення `documentUrl`: поле прокинуто до DTO для Phase 9 (iframe/download + fallback
  на локальний рецепт); одноразовість/інтранет-доступність — перевірити у Phase 9.

### Phase 5 — medicines (issue #258, code-complete + CI green, OPEN pending owner sign-off/close)

- `searchMedicineCatalog(keyword)` у real-режимі викликає `spiMedicineItemKindDetails`
  (константа `SPI_MEDICINE_PROCEDURE`), wiremock — `spzIBMedicineDictionary` до cutover.
- `MedicineMisDTO` розширено з 4 до 16 полів (legacy `id/name/categoryRef/ptgCode` +
  `itemKindCode/Atc/Unit/Manufacturer/IsDisabled/Ean/IsDivisible/Dlc` +
  `medicineCategoryId/Name` + `medicinePackageId/Name`); усі nullable, толерантні аліаси
  (`itemKindATC`/`itemKindAtc` тощо) + fallback-масиви `medicineList`/`medicines`/`items` +
  хелпер `booleanOrNull` — бо спека (a) відкрита, точний перелік полів підтвердиться з нею.
- Рішення (e) виконано: кеш видалено — `med/002-drop-allergy-and-medicine-cache.sql`
  (DROP `allergy_cache` + `medicine_catalog_cache` + індекси, повний rollback);
  `MisService.searchMedicineCatalog` читає live без кешу; `MedicineCatalogService` видалено.
- Allergy-флоу видалено end-to-end (endpoint `GET /allergies`, мапер, DTO, кеш-таблиці,
  фронтенд-гуард, fallback-каталог, фікстури, E2E-очікування); `spi`-еквівалента немає.
  Sign-off власника відстежується на issue #258 (клінічна безпека).
- `spzIBMedicineDictionary` + `spzIBPatientAllergy` у real-гілці мертві; контракт
  `medicine-catalog` не зламаний (CI run `34152000037` all 6 jobs green).

### Phase 6 — eligibility (issue #259, in progress)

- Новий `ProstheticsEligibilityService` (orchestration — тут, не в контролері/фронті):
  `getAllPatientsUnderTreatment()` (Phase 3) → dept-фільтр {19,27,37} → локальні ордери
  (`ProstheticsOrderService.list`, без ордерів — мовчки повз) → документи (Phase 4) →
  template-фільтр {120,121} → готові кандидати `{patient, orders[], documents[]`
  (з `documentUrl`), `documentsUnknown}`. Нові ендпоїнт `GET .../patients/candidates`
  (той самий `@PreAuthorize`, старі без змін) та поле `departmentId` у
  `ProstheticsPatientResponse` (джерело — `PatientDTO.departmentId`).
- Правила деградації: помилка документів одного пацієнта — `documentsUnknown=true`
  з порожніми документами (fail-open з явною позначкою, debug-лог без PII), список живе.
- N+1: batch-метода в MIS немає — обмежений паралелізм (common pool, детермінований
  порядок за MIS id) + TTL-кеш документів 5 хв (невдалі fetchі не кешуються).
  Усі три константи — припущення (b)/(c)/(f) в одному місці, клас задокументовано.
- П.6 Scope (від'єднання `MisOrderTemplateDataService`): виконувати нічого —
  сервіс і збагачення рецепта вже видалено у #257 follow-up; верифіковано grep-ом
  (нуль споживачів `getServices/getPatientBookings/getPatientInfo`). Ті три методи,
  парсери, DTO (`ServiceMisDTO`, `BookingMisDTO`, `PatientInfoMisDTO`) та їхні
  unit-тести видалено тут; runtime-стаби/фікстури лишаються до #264 (cutover),
  `stub_mapping.json` позначено `removed`.
- Без змін: інстанс-лайфсайкл, черги, PDF-трансфер, RBAC, фронтенд (споживання — Phase 9),
  E2E — немає (UI — Phase 9, повна міграція — Phase 14).

### Phase 7 — medication roster (issue #260, in progress)

- Backend: `GET /api/patients?module=medication` — той самий `searchPatients(query)`
  плюс `PatientModuleFilter` (19/37; невідомий модуль — 400, null-dept — повз).
  `?query=`-контракт без модуля незмінний; порядки фільтрів комутують.
- Фікстура: seed-пацієнти 1001/1002/1003 перемаплено 2→19/37/19 у `patients_92.json`
  (решта 1/2-рядків — non-eligible інші); `MisParityTest` пінить depts + `hasSize(92)
  без змін. `patients_52.json` (заморожений legacy-фікстур) не чіпано.
- Frontend: обидві сторінки на `patientApi.searchByModule('medication')`, клієнтський
  фільтр 1/2 видалено, тогл ділить 19|37 локально (`lib/medicationDepartments.ts` —
  єдине місце, припущення (b)); skeleton-loading, Alert + retry, empty-стани на місці.
- `Promise.all`/послідовне per-patient довантаження листків — без змін логіки
  (відомий борг на реальних обсягах, зафіксовано в issue).
