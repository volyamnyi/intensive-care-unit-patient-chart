# Audit v2 — план наскрізного аудиту реальних дій користувачів (виправлений за review)

> **Статус:** реалізація розпочата за окремою вказівкою власника; F0 завершена локально, F1 розпочата.
> Деплой/production-операції не виконуються автоматично; Liquibase-зміни перевіряються лише на local/test.
> **Мова плану:** українська. Стабільні ідентифікатори дій і полів — англійською за §B1.
> **Фази:** F0–F8 (§F). Кожна фаза має окремий GitHub Issue; номери — у §J.
> План повністю описаний множиною Issues F0–F8 (§F цього файлу — огляд, Issues — нормативний опис фази).

## Correction log (що виправлено за review)

| ID | Зауваження review | Виправлення в плані |
|---|---|---|
| R1 | `X-User-Action-Id` при retry можна сприйняти як ключ ідемпотентності | §B3: кореляція ≠ ідемпотентність; retry з тим самим Action ID не гарантує відсутності повторного ефекту |
| R2 | Relay недоспецифікований (власник, семантика, порядок, skew) | §B6: власник, at-least-once + dedupe за `auditId`, порядок best-effort, `occurredAt` авторитетний, інтервал — за вимірюванням |
| R3 | p95 `10 ms / 30 s` як acceptance без baseline | §B7, §I: цифри — стартові гіпотези в performance-test plan, не критерії приймання |
| R4 | Рядки матриці групують кілька дій; test ID виглядають як існуючі | §B2, §E: один рядок = одна атомарна дія; test ID позначені `PROP-` (запропоновані, не існуючі) з мапінгом на реальні шляхи |
| R5 | Немає мапінгу mandatory/security/read/write/destructive/automated | §E: колонка `Flags` + легенда; кожен рядок має клас події та прапорці |
| R6 | Немає таблиці snapshot/diff по сутностях | §B5: класифікація полів + таблиця по ключових сутностях; PHI-клітини — `TBD-DPO` |
| R7 | Немає таблиці failure policy | §B4: нормативна таблиця за класами подій |
| R8 | Немає naming/versioning для action codes | §B1: формат `<module>.<area>.<verb>[.<object>]`, версіонування каталогу, правило перейменувань |
| R9 | Відкриті рішення не зібрані в одному місці | §B8: D1–D7, блокують старт етапів 3–5 |
| R10 | Фактичні формулювання надто категоричні (індекси, proxy, метрики) | §A.7: caveats C1–C7 |
| R11 | Міграція naive `LocalDateTime` у UTC не описана | §H.5: явне правило міграції міток часу |
| R12 | `createdBy/updatedBy=0L` виглядає як локальна проблема medication | §A.5: зафіксовано як системний патерн замовчування actor у всіх модулях |

---

## A. Current State — фактична реалізація (стисло)

### A.1. Сховище

`audit_logs` у `my_fullstack_core` (`backend/common/src/main/resources/db/changelog/core/001-initial.sql`):

```text
id UUID, timestamp TIMESTAMP, user_id BIGINT,
entity VARCHAR(100 → 255 у core/004), entity_id UUID,
action VARCHAR(100), old_value TEXT, new_value TEXT,
correlation_id VARCHAR(100), details TEXT,
ip_address VARCHAR(255), user_role VARCHAR(255), is_deleted BOOLEAN
```

Моделі/точки: `entity/core/AuditLog.java`, `service/AuditService.java`
(`logCreate/logUpdate/logDelete/logAction/logEvent/logAuth`, `@Async logAsync`),
`controller/AuditController.java` (`GET /api/audit`, `GET /api/audit/{id}`),
`repository/core/AuditLogRepository.java` (delete-методи Spring Data заблоковані винятком),
`dto/AuditLogResponse.java` (без `details`), `mapper/AuditLogMapper.java`.

### A.2. Джерела подій

1. `auth/JwtAuthenticationFilter.java` — подія `API_<METHOD>` для кожного автентифікованого
   non-`GET`/`HEAD` запиту; створюється **до** виконання контролера, результату HTTP немає;
   URI кладеться в `entity`; виклик — `@Async logAsync` без гарантій доставки.
2. Ручні `AuditService`-виклики в сервісах `common`, `icu-chart`, `medication-sheet`,
   `prosthesis-manufacturing` (перелік — §E, колонка «Стан зараз»).
3. `service/AuthService.java` — `LOGIN`, `LOGIN_FAILED`, `LOGIN_BLOCKED`, `LOGOUT`
   (спроби/блокування — у пам’яті процесу; login потрапляє в `details` невдалих спроб).
4. SLF4J-логи: технічні + окремі бізнес-повідомлення; `application.yml` у базовому профілі має
   root `DEBUG`, `com.superhumans: TRACE`, Spring/Hibernate `TRACE/DEBUG`, а
   `config/multidb/MultiDatabaseSupport.java` задає `hibernate.show_sql=true` для всіх профілів.
5. Frontend: окремого logger/telemetry немає; `api/client.ts` обробляє лише `401`.
6. `ProcessHistoryPage.tsx` має заголовок «Аудит-лог процесу», але **не читає `/api/audit`** —
   хронологія синтезується з instance/step-executions/brak-events (без actor, частини змін, інтеграцій).

### A.3. Автоматичні процеси (знайдені в коді)

- `ClinicalDayService.autoCloseExpiredDays` (07:00) / `escalateUnsignedDays` (09:00) — `@Scheduled`,
  actor `0L`, email-результат лише в SLF4J, retry/outbox не виявлено.
- `BrakNotificationDeliveryService.sweep` — `@Scheduled` outbox-доставлення SINGLE/THRESHOLD листів;
  sweep передає `row.getCreatedBy()` як actor доставки (тобто ініціатора, а не виконавця).
- `DrugInteractionImportRunner` — `CommandLineRunner` під профілем `migration`, actor `0L`.
- `SeedDataInitializer` + `UserSeedService` — bootstrap без audit trail (лише SLF4J).

### A.4. Перегляд і тести

- `/api/audit` — фільтри `userId/entity/entityId/action/dateFrom-dateTo`, але застосовується
  **лише перша відповідна умова** (`AuditService.getAuditLogs` — ланцюжок `if/else`).
- `AdminPage.tsx` фільтрує лише за сутністю, показує першу сторінку, numeric user ID;
  детальної картки події немає; перегляд аудиту не аудититься.
- Тести: `AuditServiceTest`, `AuditControllerTest`, `AuditIntegrationTest`,
  `tests/specs/admin/audit-log.spec.ts`, точкові `verify(auditService)` у сервісах.
  Немає coverage-guard матриці, перевірок old/new-маски, ретраїв, транзакційних гарантій.

### A.5. Системний патерн замовчування actor (R12)

`createdBy/updatedBy=0L` (дефолт `BaseEntity` — теж `0L`) зустрічається не лише в medication:
user-caused записи в усіх модулях частково не несуть реального actor. Виправлення — системне
(§B, §F4–F6), а не точкове.

### A.6. Retention

Runbook (`docs/Production-Deployment-Runbook.md`, §3.6) описує 2 роки для `audit_logs` і місячне
архівування, але архіватора, cleanup-job і retention enforcement **у коді не знайдено**.
Погоджено тимчасове правило: 2 роки для нових business/security audit events до юридичного/DPO
затвердження; для старих подій і архівного видалення без окремої policy purge не виконувати.

### A.7. Factual caveats (R10)

- C1. «Індексів немає» означає: у переглянутих Liquibase-файлах індексів для `audit_logs`
  не знайдено; це не гарантія стану production DB.
- C2. Існуючі мітки часу — naive `LocalDateTime` (`LocalDateTime.now()`); міграція в UTC — §H.5.
- C3. `ErrorResponse.correlationId` генерується незалежно для кожної помилки і не пов’язаний з аудитом.
- C4. Micrometer/Prometheus/Actuator-метрик у коді не знайдено; механізм метрик — рішення D6.
- C5. `request.getRemoteAddr()` + згадка `X-Forwarded-For` у runbook передбачають proxy;
  proxy-конфігурація поза репозиторієм — зовнішня залежність (D7).
- C6. Усі test ID з префіксом `PROP-` — запропоновані; мапінг на реальні шляхи — у легенді §E.
- C7. Під час старту F0 власник обрав: fail-closed для critical writes і sensitive PHI reads;
  IP + скорочений UA лише з довіреного proxy; мінімальний clinical diff; security events —
  окремий RBAC permission; Actuator/Micrometer як метрики. Proxy CIDR/hops ще треба передати
  через deployment configuration перед production; невідомі CIDR не можна вгадувати.

---

## B. Нормативні рішення

### B1. Naming / versioning action codes (R8)

- Формат: `<module>.<area>.<verb>[.<object>]`, lowercase, крапки-роздільники;
  сегменти можуть містити `_` для узгодження з module/domain naming.
Приклади: `platform.auth.session.login`, `icu.clinical_day.sign.nurse`, `medication.dose.execute`,
  `prosthetics.brak.confirm`, `platform.rbac.permission.grant`.
- Каталог версіонується (`AUDIT_CATALOG_VERSION`, починаючи з `1`); запис несе `schemaVersion`.
- Перейменування коду = новий код + alias на старий; старі коди ніколи не перевизначаються.
- Людські назви (UA) — окремий display-шар, не частина коду.

### B2. Атомарність рядка матриці (R4)

- Один рядок §E = **одна атомарна бізнес-дія з одним outcome**.
- Операція, що змінює кілька об’єктів (створення епізоду + перша доба; brak + гілка) —
  **один root event** + `relatedEntities[]` / `affectedRecords`, а не N незалежних записів.
- Дочірні події — лише для справді окремих side effects (спроба email-доставки, крок scheduled job).

### B3. Кореляція ≠ ідемпотентність (R1)

```text
requestId      — один HTTP-запит (генерує backend filter; відповідь несе заголовок).
userActionId   — один намір користувача; створюється на межі UI intent,
                 повторюється при retry того самого наміру. Призначення — КОРЕЛЯЦІЯ.
correlationId  — наскрізна операція (UI → API → service → job → інтеграція).
parentAuditId  — зв’язок root/child.
```

**Норма:** повтор з тим самим `userActionId` не гарантує відсутності повторного бізнес-ефекту.
Бізнес-ідемпотентність — окреме рішення на команду (дозволено лише там, де воно безпечне,
наприклад find-or-create у `provisionFromMis`). Retry без ідемпотентного handler-а може
створити другу дію з тим самим Action ID — це має бути видно в аудиті, а не приховано.

### B4. Failure policy за класами подій (R7)

| Клас | Audit write недоступний | Бізнес-операція | Retry | Виявлення втрати |
|---|---|---|---|---|
| Критична мутація (пацієнт/документ/призначення/процес) | локальний outbox недоступний | **Відхилити** (fail-closed); для цих операцій виняток не допускається | outbox зберігається; relay — exponential backoff, DLQ | моніторинг backlog + oldest-event age; alert |
| Sensitive read (картка/документ/файл) | durable write недоступний | **Не віддавати контент** до запису спроби (fail-closed) | спроба пишеться окремо від транзакції читання | gap-детектор: контент віддано без access event |
| Security denial / auth attempt | — | пишеться незалежно від відхиленої транзакції; ніколи не блокує відповідь `401/403` | локальна durable черга, best-effort з alert | лічильник dropped security events; alert на >0 |
| Крок system job | outbox недоступний | job позначає крок `AUDIT_PENDING`, повторює | retry за розкладом | вік найстарішого `AUDIT_PENDING` |
| Notification delivery | — | статус `PENDING/FAILED` зберігається в outbox (як зараз); лист не губиться мовчки | attempts + `DEAD` після ліміту | DLQ + alert |
| UI intent / client-reported | транспорт недоступний | best-effort; бізнес-факт не стверджується | одноразова відправка, без черги на клієнті | не вимагається; серверна подія авторитетна |
| Technical log | — | best-effort, ніколи не блокує бізнес-операцію | ні | rate/loss метрики |

### B5. Класифікація полів і правила diff (R6)

Класи полів:

- `IDENT` — ідентифікатори (ID, номери документів): логувати дозволено.
- `STATUS` — статуси/переходи: завжди в diff (old → new).
- `CLINICAL` — медичні значення (дози, показники, результати шкал): diff за allowlist полів,
  без вільного narrative за замовчуванням.
- `NARRATIVE` — вільні тексти (нотатки, причини, коментарі): не копіювати повністю;
  зберігати факт зміни + reason code; повний текст — лише за політикою `TBD-DPO`.
- `PII/PHI` — ПІБ/контакти/адреси пацієнтів: мінімум, business key замість копій; деталі — `TBD-DPO`.
- `SECRET` — паролі, токени, credentials, тіла листів, байти файлів/PDF: **заборонено завжди**.

Таблиця по ключових сутностях (initial proposal; клітини `TBD-DPO` вирішує DPO на F0):

| Сутність | Snapshot | Diff | Критичні поля | Колекції/зв’язки | Статуси |
|---|---|---|---|---|---|
| Episode | ні | так (allowlist) | status, departmentId, dischargeDate | перша ClinicalDay — related | DRAFT→ACTIVE→COMPLETED/ARCHIVED |
| ClinicalDay | ні | так | status, підписанти, closedAt | Signatures — related | OPEN→…→CLOSED/REOPENED |
| HourlyRecord | ні | так, змінені поля | recordTime, показники `CLINICAL` | — | back-entry marker |
| MedicalOrder | ні | так | drugName/dose/route/frequency/status | executions — related | DRAFT/ACTIVE/COMPLETED/CANCELLED |
| OrderExecution | ні | так | planned/actual dose, status | parent order — parent ref | PLAN→…→EXECUTE_FINISH |
| MedicalNote | ні | факт + reason | `NARRATIVE` → TBD-DPO | — | — |
| ScaleResult | так (rawData — за політикою шкали) | так | result values | episode/day refs | — |
| LabResult / PatientState / Ventilation | ні | так, allowlist | ключові показники | — | — |
| FluidBalance | aggregates | так (delta) | intake/output/balance | trigger — parent action | RECALCULATE |
| GeneratedPdf / PrescriptionPdf | ні (ніколи bytes) | версія/checksum/pages | version, checksum | target day/list | GENERATE |
| PrescriptionList/Item/Day/DayPart | ні | так | medicine, dose, flags | 21 день/84 parts — `affectedRecords`, не 84 події | plan/cancel/replan/complete |
| PrescriptionExecution | ні | planned vs actual | witness IDs/ролі, без паролів | dayPart — parent | EXECUTE |
| VitalSign* | ні | так | показники allowlist | автоініціалізація 21 дня — групова подія | GRID_INITIALIZE |
| DrugInteraction dataset | counts+hash | ні (сирі рядки — ні) | sourceHash, severity counts | — | IMPORT |
| FlowTemplate (+tree) | version/snapshot hash | так (meta + counts) | status, version | stages/steps/elements — counts | DRAFT→ACTIVE→ARCHIVED |
| FlowInstance/StepExecution | ні | так (stage/step/status/values allowlist) | status transitions | resources/notes — related | NEW→…→COMPLETED/FAILED/BRANCHED |
| BrakEvent | ні | так | підстава/returnStage | parent/child instances | CONFIRM→BRANCH |
| EvidenceFile | метадані (ніколи bytes) | факт + метадані | fileName/MIME/size/checksum | stepExecution — parent | UPLOAD/DOWNLOAD/DELETE |
| FailureSnapshot | категорія + `NARRATIVE`→TBD | так | category | instance — parent | CREATE |
| User | ні | так | role, deleted state | — | ROLE_CHANGE/DISABLE |
| RolePermission | ні | grant old→new | role+code | — | GRANT/REVOKE |
| SystemSettings (normatives) | ні | old→new | K/N значення | — | CONFIG_UPDATE |
| MIS DTO (Patient/Document/Medicine) | ні | ні (read-only) | call status/latency | target ID як string | CALL_SUCCEEDED/FAILED |

### B6. Relay-семантика (R2)

- Власник: новий компонент `AuditRelay` (common), працює у складі app shell; читає
  module-local outbox-таблиці, пише в центральне сховище core.
- Семантика: **at-least-once** + dedupe `UNIQUE(audit_id)` у core; повторна доставка не створює дублікат.
- Порядок: best-effort за `(occurredAt, auditId)`; не гарантується між DB.
- Root/child: зв’язок через `parentAuditId`; запитний шар робить join незалежно від порядку доставки.
- Час: `occurredAt` ставить producer (авторитетний); `recordedAt` ставить core; сортування історії — за `occurredAt`.
- Інтервал polling/backoff — за вимірюванням навантаження (гіпотеза e2e ≤30 s — §B7, не гарантія).
- Метрики: throughput, backlog, вік найстарішої події, retries, DLQ, дублікати.

### B7. Performance (R3)

Цифри `p95 outbox-write ≤10 ms`, `p95 доставка до пошуку ≤30 s` — **стартові гіпотези** для
навантажувального тесту, не критерії приймання. Acceptance: виміряно на узгодженому обсязі
і retention-горизонті, target затверджено, регресії ключових сценаріїв немає.

### B8. Рішення F0 (узгоджено; реалізаційні параметри зазначено окремо)

- D1. **Тимчасово 2 роки** для нових audit/security events; це не замінює юридичне/DPO
  затвердження до production. Не видаляти legacy чи архів до окремого погодження.
- D2. **Fail-closed**: critical mutation не commit-иться без durable local outbox;
  sensitive PHI read не віддає контент без durable access event. Security denial залишається
  denial і не блокується audit failure; dropped attempt генерує alert.
- D3. **IP + скорочений UA** для security audit. Forwarded IP приймати лише від allowlist
  довірених proxy hops; allowlist/CIDR є deployment config, його значення зараз не відомі.
- D4. **Мінімальний diff**: field/fact за замовчуванням; точні клінічні значення — лише
  structured allowlist і restricted detail permission після DPO-класифікації.
- D5. **Окремий permission `AUDIT_SECURITY_ACCESS`**. Початковий default grant — лише AUDITOR;
  Administrator отримує його лише через затверджену RBAC-зміну, а не неявно через `AUDIT_ACCESS`.
- D6. **Actuator/Micrometer**; перевірити/додати dependency і безпечну інтеграцію з наявним
  моніторингом. Не відкривати Prometheus endpoint анонімно.
- D7. **Довірений reverse proxy**; forwarded headers довіряти лише від налаштованих proxy hops.
  Якщо allowlist відсутній — фіксувати socket peer лише як proxy address або не зберігати client IP;
  не довіряти клієнтському `X-Forwarded-For`.

---

## C. Audit Event Model

| Поле | Правило |
|---|---|
| `auditId` | UUID, генерує backend, незмінний; ключ dedupe |
| `schemaVersion` | версія контракту |
| `occurredAt` / `recordedAt` | UTC `Instant`; подія / потрапляння в core |
| `actorType` | `USER` / `SYSTEM` / `SERVICE` / `INTEGRATION` |
| `actorId`, `actorLogin`, `actorDisplayName` | ID + login snapshot з server-side security context; display name — лише за потреби розслідування |
| `actorRoles` | ролі на момент події; для SYSTEM/SERVICE — ідентифікатор job/service |
| `initiatedBy` | користувач-ініціатор асинхронного процесу; не підміняє фактичного actor |
| `eventClass` | `BUSINESS` / `USER_ACTIVITY` / `SECURITY` / `TECHNICAL` |
| `module`, `functionalArea` | `platform|icu|medication|prosthetics`, напр. `clinical-day`, `prescription`, `wizard`, `rbac` |
| `action`, `actionType` | код каталогу §B1 + тип (`CREATE`, `TRANSITION`, `READ`, `EXPORT`, `DOWNLOAD`, `PRINT_REQUESTED`, …) |
| `target` | `{entityType, entityId: string, businessKey?}` — ID рядком (UUID, MIS ID, user ID) |
| `parentTarget`, `relatedEntities[]` | батьківський і пов’язані об’єкти, незалежно від FK між DB |
| `outcome` | `SUCCESS|FAILURE|DENIED|PARTIAL|CANCELLED|UNKNOWN_LEGACY` |
| `errorCode`, `errorSummary` | санітизований код/опис; без stacktrace/body |
| `changes[]` | поле + old/new після masking; лише allowlist (§B5) |
| `reason` | структурований reason code; вільний коментар — лише за політикою |
| `requestId`, `userActionId`, `correlationId`, `parentAuditId` | §B3 |
| `source` | `WEB_UI|API|SCHEDULED_JOB|STARTUP_IMPORT|INTEGRATION|ADMIN_TOOL`; client-reported маркується |
| `httpContext` | method + route template, без query/body |
| `ipAddress`, `userAgent` | лише за D3 |
| `durationMs`, `affectedRecords` | де має операційне значення |
| `externalCalls[]` | service/procedure/callId/статус/latency; без credentials і PII-параметрів |
| `metadata` | типізований allowlist, не довільний map |
| `integrity` | хеш/підпис або посилання на підписаний архівний batch |

Контури: business / user activity / security / technical — розділені за типом, правами й retention (§B4, §F3).

---

## D. Taxonomy (категорії actionType)

CRUD/життєвий цикл: `CREATE VIEW UPDATE DELETE RESTORE ARCHIVE`.
Workflow: `TRANSITION SIGN REOPEN CLOSE CANCEL ASSIGN REASSIGN COMPLETE FAIL PAUSE RESUME BACKWARD`.
Клінічні/медикаментозні: `PLAN UNPLAN EXECUTE REPLAN BACKDATE RECALCULATE SCORE_CALCULATE`.
Документи: `GENERATE DOWNLOAD PRINT_REQUESTED EXPORT`.
Адміністрування/дані: `PERMISSION_GRANT PERMISSION_REVOKE IMPORT CONFIG_UPDATE`.
Доступ: `SEARCH ROSTER_VIEW RECORD_VIEW DOCUMENT_VIEW HISTORY_VIEW ACCESS_DENIED`.
Інтеграції: `CALL_SUCCEEDED CALL_FAILED DELIVERY_SENT DELIVERY_FAILED DELIVERY_SKIPPED`.
`PRINT_REQUESTED` = UI ініціював print flow; фізичний друк сервером не стверджується.

---

## E. Audit Matrix (атомарні дії)

Легенда. `Flags`: `M` mandatory · `Rec` recommended · `T` technical · `S` security ·
`R` read · `W` write · `D` destructive · `A` automated.
Клас події — §C (`BUSINESS|USER_ACTIVITY|SECURITY`).
Catalog v1: 120 атомарних кодів (`platform` 19, `icu` 43, `medication` 26, `prosthetics` 32),
зафіксованих у `AuditActionCatalog`; underscore дозволений у сегментах коду.
`Tests`: ID з префіксом `PROP-` — **запропоновані** (не існуючі); мапінг:
`PROP-IT-*` → `backend/*/src/test/...` (інтеграційні),
`PROP-SEC-*` → security-інтеграційні, `PROP-E2E-*` → `tests/specs/...`,
`PROP-U-*` → unit-тести сервісів/каталогу.

### E.0 Platform / common

| # | Action (код) | Дія користувача | Target | Type / Flags / Клас | Audit data | Actor | Outcome | Prio | Tests | Стан зараз |
|---|---|---|---|---|---|---|---|---|---|---|
| P01 | `platform.auth.session.login` | Увійти (LOCAL/LDAP) | User | `AUTHENTICATE` / M,S,W / SECURITY | provider, role snapshot, error code; без пароля/токена | USER / SYSTEM(initiatedBy: LDAP identity) | SUCCESS/FAILURE | M0 | PROP-IT-PLAT-01, PROP-SEC-01, PROP-E2E-PLAT-01 | LOGIN є |
| P02 | `platform.auth.session.login.failed` | Невдала спроба входу | User? | `AUTHENTICATE` / M,S,W / SECURITY | login у sanitized details, attempt counters | USER | FAILURE | M0 | PROP-SEC-01 | LOGIN_FAILED є |
| P03 | `platform.auth.session.login.blocked` | Заблокована спроба (rate-limit) | Login identity | `AUTHENTICATE` / M,S,W / SECURITY | причина блокування | SYSTEM | DENIED | M0 | PROP-SEC-01 | LOGIN_BLOCKED є |
| P04 | `platform.auth.session.logout` | Вийти | Session | `LOGOUT` / M,S,W / SECURITY | session ref | USER | SUCCESS | M0 | PROP-IT-PLAT-01 | LOGOUT є |
| P05 | `platform.auth.directory.provision` | Перший LDAP bind (створення GUEST) | User | `PROVISION` / M,S,W / SECURITY | LDAP login, collision outcome | SYSTEM (initiatedBy: LDAP identity) | SUCCESS/FAILURE | M0 | PROP-SEC-02 | окремої події немає |
| P06 | `platform.auth.token.rejected` | Невалідний/відкликаний токен | Session/token jti | `ACCESS_DENIED` / M,S,R / SECURITY | причина (expired/revoked/unknown), без токена | SYSTEM | DENIED | M0 | PROP-SEC-03 | немає структурованої події |
| P07 | `platform.user.view` | Переглянути користувачів | User list | `VIEW` / Rec,S,R / USER_ACTIVITY | scope/count | ADMIN | SUCCESS/DENIED | M1 | PROP-E2E-PLAT-02 | немає |
| P08 | `platform.user.role.change` | Змінити роль | User (numeric ID string) | `ROLE_CHANGE` / M,S,W / SECURITY | old→new role, target user ID | ADMIN | SUCCESS/FAILURE | M0 | PROP-IT-PLAT-02, PROP-SEC-04 | дія є, але `entityId=null`, старої ролі немає |
| P09 | `platform.user.disable` | Soft-delete користувача | User | `DISABLE` / M,S,W,D / BUSINESS | попередній/новий state | ADMIN | SUCCESS/FAILURE | M0 | PROP-IT-PLAT-02 | дія є, без target ID |
| P10 | `platform.rbac.permission.grant` | Надати permission ролі | RolePermission | `PERMISSION_GRANT` / M,S,W / SECURITY | role, code, old→new | ADMIN | SUCCESS/FAILURE | M0 | PROP-IT-PLAT-03, PROP-SEC-05 | подія є, неструктурована |
| P11 | `platform.rbac.permission.revoke` | Забрати permission | RolePermission | `PERMISSION_REVOKE` / M,S,W / SECURITY | role, code, old→new | ADMIN | SUCCESS/FAILURE | M0 | PROP-IT-PLAT-03 | подія є, неструктурована |
| P12 | `platform.audit.search` | Шукати в аудиті | Audit query | `SEARCH` / Rec,S,R / USER_ACTIVITY | фільтри без PII, result count | USER | SUCCESS/DENIED | M1 | PROP-IT-AUD-01, PROP-E2E-AUD-01 | сам перегляд не аудититься |
| P13 | `platform.audit.detail.view` | Відкрити картку події | AuditEvent | `RECORD_VIEW` / Rec,S,R / USER_ACTIVITY | auditId | USER | SUCCESS/DENIED | M1 | PROP-IT-AUD-01 | немає |
| P14 | `platform.patient.search` | Пошук пацієнта (ICU/med/prosth scope) | Patient roster | `SEARCH` / M(S PHI),R / USER_ACTIVITY | scope, result count; без сирого query | USER | SUCCESS/DENIED | M0 | PROP-IT-PLAT-04, PROP-E2E-READ-01 | загальний `MIS GET_ALL_PATIENTS` |
| P15 | `platform.patient.record.view` | Відкрити профіль пацієнта | Patient (MIS ID string) | `RECORD_VIEW` / M,R / USER_ACTIVITY | target ID | USER | SUCCESS/DENIED | M0 | PROP-IT-PLAT-04 | точної view-події немає |
| P16 | `platform.mis.document.view` | Переглянути документ MIS | MIS document | `DOCUMENT_VIEW` / M,R / USER_ACTIVITY | document ID, availability outcome | USER; child SERVICE→INTEGRATION | SUCCESS/FAILURE | M0 | PROP-IT-PLAT-05 | загальний `GET_PATIENT_DOCUMENTS` |
| P17 | `platform.mis.catalog.view` | Пошук у каталозі ліків | Medicine catalog | `CATALOG_VIEW` / Rec,R / USER_ACTIVITY | scope/count | USER | SUCCESS | M2 | PROP-IT-PLAT-05 | загальний `SEARCH_MEDICINE_CATALOG` |
| P18 | `platform.bootstrap.seed` | Стартовий сід users/roles/data | Seed scope | `BOOTSTRAP` / Rec,W,A / BUSINESS | версія, counts; без env values | SYSTEM | SUCCESS/PARTIAL/FAILURE | M1 | PROP-IT-SYS-01 | лише SLF4J |
| P19 | `platform.auth.access.denied` | Відмова в доступі (403) | Захищений ресурс | `ACCESS_DENIED` / M,S,W / SECURITY | route, actor, ролі | USER | DENIED | M0 | PROP-SEC-04 | немає структурованої події |

### E.1 ICU Chart (I01–I43)

| # | Action | Дія | Target | Type/Flags/Клас | Audit data | Actor | Outcome | Prio | Tests | Стан |
|---|---|---|---|---|---|---|---|---|---|---|
| I01 | `icu.roster.view` | Переглянути dashboard/department roster | Roster scope | `ROSTER_VIEW` / Rec,R / USER_ACTIVITY | scope/count | USER | SUCCESS | M1 | PROP-IT-ICU-01 | немає |
| I02 | `icu.episode.record.view` | Відкрити картку пацієнта/епізод | Episode | `RECORD_VIEW` / M,R / USER_ACTIVITY | target + action ID | USER | SUCCESS/DENIED | M0 | PROP-E2E-ICU-READ-01 | немає |
| I03 | `icu.episode.create` | Створити картку (+ перша доба) — root | Episode (+ClinicalDay related) | `CREATE` / M,W / BUSINESS | fields allowlist, initial day ref | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-02, PROP-E2E-ICU-01 | 2 окремі події без root |
| I04 | `icu.episode.update` | Редагувати епізод | Episode | `UPDATE` / M,W / BUSINESS | field diff | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-02 | без diff |
| I05 | `icu.episode.close` | Закрити епізод | Episode | `CLOSE` / M,W / BUSINESS | status old→new, discharge data | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-02 | без status diff |
| I06 | `icu.episode.archive` | Архівувати | Episode | `ARCHIVE` / M,W / BUSINESS | status old→new | USER (передати actor з контролера) | SUCCESS/FAILURE | M0 | PROP-IT-ICU-02 | без actor і audit |
| I07 | `icu.clinical_day.create` | Створити добу | ClinicalDay | `CREATE` / M,W / BUSINESS | dayNumber, interval | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-03 | частково є |
| I08 | `icu.clinical_day.update` | Редагувати добу | ClinicalDay | `UPDATE` / M,W / BUSINESS | field diff | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-03 | — |
| I09 | `icu.clinical_day.sign.nurse` | Підпис медсестри | ClinicalDay | `SIGN` / M,W / BUSINESS | signer ID/role, час, version | USER | SUCCESS/DENIED/FAILURE | M0 | PROP-IT-ICU-03, PROP-E2E-ICU-02 | SIGN_NURSE є |
| I10 | `icu.clinical_day.sign.doctor` | Підпис лікаря | ClinicalDay | `SIGN` / M,W / BUSINESS | те саме | USER | SUCCESS/DENIED/FAILURE | M0 | PROP-IT-ICU-03 | SIGN_DOCTOR є |
| I11 | `icu.clinical_day.close.early` | Дострокове закриття | ClinicalDay | `CLOSE_EARLY` / M,W / BUSINESS | status diff, reason code | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-03 | є, без diff |
| I12 | `icu.clinical_day.reopen` | Перевідкрити | ClinicalDay | `REOPEN` / M,W / BUSINESS | status diff, reason | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-03 | є |
| I13 | `icu.hourly_record.create` | Створити годинний запис | HourlyRecord | `CREATE` / M,W / BUSINESS | структуровані поля | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-04 | є |
| I14 | `icu.hourly_record.update` | Змінити запис | HourlyRecord | `UPDATE` / M,W / BUSINESS | field diff | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-04 | загальна фраза |
| I15 | `icu.hourly_record.backdate` | Внести заднім числом | HourlyRecord | `BACKDATE` / M,W / BUSINESS | record time, marker | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-04 | BACK_ENTRY є |
| I16 | `icu.medical_order.create` | Створити призначення | MedicalOrder | `CREATE` / M,W / BUSINESS | allowlist (PHI-клас) | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-05 | є |
| I17 | `icu.medical_order.update` | Змінити призначення | MedicalOrder | `UPDATE` / M,W / BUSINESS | field diff | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-05 | без diff |
| I18 | `icu.medical_order.cancel` | Скасувати призначення | MedicalOrder | `CANCEL` / M,W / BUSINESS | status old→new | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-05 | є |
| I19 | `icu.order_execution.plan` | Запланувати виконання | OrderExecution | `PLAN` / M,W / BUSINESS | parent order, dose | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-06 | є |
| I20 | `icu.order_execution.plan.finish` | Завершити планування | OrderExecution | `PLAN_FINISH` / M,W / BUSINESS | transition | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-06 | є |
| I21 | `icu.order_execution.cancel` | Скасувати виконання | OrderExecution | `CANCEL` / M,W / BUSINESS | transition | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-06 | є |
| I22 | `icu.order_execution.execute` | Виконати | OrderExecution | `EXECUTE` / M,W / BUSINESS | dose/status diff, executor | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-06, PROP-E2E-ICU-05 | є |
| I23 | `icu.order_execution.execute.finish` | Завершити виконання | OrderExecution | `EXECUTE_FINISH` / M,W / BUSINESS | transition | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-06 | є |
| I24 | `icu.order_execution.backdate` | Виконати заднім числом | OrderExecution | `BACKDATE` / M,W / BUSINESS | hour, marker | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-06 | BACK_ENTRY є |
| I25 | `icu.medical_note.create` | Додати нотатку | MedicalNote | `CREATE` / M,W / BUSINESS | факт + reason; narrative TBD | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-07 | є |
| I26 | `icu.medical_note.update` | Змінити нотатку | MedicalNote | `UPDATE` / M,W / BUSINESS | факт зміни | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-07 | загальна фраза |
| I27 | `icu.scale.create` | Додати результат шкали | ScaleResult | `CREATE` / M,W / BUSINESS | values за політикою | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-07 | є |
| I28 | `icu.scale.calculate` | Розрахувати шкалу | ScaleResult | `SCORE_CALCULATE` / M,W / BUSINESS | inputs hash/class, result | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-07 | виглядає як CREATE |
| I29 | `icu.scale.update` | Змінити результат | ScaleResult | `UPDATE` / M,W / BUSINESS | diff | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-07 | загальна фраза |
| I30 | `icu.lab_result.create` | Додати лабораторний результат | LabResult | `CREATE` / M,W / BUSINESS | allowlist значень | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-07 | є |
| I31 | `icu.lab_result.update` | Змінити лабораторний результат | LabResult | `UPDATE` / M,W / BUSINESS | field diff | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-07 | є, без diff |
| I32 | `icu.patient_state.create` | Додати оцінку стану | PatientStateAssessment | `CREATE` / M,W / BUSINESS | allowlist значень | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-07 | є |
| I33 | `icu.patient_state.update` | Змінити оцінку стану | PatientStateAssessment | `UPDATE` / M,W / BUSINESS | field diff | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-07 | є, без diff |
| I34 | `icu.ventilation.create` | Додати налаштування вентиляції | VentilationSettings | `CREATE` / M,W / BUSINESS | allowlist значень | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-07 | є |
| I35 | `icu.ventilation.update` | Змінити налаштування вентиляції | VentilationSettings | `UPDATE` / M,W / BUSINESS | field diff | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-07 | є, без diff |
| I36 | `icu.fluid_balance.recalculate` | Перерахунок балансу | FluidBalance | `RECALCULATE` / Rec,W(,A) / BUSINESS | trigger/parent action, delta | USER або SYSTEM | SUCCESS/FAILURE | M1 | PROP-IT-ICU-08 | є, без кореляції |
| I37 | `icu.pdf.generate` | Сформувати PDF доби | GeneratedPdf | `GENERATE` / M,W / BUSINESS | version/checksum, target PDF ID + day relation | USER або SYSTEM | SUCCESS/FAILURE | M0 | PROP-IT-ICU-09 | є (`entityId`=day — виправити target) |
| I38 | `icu.pdf.download` | Завантажити PDF | GeneratedPdf | `DOWNLOAD` / M,R / USER_ACTIVITY | PDF ID/version | USER | SUCCESS/DENIED | M0 | PROP-E2E-ICU-08 | не розділено |
| I39 | `icu.pdf.print.requested` | Ініціювати друк | GeneratedPdf | `PRINT_REQUESTED` / M,R / USER_ACTIVITY | client intent | USER | SUCCESS | M0 | PROP-E2E-ICU-08 | не розділено |
| I40 | `icu.clinical_day.auto_close` | Автозакриття 07:00 (root: close+recalc+pdf+email) | ClinicalDay | `AUTO_CLOSE` / M,W,A / BUSINESS | job ID, status diff, children | SYSTEM | SUCCESS/PARTIAL/FAILURE | M0 | PROP-IT-ICU-10, PROP-E2E-SYS-01 | є з actor `0L` |
| I41 | `icu.clinical_day.escalate` | Ескалація 09:00 | ClinicalDay | `ESCALATE` / M,W,A / BUSINESS | job ID | SYSTEM | SUCCESS/FAILURE | M1 | PROP-IT-ICU-10 | є з actor `0L` |
| I42 | `icu.notification.delivery.outcome` | Email-доставлення ескалації | Email delivery | `DELIVERY_*` / M,W,A / BUSINESS | attempts, provider outcome | SERVICE | SUCCESS/FAILURE | M0 | PROP-IT-ICU-10 | лише SLF4J |
| I43 | `icu.order_execution.correct` | Виправити заплановане виконання (доза/коментар) | OrderExecution | `UPDATE` / M,W / BUSINESS | field fact (без точних значень до restricted storage) | USER | SUCCESS/FAILURE | M0 | PROP-IT-ICU-06 | логувалось як загальний UPDATE без дії |

### E.2 Medication Sheet

| # | Action | Дія | Target | Type/Flags/Клас | Audit data | Actor | Outcome | Prio | Tests | Стан |
|---|---|---|---|---|---|---|---|---|---|---|
| D01 | `medication.list.view` | Відкрити листок | PrescriptionList | `RECORD_VIEW` / M,R / USER_ACTIVITY | list/patient ref | USER | SUCCESS/DENIED | M0 | PROP-E2E-MED-01 | немає |
| D02 | `medication.list.create` | Створити листок | PrescriptionList | `CREATE` / M,W / BUSINESS | patient ref, business key | USER | SUCCESS/FAILURE | M0 | PROP-IT-MED-01 | лише SLF4J |
| D03 | `medication.list.rename` | Перейменувати (якщо викликається) | PrescriptionList | `UPDATE` / M,W / BUSINESS | old→new name | USER | SUCCESS/FAILURE | M0 | PROP-IT-MED-01 | немає |
| D04 | `medication.list.close` | Закрити листок | PrescriptionList | `CLOSE` / M,W / BUSINESS | status old→new | USER | SUCCESS/FAILURE | M0 | PROP-IT-MED-01 | лише SLF4J |
| D05 | `medication.list.delete` | Soft-delete | PrescriptionList | `DELETE` / M,W,D / BUSINESS | deleted state | USER | SUCCESS/FAILURE | M0 | PROP-IT-MED-01 | лише SLF4J |
| D06 | `medication.item.add` | Додати item (+21 день/parts — root) | PrescriptionItem | `ITEM_ADD` / M,W / BUSINESS | medicine ref, affectedRecords | USER | SUCCESS/FAILURE | M0 | PROP-IT-MED-02, PROP-E2E-MED-02 | немає audit |
| D07 | `medication.item.remove` | Видалити item | PrescriptionItem | `ITEM_REMOVE` / M,W,D / BUSINESS | item ref, parent list | USER | SUCCESS/FAILURE | M0 | PROP-IT-MED-02 | немає audit |
| D08 | `medication.item.day.add` | Додати день | PrescriptionItemDay | `DAY_ADD` / M,W / BUSINESS | dayDate, parent item | USER | SUCCESS/FAILURE | M0 | PROP-IT-MED-02 | немає audit |
| D09 | `medication.item.day.remove` | Видалити день | PrescriptionItemDay | `DAY_REMOVE` / M,W,D / BUSINESS | day ref | USER | SUCCESS/FAILURE | M0 | PROP-IT-MED-02 | REMOVE є |
| D10 | `medication.dose.plan` | Запланувати дозу | PrescriptionDayPart | `PLAN` / M,W / BUSINESS | dose/status diff, period refs | USER | SUCCESS/FAILURE | M0 | PROP-IT-MED-03 | PLAN є |
| D11 | `medication.dose.cancel` | Відмінити препарат | PrescriptionDayPart | `CANCEL` / M,W / BUSINESS | diff + reason | USER | SUCCESS/FAILURE | M0 | PROP-IT-MED-03 | CANCEL є |
| D12 | `medication.dose.replan` | Повернути в заплановані | PrescriptionDayPart | `REPLAN` / M,W / BUSINESS | diff | USER | SUCCESS/FAILURE | M0 | PROP-IT-MED-03 | REPLAN є |
| D13 | `medication.dose.unassign` | Відмінити призначення клітинки | PrescriptionDayPart | `UNASSIGN` / M,W / BUSINESS | diff | USER | SUCCESS/FAILURE | M0 | PROP-IT-MED-03 | CANCEL_ASSIGNMENT є |
| D14 | `medication.dose.complete` | Позначити завершеним | PrescriptionDayPart | `COMPLETE` / M,W / BUSINESS | transition | USER | SUCCESS/FAILURE | M0 | PROP-IT-MED-03, PROP-E2E-MED-03 | `markCompleted` без audit |
| D15 | `medication.dose.execute` | Виконати дозу (2 медсестри) | PrescriptionExecution | `DOSE_EXECUTE` / M,W / BUSINESS | planned vs actual, executor+witness IDs/ролі; без паролів | USER + witness | SUCCESS/FAILURE | M0 | PROP-IT-MED-04, PROP-SEC-07, PROP-E2E-MED-04 | немає business audit; login-и в SLF4J |
| D16 | `medication.vitals.grid.view` | Відкрити сітку показників | VitalSignList | `GRID_VIEW` / M,R / USER_ACTIVITY | list ref | USER | SUCCESS | M0 | PROP-E2E-MED-05 | немає |
| D17 | `medication.vitals.grid.initialize` | Автоініціалізація 21 дня (групова) | VitalSignList | `GRID_INITIALIZE` / M,W(,A) / BUSINESS | affectedRecords, causation | USER-triggered / SYSTEM+initiatedBy | SUCCESS/FAILURE | M0 | PROP-IT-MED-05 | створюється під GET, actor `0L` |
| D18 | `medication.vitals.entry.create` | Створити entry | VitalSignEntry | `ENTRY_CREATE` / M,W / BUSINESS | period/day refs | USER | SUCCESS/FAILURE | M0 | PROP-IT-MED-05 | немає |
| D19 | `medication.vitals.entry.update` | Змінити показник | VitalSignEntry | `ENTRY_UPDATE` / M,W / BUSINESS | field diff | USER | SUCCESS/FAILURE | M0 | PROP-IT-MED-05 | немає |
| D20 | `medication.interactions.import` | Імпорт dataset (admin) | DrugInteraction dataset | `IMPORT` / M,S,W / BUSINESS | counts, sourceHash, duration; без сирих рядків | ADMIN / SERVICE(runner) | SUCCESS/FAILURE | M0 | PROP-IT-MED-06, PROP-E2E-MED-06 | JSON report у `new_value` — прибрати payload |
| D21 | `medication.interactions.catalog.view` | Перегляд бази взаємодій | Catalog query | `CATALOG_VIEW` / Rec,R / USER_ACTIVITY | scope/count | ADMIN | SUCCESS/DENIED | M1 | PROP-IT-MED-06 | немає повного access event |
| D22 | `medication.interactions.warning.view` | Перегляд попереджень листка | PrescriptionList | `INTERACTION_WARNING_VIEW` / Rec,R / USER_ACTIVITY | warned count | USER | SUCCESS | M1 | PROP-IT-MED-06 | немає |
| D23 | `medication.pdf.info` | Переглянути info батчу | PrescriptionList | `PDF_INFO` / Rec,R / USER_ACTIVITY | pages | USER | SUCCESS | M2 | PROP-IT-MED-07 | немає |
| D24 | `medication.pdf.generate` | Сформувати ZIP/сторінку | Prescription PDF batch | `GENERATE` / M,W / BUSINESS | pages/version, без bytes | USER | SUCCESS/FAILURE | M0 | PROP-IT-MED-07, PROP-E2E-MED-07 | GENERATE є |
| D25 | `medication.pdf.download` | Завантажити ZIP/PDF | Prescription PDF batch | `DOWNLOAD` / M,R / USER_ACTIVITY | format | USER | SUCCESS/DENIED | M0 | PROP-E2E-MED-07 | не розділено |
| D26 | `medication.pdf.print.requested` | Ініціювати друк | Prescription PDF batch | `PRINT_REQUESTED` / M,R / USER_ACTIVITY | intent | USER | SUCCESS | M0 | PROP-E2E-MED-07 | не розділено |

### E.3 Prosthetics Manufacturing (R01–R32)

| # | Action | Дія | Target | Type/Flags/Клас | Audit data | Actor | Outcome | Prio | Tests | Стан |
|---|---|---|---|---|---|---|---|---|---|---|
| R01 | `prosthetics.candidate.search` | Пошук кандидатів | Candidate roster | `SEARCH` / M,R / USER_ACTIVITY | scope/count | USER | SUCCESS | M0 | PROP-IT-PRO-01 | загальний MIS audit |
| R02 | `prosthetics.patient.record.view` | Відкрити пацієнта | Patient (string ID) | `RECORD_VIEW` / M,R / USER_ACTIVITY | target | USER | SUCCESS/DENIED | M0 | PROP-E2E-PRO-01 | немає |
| R03 | `prosthetics.order.document.view` | Переглянути документ MIS | MIS document | `DOCUMENT_VIEW` / M,R / USER_ACTIVITY | document ID, availability | USER | SUCCESS/FAILURE | M0 | PROP-IT-PRO-01 | загальний audit |
| R04 | `prosthetics.order.provision` | Provision local order (root; +local patient related) | ProstheticsOrder | `ORDER_PROVISION` / M,W / BUSINESS | MIS IDs, business key `MIS-{p}-{d}` | USER; child SERVICE→INTEGRATION | SUCCESS/FAILURE | M0 | PROP-IT-PRO-01, PROP-E2E-PRO-01 | створює order/patient без audit |
| R05 | `prosthetics.template.create.version` | Створити версію (+tree) | FlowTemplate | `CREATE_VERSION` / M,W / BUSINESS | snapshot hash, counts | ADMIN | SUCCESS/FAILURE | M0 | PROP-IT-PRO-02 | CREATE є |
| R06 | `prosthetics.template.update` | Змінити meta/status | FlowTemplate | `UPDATE` / M,W / BUSINESS | diff | ADMIN | SUCCESS/FAILURE | M0 | PROP-IT-PRO-02 | без diff |
| R07 | `prosthetics.template.archive` | Архівувати | FlowTemplate | `ARCHIVE` / M,W,D / BUSINESS | status old→new | ADMIN | SUCCESS/FAILURE | M0 | PROP-IT-PRO-02 | ARCHIVE є |
| R08 | `prosthetics.instance.create` | Створити instance | FlowInstance | `CREATE` / M,W / BUSINESS | order/patient refs, template version/hash | USER | SUCCESS/DENIED/FAILURE | M0 | PROP-IT-PRO-03, PROP-E2E-PRO-03 | CREATE є |
| R09 | `prosthetics.instance.view` | Відкрити деталі процесу | FlowInstance | `RECORD_VIEW` / Rec,R / USER_ACTIVITY | target | USER | SUCCESS/DENIED | M1 | PROP-IT-PRO-03 | немає |
| R10 | `prosthetics.instance.snapshot.view` | Відкрити snapshot шаблону | FlowInstance snapshot | `SNAPSHOT_VIEW` / M,R / USER_ACTIVITY | target, template version/hash | USER | SUCCESS/DENIED | M0 | PROP-IT-PRO-03 | немає |
| R11 | `prosthetics.instance.start` | Start | FlowInstance | `START` / M,W / BUSINESS | status/stage/step old→new | USER | SUCCESS/FAILURE | M0 | PROP-IT-PRO-04 | є |
| R12 | `prosthetics.step.complete` | Complete step (root: values+resources+advance) | StepExecution | `STEP_COMPLETE` / M,W / BUSINESS | values allowlist, resources, advance refs | USER | SUCCESS/FAILURE | M0 | PROP-IT-PRO-04, PROP-E2E-PRO-04 | COMPLETE є, без зібраного diff |
| R13 | `prosthetics.resource.record` | Зберегти витрати (child step-complete) | ResourceUsage | `RESOURCE_RECORD` / M,W / BUSINESS | amounts | USER | SUCCESS/FAILURE | M0 | PROP-IT-PRO-04 | окремої події немає |
| R14 | `prosthetics.step.note.update` | Змінити нотатку кроку | StepExecution | `UPDATE` / M,W / BUSINESS | факт + reason; текст TBD | USER | SUCCESS/FAILURE | M0 | PROP-IT-PRO-04 | NOTE є |
| R15 | `prosthetics.instance.backward` | Повернення на крок | FlowInstance | `BACKWARD` / M,W / BUSINESS | old→new step | USER | SUCCESS/FAILURE | M0 | PROP-IT-PRO-04 | є |
| R16 | `prosthetics.instance.pause` | Пауза з категорією | FlowInstance | `PAUSE` / M,W / BUSINESS | status, pause code | USER | SUCCESS/FAILURE | M0 | PROP-IT-PRO-05 | є |
| R17 | `prosthetics.instance.resume` | Відновлення | FlowInstance | `RESUME` / M,W / BUSINESS | status, idle delta | USER | SUCCESS/FAILURE | M0 | PROP-IT-PRO-05 | є |
| R18 | `prosthetics.instance.fail` | Terminal failure + snapshot | FlowInstance (+FailureSnapshot) | `FAIL` / M,W / BUSINESS | category, status old→new | USER | SUCCESS/FAILURE | M0 | PROP-IT-PRO-05, PROP-E2E-PRO-05 | FAIL є |
| R19 | `prosthetics.brak.confirm` | Підтвердити брак (root: R19–R21) | BrakEvent | `BRAK_CONFIRM` / M,W / BUSINESS | підстава/returnStage | USER | SUCCESS/FAILURE | M0 | PROP-IT-PRO-06, PROP-E2E-PRO-06 | розбито на 3 записи без root |
| R20 | `prosthetics.brak.branch` | Змінити original + створити branch (child brаk) | FlowInstance | `BRANCH` / M,W / BUSINESS | parent/child refs, статуси | USER | SUCCESS/FAILURE | M0 | PROP-IT-PRO-06 | є як окремі записи |
| R21 | `prosthetics.notification.queue` | Поставити SINGLE/THRESHOLD | Outbox row | `QUEUE` / M,W / BUSINESS | kind, order chain count | USER (ініціатор) | SUCCESS | M0 | PROP-IT-PRO-06 | outbox є |
| R22 | `prosthetics.evidence.upload` | Завантажити файл | EvidenceFile | `UPLOAD` / M,W / BUSINESS | name/MIME/size/checksum, parent | USER | SUCCESS/FAILURE/DENIED | M0 | PROP-IT-PRO-07, PROP-SEC-10 | UPLOAD є |
| R23 | `prosthetics.evidence.list.view` | Переглянути список файлів | StepExecution files | `LIST_VIEW` / Rec,R / USER_ACTIVITY | count | USER | SUCCESS/DENIED | M1 | PROP-IT-PRO-07 | немає |
| R24 | `prosthetics.evidence.download` | Завантажити bytes | EvidenceFile | `DOWNLOAD` / M,R / USER_ACTIVITY | file ID, checksum | USER | SUCCESS/DENIED | M0 | PROP-IT-PRO-07, PROP-E2E-PRO-07 | немає |
| R25 | `prosthetics.evidence.delete` | Видалити файл | EvidenceFile | `DELETE` / M,W,D / BUSINESS | file ref | USER | SUCCESS/FAILURE | M0 | PROP-IT-PRO-07 | DELETE є |
| R26 | `prosthetics.report.generate` | Сформувати звіт | FlowInstance report | `GENERATE` / M,W / BUSINESS | type/version/checksum | USER | SUCCESS/FAILURE | M0 | PROP-IT-PRO-08 | GET PDF не аудититься |
| R27 | `prosthetics.report.download` | Завантажити звіт | Report | `DOWNLOAD` / M,R / USER_ACTIVITY | intent/format | USER | SUCCESS/DENIED | M0 | PROP-E2E-PRO-08 | не розділено |
| R28 | `prosthetics.report.print.requested` | Ініціювати друк звіту | Report | `PRINT_REQUESTED` / M,R / USER_ACTIVITY | client intent | USER | SUCCESS | M0 | PROP-E2E-PRO-08 | не розділено |
| R29 | `prosthetics.production.worklist.view` | Списки/KPI/attention/team | Worklist scope | `WORKLIST_VIEW` / Rec,R / USER_ACTIVITY | scope/filters | USER | SUCCESS/DENIED | M1 | PROP-IT-PRO-09 | немає |
| R30 | `prosthetics.production.detail.view` | Деталі work item | Work item | `DETAIL_VIEW` / M,R / USER_ACTIVITY | target; masking за PATIENT_VIEW | USER | SUCCESS/DENIED | M0 | PROP-IT-PRO-09, PROP-E2E-PRO-09 | немає |
| R31 | `prosthetics.production.normative.update` | Змінити K/N | SystemSettings | `CONFIG_UPDATE` / M,S,W / BUSINESS | old→new | ADMIN | SUCCESS/FAILURE | M0 | PROP-IT-PRO-09 | без old/new |
| R32 | `prosthetics.notification.delivery.outcome` | Доставлення листа (sweep) | Outbox row | `DELIVERY_*` / M,W,A / BUSINESS | kind/attempt/provider outcome/recipient count | SERVICE (initiatedBy: ініціатор браку) | SUCCESS/PARTIAL/FAILURE | M0 | PROP-IT-PRO-10, PROP-E2E-PRO-10 | частково є; actor доставлення виправити |

App shell окремої бізнес-моделі не має; його scope — кореляція, security filter, bootstrap/seeds,
scheduled/integration результати (відображені вище). CSV-експортів і активного legacy MedicineList
importer у поточному коді не знайдено; єдиний migration runner — `DrugInteractionImportRunner`.

---

## F. Фази реалізації (F0–F8)

> Нормативний опис кожної фази — у відповідному GitHub Issue (§J). Нижче — огляд,
> порядок і залежності. Код — тільки після окремої вказівки.

| Фаза | Назва | Залежності | Ключовий результат |
|---|---|---|---|
| F0 | Foundation: рішення, каталог, політики | — (D1–D7 owner decisions отримані; DPO field allowlist лишається production prerequisite) | Catalog v1 (118 atomic actions), §B4, §B5-TBD, naming, failure/metrics правила; catalog + unit/ArchUnit tests implemented locally |
| F1 | Event model + storage | F0 | Contract, `audit_events` + module outbox tables + `audit_event_targets`, indexes and UTC rules; schema smoke on PostgreSQL remains |
| F2 | Correlation + relay | F1 | request/action/correlation IDs, `AuditRelay` at-least-once + dedupe, backlog-метрики |
| F3 | Security audit | F0–F2 | Незалежний security path: auth/token/denials/provisioning/admin |
| F4 | ICU coverage | F0–F3, D-рішення | E.1 повністю: writes + значущі reads + scheduled (I40–I42 як root/children) |
| F5 | Medication coverage | F0–F3, D-рішення | E.2 повністю; прибрати `0L`, SLF4J-login-и, payload у `new_value` |
| F6 | Prosthetics coverage | F0–F3, D-рішення | E.3 повністю (R01–R32); root для brak/step; actor доставки = SERVICE |
| F7 | Read audit + frontend + консоль | F0–F6 | Межі read events, UI intent hook, новий пошук/detail/object history, аудит самої консолі |
| F8 | Migration + integrity + retention + monitoring + gate | F0–F7 | Dual write, legacy-backfill без вигадок, append-only, архів, coverage-guard, виміряна продуктивність |

Порядок: F0 → F1 → F2 → F3, далі F4/F5/F6 можуть іти паралельно після F3, F7 після F4–F6
(read-межі залежать від матриць), F8 останньою (потрібні всі події і каталог).

---

## G. Test Plan (зв’язок з фазами)

- Unit (F0–F1): builder, унікальність каталогу, actor з security context, diff/no-op, masking,
  string entityId для UUID/MIS/user ID, серіалізація без PII/секретів.
- Transactional integration (F1–F2, F4–F6): мутація + outbox commit; rollback — немає success event;
  outbox-failure — rollback критичної мутації; relay outage → outbox зберігається → доставка після відновлення.
- Idempotency/retry (F2): повторна доставка — без дубліката; однакові команди — різні action ID;
  optimistic locking — чіткий outcome.
- Security (F3): LOGIN_FAILED/BLOCKED, invalid/revoked JWT, `401/403`, denial матриці,
  LDAP provisioning, доступ до audit API; жодних паролів/токенів у подіях і логах.
- Privacy (F0, F7): fixtures з PHI/секретоподібними значеннями; заборона body/файлів/bytes/credentials/narrative;
  masking у API + окремий restricted доступ.
- Read access (F7): одна значуща подія на відкриття; кореляція внутрішніх fetch; без події на символ autocomplete.
- System/integrations (F4, F6): auto-close/escalation/recalc/PDF/email; SINGLE/THRESHOLD queue/deliver/retry/skip/dead;
  migration-profile import; MIS success/failure без PII-параметрів.
- API/UI (F7): комбіновані фільтри, стабільна пагінація, detail зі зв’язками, аудит доступу до консолі,
  deny для ролей без доступу, object history з root/child.
- E2E (F4–F7): щонайменше один повний ланцюг на кожен M0-ряд:
  `UI → API → service → DB/outbox → relay → core event → audit UI/object history`.
- Coverage guard (F8): тест звіряє каталог + §E: кожна дія має module, actor policy, outcome,
  data classification і test reference; кожен write route — mapping або затверджене виключення.
- Load/performance (F8): обсяг за retention-горизонтом, latency пошуку, throughput relay,
  поведінка при backlog; target — затвердити вимірюванням (§B7).

---

## H. Migration Plan

1. `audit_logs` не видаляти і не переписувати; зберегти всі записи включно з `is_deleted=true` для backfill.
2. Нові таблиці — лише новими changeset-ами (core після `core/008`, ICU після `icu/007`,
   med після `med/006`, prosth після `prosth/012`); застосовані changeset-и не редагувати.
3. Dual write для нових M0/M1: новий event → local outbox; старий endpoint/table — read-only.
4. Backfill ідемпотентно, зі збереженням source table/id і checksum; мітки:
   `legacy.http.request` (`API_*`), `legacy.audit.action` (старі ручні події),
   `schemaVersion=0`, `contextCompleteness=LEGACY`, `outcome=UNKNOWN_LEGACY` де результат невідомий.
5. Не реконструювати відсутні old/new з тексту чи URI; старі `entity/action` зберігати дослівно.
6. У новому UI розрізняти legacy і повні події; legacy не подавати як доказ успішної операції.
7. Після звірки counts/checksums — перемкнути UI на новий API; старий лишити read-only на період сумісності.
8. Retention/архівування legacy — лише за D1; історію при rollout не видаляти.
9. **H.5 (R11).** Naive `LocalDateTime` → UTC: існуючі мітки мігрувати як «server-local, точний offset
   невідомий», з прапорцем `timestampPrecision=LEGACY_NAIVE`; нові — `Instant` UTC.

---

## I. Acceptance Criteria

1. Кожен рядок §E має код каталогу, actor/target/result/data policy і автотест (`PROP-` ID реалізовано).
2. Для кожної M0-команди встановлюються actor, UTC timestamp, module/area, action, target,
   outcome і релевантні зміни.
3. Успішна мутація і outbox event commit-яться атомарно в межах локальної module DB.
4. Relay — at-least-once без дублікатів (§B6); backlog/втрата виявляються alert-ом.
5. Один user intent корелюється через UI/HTTP/service/зміни/інтеграції (root/child, не незалежні дії).
6. Auth/authz-відмови і критичні відхилення мають окремі records із санітизованими кодами.
7. Критичні reads фіксуються без аудиту кожного технічного GET/autocomplete (за D2).
8. Object history відновлює хронологію з пов’язаними об’єктами і side effects; history протезування читає canonical events.
9. Business/security/user-activity/technical розділені за типом і доступом.
10. Жодних паролів, токенів, credentials, файлів/PDF-bytes і неконтрольованих PHI в audit і логах.
11. Canonical rows недоступні для UPDATE/DELETE роллю застосунку; цілісність архіву перевіряється автоматично.
12. Пошук комбінує користувача, період, модуль, дію, target, результат, actor type, correlation ID.
13. Старі `/api/audit` endpoint-и сумісні на період міграції; історичним даним не приписуються невідомі факти.
14. Retention/archive/backup/monitoring/fail-policy затверджені (D1–D7).
15. Продуктивність виміряна на узгодженому обсязі; target затверджено вимірюванням (§B7), регресії ключових сценаріїв немає.

---

## J. GitHub Issues фаз

| Фаза | Issue | Назва |
|---|---|---|
| Epic | #329 | [Audit v2] Epic: наскрізний аудит реальних дій користувачів |
| F0 | #330 | [Audit v2] Phase F0 — Foundation: рішення, каталог, політики |
| F1 | #331 | [Audit v2] Phase F1 — Event model + storage |
| F2 | #332 | [Audit v2] Phase F2 — Correlation + relay |
| F3 | #333 | [Audit v2] Phase F3 — Security audit |
| F4 | #334 | [Audit v2] Phase F4 — ICU coverage |
| F5 | #335 | [Audit v2] Phase F5 — Medication coverage |
| F6 | #336 | [Audit v2] Phase F6 — Prosthetics coverage |
| F7 | #337 | [Audit v2] Phase F7 — Read audit + frontend + консоль |
| F8 | #338 | [Audit v2] Phase F8 — Migration + integrity + retention + monitoring + gate |
