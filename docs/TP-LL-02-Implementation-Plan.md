# План реалізації процесу «Етапи технологічного процесу нижніх кінцівок» (TP-LL-02)

> **Шаблон-джерело:** `docs/Етапи технологічного процесу нижніх кінцівок v2.md` (v2.1, без Quality Gate)
> **Універсальний шаблон:** `docs/Універсальний шаблон опису технологічного процесу.md` (v1.1)
> **Код шаблону:** `TP-LL-02` — `LOWER_LIMB` / `generic_lower_limb` / `BOTH`, `DRAFT→ACTIVE`, 10 етапів у інстансі (16 кроків) + setup-фаза, `estimatedDurationMin=540`
> **Рушій:** `prosthesis-manufacturing` (`FlowTemplate`→`FlowInstance`→`StepExecution`, `FlowInstanceService`/`FlowTemplateService`, `TemplateSnapshotParser`, `WizardScreen`, `ProstheticsContext`)
> **Принцип:** Quality Gate повністю вилучено; процес лінійний, брак — через `fail`/`backward`, умовний вкладиш — `mandatory=false`.

---

## Огляд фаз

| Фаза | Назва | Фокус (розділи v2) | Основні артефакти | Залежності |
|---|---|---|---|---|
| **1** | Persistence & Seed | §5 (ієрархія), §1 (паспорт) | Liquibase/seed `TP-LL-02`, `FlowTemplateService`, snapshot | — |
| **2** | Business Rules & State Machine | §6–§9 | `validateValues` (MEASUREMENT ≥3, умовний 7.1), час `totalActiveSeconds/IdleSeconds`, `backward`/`fail`/`replacement` без gate | Фаза 1 |
| **3** | Setup Flow & Template Selection | §2, §4, §14 Screens 3–6 | `ProstheticsContext`, `PatientSearch/OrderSelect/Review/TemplateSelect`, фільтр `LOWER_LIMB/BOTH` | Фаза 1 |
| **4** | Wizard Execution | §5.3–5.4, §10–§11, §14 Screen 8 | Динамічні форми (MEASUREMENT/CHECKLIST/INFORMATION), альтернатива «Вкладиш не потрібен», `EvidenceFile`, Dashboard/History | Фаза 1, 2 |
| **5** | Finalization & QA | §10 (PDF), §12–§15, §17 | `ProstheticsPdfService`, аудит, RBAC, повний E2E `NEW→COMPLETED/FAILED`, регресія vs `TP-UL-01`, доки/rollout | Фази 1–4 |

Загальна тривалість — 5 інкрементів, кожен закінчується зеленим CI (`.github/workflows/playwright.yml`: `format-check` + `backend-test` + `backend-integration` + `frontend-test` + `e2e-test` + `build`).

---

## Фаза 1 — Persistence & Seed (TP-LL-02 у БД)

### Мета
Матеріалізувати паспорт ( §1 ) та ієрархію §5 у БД так, щоб `TP-LL-02` можна було обрати на Screen 6 і створити інстанс без зміни коду рушія.

### Scope

**Backend:**
- Нова Liquibase-зміна (або розширення `data-prosth.sql`): `prosthetics_flow_templates` (`TP-LL-02`, `LOWER_LIMB`, `generic_lower_limb`, `BOTH`, `ACTIVE`, 540), 10 `TemplateStage` (1 setup поза інстансом → 10 у інстансі), 16 `TemplateStep` (типи §5.3, `mandatory`/`allowBackward`/`autoStartTimer`/`normDurationMin`), ~28 `TemplateElement` (`CHECKBOX`/`NUMERIC_INPUT`/`STEP_MESSAGE`/`IMAGE_UPLOAD` опційно) з `orderIndex`, `required`, `unit=см`, `min/max`.
- `FlowTemplateService.create()` — перевірка унікальності `name+version`, `TemplateSnapshotParser.toJson()` / `parse()` для `templateSnapshot`.
- Міграція сиду: пацієнт `900002` Гаврилюк або новий тестовий пацієнт + замовлення `PR-2026-000X` (`LOWER_LIMB`, `BOTH`) для ручного тесту.

**Frontend:** без змін (дані підтягуються через `GET /templates`).

### Тест-план

**Unit (backend, `mvn test`):**
- `FlowTemplateServiceTest.create_TP_LL_02_success` — створення `TP-LL-02` з повним графом, перевірка `status=DRAFT`, `stages=10`, `steps=16`, `elements` count.
- `FlowTemplateServiceTest.create_duplicate_version_autoIncrement` — другий `TP-LL-02` → `version=2`.
- `TemplateSnapshotParserTest.roundTrip_TP_LL_02` — `toJson` → `parse` зберігає всі поля, `estimatedDurationMin=540`.
- `FlowTemplateValidationTest` — `@PrePersist` границі (`orderIndex>=0`, `min<=max`, `maxSizeMb>=1`).
- `SeedDataSqlTest.TP_LL_02_integrity` — парс `data-prosth.sql` / Liquibase changelog: відсутність дублікатів `id`, коректні FK `template_id→stage→step→element`.

**Integration (backend, `mvn test -Pintegration-test`, 4 БД PostgreSQL):**
- `TP_LL_02_SeedIntegrationTest` — після підняття контексту `GET /templates?productType=LOWER_LIMB&status=ACTIVE` повертає `TP-LL-02`; `GET /templates/{id}` — повний граф, порядок за `order_index`.
- `FlowInstanceCreationIntegrationTest.TP_LL_02` — `POST /instances {orderId, templateId=TP-LL-02}` → `201 NEW`, `templateSnapshot` не `null`, `assignedUserId` = поточний `PROSTHETIST`; повторний `POST` на той самий `orderId` з `IN_PROGRESS` → `400 duplicate`; з `FAILED` після `fail` → `201` (дозволено).
- `FlowTemplateStatusIntegrationTest` — `TP-LL-02` у `DRAFT` не створює інстанс → `400 Only active templates…`; після `PATCH status=ACTIVE` — успіх.

**E2E (Playwright, `tests/`):**
- `tests/specs/prosthetics/template-tp-ll-02.spec.ts` (потребує `PROSTHETICS_ADMINISTRATOR`): `POST /templates` (скелет Додаток A) → `GET /templates?productType=LOWER_LIMB` містить картку `TP-LL-02`; `Setup Flow` — `PatientSearch → OrderSelect → TemplateSelect` показує `TP-LL-02` серед `ACTIVE` (фільтр `LOWER_LIMB`), `IN_PROGRESS` інстанс відображається на Dashboard.

### Критерії готовності
- `TP-LL-02` у `ACTIVE` доступний через API/UI; сид ідемпотентний (`ON CONFLICT DO UPDATE`); `templateSnapshot` коректний; всі тести фази зелені.

---

## Фаза 2 — Business Rules & State Machine (без Quality Gate)

### Мета
Реалізувати валідацію, час і переходи §6–§9 для `TP-LL-02`, підтвердити що вилучення Quality Gate не зламало лінійний потік.

### Scope

**Backend:**
- `FlowInstanceService.validateValues()`:
  - Крок 1.1 `MEASUREMENT` — `filledNonCheckboxValues ≥3` + усі `CHECKBOX required=true` → `true`; крок 7.1 `mandatory=false` — порожні `values` дозволені (альтернатива «Вкладиш не потрібен»).
  - Всі інші `CHECKLIST`/`INFORMATION` — `!isBlank` для `required`, `NUMERIC_INPUT` `isNaN` + `min/max`, `TEXT_*` `regex`, `DROPDOWN/RADIO` `∈ options`.
- `FlowInstanceService.advance()` — лінійний (без перевірки `nextStage.gate`), `moveToNextStage` послідовно; `backward()` — `targetStep.allowBackward` ( `false` лише для 10.1 видачі).
- Час: `activeSeconds` (`max(startedAt,resumedAt)→completedAt`), `totalActiveSeconds`, `totalIdleSeconds` (`pausedAt→resumedAt`), `fmt() HH:MM:SS`, `computeProgress()`.
- `fail` (`IN_PROGRESS→FAILED` + `FailureSnapshot`) / `replacement` (`FAILED→NEW` з тим же `orderId+snapshot`) без `WAITING_REVIEW`/`FAILED_QC`.

**Frontend:** без змін логіки (guards дублюються Hard Block у Wizard).

### Тест-план

**Unit:**
- `ValidateValuesTest.TP_LL_02_measurement_insufficient` — 1.1 з 2 значеннями → `400 "Заповніть щонайменше 3"`; з 3 → успіх.
- `ValidateValuesTest.TP_LL_02_conditional_insert_skip` — 7.1 порожньо → успіх ( `mandatory=false` ); 7.1 з одним чекбоксом `false` при `required=false` → успіх; 7.2 порожньо → `400 required`.
- `ValidateValuesTest.TP_LL_02_numeric_range` — `NUMERIC_INPUT` `300 см` при `max=200` → `400 "не більше"`.
- `FlowInstanceServiceTest.backward_TP_LL_02_lastStepForbidden` — `backward` з 10.1 (`allowBackward=false`) → `400`; з 7.1 → успіх, `CANCELLED` на поточному, нове `IN_PROGRESS` на цілі, `attemptNumber++`.
- `TimeCalculationTest.activeIdleAggregation` — `pause/resume` кілька разів → `totalActiveSeconds`/`IdleSeconds` коректно сумуються.

**Integration:**
- `TP_LL_02_ValidationIntegrationTest` — `POST /step-executions/{id}/complete` з невалідними `values` → `400` з текстом валідатора; з валідними → `200` і `currentStage/StepId` просунуто.
- `TP_LL_02_StateMachineIntegrationTest` — повний лінійний прохід `NEW→IN_PROGRESS→PAUSED→IN_PROGRESS→COMPLETED` без `WAITING_REVIEW`; `fail` з `IN_PROGRESS` → `FAILED` + `FailureSnapshot`; `fail` з `COMPLETED` → `400`; `replacement` з `FAILED` → `NEW` з тим же `snapshot`; з `COMPLETED` → `400`.
- `TP_LL_02_ConditionalFlowIntegrationTest` — прохід з пропуском 7.1 (порожні `values`) → `advance` до 7.2; прохід з заповненим 7.1 → також до 7.2; обидва шляхи доходять до `COMPLETED`.

**E2E:**
- `tests/specs/prosthetics/tp-ll-02-validation.spec.ts` — Wizard: спроба «Далі» на 1.1 з 1 значенням — кнопка заблокована + серверний `400`; заповнення 3 значень → «Далі» активна.
- `tests/specs/prosthetics/tp-ll-02-conditional.spec.ts` — на кроці 7.1 CTA «Пом'якшуючий вкладиш не потрібен» → перехід на 7.2 без валідації; альтернативний шлях із заповненням 7.1 → також на 7.2.

### Критерії готовності
- Валідація 1.1/7.1/7.2, `backward`/`fail`/`replacement`, час — покриті; `WAITING_REVIEW`/`FAILED_QC` відсутні в коді та тестах; CI зелений.

---

## Фаза 3 — Setup Flow & Template Selection (Screens 3–6)

### Мета
Забезпечити вибір пацієнта/замовлення та фільтрацію `TP-LL-02` на Screen 6 (§2, §4, §14).

### Scope

**Backend:**
- `ProstheticsPatientService` / `ProstheticsOrderService` — пошук `?q` / `?patientId`, фільтр `productType=LOWER_LIMB` (або `BOTH` як wildcard).
- `FlowTemplateService.list()` — фільтри `productType`, `amputationLevel` (`equalsIgnoreCase`), `limbSide` (`BOTH` матчає `LEFT`/`RIGHT`), `status=ACTIVE` — для `TemplateSelectPage`.

**Frontend:**
- `ProstheticsContext` (`ProstheticsDraft: {patientId, orderId, templateId, instanceId}`) у `localStorage+sessionStorage`; «Назад» не скидає, `F5` без `sessionStorage` — скидає.
- `setup/PatientSearchPage` → `OrderSelectPage` (sticky пацієнт) → `OrderReviewPage` (PDF прев'ю) → `TemplateSelectPage` (картки `TP-LL-02` з `productType/amputationLevel/limbSide/estimatedDurationMin`).
- `GET /templates?productType=LOWER_LIMB&amputationLevel=generic_lower_limb&limbSide=BOTH&status=ACTIVE` — відображає `TP-LL-02`.
- `POST /instances` → `NEW` (deep clone `templateSnapshot`), редірект на `ProcessDetail` (Screen 7).

### Тест-план

**Unit (frontend, Vitest + RTL):**
- `PatientSearchPage.test.tsx` — `GET /patients?q=Гаврилюк` → вибір рядка → `draft.patientId` встановлено.
- `OrderSelectPage.test.tsx` — `GET /orders?patientId=900002` → фільтр `LOWER_LIMB` → вибір `PR-2026-000X` → `draft.orderId`.
- `TemplateSelectPage.test.tsx` — `GET /templates?productType=LOWER_LIMB` повертає `TP-LL-02` (карта видима), `TP-UL-01` не показується при `LOWER_LIMB`; `generic_lower_limb`/`BOTH` матчаться.
- `ProstheticsContext.test.tsx` — `localStorage` + `sessionStorage` персистенція, «Назад» зберігає, `clearDraft` скидає.

**Integration:**
- `SetupFlowIntegrationTest.TP_LL_02` — `GET /patients?q=900002` → `GET /orders?patientId=900002` (є `LOWER_LIMB`) → `GET /templates?productType=LOWER_LIMB` містить `TP-LL-02` → `POST /instances` → `NEW` з коректним `snapshot`.
- `TemplateFilterIntegrationTest` — `amputationLevel=generic_lower_limb` повертає `TP-LL-02`; `below_knee` без окремої версії — не повертає (очікувано, до створення спеціалізованої версії).

**E2E (Playwright, `prosthetics-chromium`, serial):**
- `tests/specs/prosthetics/tp-ll-02-setup.spec.ts` — `prosthetist1` логін → Dashboard «Новий процес» → `PatientSearch` (Гаврилюк) → `OrderSelect` (`PR-2026-000X`) → `OrderReview` (PDF прев'ю завантажено, кнопка «Старт» активна) → `TemplateSelect` (картка `TP-LL-02` видима, `TP-UL-01` відфільтровано при `LOWER_LIMB`) → Створення інстансу → `ProcessDetail` дерево 10 етапів.
- `tests/specs/prosthetics/tp-ll-02-setup-back.spec.ts` — «Назад» на кожному з Screens 3–6 зберігає `draft`; `F5` без `sessionStorage` → редірект на Dashboard.

### Критерії готовності
- Setup flow для `LOWER_LIMB` повністю клікабельний; фільтри `BOTH`/`generic_lower_limb` працюють; `draft` не втрачається на «Назад»; E2E зелені.

---

## Фаза 4 — Wizard Execution (Screen 8, Dashboard, History)

### Мета
Реалізувати динамічне проходження 16 кроків `TP-LL-02` у Wizard, включаючи умовний вкладиш, докази, паузи та моніторинг.

### Scope

**Backend:**
- `StepExecution` — `attemptNumber++` при `backward`; `priorStepValues` у `FlowInstanceResponse` (для read-only підсумків).
- `ResourceUsage` — `POST /step-executions/{id}/complete {resources: [{material,qty,unit,minutes}]}` → `saveResources()` ( `material` NOT NULL).
- `EvidenceFile` — `POST /evidence-files` multipart до 10 MB, `mimeTypes`/`maxSizeMb` з `TemplateElement` ( `IMAGE_UPLOAD` опційно для 1.2/2.1/3.1/6.1/9.1).

**Frontend:**
- `process/WizardScreen` — рендер за `SnapshotStep.stepType`/`elements`:
  - `MEASUREMENT` (1.1) — числові поля `см` + `STEP_MESSAGE` (опційно) + Hard Block `filled≥3`.
  - `CHECKLIST` (2.2,3.2,4.1,5.1,6.1,7.1,7.2,9.1,10.1) — `CheckboxRow` на весь рядок.
  - `INFORMATION` (1.2,2.1,3.1,8.1) — чекбокс підтвердження.
  - Умовний CTA «Пом'якшуючий вкладиш не потрібен» на 7.1 → `completeStep({values:{}})` .
- `EvidenceFile` upload — `IMAGE_UPLOAD` на кроках гіпсу/гільз.
- `ProstheticsDashboard` — таблиця `GET /instances?assignedUserId=me` (ID, Пацієнт, Замовлення `TP-LL-02`, Етап, Крок, Статус, Оновлено) + фільтр `LOWER_LIMB`.
- `ProcessDetail` / `ProcessLayout` — дерево `Stage→Step` (лінійне, без ромбів), `ProcessHistoryPage` — `step-executions` + `resources` + `audit`.

### Тест-план

**Unit (frontend):**
- `WizardScreen.test.tsx` — рендер 1.1 `MEASUREMENT` (4 `NUMERIC_INPUT`), Hard Block при <3; 7.1 `mandatory=false` — CTA «не потрібен» активний навіть при порожніх чекбоксах; `CheckboxRow` — клік будь-де тоглить.
- `EvidenceUpload.test.tsx` — `IMAGE_UPLOAD` приймає `image/jpeg` ≤10 MB, відхиляє `>10 MB` та не-дозволений MIME.
- `DashboardPage.test.tsx` — `GET /instances` фільтр `LOWER_LIMB` показує `TP-LL-02` інстанси, `TP-UL-01` — ні.

**Integration:**
- `WizardFlowIntegrationTest.TP_LL_02` — `start` → послідовний `completeStep` 16 кроків з різними типами → `COMPLETED`; `pause` (`MATERIAL` на 3.1) → `PAUSED` → `resume` → продовження; `backward` з 6.1 на 5.1 → `CANCELLED`/`IN_PROGRESS`; `fail` з `IN_PROGRESS` → `FAILED`.
- `EvidenceFileIntegrationTest` — `POST /evidence-files` з `image/jpeg` 5 MB → `201`; 11 MB → `400`; прив'язка до `StepExecution` зберігається в `failure-snapshot`/`pdf`.

**E2E:**
- `tests/specs/prosthetics/tp-ll-02-wizard.spec.ts` (serial, `prosthetics-chromium`) — API-створення `TP-LL-02` інстансу → Wizard: 1.1 заповнення 3 значень → 1.2–10.1 чекбокси → альтернатива «не потрібен» на 7.1 → `COMPLETED` → `DoneScreen` «Виріб готовий».
- `tests/specs/prosthetics/tp-ll-02-wizard-backward.spec.ts` — під час Wizard `backward` з 6.1 на 5.1 → повторне `completeStep` → `COMPLETED` (перевірка `attemptNumber`).
- `tests/specs/prosthetics/tp-ll-02-dashboard-history.spec.ts` — Dashboard фільтр `LOWER_LIMB` + `ProcessHistoryPage` хронологія `step-executions`/`resources`/`audit` для `TP-LL-02`.

### Критерії готовності
- Wizard рендерить всі типи кроків `TP-LL-02`; умовний 7.1, пауза/evidence/Dashboard/History — зелені.

---

## Фаза 5 — Finalization & QA (PDF, RBAC, повний E2E, регресія)

### Мета
Закрити життєвий цикл інстансу, підтвердити безпеку, продуктивність і регресію, задокументувати реліз.

### Scope

**Backend:**
- `ProstheticsPdfService` — `generateFinalReport` (`COMPLETED`) / `generateFailureReport` (`FAILED`) з `FlowInstance`, `ProstheticsOrder`, `SnapshotTemplate`, `StepExecution[]`, `ResourceUsage[]`; `GET /instances/{id}/pdf` → `application/pdf`, `sendPdf()` в МІС.
- `FlowInstance` — `fail`/`replacement` без gate; `requireOwner` (404, не 403), `@PreAuthorize` для `fail`/`replacement`/`evidence`.
- Аудит — `auditService.logAction` для всіх мутацій (`CREATE/START/COMPLETE/PAUSE/RESUME/BACKWARD/FAIL/REPLACEMENT/ARCHIVE`).

**Frontend:**
- `DoneScreen` (`COMPLETED` → «Виріб готовий», «Експорт PDF») / `FailedScreen` (`FAILED` → «Брак», `replacement` CTA).
- RBAC — `PROSTHETIST` не бачить чужі інстанси без `allowAll`; `ADMINISTRATOR` — аудит.

**Cross-cutting:** продуктивність Wizard (16 кроків, `advance` <200 мс), безпека (mutation МІС заборонені), регресія `TP-UL-01`.

### Тест-план

**Unit:**
- `ProstheticsPdfServiceTest.TP_LL_02` — `generateFinalReport` містить всі 10 етапів, `normDurationMin`, `totalActiveSeconds`; `generateFailureReport` містить `failReason`/`category`.
- `AuditServiceTest.TP_LL_02` — кожна мутація (`START/COMPLETE/PAUSE/RESUME/BACKWARD/FAIL/REPLACEMENT`) створює `AuditLog` з `entity=FlowInstance`, `entityId`, `action`, `userId`, `ipAddress`.

**Integration:**
- `PdfIntegrationTest.TP_LL_02` — `COMPLETED` інстанс → `GET /pdf` → `200 application/pdf` (iText, не порожній); `FAILED` → failure-PDF з `failReason`; `GET /pdf` для `NEW` → `404` або порожній.
- `RbacIntegrationTest.TP_LL_02` — `prosthetist2` `GET /instances/{id}` чужого `TP-LL-02` → `404`; `PROSTHETICS_ADMINISTRATOR` з `allowAll=true` → `200`; `ADMINISTRATOR` без `PROSTHETICS_*` — `403` на `POST /instances`.
- `ReplacementIntegrationTest.TP_LL_02` — `FAILED` → `POST /replacement` → `NEW` з тим же `orderId+snapshot`; `COMPLETED` → `replacement` → `400`.

**E2E (Playwright, 3 проекти: `prosthetics-chromium` serial, `api-chromium`, `admin-chromium`):**
- `tests/specs/prosthetics/tp-ll-02-full-lifecycle.spec.ts` — `prosthetist1` happy path `NEW→COMPLETED` (16 кроків, з альтернативою 7.1) → `DoneScreen` → `Експорт PDF` → `admin` бачить в `audit` (`GET /audit?entity=FlowInstance`).
- `tests/specs/prosthetics/tp-ll-02-failure-replacement.spec.ts` — `fail(category=material_defect)` на 6.1 → `FAILED` → `FailedScreen` → `replacement` → новий `NEW` → повторний прохід до `COMPLETED`.
- `tests/specs/prosthetics/tp-ll-02-rbac.spec.ts` — `nurse1` / неавторизований → `403` на `POST /instances`; `prosthetist1` не бачить `prosthetist2` `TP-LL-02` без `allowAll`.
- `tests/specs/prosthetics/tp-ll-02-no-gate-regression.spec.ts` — перевірка відсутності Quality Gate: `GET /instances/{id}/snapshot` не містить `quality_gates`/`rework_loops`/`WAITING_REVIEW`; `POST /instances/{id}/quality-gates/{id}/decision` → `404`/`400` (ендпоінт вилучено або повертає помилку); `TP-UL-01` регресія — лінійний прохід `TP-UL-01` досі `COMPLETED` (без деградації).
- Перфоманс: `advance` 16 кроків <3 с сумарно; `GET /instances` з 50 `TP-LL-02` <500 мс.

**Документація & Rollout:**
- `docs/TP-LL-02-Implementation-Plan.md` (цей файл) + оновлення `README.md`/`UseManual.md` (новий шаблон `TP-LL-02` у каталозі).
- Міграція: `data-prosth.sql` або Liquibase changelog з `ON CONFLICT DO UPDATE` (ідемпотентно); перевірка на staging з 4 БД.
- Чекліст релізу: сид застосовано, `ACTIVE`, сид-rollback план, `gh run watch` зелений, ручне `NEW→COMPLETED` на staging.

### Критерії готовності (Definition of Done)
- `TP-LL-02` `ACTIVE` з 10 етапів/16 кроків; повний `NEW→COMPLETED` та `FAILED→replacement` E2E зелені; Quality Gate відсутній і тест це доводить; `TP-UL-01` регресія зелена; PDF/аудит/RBAC — покриті; доки оновлені; CI 6 jobs зелені.

---

## Ризики та мітигації

| Ризик | Мітигація (фаза) |
|---|---|
| `generic_lower_limb` не матчає `below_knee`/`above_knee` замовлення ( `equalsIgnoreCase` ) | Фаза 1/3: створити спеціалізовані версії `TP-LL-02-below-knee`/`above-knee` або змінити `list()` на `contains` (окрема задача) |
| Альтернатива «Вкладиш не потрібен» обходиться валідацією | Фаза 2: `mandatory=false` + окремий CTA + інтеграційний тест з порожніми `values` |
| Вилучення Quality Gate зламає `TP-UL-01` (якщо код ще очікує `gate`) | Фаза 2/5: `advance()` вже лінійний (перевірено в `TP-UL-01` без gate), регресійний E2E |
| Велика кількість `CHECKBOX` (28) — монотонність Wizard | Фаза 4: `CheckboxRow` на весь рядок, групування за етапом, прогрес-бар |
| E2E фланкі через `prosthetist1`/`prosthetist2` contention на одному `orderId` | Фаза 3/5: кожен spec створює власне замовлення `PR-2026-000X` через API, ізоляція за `orderId` |

---

## Залежності між фазами (Gantt-логіка)

```
[1 Persistence] ─┬─► [2 Rules & State] ──► [4 Wizard] ──► [5 Finalization]
                 └─► [3 Setup Flow] ──────► [4 Wizard] ──► [5 Finalization]
```

Фази 2 і 3 можуть йти паралельно після 1; Фаза 4 потребує 1+2+3; Фаза 5 — фінальна.

