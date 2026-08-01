# ICU: вирівнювання механізму призначень/виконань з medication-sheet

**Дата:** 2026-08-01
**Статус:** план до реалізації
**Мета:** перенести повний робочий процес «план → виконання» з medication-sheet у годинну сітку ICU-чарту (форма 003-15/о), зберігши статуси ТЗ §35, семантику planned/completed-finished, блокування підписаної доби, аудит і рольову безпеку. **Без 2FA** (двособової автентифікації — виняток, узгоджений із користувачем).

## Поточний стан (досліджено)

- `MedicalOrder`: category, drugName, dose, unit, route, frequency, startTime, endTime, status (DRAFT/ACTIVE/COMPLETED/CANCELLED). Створення — PRESCRIBER (`IcuSecurityRules` рядок 28).
- `OrderExecution`: order, executedBy (Long userId), executedAt, actualDose, status (enum PLANNED/IN_PROGRESS/COMPLETED/PARTIALLY_COMPLETED/CANCELLED — вже відповідає ТЗ §35), comment.
- `OrderExecutionService.createExecution`: перевірка ACTIVE + підписана доба; `executedBy` приймається з клієнта; аудит EXECUTE/BACK_ENTRY; перерахунок водного балансу.
- Безпека: URL-патерни в `IcuSecurityRules` (аннотацій `@PreAuthorize` в ICU немає). `POST /api/orders/*/execute` → CLINICAL_ROLES (може виконувати будь-яка клінічна роль); `POST /api/orders/**` → PRESCRIBER.
- Frontend: `HourlyGrid.TherapyCell` — клік по годині → діалог дози → `POST /api/orders/{id}/execute` (IntensiveCareCard.tsx:202-211). Лікар створює призначення інлайн-формою (HourlyGrid.tsx:455-467).
- medication-sheet (еталон): `PrescriptionDayPart` (period, dose, isPlanned, isPlannedFinished, isCompleted, isCompletedFinished, doctorName, nurseName); endpoints: `PUT day-parts/{id}/plan` (PRESCRIBER, доза), `PUT day-parts/{id}/complete` (EXECUTOR), `PUT day-parts/{id}/cancel` (PRESCRIBER → isPlannedFinished), `POST day-parts/{id}/execute` (EXECUTOR, actualDose; 2FA-блок — виключаємо). Візуалізація spreadsheet: план — синій + доза, виконано — зелений ✓, відмінено — фіолетовий ✕.
- ТЗ §34-35: поля призначення і виконання, статуси «Заплановано / Виконується / Виконано / Частково виконано / Скасовано»; §50-51: призначення створює лише лікар, редагування заборонено після виконання, все журналюється.

## Цільова модель

Одиниця роботи — клітина (order, hour). Один запис `OrderExecution` на (order, hour), унікальність `(order_id, hour)`.

### Зміни в OrderExecution (Liquibase changeset #7, таблиця `order_executions`)

| Колонка | Тип | Призначення |
|---|---|---|
| `hour` | INTEGER NULL | година медичної доби (0-23); для планових записів; backfill з `executed_at` |
| `planned` | BOOLEAN NOT NULL DEFAULT FALSE | заплановано лікарем |
| `planned_by` | BIGINT NULL | лікар (userId) |
| `planned_at` | TIMESTAMP NULL | коли заплановано |
| `planned_dose` | VARCHAR(100) NULL | доза за планом |
| `planned_finished` | BOOLEAN NOT NULL DEFAULT FALSE | план фіналізовано (відмінено / завершено планування) |
| `completed_finished` | BOOLEAN NOT NULL DEFAULT FALSE | виконання фіналізовано |
| UNIQUE | (order_id, hour) | один запис на годину |

Статуси (вже існують): PLANNED → IN_PROGRESS (опційно) → COMPLETED / PARTIALLY_COMPLETED / CANCELLED.

### Endpoints (OrderExecutionController)

| Метод | Шлях | Роль | Дія |
|---|---|---|---|
| PUT | `/api/orders/{orderId}/plan` | PRESCRIBER | upsert (order, hour): planned=true, plannedDose, plannedBy=userId, plannedAt=now, status=PLANNED; body {hour, dose} |
| PUT | `/api/orders/{orderId}/plan/finish` | PRESCRIBER | plannedFinished=true; body {hour} |
| PUT | `/api/orders/{orderId}/cancel` | PRESCRIBER | status=CANCELLED, plannedFinished=true; body {hour} |
| POST | `/api/orders/{orderId}/execute` | EXECUTOR | вимагає planned && !cancelled && !completedFinished; executedBy=userId (з auth), executedAt=now, actualDose, status=COMPLETED (або PARTIALLY_COMPLETED при actualDose < plannedDose); body {hour, actualDose, comment} |
| POST | `/api/orders/{orderId}/execute/finish` | EXECUTOR | completedFinished=true; body {hour} |
| GET | `/api/orders/{id}/executions` | CLINICAL | без змін |
| PATCH | `/api/executions/{id}` | EXECUTOR | заборона зміни після COMPLETED/CANCELLED/completedFinished (ТЗ §50); `executedBy` з клієнта прибрати |

Валідації: order ACTIVE; доба не підписана (DocumentLockedException — вже є); hour у межах [startTime, endTime] (якщо задано); plan/cancel лише якщо не виконано; execute лише якщо заплановано.

### Безпека (IcuSecurityRules — конкретні патерни раніше за загальні)

```java
.requestMatchers(HttpMethod.POST, "/api/orders/*/execute/finish").hasAnyRole(EXECUTOR_ROLES)
.requestMatchers(HttpMethod.POST, "/api/orders/*/execute").hasAnyRole(EXECUTOR_ROLES)
.requestMatchers(HttpMethod.PUT, "/api/orders/*/plan/finish").hasAnyRole(PRESCRIBER_ROLES)
.requestMatchers(HttpMethod.PUT, "/api/orders/*/plan").hasAnyRole(PRESCRIBER_ROLES)
.requestMatchers(HttpMethod.PUT, "/api/orders/*/cancel").hasAnyRole(PRESCRIBER_ROLES)
.requestMatchers(HttpMethod.POST, "/api/orders/**").hasAnyRole(PRESCRIBER_ROLES)
.requestMatchers(HttpMethod.PATCH, "/api/executions/**").hasAnyRole(EXECUTOR_ROLES)
.requestMatchers("/api/orders/**").hasAnyRole(CLINICAL_ROLES)
```

### Аудит
- PLAN, PLAN_FINISH, CANCEL, EXECUTE, EXECUTE_FINISH через `AuditService` (шаблон уже є: EXECUTE/BACK_ENTRY); BACK_ENTRY — при виконанні за минулу годину.

## Frontend

- `frontend/src/types/index.ts`: OrderExecution + planned/plannedBy/plannedAt/plannedDose/plannedFinished/completedFinished/hour; нові request-типи (plan {hour, dose}, execute {hour, actualDose, comment}).
- `frontend/src/api/endpoints.ts`: `orderExecutionApi.plan/planFinish/cancel/execute/executeFinish`.
- `HourlyGrid.TherapyCell`: стан клітини за даними виконання (план/виконано/скасовано), кольори як у spreadsheet (синій/зелений/фіолетовий), підказка з іменами лікаря/медсестри та дозами:
  - лікар: клік по порожній клітині → діалог планування (доза); клік по запланованій → редагування дози/скасування; виконані — readonly.
  - медсестра: клік по запланованій (planned && !cancelled && !completed) → діалог виконання (фактична доза, коментар); решта — readonly.
  - isLocked (підписана/закрита доба) — усе readonly (вже є).
- `IntensiveCareCard.tsx:202-211`: `toggleOrder` → окремі обробники onPlanOrder/onExecuteOrder/onCancelOrder з новими викликами API; стан `executing` зберегти.
- `MedicalOrdersPanel.tsx` не чіпаємо (осиротілий, використовується лише в тесті) — оновити типи тільки якщо ламається збірка.

## Seed-дані та тести

- `data.sql` / `data-test.sql`: оновити INSERT order_executions (hour, planned, planned_by, planned_at, planned_dose, planned_finished, completed_finished); додати 1-2 planned-записи для демо; оновити FIX-блок (рядки ~2971-2976).
- Backend: unit + integration тести — transitions plan→execute, cancel, finish, заборона після підпису, заборона редагування після виконання, рольові перевірки.
- Frontend: оновити тести HourlyGrid/IntensiveCareCard/endpoints під нові пропси та API.
- Playwright: `nurse/order-execution-full.spec.ts`, `nurse/order-execution.spec.ts` — флоу «лікар планує (API в beforeAll) → медсестра виконує»; `api/security-rules.spec.ts` — додати перевірки: nurse не може plan/cancel (403), doctor не може execute (403), nurse може execute.

## Порядок робіт

1. Backend: Liquibase changeset → entity → DTO/mapper → service → controller → IcuSecurityRules → аудит.
2. Компіляція: `mvn compile` (локально дозволено; тести — лише CI).
3. Frontend: типи → endpoints → HourlyGrid → IntensiveCareCard → тести.
4. Seed-дані та оновлення тестів (backend unit/integration, Vitest, Playwright).
5. Commit → push → CI (test, integration-tests, format-check) → poll → фікс до зеленого.

## Ризики / відкриті питання

- Зміна `POST /api/orders/{id}/execute` (тіло: executedBy/executedAt → hour/actualDose/comment) ламає існуючі E2E-тести виконання — оновлюються разом (п.4).
- Backfill `hour` для старих записів: `EXTRACT(HOUR FROM executed_at)`; запланованих записів у старій схемі немає.
- PARTIALLY_COMPLETED: визначається як actualDose < plannedDose (числове порівняння при можливості), інакше — COMPLETED.
