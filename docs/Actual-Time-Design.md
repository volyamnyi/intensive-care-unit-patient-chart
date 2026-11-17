# Дизайн-документ: ручна фіксація фактичного часу видачі / введення препарату

| | |
|---|---|
| **Статус** | ЗАТВЕРДЖЕНО (owner, 2026-11-17) |
| **Епік** | #442 (Фаза 1) → реалізація #443–#447 |
| **Модуль** | «Листок лікарських призначень» (`medication-sheet`) |
| **Тип** | Медично значущі дані — максимальний аудит і захист від непомітного перезапису |

Цей документ — єдине джерело істини для реалізації фаз #443–#447. Він фіксує модель даних, wire-контракт, правила валідації, підхід до часових зон, RBAC, аудит, UX і тестову архітектуру так, щоб кожну фазу можна було реалізувати без повторного дослідження кодової бази.

---

## 1. Мета і принципи

Медичний персонал за потреби вручну фіксує **фактичний час видачі** та/або **фактичний час введення** препарату пацієнтові — значення, яке може відрізнятися від поточного системного часу (ретроспективна фіксація).

Непорушні принципи:

1. Фактичний час — **медично значуще значення**. Його не можна непомітно підміняти плановим часом, серверним `now()` чи часом останнього редагування запису.
2. **Планове ≠ фактичне.** Планове виконання в цій системі = календарна дата дня (`prescription_item_days.day_date`) + період доби (`prescription_day_parts.period`: morning/day/evening/night) — конкретної години планування не існує. Фактичне = повний timestamp до секунд. Редагування фактичного часу ніколи не мутує планові значення (день, період, дозу, прапори `is_planned*`/`is_completed*`).
3. **Медичний факт ≠ системний час операції.** `administered_at` — коли сталося; `executed_at` та `BaseEntity.created_at/updated_at` — коли створено/змінено запис. Ретроспективна фіксація не зміщує метадані операції.
4. **Видача ≠ введення** — два незалежні часові атрибути. Введення без окремої фіксації видачі — нормальний випадок (`dispensed_at = NULL`). «Видача без введення» не моделюється (доза лишається запланованою; інша статусна модель — out of scope).
5. Аудит — виключно через наявну систему audit v2 (`DomainAuditEmitter`); жодного паралельного механізму.
6. Сервер — джерело істини щодо валідності часу та прав.

## 2. Зафіксовані рішення owner'а (2026-11-17)

| # | Рішення | Наслідок |
|---|---|---|
| 1 | Коригування зафіксованого часу — через **окремий RBAC permission** `PRESCRIPTION_ACTUAL_TIME_EDIT` | Фаза #444: каталог 28→29; NURSE/ADMINISTRATOR — 403 за замовчуванням |
| 2 | `dispensed_at` додається **одразу**, разом з `administered_at` | Фаза #443: міграція одразу з трьома колонками; UI — опційне поле |
| 3 | Причина коригування — **обов'язкова** | Фаза #444: `@NotBlank reason` → `execution.comment` (append) + audit NARRATIVE |
| 4 | Діапазон дат фактичного часу: `[перший день листка; день клітинки + 1 доба]` | Валідатор V3 (§5); нічна доза, зафіксована вранці, проходить; минуле за межами листка — ні |
| 5 | Гранти нового permission на **існуючих** БД — **задокументована дія owner'а через admin-UI** (не SQL-backfill) | §7.2 цього документа — канонічна інструкція; `core/016` містить лише definition-рядок |

## 3. Модель даних

### 3.1 Розширення існуючої сутності

Окрема сутність **не створюється**: `prescription_executions` вже є подією введення (2P-автентифікація, #359). Додаються три колонки:

| Колонка | Тип | Семантика |
|---|---|---|
| `administered_at` | `TIMESTAMP` (nullable) | **Фактичний час введення** препарату пацієнтові |
| `dispensed_at` | `TIMESTAMP` (nullable) | **Фактичний час видачі** препарату (опційний) |
| `administered_at_manual` | `BOOLEAN NOT NULL DEFAULT FALSE` | Прапор ручного введення часу — джерело UI-індикатора коригування |

Інваріант: `administered_at IS NOT NULL` для завершених доз (гарантується шляхом виконання — §4.2). `dispensed_at <= administered_at` — валідується (V4).

### 3.2 Backfill legacy-рядків

```sql
UPDATE prescription_executions SET administered_at = executed_at
 WHERE administered_at IS NULL AND executed_at IS NOT NULL;
```

Legacy-рядок стає медично коректним: факт = час запису операції, `administered_at_manual = false` (не ручне значення — індикатор не показується). Даних не дублюємо семантично: `administered_at` — медичний факт, `executed_at` — технічний час операції; вони збігаються лише для legacy.

### 3.3 Що свідомо не змінюється

- `executed_at` — як ставився сервером, так і ставиться (системний час операції).
- Планова модель (`prescription_item_days.day_date` + `prescription_day_parts.period/dose/is_planned*`) — недоторкана.
- Статусна модель (`isPlanned/isPlannedFinished/isCompleted/isCompletedFinished`) — недоторкана.
- Існуючі маршрути планування/скасування — недоторкані.

### 3.4 Міграції (тільки Liquibase)

- **`med/030-execution-actual-time.sql`** (changeset `split-med:34`, реєстрація в `db.changelog-master-med.yaml`): три `ADD COLUMN IF NOT EXISTS` + backfill; повний rollback (DROP колонок у зворотному порядку). Коментарі — всередині changeset (Liquibase забороняє коментарі поза ним).
- **`core/016-actual-time-edit-permission.sql`** (changeset `split-core:17`): **лише definition-рядок** нового permission — `INSERT ... ON CONFLICT (code) DO UPDATE SET label/description` (див. §7.2 — SQL-гранти заборонені рішенням owner'а). Чек-суми застосованих changeset'ів не чіпаються (новий файл, новий id).
- Нових індексів не потрібно: пошук іде за `day_part_id` (індекс існує з `med/001`).

## 4. Wire-контракт і API

### 4.1 Віддача execution-полів на wire

`PrescriptionDayPartResponse` та `PrescriptionDayPartNested` доповнюються:

| Поле | Тип wire | Значення |
|---|---|---|
| `executionId` | `UUID \| null` | останній execution day part'а (null — доза не виконана) |
| `administeredAt` | `string \| null` | фактичний час введення, ISO local |
| `dispensedAt` | `string \| null` | фактичний час видачі |
| `administeredAtManual` | `boolean` | ручне введення/коригування часу |
| `executionVersion` | `integer \| null` | optimistic-lock версія execution'а — обов'язкова для endpoint'у коригування (стale → 409 `VERSION_CONFLICT`) |

Завантаження для грида — **batch** через існуючий `PrescriptionExecutionRepository.findByDayPartIdIn(...)` (використовується `PrescriptionPdfDataLoader`; 1 IN-запит на грид, без N+1). На day part мапиться **останній** execution (за `created_at`/id). PDF-рендер 003-4/о не змінюється.

### 4.2 Первинна фіксація при виконанні (без нових endpoint'ів)

`PrescriptionExecuteRequest` (+ `PUT /day-parts/{id}/complete`, `POST /day-parts/{id}/execute`) доповнюється optional-полями `administeredAt` / `dispensedAt`:

- не передано → сервер ставить `administered_at = now()`, `administered_at_manual = false`, `dispensed_at = NULL`;
- передано → валідація V1–V4 (§5) **до** 2P-автентифікації (невалідний час не тригерить перевірку другої особи), `administered_at_manual = true`.

Права незмінні: `PRESCRIPTION_EXECUTE` (NURSE/HOD), 2P server-owned (#359), `requireNotFinished`.

### 4.3 Коригування зафіксованого часу (новий endpoint)

`PUT /api/prescriptions/executions/{executionId}/actual-time` — `@PreAuthorize("@permissionService.has('PRESCRIPTION_ACTUAL_TIME_EDIT')")`.

Тіло: `{ administeredAt (required), dispensedAt (optional), reason (required, ≤500), version (required) }`.

Семантика:

- guards: execution існує (404) → day part completed (інакше 422 «Доза не виконана — немає що коригувати») → листок не finished (`DocumentLockededException` → 422);
- валідація часу V1–V4 проти day part / листка клітинки;
- `version != execution.version` → `VersionConflictException` → **409 `VERSION_CONFLICT`** (захист від мовчазного перезапису медично значущого значення);
- застосування: нові значення, `administered_at_manual = true`; `execution.comment` — **append** рядка `[коригування dd.MM.yyyy HH:mm:ss] {login}: {reason}` (внутрішньорядкова історія не втрачається; повна історія — audit v2 + object history F7);
- аудит-подія (§8) — в межах тієї ж транзакції, fail-safe emitter;
- повертає оновлений `PrescriptionDayPartResponse` (консистентно з іншими mutation-endpoint'ами).

### 4.4 Формат часу на wire

- Передача: ISO-8601 **local** `yyyy-MM-dd'T'HH:mm:ss`, без таймзони (§6). Секунди опційні на вході: `HH:mm` приймається → нормалізується `:00`.
- Віддача: завжди з секундами (`:00` pad; Jackson для `LocalDateTime` опускає `:00` — нормалізація на фронті в `lib/actualTime.ts`).
- Точність зберігання: Postgres `TIMESTAMP` — мікросекунди; секунди гарантовані.

## 5. Валідація фактичного часу (матриця V1–V4)

Єдиний валідатор `ActualTimeValidator` (pure static) для первинної фіксації та коригування. Клієнт дзеркалить правила для inline-UX (`lib/actualTime.ts`), сервер — джерело істини.

| # | Правило | Межа | Порушення |
|---|---|---|---|
| V1 | Парсабельність `yyyy-MM-dd'T'HH:mm[:ss]` | — | **400 `VALIDATION_ERROR`** (через `GlobalExceptionHandler`) |
| V2 | Не майбутнє | `value <= now + 5 хв` (tolerance на розсинхрон годинників клієнт/сервер; межа включно) | **422 `BUSINESS_RULE`** «Фактичний час не може бути в майбутньому» |
| V3 | Вікно дат | `value.toLocalDate() ∈ [minDayDate листка (00:00, включно); cellDayDate + 1 доба (23:59:59, включно)]` | **422** «Фактичний час поза межами періоду листка» |
| V4 | Видача не пізніше введення | `dispensedAt <= administeredAt` (рівні — ok) | **422** |

Додаткові правила:

- Дата фактичного часу **не обязана** дорівнювати `dayDate` клітинки: нічна доза («Ніч» дня D), зафіксована вранці дня D+1, — валідна (саме тому верхня межа V3 — `cellDayDate + 1`). Значення за межами листка — невалідні (безпідставних обмежень ретроспективної фіксації всередині періоду лікування не вводимо).
- Секунди зберігаються як передано; `HH:mm` → `:00`.
- Невалідне значення не змінює жодного стану (валідація до будь-яких мутацій).

## 6. Часові зони та DST

**Єдиний узгоджений підхід — naive local (server-local), як уся система.**

- Схема: `TIMESTAMP WITHOUT TIME ZONE`; entities: `LocalDateTime`; Jackson: ISO local (`write-dates-as-timestamps: false`, `date-format` стосується лише `java.util.Date`). Без `hibernate.jdbc.time_zone`.
- Уведення `timestamptz`/`Instant` **лише** в одній таблиці створило б розсинхрон з рештою системи — заборонено.
- `LocalDateTime` у `TIMESTAMP WITHOUT TIME ZONE` не зсувається при збереженні/повторному читанні за визначенням — вимога «без зміщення після save/reload» виконується конструктивно.
- **DST-перехід:** значення у «двійній годині» (напр. 02:00–03:00 весняного переходу) зберігається як введено (naive). Це свідомий компроміс усього проєкту; ретроспективна фіксація приймає такі значення без перетворень.
- Frontend: клієнтський «зараз» (дефолт popover'а) — локальний годинник браузера; сервер валідує V2 з tolerance 5 хв. Зміна часового поясу браузера **не** змінює збережені значення (не «їдуть» — naive-контракт; E2E-кейс №15, фаза #447).

## 7. RBAC і безпека

### 7.1 Новий permission

- Код: `PRESCRIPTION_ACTUAL_TIME_EDIT` — «Коригування фактичного часу виконання призначення» (опис: «Ручне виправлення зафіксованого фактичного часу видачі/введення препарату з обов'язковою причиною»). Категорія: «Клінічні операції». Каталог: **28 → 29**.
- Канонічне значення — у `PermissionCodes` (#427 item 2); alias у `PermissionCatalog` (існуючий патерн); definition у `PermissionCatalog.definitions()`; `defaultMatrix()` — гранти **DOCTOR + HEAD_OF_DEPARTMENT** (NURSE, ADMINISTRATOR, AUDITOR — ні).

### 7.2 Розсіяння грантів: свіжі vs існуючі БД — РІШЕННЯ OWNER'А

Механіка `PermissionService.seedIfEmpty()` (рядки 181–201): definitions сіються лише якщо `permissions` порожня; grants — лише якщо `role_permissions` порожня. Тому:

| Сценарій | Definitions | Grants |
|---|---|---|
| **Свіжа інсталяція** | `core/003` + `core/016` (SQL, всі 29) | `PermissionCatalog.defaultMatrix()` сіє повну матрицю автоматично (role_permissions порожня) → DOCTOR/HOD отримують новий код без дій |
| **Існуюча БД** | `core/016` додає definition-рядок (SQL `ON CONFLICT (code) DO UPDATE`) — обов'язково, бо `role_permissions.permission_code` має FK на `permissions(code)` і без рядка grant неможливий | **Задокументована дія owner'а через admin-UI** (рішення від 2026-11-17) |

**Обґрунтування вибору admin-UI (проти SQL-backfill у `core/016`):**

1. Урок `core/005` (2026-08-18): SQL-гранти у changeset фліпають `rolePermissionRepository.count()` у ненульове значення → на свіжій інсталяції Java-seeding повної default-матриці пригнічується → майбутні доповнення `defaultMatrix()` ніколи не підсіються. SQL-гранти ламають інваріант «гранти — виключно Java-seed на свіжих + admin-UI у рантаймі».
2. Матриця доступів прод-системи — зона відповідальності owner'а; рантайм-грант через admin-UI **аудитується** (matrix save події), тоді як SQL-backfill пройшов би поза аудит-слідом.
3. Owner контролює момент і склад грантів (може видати не лише дефолтні DOCTOR/HOD).

**Канонічна інструкція для owner'а (існуючі БД):** `/admin` → вкладка «Доступи та ролі» → категорія «Клінічні операції» → «Коригування фактичного часу виконання призначення» → відмітити DOCTOR та HEAD_OF_DEPARTMENT → «Зберегти зміни». Кожен grant валідується FK (definition з'являється після застосування `core/016`). Цей пункт синхронізується в README/UseManual у фазі #447.

### 7.3 Підсумкова матриця прав

| Операція | Permission | Дефолтні ролі |
|---|---|---|
| Первинна фіксація часу при виконанні дози | `PRESCRIPTION_EXECUTE` (незмінний) | NURSE, HEAD_OF_DEPARTMENT (+2P медсестра) |
| Коригування зафіксованого часу | `PRESCRIPTION_ACTUAL_TIME_EDIT` (новий) | DOCTOR, HEAD_OF_DEPARTMENT |
| Перегляд фактичного часу (грид/tooltip) | `PATIENT_VIEW` (незмінний) | як зараз |

Frontend-gate (`useAuth().hasPermission(...)`) — тільки зручність; сервер лишається джерелом істини. Прямий API-виклик без права → 403 (integration-покриття #444, E2E-матриця #447).

### 7.4 Серверна валідація як захист

- Час валідовується сервером (V1–V4) — підроблені/некоректні значення відхиляються незалежно від UI.
- Optimistic lock (`version`) — захист від конкурентного перезапису.
- Право на рівні коду + цілосність посилань (execution ↔ day part) — захист від зміни чужих записів.
- A12-інваріант не порушується: 2P-правило server-owned, клієнт не може його вимкнути.

## 8. Аудит

Виключно audit v2 (`DomainAuditEmitter`); append-only; значення — у **changes**, не в metadata (allowlist-обмеження #436).

| Подія | Коли | Changes |
|---|---|---|
| `medication.dose.execute` (існуюча, USER_AND_WITNESS) | первинна фіксація | + `administeredAt` (CLINICAL) при ручному значенні; + `dispensedAt` за наявності. Дію/маршрут не змінюємо — coverage gate не чіпається |
| `medication.dose.actual_time.update` (нова дія) | коригування | `ActionType.UPDATE`, `EventClass.BUSINESS`, `ActorPolicy.USER`, `DataClass.CLINICAL`, `Flag.MANDATORY, Flag.WRITE`; target `PrescriptionExecution {id}`; changes: `administeredAt` old→new (CLINICAL), `dispensedAt` old→new (CLINICAL, якщо змінювалась), `reason` (NARRATIVE) |

- Повторні коригування — append-only (нічого не перезаписується); усі події доступні через object history (F7) на prescription-сторінках.
- Реєстрація нового маршруту **обов'язкова** в обох coverage-файлах (`backend/icu-chart/src/test/resources/audit-route-coverage.json` + `audit-action-coverage.json`) — інакше `AuditCoverageGateIntegrationTest` червоний у CI.
- Розмежування в події: медичний час (`administeredAt` у changes) vs системний час операції (автоматично фіксується подією). Актор — виконавець коригування; reason — обов'язковий.

## 9. UX-мапінг

Без змін існуючого UX поза межами нового функціоналу.

| Точка | Компонент | Поведінка |
|---|---|---|
| **Введення при виконанні** | `ExecuteDosePopover` (крок «Фактична доза», до 2FA) | Секція «Фактичний час введення»: `DatetimeInput`, дефолт = локальний «зараз», кнопка «Зараз» (скидання); чекбокс «Окремо фіксувати час видачі» → опційний `DatetimeInput`; inline-помилки клієнтського валідатора блокують «Продовжити» без запиту. Плановий контекст лишається в заголовку popover'а («Ранок: доза») — візуальне розмежування |
| **Компонент введення** | новий `components/ui/datetime-input.tsx` + `lib/actualTime.ts` | Нативні `type="date"` + `type="time" step="1"` в одному керованому компоненті; **нуль нових залежностей** (react-day-picker/Radix відхилені: Radix у Base UI-проєкт — новий runtime dep без обґрунтування; `datetime-local` — поведінка секунд залежить від браузера); стиль через `Input` + `appearance-none` + прихований індикатор (за орієнтиром owner'а); тач-таргети `pointer-coarse:min-h-11`; `aria-invalid` + `aria-describedby` |
| **Відображення у гріді** | `PrescriptionSpreadsheet` | Tooltip на completed-клітинці (✓): «Введено: dd.MM.yyyy HH:mm:ss» (+ «Видано: …» за наявності) через існуючий tooltip-механізм клітинок; індикатор ручного коригування — іконка `Clock` при `administeredAtManual` + позначка «(уточнено вручну)» у tooltip |
| **Коригування** | новий `ActualTimeCorrectionDialog` | Клік по completed-клітинці користувачем з `PRESCRIPTION_ACTUAL_TIME_EDIT` (для решти — no-op як зараз). Поточні значення read-only → нові `DatetimeInput` → textarea «Причина коригування» (обов'язкова, ≤500, лічильник). `409` → банер «Дані змінено іншим користувачем — оновіть сторінку»; `422`/`400` → inline-помилка сервера; успіх → sonner toast «Фактичний час оновлено» + рефреш грида (патерн PrescriptionDetailPage) |
| **Запобігання випадковому перезапису** | діалог | Поточне значення завжди показане; збереження без причини неможливе; stale `version` → 409-банер, а не мовчазний перезапис |

Доступність: послідовність табів (дата → час → кнопки), aria-label обох частин, повна клавіатурна керованість нативних input'ів; responsive-прохід 360px клавіатурним вводом (фаза #447).

## 10. Тестова архітектура

Три рівні; без реального MIS (стор-стаб) і без залежності від реального часу.

### Unit (Vitest / JUnit)

- `lib/actualTime.test.ts`: normalize (`HH:mm`/`HH:mm:ss`/сміття), compose, format (`dd.MM.yyyy HH:mm:ss`), validate-матриця V1–V4 (межі включно, tolerance ±5 хв) — чиста логіка без зсувів реального часу.
- `datetime-input.test.tsx`: controlled round-trip до секунд; `HH:mm` → `:00`; disabled/invalid/aria; очищення → `onChange(null)`; порядок табів.
- Backend `ActualTimeValidatorTest`: та сама матриця + серверні коди помилок.
- `PrescriptionExecutionServiceTest`: клієнтський час збережено до секунд (`manual=true`); дефолт `now()` (`manual=false`); валідація **до** 2P-перевірки; `updateActualTime_*` (guards, 409, append-comment, подія з old→new+NARRATIVE).
- `PermissionServiceTest`: catalog hasSize(29); DOCTOR/HOD мають, NURSE/ADMINISTRATOR — ні.

### Integration (профіль `integration-test`)

- Round-trip секунд через API (строкова рівність `…T10:30:07`).
- Планові значення недоторкані (день/період/доза/прапори до/після).
- Backfill: legacy-рядок (лише `executed_at`) → `administered_at == executed_at`, `manual == false`.
- RBAC: NURSE 403 / DOCTOR 200 / HOD 200 / ADMINISTRATOR 403 + revoke→grant цикл (патерн `AdminPermissionsIntegrationTest`).
- 409 (stale version), 422 (finished, не-completed), 400 (малформений).
- Аудит: append-only, два коригування → дві події з різними old/new (ассерт scoped до execution id — не stale-row trap, урок #434).
- Coverage gate зелений (route + action зареєстровані).

### Playwright E2E

- Контрольований час: `page.clock.install({ time: fixed })` (патерн overlay-тесту #355); фіксовані тестові дати; timezone-кейс — `context.timezoneId`.
- Фазові спеки (#446): nurse — виконання з ручним часом → tooltip → reload-persistence → плановий заголовок незмінний; doctor — коригування з причиною → індикатор manual; без права — діалог недоступний.
- Наскрізні сценарії ТЗ §7.3 — 15 пунктів (#447): відкриття листка → вибір призначення → редактор → дата → час → секунди → збереження → відображення → reload → повторне редагування → планове незмінне → невалідні → API-помилки → різні права → зміна timezone браузера.
- Граничні: DST-дата; 23:59:59 → 00:00:30 наступного дня (вікно +1); секунди `:00` round-trip.
- Статуси: cancelled → 422; finished → DOCUMENT_LOCKED; planned (не completed) → 422.
- RBAC-матриця через API-спеку (`prescription-actual-time-access.spec.ts`).

## 11. Межі рішення (out of scope)

- Друк фактичного часу у PDF-формі 003-4/о.
- «Видача без введення» як окремий стан; пропущені призначення.
- Зміна планової моделі (дата + період доби) і статусної моделі призначень.
- `timestamptz`/`Instant` для окремих таблиць; перегляд фактичного часу в audit-консолі UI.
- Додаткові runtime-залежності frontend (календарні бібліотеки).

## 12. Трассування на фази

| Фаза | Issue | Бере з документа |
|---|---|---|
| 2 | #443 | §3 (міграція med/030, backfill), §4.1–4.2 (wire, execute), §5 (валідатор), §6, §8 (execute changes) |
| 3 | #444 | §4.3 (endpoint), §5 (валідатор), §7 (permission + admin-UI рішення), §8 (нова дія + coverage) |
| 4 | #445 | §9 (компонент, нуль deps), §5 (клієнтський дзеркальний валідатор) |
| 5 | #446 | §9 (popover/tooltip/діалог), §4 (wire), §10 (E2E фазові) |
| 6 | #447 | §10 (наскрізні + граничні + RBAC-матриця), §7.2 (документація admin-UI кроку) |
