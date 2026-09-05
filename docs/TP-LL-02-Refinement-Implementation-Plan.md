# План доопрацювання процесу «Етапи технологічного процесу нижніх кінцівок» (TP-LL-02)

> **Note (QG-Removal, issues #229–#234):** the Quality Gate subsystem referenced below
> (`QualityGateService`, `quality_gate` failure marker, `FAILED_QC`, gate E2E specs such as
> `tp-ll-02-no-gate-regression.spec.ts`) has since been fully deleted from code, schema,
> tests and E2E. This plan is retained as history.

> **Статус:** ПЛАН — 10 фаз, GitHub Issues [#211–#220](https://github.com/volyamnyi/intensive-care-unit-patient-chart/issues?q=is%3Aissue%20is%3Aopen%20lower-limb%20workflow) створено (див. §3)
> **Модуль:** `prosthesis-manufacturing` (backend) + `frontend/src/{pages/prosthetics,prosthetics,components/prosthetics,api}` + `tests/specs/prosthetics`
> **Шаблон:** `TP-LL-02` (`c0000003-…`, 10 етапів `d0000012–d0000021`, 14 кроків `e0000020–e0000033`), статус `ACTIVE`
> **Відношення до ТЗ:** «Етапи технологічного процесу нижніх кінцівок» — комплекс змін №1–№6 (брак на етапі 9, «вкладиш не потрібен», «Попередній» на етапі 10, примітки+файли, причини провалу, причини паузи)

---

## 1. Результати аудиту існуючої реалізації (Phase 1 — короткий виклад)

Повний аудит — в Issue Phase 1. Ключові факти, від яких залежить весь план:

### 1.1 Структура шаблону TP-LL-02 (seed: `backend/common/src/main/resources/data-prosth.sql`)

| # | Етап (stage) | Крок (step) | mandatory | allow_backward | Примітка |
|---|---|---|---|---|---|
| 1 | `d0000012` Виготовлення гіпсового негатива | `e0000020` Зняття та внесення обʼємних розмірів (MEASUREMENT), `e0000021` | true/true | true/true | спец. правило: без мінімуму значень |
| 2 | `d0000013` Виготовлення гіпсової моделі кукси | `e0000022`, `e0000023` | true | true | — |
| 3 | `d0000014` Виготовлення тренувальної гільзи | `e0000024`, `e0000025` | true | true | — |
| 4 | `d0000015` Примірка тренувальної гільзи | `e0000026` | true | true | — |
| 5 | `d0000016` Складання тренувального протеза | `e0000027` | true | true | — |
| 6 | `d0000017` Примірювання та коректування тренувального протеза | `e0000028` | true | true | **поточний брак-тригер** |
| 7 | `d0000018` Виготовлення помʼякшуючого вкладиша та постійної гільзи | `e0000029` (КРОК 1), `e0000030` (КРОК 2) | **false** / true | true | **Функціонал №2** |
| 8 | `d0000019` Складання постійного протеза | `e0000031` | true | true | — |
| 9 | `d0000020` Примірювання та коректування постійного протеза | `e0000032` | true | **false** ⚠️ | **Функціонал №1** |
| 10 | `d0000021` Видача протеза (ADMINISTRATIVE, requires_approval=true) | `e0000033` | true | **false** ⚠️ | **Функціонал №3** |

- Елементи кроку 7.1 (`e0000029`): `f0000214` «Візуальний контроль чистоти…» (required=false), `f0000215` «Тактильний контроль поверхні…» (required=false). Третій елемент (новий checkbox «Помʼякшуючий вкладиш не потрібен») — **відсутній**.
- `INSERT INTO prosthetics_template_steps` використовує `ON CONFLICT (id) DO NOTHING` — правки seed **не** застосовуються до вже створених рядків БД. Елементи (`prosthetics_template_elements`) використовують повний `DO UPDATE SET` — правки елементів застосовуються.
- Snapshot шаблону заморожується в `FlowInstance.templateSnapshot` (jsonb) у момент створення інстансу; wizard працює зі snapshot, а не з живим шаблоном.

### 1.2 Брак (Функціонал №1) — механізм УЖЕ існує, але тільки для етапу 6

- **Backend** `BrakService.createBrakAndBranch` (267 рядків): тригер жорстко = `STAGE_D17` (етап 6) + `STEP_E0000028`; дозволені етапи повернення `ALLOWED_RETURN_STAGE_IDS = {d0000012, d0000013, d0000014}` (збігається з ТЗ №1); причини браку = `softTissueMisalignment`, `painDiscomfort`, `note` (≤1000 символів); оригінал → `BRANCHED` + `defect_payload` (jsonb) + `originStageId/originStepId` + `endTime`; нова гілка `IN_PROGRESS` з `parentInstanceId`, `branchSequence = max+1`, перший `StepExecution` IN_PROGRESS; `BrakEvent` + audit. RBAC: `requireOwner` (тільки власник, інакше 404).
- **DB**: Liquibase `prosth/005-brak-branch.sql` — `parent_instance_id`, `branch_sequence`, `origin_stage_id`, `origin_step_id`, `defect_payload`, таблиця `prosthetics_brak_events`, частковий унікальний індекс `uq_flow_instances_active_order` (status NOT IN FAILED/COMPLETED/BRANCHED).
- **API**: `POST /instances/{id}/brak` (201), `GET /instances/{id}/brak-events`, `GET /instances/{id}/branches`.
- **Frontend**: `WizardScreen.isBrakStep` жорстко = етап 6/крок `e0000028`; діалоги «Брак» (причини) + «Повернутись на етап» (радіо з `ALLOWED_RETURN_STAGE_IDS/LABELS` у `prosthetics/types.ts`).
- **Тести**: unit `BrakServiceTest`; integration `BrakIntegrationTest` (18 сценаріїв); E2E `tp-ll-02-brak-branch.spec.ts` (9 тестів) + `tests/helpers/tp-ll-02-flow.ts` (`createBrakViaApi`, `completeToStep`).

**Висновок для ТЗ №1:** механізм гілок/повернення повністю реалізовано та протестовано для етапу 6. Робота Phase 5 = **узагальнити тригер на етап 9** (`d0000020`/`e0000032`) без переписування механізму (не дублювати логіку — §12 ТЗ).

### 1.3 «Помʼякшуючий вкладиш не потрібен» (Функціонал №2) — поточний стан

- Кнопка `WizardScreen.tsx:2073–2082` показується лише для `e0000029`; `skipConditionalInsert()` викликає `completeStep({})` — крок можна «пропустити» з порожніми values (бо `mandatory=false`).
- Крок 7.1 має 2 optional-елементи `f0000214`/`f0000215`; жодної логіки взаємовиключення немає ні на backend, ні на frontend.
- Задокументовано в `docs/TP-LL-02-Implementation-Plan.md` (Фаза 2) як «mandatory=false → порожній values дозволено».
- E2E `tp-ll-02-conditional.spec.ts` і `tp-ll-02-validation.spec.ts` — у стані `describe.skip` (стабілізація).

**Висновок:** потрібно: (1) прибрати кнопку; (2) додати 3-й елемент-CHECKBOX; (3) серверне правило взаємовиключення у `FlowInstanceService.validateValues` (крок `e0000029`); (4) дзеркальне правило у frontend (`invalid` map), щоб CTA блокувалась ще до запиту.

### 1.4 «Попередній» на етапі 10 (Функціонал №3) — root cause знайдено

- **Root cause = дані шаблону (seed), а не код.** `e0000032.allow_backward = false` та `e0000033.allow_backward = false` у `data-prosth.sql:211–212`.
- Frontend: `WizardScreen.canGoBack = !!prevStep?.allowBackward` (рядок 1540) → для етапу 10/кроку 1 `prevStep = e0000032` → `false` → кнопка `disabled`.
- Backend: `FlowInstanceService.backward()` (рядки 274–324) перевіряє `target.isAllowBackward()` → повернув би 400 «Повернення до кроку … заборонено правилами шаблону». Тобто блокування двостороннє і походить з конфігурації.
- `requires_approval=true` на `d0000021` — це sign-off «Видача протеза», він НЕ блокує `backward` і не є Quality Gate (див. видалений у QG-Removal E2E `tp-ll-02-no-gate-regression.spec.ts`).
- **Фікс без workaround:** змінити seed `allow_backward=true` для `e0000032` (ціль повернення з етапу 10; `e0000033` залишається false — повернення НА нього неможливе, він останній) та змінити conflict-клаузу steps-INSERT на `DO UPDATE SET` (інакше правка не дійде до існуючих БД). Код backend/frontend **не змінюється** — логіка вже правильна, працює конфігурація.

### 1.5 Примітки та файли (Функціонал №4) — поточний стан

- **Файли:** `EvidenceFile` (`prosthetics_evidence_files`) уже привʼязані до `step_execution_id`; upload через `POST /instances/{id}/evidence?executionId=…` (мультипарт); правила: лише image/* та PDF, ≤10 MB, magic-byte перевірка, SVG заборонено, SHA-256 checksum, audit `EvidenceFile UPLOAD`; download `GET /instances/{id}/evidence/{fileId}` (власник/allowAll). **Відсутні:** list за executionId, DELETE.
- **Примітки:** окреме поле/таблиця для примітки кроку відсутня. Примітка існує лише як шаблонний елемент (напр., `f0000311` «Примітки» на кроці замірів — це значення елемента, не універсальна примітка).
- **Стандартизація:** `StepExecution.values` (jsonb) зберігає значення елементів; `StepExecutionResponse` не містить note.

**Висновок:** потрібно: колонка `note` на `prosthetics_step_executions` (Liquibase), PATCH-ендпоінт, list+delete для evidence, універсальний shared-компонент `StepNoteAttachments` для всіх кроків (reuse EvidenceFile, не дублювати upload-логіку).

### 1.6 Причини провалу (Функціонал №5) — поточний стан

- Source of truth довідника: **frontend** `frontend/src/prosthetics/failureCategories.ts` (`FAILURE_CATEGORIES`): defect, materials, **quality_gate «Повторна невдача на Quality Gate»**, component_damage, order_cancelled, patient, other.
- Backend `FlowInstanceService.fail()` **не валідує** category (вільний текст у `FailureSnapshot.category`). API приймає будь-який рядок.
- **Нюанс:** `QualityGateService` внутрішньо пише `category = "quality_gate"` для провалів на гейті (`GATE_FAIL`, перевищення rework-спроб) — це внутрішній технічний маркер, НЕ опція користувача. При видаленні опції цей внутрішній маркер лишається (інакше ламається QC-флоу та `FailedScreen` для FAILED_QC).
- `FailedScreen.tsx:171–175` відображає `FAILURE_CATEGORY_LABELS[category] ?? category`.
- E2E `tp-ll-02-failure-replacement.spec.ts` використовує category `MATERIAL_DEFECT` (не з довідника frontend!) — врахувати при backend-валідації.

**Висновок:** видалити опцію з довідника; на backend додати валідацію allowlist (source of truth переноситься на backend), зберігши внутрішній `quality_gate` маркер QualityGateService та історичну сумісність (старий `MATERIAL_DEFECT` у тестах привести до `materials`).

### 1.7 Причини паузи (Функціонал №6) — поточний стан

- **Backend enum** `PauseCategory { PATIENT, MATERIAL, TECH_IDLE }`; колонка `prosthetics_flow_instances.pause_category VARCHAR(16)` (Liquibase `prosth/002-pause-tracking.sql`); значення пишеться у `FlowInstanceService.pause()` через `PauseRequest.category` (Jackson валідує enum автоматично → API вже відхиляє невідомі значення 400).
- **Frontend**: `types.ts` `PauseCategory`, `WizardScreen.PAUSE_OPTIONS` (3 позиції: «Очікування пацієнта», «Відсутні матеріали», «Технологічний простій…»), діалог `RadioGroup`.
- Кешування/DB-reference даних немає — enum у коді.
- `FlowInstanceStatus.BLOCKED_PATIENT/BLOCKED_MATERIAL` — окремі **статуси інстансу** (легасі), НЕ причини паузи; за межі ТЗ.
- Використання у тестах: `FlowInstanceServiceTest` (MATERIAL), `FlowInstanceControllerTest` (MATERIAL), `BrakIntegrationTest` (PATIENT), `TpLl02BusinessRulesIntegrationTest` (PATIENT), `WizardScreen.test.tsx` (MATERIAL), `prostheticsApi.test.ts` (MATERIAL), E2E `prosthetics-e2e.spec.ts:828` (`pauseProcess('PATIENT')`), POM `tests/pages/prosthetics/WizardExecutionPage.ts:379`.

**Висновок:** нові значення enum: `OPERATIVE_INTERVENTION` «Оперативне втручання у пацієнта», `VLC_PASSING` «Проходження ВЛК», `WENT_ABROAD` «Поїхав за кордон», `REAMPUTATION` «Реампутація». Потрібен Liquibase-чейнджсет: розширення колонки до VARCHAR(32) + data-міграція старих значень (→ NULL). Оновити всі перелічені тести/файли.

---

## 2. Ключові архітектурні рішення (що буде реалізовано у фазах)

### 2.1 Гілки браку (P2/P5)
- **Дизайн вже існує** (ADR `docs/ADR-002-Brak-Branching.md`, Issues #203–#210). Phase 2 валідує його проти ТЗ №1 та фіксує розширення: множина брак-тригерів `{(d0000017,e0000028), (d0000020,e0000032)}`, ті ж 3 дозволені етапи повернення, та сама модель `parentInstanceId/branchSequence/BrakEvent/defectPayload`.
- Транзакційність: весь `createBrakAndBranch` — один `@Transactional`; `findByIdForUpdate` (PESSIMISTIC_WRITE) проти гонок; частковий унікальний індекс захищає від двох активних інстансів на замовлення.

### 2.2 Взаємовиключні checkbox (P3)
- Новий елемент `f0000240` (CHECKBOX, required=false, order_index=2) на кроці `e0000029` з label «Помʼякшуючий вкладиш не потрібен».
- **Backend — джерело істини переходу:** `FlowInstanceService.validateValues` для кроку `e0000029` додає правило:
  `ALLOW = (v && t && !n) || (n && !v && !t)`; інакше `BadRequestException` з поясненням. (Патерн жорсткої привʼязки до stepId уже є: `LOWER_LIMB_MEASUREMENT_STEP_ID`, `e0000002` ЗІЗ.)
- **Frontend — дзеркало:** та сама функція у `invalid` map (`WizardScreen`), CTA блокується + показується помилка; API-помилка також обробляється (toast).
- Крок лишається `mandatory=false` (генерік-валідація required-елементів не зачіпає), але «пропуск» порожніми values тепер = `false/false/false` → DENY.
- Вплив на автотести: обидва E2E-хелпери `buildValues` (tp-ll-02-flow.ts і prosthetics-flow.ts) ставлять **усі** checkbox у true — для `e0000029` додати special-case `n=false`. Те саме для `completeToCompleted`-шляхів (full-lifecycle, failure-replacement, brak-branch, no-gate-regression, wizard-checkbox-surface).

### 2.3 Повернення з етапу 10 (P4)
- Тільки дані: `e0000032.allow_backward=true`; steps-INSERT → `DO UPDATE SET` (name, description, step_type, mandatory, allow_backward, auto_start_timer, norm_duration_min — за патерном elements-INSERT). Код не чіпаємо. Регресія переходів інших етапів — тести.

### 2.4 Примітка+файли на кроці (P6)
- `StepExecution.note` (TEXT, nullable) + Liquibase; `StepExecutionResponse.note`; `PATCH /instances/{id}/step-executions/{executionId}` з `{note}` — тільки поки execution `IN_PROGRESS` і інстанс належить користувачу; audit `StepExecution NOTE`.
- Evidence: `GET /instances/{id}/evidence?executionId=` (метадані), `DELETE /instances/{id}/evidence/{fileId}` (власник; лише поки крок IN_PROGRESS або поки інстанс IN_PROGRESS; audit `EvidenceFile DELETE`; ліміт ≤10 файлів на execution).
- Frontend: новий shared-компонент `components/prosthetics/StepNoteAttachments.tsx` (Textarea «Примітка» + список файлів + upload + видалення + download), монтується у `WizardScreen` для **кожного** кроку (універсально, не по-компонентно). Збереження note: дебаунс-PATCH на blur/зміну + при completeStep. Завантаження — через існуючий `uploadEvidence` (reuse).

### 2.5 Довідники причин (P7, P8)
- P7: `FAILURE_CATEGORIES` мінус `quality_gate`; backend — константа-allowlist `FailureCategory` у `FlowInstanceService.fail()` (400 на невідомі); внутрішній `quality_gate` QualityGateService зберігається; `FailedScreen` отримує окрему legacy-label мапу для внутрішньої категорії; E2E `MATERIAL_DEFECT` → `materials`.
- P8: enum + колонка VARCHAR(32) + міграція даних + frontend `PAUSE_OPTIONS` (4 позиції) + fallback «—» для null/невідомих + всі перелічені тести.

---

## 3. Фази, dependencies та Issue-мапінг

| Фаза | Issue | Залежить від | Ключовий зміст |
|---|---|---|---|
| 1 | [#211 [Phase 1] Audit existing lower-limb prosthesis workflow architecture](https://github.com/volyamnyi/intensive-care-unit-patient-chart/issues/211) | — | повний аудит (цей документ §1 у деталях) |
| 2 | [#212 [Phase 2] Design defect handling and process branching](https://github.com/volyamnyi/intensive-care-unit-patient-chart/issues/212) | P1 | валідація ADR-002 проти ТЗ №1; розширення тригерів |
| 3 | [#213 [Phase 3] Update soft liner workflow and transition rules](https://github.com/volyamnyi/intensive-care-unit-patient-chart/issues/213) | P1 | Функціонал №2 |
| 4 | [#214 [Phase 4] Fix previous-stage navigation for prosthesis issuance](https://github.com/volyamnyi/intensive-care-unit-patient-chart/issues/214) | P1 | Функціонал №3 (дані) |
| 5 | [#215 [Phase 5] Implement defect and branch workflow for permanent prosthesis fitting](https://github.com/volyamnyi/intensive-care-unit-patient-chart/issues/215) | P2 | Функціонал №1 (етап 9) |
| 6 | [#216 [Phase 6] Add step-level notes and file attachments](https://github.com/volyamnyi/intensive-care-unit-patient-chart/issues/216) | P1 | Функціонал №4 |
| 7 | [#217 [Phase 7] Remove obsolete failed-process reason](https://github.com/volyamnyi/intensive-care-unit-patient-chart/issues/217) | P1 | Функціонал №5 |
| 8 | [#218 [Phase 8] Replace process pause reasons](https://github.com/volyamnyi/intensive-care-unit-patient-chart/issues/218) | P1 | Функціонал №6 |
| 9 | [#219 [Phase 9] Complete automated test coverage and regression validation](https://github.com/volyamnyi/intensive-care-unit-patient-chart/issues/219) | P3–P8 | крос-функціональні тести |
| 10 | [#220 [Phase 10] Final verification and acceptance of lower-limb workflow changes](https://github.com/volyamnyi/intensive-care-unit-patient-chart/issues/220) | P9 | docs, фінальна регресія, закриття |

Паралельність: P3, P4, P6, P7, P8 незалежні між собою (крім спільних файлів `WizardScreen.tsx`/`data-prosth.sql`/Liquibase — рекомендовано виконувати послідовно, щоб уникнути конфліктів правок; P5 — після P2; P9/P10 — завершальні).

## 4. Зведений impact-аналіз по файлах (по фазах)

### Backend (main)
| Файл | P1 | P2 | P3 | P4 | P5 | P6 | P7 | P8 |
|---|---|---|---|---|---|---|---|---|
| `service/BrakService.java` | ауд. | проєкт. | | | ✏️ | | | |
| `service/FlowInstanceService.java` | ауд. | проєкт. | ✏️ | | | ✏️ | ✏️ | ✏️(enum usage) |
| `service/EvidenceFileService.java` | ауд. | | | | | ✏️ | | |
| `service/QualityGateService.java` | ауд. | | | | | | ✔(не чіпати) | |
| `controller/FlowInstanceController.java` | ауд. | | | | | ✏️ | | |
| `entity/PauseCategory.java` | ауд. | | | | | | | ✏️ |
| `entity/StepExecution.java` | ауд. | | | | | ✏️ | | |
| `dto/StepExecutionResponse.java` + `mapper/FlowInstanceMapper` | | | | | | ✏️ | | |
| `dto/PauseRequest.java` | | | | | | | | ✔ |
| `dto/FailRequest.java` | | | | | | | ✔ | |
| новий `dto/StepNotePatchRequest.java` | | | | | | ✏️ | | |
| `common/…/data-prosth.sql` | ауд. | | ✏️ | ✏️ | | | | |
| новий `db/changelog/prosth/006-step-note-pause-categories.sql` + реєстрація у `db.changelog-master-prosth.yaml` | | | | | | ✏️ | | ✏️ |

### Backend (tests)
| Файл | Фази |
|---|---|
| `service/BrakServiceTest.java` | P5 (етап 9 кейси) |
| `service/FlowInstanceServiceTest.java` | P3 (комбінації), P6 (note), P8 (enum) |
| `service/TpLl02ValidationUnitTest.java` | P3 (8 edge-кейсів) |
| `controller/FlowInstanceControllerTest.java` | P6, P8 |
| `service/EvidenceFileServiceTest.java` | P6 (list/delete) |
| `integration/BrakIntegrationTest.java` | P5 (етап 9 сценарії), P8 |
| `integration/TpLl02BusinessRulesIntegrationTest.java` | P3, P8 |
| новий `integration/Stage7ConditionalIntegrationTest.java` | P3 |
| новий `integration/Stage10BackwardIntegrationTest.java` | P4 |
| `TpLl02SeedValidationTest.java` | P3 (69→70 елементів), P4 (allow_backward), P8 |
| `service/FailureSnapshotServiceTest.java` | P7 (внутрішній quality_gate лишається) |
| `service/AuditServiceTest.java` | P7 (`MATERIAL_DEFECT`→`materials`) |

### Frontend
| Файл | P3 | P4 | P5 | P6 | P7 | P8 |
|---|---|---|---|---|---|---|
| `pages/prosthetics/process/WizardScreen.tsx` | ✏️ | ✔(регресія) | ✏️ | ✏️ | ✔ | ✏️ |
| `prosthetics/failureCategories.ts` | | | | | ✏️ | |
| `prosthetics/types.ts` | | | | ✏️ | | ✏️ |
| `api/prosthetics.ts` | | | | ✏️ | | |
| новий `components/prosthetics/StepNoteAttachments.tsx` | | | | ✏️ | | |
| новий `prosthetics/softLinerRules.ts` (спільний предикат) | ✏️ | | | | | |
| `pages/prosthetics/process/FailedScreen.tsx` | | | | | ✏️ | |

### Frontend tests
| Файл | Фази |
|---|---|
| `test/prosthetics/WizardScreen.test.tsx` | P3, P5, P6, P8 |
| `test/prosthetics/WizardScreen.brak.test.tsx` | P5 |
| `test/prosthetics/validation.test.ts` | P3 |
| `test/prosthetics/prostheticsApi.test.ts` | P6, P8 |
| новий `test/prosthetics/StepNoteAttachments.test.tsx` | P6 |
| `test/prosthetics/FailedScreen.test.tsx` | P7 |

### E2E (tests/)
| Файл | Фази |
|---|---|
| `helpers/tp-ll-02-flow.ts` | P3 (buildValues special-case), P5, P6 |
| `helpers/prosthetics-flow.ts` | P3 (buildValues special-case) |
| `pages/prosthetics/WizardExecutionPage.ts` | P8 (pauseProcess) |
| новий `specs/prosthetics/tp-ll-02-stage7-soft-liner.spec.ts` | P3 |
| новий `specs/prosthetics/tp-ll-02-stage9-brak.spec.ts` (або розширення `tp-ll-02-brak-branch.spec.ts`) | P5 |
| новий `specs/prosthetics/tp-ll-02-stage10-backward.spec.ts` | P4 |
| новий `specs/prosthetics/tp-ll-02-step-notes-files.spec.ts` | P6 |
| новий `specs/prosthetics/tp-ll-02-reasons.spec.ts` | P7+P8 |
| `prosthetics-e2e.spec.ts` | P8 |
| `tp-ll-02-conditional.spec.ts` (зняти skip) | P3 |
| `wizard-checkbox-surface.spec.ts`, `tp-ll-02-full-lifecycle.spec.ts`, `tp-ll-02-failure-replacement.spec.ts`, `tp-ll-02-no-gate-regression.spec.ts`, `tp-ll-02-brak-branch.spec.ts` | P3 (хелпер), P9 (регресія) |

### БД (Liquibase prosth)
| Зміна | Фаза | Деталі |
|---|---|---|
| 006-1: `ALTER TABLE prosthetics_step_executions ADD COLUMN note TEXT` | P6 | + rollback |
| 006-2: `ALTER TABLE prosthetics_flow_instances ALTER COLUMN pause_category TYPE VARCHAR(32)` | P8 | + rollback |
| 006-3: data-міграція `UPDATE … SET pause_category = NULL WHERE pause_category IN ('PATIENT','MATERIAL','TECH_IDLE')` | P8 | idempotent, forward-fix (див. Production-Deployment-Runbook) |
| Seed `data-prosth.sql`: новий елемент `f0000240` (DO UPDATE SET), steps-INSERT → DO UPDATE SET, `e0000032.allow_backward=true` | P3/P4 | seed-only, не Liquibase |

### API impact
| Зміна | Фаза |
|---|---|
| `POST /instances/{id}/brak` — розширено валідацію тригера (етап 9) | P5 |
| `PATCH /instances/{id}/step-executions/{executionId}` `{note}` — новий | P6 |
| `GET /instances/{id}/evidence?executionId=` — новий | P6 |
| `DELETE /instances/{id}/evidence/{fileId}` — новий | P6 |
| `POST /instances/{id}/fail` — allowlist категорій (400 на вилучену) | P7 |
| `POST /instances/{id}/pause` — приймає лише 4 нові enum-значення (автоматично) | P8 |

---

## 5. План тестування (зведений)

### Unit (backend)
- P3: 8 комбінацій трьох checkbox (DENY/ALLOW) у `validateValues`; P4: `backward` з етапу 10 (target allowBackward=true у snapshot); P5: brak-валідація для етапу 9 (trigger, allowed stages 1/2/3, відхилення інших, RBAC); P6: note-валідація (порожнеча/довжина), evidence list/delete rules; P7: fail-category allowlist; P8: enum-паритет (4 значення).

### Integration (backend, `mvn test -Pintegration-test`)
- P3: повний цикл API→Service→DB для всіх комбінацій кроку 7.1; P4: backward етап 10→9→8; P5: brak етапу 9 → BRANCHED + нова гілка + історія; P6: note PATCH + upload + metadata + delete; P7: fail з вилученою категорією → 400; P8: pause зі старими значеннями → 400, з новими → 200.

### Playwright E2E
- P3: етап 7 UI — кнопки немає, checkbox під «Тактильним контролем», 8 комбінацій через UI/API; P4: етап 10 «Попередній» активна → перехід; P5: повний сценарій етапу 9 (Брак → підтвердити → етап 1/2/3 → нова гілка → історія); P6: примітка+файл → зберегти → перевідкрити; P7: «Повторна невдача на Quality Gate» відсутня у popup та недоступна через API; P8: popup містить рівно 4 причини.

### Регресія (P9/P10)
- Усі існуючі prosthetics-спеки (full-lifecycle, failure-replacement, brak-branch, no-gate-regression, wizard-checkbox-surface, prosthetics-e2e, responsive mobile/tablet) + міжфункціональна взаємодія: brak→нова гілка→крок 7.1→файли→етап 10→пауза→провал.

---

## 6. Критерії якості (з ТЗ §12) — як забезпечуються

- **Не поверхневі UI-зміни:** P3/P4/P5 мають серверні правила (validateValues/backward-конфігурація/brak-валідація).
- **Не дублювати бізнес-логіку:** brake-механізм перевикористовується (P5), upload — через існуючий EvidenceFile (P6).
- **Source of truth:** довідники — backend enum/allowlist (P7, P8); frontend — дзеркало.
- **Backward compatibility:** legacy-категорія quality_gate лишається внутрішньо; pause data-міграція forward-fix; старі snapshots не конвертуються.
- **Історія/audit:** brak-events, audit-логи на кожну мутацію (NOTE, UPLOAD, DELETE, PAUSE, FAIL, BRANCH).
- **Транзакційність:** @Transactional + findByIdForUpdate; частковий унікальний індекс.
- **Authorization:** requireOwner на всі нові ендпоінти; E2E RBAC-сценарії.
- **Позитивні+негативні сценарії:** 8 комбінацій checkbox, недозволені етапи повернення, чужі файли → 404.

## 7. Фінальний regression/verification план (Phase 10)

1. Pre-flight: `mvn compile`, `npm run lint`, `npx tsc --noEmit`, `npm run build`.
2. CI повний цикл: `format-check`, `backend-test`, `backend-integration`, `frontend-test`, `e2e-test`, `build` — усі green (за правилом проєкту тести — тільки через CI).
3. Перевірка acceptance criteria кожної фази (її Issue).
4. Мануальний сценарій (dev): етап 7 (комбінації) → етап 8 → етап 9 (брак → гілка на етап 1/2/3) → повторний прохід → етап 10 («Попередній» туди-назад) → пауза (4 причини) → провал (без вилученої причини) → примітки/файли на всіх кроках.
5. Docs: README/AGENTS — оновлення counts (backend/frontend/E2E), API-таблиця (3 нові ендпоінти), розділ Seed Data (TP-LL-02 правки).
6. Закриття Issues #211–#220 з посиланнями на CI-рани.

---

## 8. Phase 9 / Phase 10 completion status (2026-09-03)

### Phase 9 — #219 CLOSED
- Coverage matrix 100%: unit (8 combos of 7.1 ALLOW/DENY in TpLl02ValidationUnitTest;
  brak triggers D17+D20 in BrakServiceTest; backward config; note validation; fail
  allowlist; pause enum parity) + integration (CrossFeatureRegressionIntegrationTest:
  brak stage6/9, 7.1 both ALLOW variants, note verbatim, evidence upload/list,
  backward 30->29 and 33->32, pause WENT_ABROAD, fail other->replacement NEW + audit)
  + E2E (tp-ll-02-cross-feature.spec.ts, 3 serial tests).
- Commits: f723003 (coverage), 83995e6 (BrakServiceTest NPE fix), c4b2c6d
  (wizard heading-assertion fix — step view has no h1/h2, wait for progressbar +
  pause CTA instead).
- CI: red 33684392642 and 33685162607 triaged/fixed; GREEN 33721486258 (all 6 jobs,
  E2E 20m22s). Flake-check repeat waived per user decision.
- No describe.skip/test.skip left without justification (conditional/validation specs
  marked stale + superseded; validation-edge-cases time-window skip pre-existing).

### Phase 10 — #220 acceptance (this issue)
- [x] Full CI green on main: 33721486258 (format-check, backend-test,
      backend-integration, frontend-test, e2e-test, build).
- [x] Phases 3-8 acceptance criteria verified against tests:
      P3 soft-liner ALLOW rule (unit+integration+E2E), P4 issuance backward
      (integration+E2E), P5 stage-9 brak branching (unit+integration+E2E),
      P6 notes/evidence CRUD (unit+integration+E2E), P7 fail allowlist without
      quality_gate (unit+integration+E2E), P8 four pause reasons (unit+integration+E2E).
- [x] Branch integrity (parent/child, branchSequence, brak-events, history, audit)
      covered by BrakIntegrationTest + CrossFeatureRegressionIntegrationTest +
      tp-ll-02-cross-feature.spec.ts.
- [x] Docs: README prosthetics API table (PATCH note, backward, brak trio, evidence
      quad, fail allowlist, 4 pause reasons) + counts (backend 360/142, frontend
      135/89 ~718 tests, E2E 83 specs/384 tests); AGENTS.md session + counts synced.
- [x] Issues #211-#220 closed with summaries and CI run links.