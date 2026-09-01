# ADR-002 — Branch-модель для «Брак» (TP-LL-02, етап 6 крок 1)

> **Статус:** `ACCEPTED` (2026-09-01) · **Контекст:** Issue #204 · **Автор:** OpenCode (Muse Spark)
> **Пов'язані:** ADR-001 не існує (перший ADR проєкту), `docs/TP-LL-02-Brak-Analysis-Issue-203.md`, Issue #203/#205

---

## 1. Контекст та проблема

TP-LL-02 «Етапи технологічного процесу нижніх кінцівок» — 10 етапів / 14 кроків, лінійний, без Quality Gate (`data-prosth.sql:177-241` — 0 `prosthetics_quality_gates` для `d0000012..021`). На кроці `e0000028` (етап `d0000017` «Примірювання та коректування тренувального протеза») оператор виявляє дефект посадки кукси та має створити нову **гілку** процесу з поверненням на один з трьох дозволених етапів (`d0000012, d0000013, d0000014`), при цьому стара гілка лишається незмінною в історії (§4-§10 ТЗ).

Поточна модель (`FlowInstance:14-91` + `FlowInstanceService:79-843` + `FailureSnapshot:14-39` + `prosth/001-manufacturing.sql:199-`) має єдиний глобальний `POST /fail → FAILED + FailureSnapshot(1:1)` та `POST /replacement → NEW(копія snapshot)`. Для гілок бракує:

- зв'язку `parent → child`,
- `origin/target` координат,
- `1:N` дефект-подій,
- термінального статусу що відрізняє брак від провалу,
- послабленого `uq_flow_instances_active_order` (`UNIQUE(order_id) WHERE status NOT IN ('FAILED','COMPLETED')`).

Потрібен вибір моделі з мінімальною інвазивністю, сумісністю з `replacement` та явним FK для історії.

---

## 2. Розглянуті варіанти

### Варіант A — Розширення `FlowInstance` + нова `prosthetics_brak_events` (рекомендовано)

```
FlowInstance.parent_instance_id UUID FK → prosthetics_flow_instances.id (nullable, лише для гілок)
             branch_sequence INT DEFAULT 1   -- 1=оригінал, 2+=гілки, orderId+branch_sequence UNIQUE опційно
             origin_stage_id UUID           -- d0000017 (етап браку)
             origin_step_id  UUID           -- e0000028
             defect_payload JSONB (опц., копія note/checkboxes)
             + новий статус BRANCHED

prosthetics_brak_events(id PK, instance_id FK NOT NULL, stage_id, step_id,
                        soft_tissue_misalignment BOOL, pain_discomfort BOOL,
                        note TEXT ≤1000, return_stage_id UUID NOT NULL,
                        new_instance_id FK, created_by, created_at)
  FK instance_id → стара гілка, FK new_instance_id → нова гілка (nullable до commit)
  1:N (одна гілка може мати теор. >1 брак, але бізнес-правило — 1 брак на e0000028, БД не обмежує)
```

*Плюси:* мінімальні зміни (4 колонки + 1 таблиця), сумісність з `replacement` (копія `templateSnapshot` як у `FlowInstanceService.replacement:425`), явний FK для `ProcessHistoryPage` («перейти до гілки»), `AuditService` без змін, `uq` фікситься додаванням `BRANCHED` до `NOT IN` або `WHERE status IN (NEW,IN_PROGRESS,PAUSED,WAITING_REVIEW)` — не ламає `TP-UL-01`.

*Мінуси:* `FlowInstance` товстішає на 4 колонки (прийнятно — nullable). Потрібен новий статус.

### Варіант B — Окрема `prosthetics_branches` таблиця

```
prosthetics_branches(id PK, root_instance_id, parent_instance_id, child_instance_id,
                     origin_stage_id, return_stage_id, brak_event_id, ...)
```

*Плюси:* чиста нормалізація, не чіпає `FlowInstance`.

*Мінуси:* додаткова таблиця + JOIN для кожного `GET /instances/{id}`, дублює зв'язок який природно належить `FlowInstance.parent_instance_id` (як у git-коміті), ускладнює `findByOrderId`/`findByParentInstanceId`, потребує ще одну міграцію. Відхилено як over-engineering для 1 бізнес-кейсу.

### Варіант C — Версіонування через `template_snapshot` fork

Дублювати `templateSnapshot` зі зсунутим `currentStageId` всередині старої гілки (без нового `FlowInstance`).

*Плюси:* 0 нових таблиць.

*Мінуси:* ламає `StepExecution` модель (історія старої гілки + нової змішані в одному `instance_id`), `totalActiveSeconds` контамінується, `requireOwner`/`AuditLog` плутаються, `ProcessDashboard` не може показати 2 рядки. Відхилено — порушує append-only принцип §10.

---

## 3. Рішення

**Прийнято Варіант A.**

### 3.1 Статус старої гілки

| Кандидат | Семантика | `uq_flow_instances_active_order` | Відмінність від `FAILED` | Рішення |
|---|---|---|---|---|
| `FAILED` з `category='brak_training_sleeve'` | Перевикористовує існуючий термінальний статус | Вже в `NOT IN` — 2-а гілка дозволена | Не відрізняє брак від глобального провалу (плутає `FailedScreen`/`generateFailureReport`) | Відхилено |
| `COMPLETED` | «Завершено з дефектом» | Вже в `NOT IN` | §4.4 «не завершувати процес» — стара гілка ≠ завершена | Відхилено |
| **`BRANCHED` (новий `FlowInstanceStatus`)** | «Розгалужено через брак, лишається в історії, не активна» | Додати `'BRANCHED'` до `NOT IN ('FAILED','COMPLETED','BRANCHED')` **або** змінити predicate на `WHERE status IN ('NEW','IN_PROGRESS','PAUSED','WAITING_REVIEW','CORRECTION')` | Чітко відрізняє брак від провалу, `FailedScreen` не показує брак як провал, PDF `generateReport` може рендерити `BRANCHED` окремо | **ПРИЙНЯТО** |

Додати `BRANCHED` в `FlowInstanceStatus.java:3-` (`NEW, IN_PROGRESS, PAUSED, ..., COMPLETED, FAILED, BRANCHED`), оновити `prosth/005-brak-branch.sql` — `ALTER TYPE?` не потрібен (VARCHAR 32), лише індекс:

```sql
DROP INDEX IF EXISTS uq_flow_instances_active_order;
CREATE UNIQUE INDEX uq_flow_instances_active_order
  ON prosthetics_flow_instances(order_id)
  WHERE status NOT IN ('FAILED','COMPLETED','BRANCHED');
```

Альтернатива (більш явна, краща для майбутніх статусів): `WHERE status IN ('NEW','IN_PROGRESS','PAUSED','BLOCKED_PATIENT','BLOCKED_MATERIAL','WAITING_REVIEW','CORRECTION')` — але `BRANCHED` у `NOT IN` мінімально інвазивно для Issue #204; повний allow-list — follow-up.

### 3.2 Зв'язок

- Стара гілка: `status = BRANCHED`, `origin_stage_id = d0000017`, `origin_step_id = e0000028`, `failReason = brakNote` (опц., для сумісності з `FailedScreen` якщо хтось фільтрує `FAILED|BRANCHED`), `endTime = now`.
- Нова гілка: `status = NEW → IN_PROGRESS` (після `start` або одразу `IN_PROGRESS` зі створенням першого `StepExecution`), `parent_instance_id = old.id`, `branch_sequence = (max sibling +1)`, `currentStageId = returnStageId`, `currentStepId = firstStepOf(returnStage)` (напр. `d0000012 → e0000020`), `templateSnapshot = old.templateSnapshot` (копія без змін), `assignedUserId = old.assignedUserId` (або `userId` викликача).

### 3.3 `BrakEvent`

`1:N` до старої гілки (`instance_id`), `new_instance_id` — FK до нової (nullable до commit, заповнюється після `INSERT` нової). Поля `softTissueMisalignment, painDiscomfort BOOL NOT NULL DEFAULT FALSE`, `note TEXT CHECK(length≤1000)`, `stage_id/step_id = d0000017/e0000028` (денормовано для аудиту), `return_stage_id NOT NULL CHECK IN (d0000012, d0000013, d0000014)` (додатковий `CHECK` або лише app-валідка — вибрано app `ALLOWED_RETURN_STAGE_IDS` в `BrakService` щоб не хардкодити в DDL).

---

## 4. Наслідки

- **Міграція:** `prosth/005-brak-branch.sql` (`split-prosth:17-19`) — 3 changesets: `ADD COLUMN parent_instance_id/branch_sequence/origin_stage_id/origin_step_id`, `CREATE INDEX idx_flow_instances_parent`, `CREATE TABLE prosthetics_brak_events`, `DROP/CREATE INDEX uq_…`. `DATABASECHANGELOG` — `split-prosth:17,18,19`.
- **Сумісність:** `parent_instance_id=null` для всіх історичних рядків; `BRANCHED` не ламає існуючі `findByOrderId`/`findByStatus` (нові статуси фільтруються автоматично); `TP-UL-01` регресія не зачеплена.
- **API:** `POST /instances/{id}/brak` атомарно (див. `docs/Brak-Design-Issue-204.md`).
- **UI:** `WizardScreen.tsx:1990` умовна кнопка `BRANCHED` не показує wizard (як `FAILED`).

---

## 5. Альтернативи що відхилено

Див. §2 — B (over-engineering), C (змішування історій).

---

## 6. Посилання

- `docs/TP-LL-02-Brak-Analysis-Issue-203.md:§5` (R1 UQ блокер)
- `backend/common/src/main/resources/db/changelog/prosth/001-manufacturing.sql:342` (індекс)
- Issue #204, #205, #206, #207
