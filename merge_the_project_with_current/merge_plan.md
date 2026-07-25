# Merge Plan: Карта інтенсивної терапії + Листок лікарських призначень

**Дата:** 2026-07-25
**Статус:** План затверджено, очікування старту

---

## Правило імплементації (цикл CI)

Уся розробка виконується ітераціями за наступним циклом:

```
[Імплементація] → [Написання тестів] → [Commit + Push] → [CI виконання] → [Очікування результатів (polling)] → [FAILURE?] → [Bug/Test fixes] → [Повтор CI] → [УСІ ТЕСТИ ЗЕЛЕНІ] → [Початок наступної ітерації]
```

**Правила:**
1. Кожна ітерація = одна або кілька GitHub Issues.
2. Код + тести пишуться разом, одним комітом.
3. Після push — обов'язкове очікування CI (polling GitHub Actions).
4. Якщо CI червоний — фікс, коміт, повторне очікування.
5. Перехід до наступної ітерації ТІЛЬКИ після зеленого CI.
6. Тести запускаються ВИКЛЮЧНО через GitHub Actions — ніколи локально.

---

## Архітектурне рішення

### Єдиний уніфікований додаток

Обидва модулі об'єднуються в один Spring Boot + React SPA додаток зі спільними:
- **PostgreSQL 16** — усі клінічні дані
- **Spring Boot backend** — розширюється новими сервісами/контролерами
- **React 19 + TypeScript + MUI** — розширюється новими маршрутами
- **JWT auth** — локальна (LDAP — TODO, Issue #58)
- **Integration Layer (MisService)** — read-only MIS API
- **CI/CD** — GitHub Actions (3 jobs: format, integration-tests, test)
- **Тестування** — JUnit 5 + Vitest + Playwright

### Чому не мікросервіси

Обидва модулі працюють з одними пацієнтами, користувачами, відділеннями через один Integration Layer до МІС. Мікросервісна архітектура створить дублювання логіки (аудит, авторизація, MIS-шлюз), ускладнить деплой без вигоди для масштабу в межах одного госпіталю.

---

## Порівняння двох проектів

| Характеристика | ICU Chart (поточний) | Medicine Prescription List | Цільовий стан |
|---|---|---|---|
| **Backend** | Spring Boot 3.2.5 + JPA/Hibernate | Spring Boot 3.3.9 + raw JDBC | **Spring Boot 3.3.9 + JPA/Hibernate** |
| **БД** | PostgreSQL 16 | MS SQL Server (чужа БД DoctorEleks!) | **PostgreSQL 16 (власна)** |
| **Frontend** | React 19 + TypeScript 6 + MUI 9 | React 19 + JavaScript | **React 19 + TypeScript + MUI** |
| **Аутентифікація** | JWT (локальні користувачі) | LDAP/AD + JWT | **JWT (локальні) + LDAP TODO** |
| **MIS інтеграція** | ✅ API-only (read-only) | ❌ Прямі SQL-запити до БД | **API-only (read-only)** |
| **Тести** | 312 backend + ~190 frontend + 38 E2E | 1 smoke test | **Повне покриття** |
| **CI/CD** | GitHub Actions (3 jobs) | Відсутній | **GitHub Actions** |
| **Секрети** | application.yml | hardcoded production secrets | **Змінні середовища** |
| **Сповіщення** | Немає | Telegram (disabled) + Email (commented out) | **LogNotificationService (заглушка) + Telegram TODO** |

---

## Що зберігається в PostgreSQL проекту

### Власні клінічні дані (нові таблиці)

| Таблиця | Призначення |
|---|---|
| `prescription_lists` | Кореневий документ листка призначень (пацієнт, статус, дати) |
| `prescription_items` | Рядки призначень (препарат, метод, режим, 21-денний розклад) |
| `prescription_days` | День розкладу (дата, 4 періоди) |
| `prescription_day_parts` | Доза на період (ранок/день/вечір/ніч) |
| `prescription_executions` | Виконання дози медсестрою (хто, коли, статус) |
| `prescription_signatures` | Підписи лікаря/медсестри |
| `vital_sign_lists` | Документ життєвих показників |
| `vital_sign_days` | День показників (ранок + вечір) |
| `vital_sign_entries` | Конкретний показник (t°, AT, SpO₂, пульс, стілець, біль) |
| `medicine_catalog_cache` | Локальний кеш каталогу ліків з MIS |
| `allergy_cache` | Кеш алергій пацієнта з MIS |
| `drug_interaction_rules` | PTG-правила конфліктів між препаратами |
| `telegram_subscriptions` | Chat ID для Telegram-сповіщень |

### Що НЕ зберігається — отримується через MIS API (read-only)

| Дані | MIS метод |
|---|---|
| Пацієнти (демографія) | `spzIBPatientSearch` |
| Госпіталізації | `spzIBPatientScheduleList` |
| Лікарі/медсестри | `spzIBUserDetails` |
| Відділення | `spzIBCompanyDetails` |
| Каталог препаратів (актуальний) | `spzIBServiceDetails` / окремий метод |

---

## Цільова архітектура

```
┌──────────────────────────────────────────────────────────────┐
│               React 19 SPA (TypeScript + MUI)                │
│  ┌──────────┐  ┌──────────────┐  ┌────────────┐  ┌────────┐ │
│  │ICU Card  │  │ Prescription │  │Vital Signs │  │ Admin  │ │
│  │ Module   │  │   Module     │  │  Module    │  │ Module │ │
│  └────┬─────┘  └──────┬───────┘  └─────┬──────┘  └───┬────┘ │
│       └───────────────┴───────────────┴──────────────┘       │
│                           │ REST API                         │
└───────────────────────────┼──────────────────────────────────┘
                            │
┌───────────────────────────┼──────────────────────────────────┐
│             Spring Boot 3.3.9 (Java 17)                      │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐     │
│  │Episode   │  │Prescript.│  │VitalSign │  │   Auth   │     │
│  │Service   │  │Service   │  │Service   │  │ Service  │     │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘     │
│       └──────────────┴────────────┴──────────────┘           │
│                           │                                   │
│                Integration Layer (MisService)                 │
│                           │                                   │
│           ┌───────────────┴───────────────┐                   │
│           │         READ-ONLY             │                   │
│           ▼                               ▼                   │
│    Mock MIS Adapter              Production MIS Adapter       │
│    (dev/test)                     (майбутнє)                  │
└───────────────────────────────────────────────────────────────┘
                            │
                     ┌──────┴──────┐
                     │ PostgreSQL  │
                     │     16      │
                     └─────────────┘
```

---

## Фази імплементації

### Фаза 0: Підготовка та аудит (тиждень 1)

**GitHub Issues:**

| Issue | Назва | Опис |
|-------|-------|------|
| #13 | Аудит безпеки: вилучення секретів | application.properties → змінні середовища, .env.example |
| #14 | Інвентаризація прямих SQL-запитів | Виявлення всіх запитів до БД DoctorEleks у MedicineListRepository.java (643 рядки) |
| #15 | Мапування моделі даних | JSON-in-column → реляційні таблиці (ER-діаграма) |
| #16 | Мапування ролей | EMPLOYEE/ADMIN → DOCTOR/NURSE/HEAD/ADMIN |
| #17 | Інвентаризація API | 18 ендпоінтів MedicineListController → що потребує MIS API |
| #18 | Знімок тестових даних | Створення integration-test даних для Prescription List |

---

### Фаза 1: PostgreSQL схема та доменна модель (тижні 2–3)

**GitHub Issues:**

| Issue | Назва |
|-------|-------|
| #19 | JPA entity: prescription_lists |
| #20 | JPA entity: prescription_items, prescription_days, prescription_day_parts |
| #21 | JPA entity: prescription_executions, prescription_signatures |
| #22 | JPA entity: vital_sign_lists, vital_sign_days, vital_sign_entries |
| #23 | JPA entity: medicine_catalog_cache, allergy_cache, drug_interaction_rules |
| #24 | Liquibase/Flyway міграції для всіх нових таблиць |
| #25 | MisService: getMedicineCatalog(), getPatientAllergies() + WireMock стаби |
| #26 | Інтеграційні тести для всіх нових репозиторіїв (Testcontainers PostgreSQL) |

---

### Фаза 2: Backend рефакторинг (тижні 4–6)

**Що видаляється:**
- `MedicineListRepository.java` (643 рядки raw JDBC)
- Прямі SQL-запити до таблиць DoctorEleks: `Patient`, `Residence`, `Venue`, `Item`, `ItemKind`, `Document`, `DocumentNode`, `Users`
- JSON-серіалізація в колонки (MedicineDetails, VitalList, ApprovedRowIndexes)

**GitHub Issues:**

| Issue | Назва |
|-------|-------|
| #27 | PrescriptionListService (+ unit-тести) |
| #28 | PrescriptionItemService з high-risk/конфлікт-перевірками |
| #29 | PrescriptionExecutionService з 2P-верифікацією |
| #30 | VitalSignService |
| #31 | MedicineCatalogService (кешування з MIS API) |
| #32 | DrugInteractionService (PTG-матриця) |
| #33 | PrescriptionPdfService (iText, форма № 003-4/о) |
| #34 | NotificationService інтерфейс + LogNotificationService (заглушка) |
| #35 | PrescriptionController + VitalSignController |
| #36 | JSR-380 валідація на всі DTO |
| #37 | Інтеграційні тести для всіх контролерів |

---

### Фаза 3: Auth уніфікація (тиждень 7)

**GitHub Issues:**

| Issue | Назва |
|-------|-------|
| #38 | UserRole/Permission: prescription-операції |
| #39 | LDAP authentication provider (spring.ldap.enabled=false) — TODO #58 |
| #40 | Міграція: SH_Users → users (скрипт конвертації) |

---

### Фаза 4: Frontend рефакторинг (тижні 8–9)

**Підхід:** НЕ копіювати старий JS-код. Писати нові TypeScript + MUI компоненти, використовуючи існуючий ICU-фронтенд як референс.

**Перевикористовується:** `AuthContext`, `client.ts`, `endpoints.ts`, layout, `Guard`, `getErrorMessage()`

**Нові маршрути:** `/doctor/prescriptions`, `/nurse/prescriptions`, `/nurse/prescriptions/execute`

**GitHub Issues:**

| Issue | Назва |
|-------|-------|
| #41 | PrescriptionDashboardPage.tsx |
| #42 | PrescriptionListPage.tsx |
| #43 | PrescriptionGrid.tsx + PrescriptionItemRow.tsx + PrescriptionDayCell.tsx |
| #44 | VitalSignGrid.tsx |
| #45 | MedicineSearch.tsx (автодоповнення через API) |
| #46 | DrugConflictWarning.tsx (high-risk/конфлікт) |
| #47 | TwoPersonAuthDialog.tsx |
| #48 | PrescriptionPrintView.tsx (форма № 003-4/о) |
| #49 | Маршрути /prescriptions у App.tsx з Guard |

---

### Фаза 5: Тестування (тижні 10–11)

| Issue | Назва |
|-------|-------|
| #50 | Unit-тести: PrescriptionListService, PrescriptionItemService, VitalSignService |
| #51 | Unit-тести: DrugInteractionService |
| #52 | Інтеграційні тести: PrescriptionController |
| #53 | Інтеграційні тести: VitalSignController |
| #54 | Vitest-тести: PrescriptionGrid, VitalSignGrid, MedicineSearch |
| #55 | Playwright E2E: повний сценарій prescription list |

---

### Фаза 6: CI/CD + Документація + TODO (тиждень 12)

| Issue | Назва |
|-------|-------|
| #56 | GitHub Actions: Prescription List модуль у CI pipeline |
| #57 | JaCoCo coverage для prescription-пакетів |
| #58 | **[TODO]** LDAP authentication provider |
| #59 | **[TODO]** TelegramNotificationService (замінити LogNotificationService) |
| #60 | AGENTS.md: prescription workflow, ролі, архітектура |
| #61 | Технічне завдання.md: розділ «Листок лікарських призначень» |

---

## Загальна оцінка

| Параметр | Значення |
|---|---|
| Фаз | 7 (0–6) |
| Тижнів | ~12 |
| GitHub Issues | 49 (#13–#61) |
| Нових таблиць PostgreSQL | 13 |
| Нових backend-сервісів | 10 |
| Нових frontend-компонентів | 12 |
| TODO-компонентів | 2 (LDAP #58, Telegram #59) |
