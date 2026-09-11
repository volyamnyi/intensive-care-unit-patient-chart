# Release notes — Моніторинг виробництва (epic #271, issues #272–#282)

Read-only огляд виробництва протезів: `/prosthetics/production`.

## Що входить

- **Дашборд** (`ProductionPage.tsx`): 9 KPI-карток, фільтри (статус / протезист / прострочення), таблиця виробів (Протезист → Виріб → Пацієнт) з пагінацією.
- **Картка виробу** (`ProductionWorkItemDrawer`): шапка, пацієнт, замовлення, час факт/норма, таймлайн етапів, якість.
- **Команда** (вкладка, лише `VIEW_ALL`): навантаження по протезистах, drill-down до виробів.
- **Увага** (вкладка): черга проблемних виробів за тяжкістю FAILED > OVERDUE > REPEAT_BRAK > REWORK > STALE > NO_ASSIGNEE.
- **Нормативи** (`ProductionNormativeSettings`, лише `VIEW_ALL`, зміни аудитуються).
- **Графік** (`ProductionTrendChart`, `recharts@3.10.1` exact + `ui/chart.tsx` + `--chart-1..5`): динаміка створення/завершення/провалів по днях, рахується клієнтськи (`bucketTrend` → `TrendPoint{date,label,created,completed,failed}`: `created = startTime ?? createdAt`, термінал = `endTime`, майбутнє ігнорується).

## Доступи (4 нові коди, категорія «Протезування»)

| Код | Мітка | Типово |
|---|---|---|
| `PROSTHETICS_PRODUCTION_VIEW` | Моніторинг виробництва | PROSTHETIST, PROSTHETICS_ADMINISTRATOR, HEAD_OF_DEPARTMENT |
| `PROSTHETICS_PRODUCTION_VIEW_ALL` | Виробництво всіх протезистів | PROSTHETICS_ADMINISTRATOR, HEAD_OF_DEPARTMENT |
| `PROSTHETICS_PRODUCTION_PATIENT_VIEW` | Дані пацієнта у виробництві | PROSTHETICS_ADMINISTRATOR |
| `PROSTHETICS_PRODUCTION_QUALITY_VIEW` | Якість виробництва | PROSTHETIST, PROSTHETICS_ADMINISTRATOR |

Без `VIEW_ALL` видно лише власні вироби (assignee примусово = self; чужі деталі — 404). Без `PATIENT_VIEW` пацієнт маскований (тільки ПІБ, без документів і `documentUrl`). Без `QUALITY_VIEW` прихована серія провалів. `ADMINISTRATOR` типово не має жодного з чотирьох кодів.

## API (`/api/prosthesis-manufacturing/production`)

`GET /production` (сторінка `ProductionWorkItemDto`), `GET /summary` (`ProductionSummaryDto`), `GET /attention`, `GET /team` (лише `VIEW_ALL`), `GET /{id}` (`ProductionDetailDto`), `GET/PUT /settings/normative` (лише `VIEW_ALL`).

## Правила розрахунку

- Активний час = SUM(`StepExecution.activeSeconds`); доопрацювання = COUNT(нащадків за `parentInstanceId`); провал = `status FAILED`.
- Назви/норми — з незмінного `templateSnapshot`; номер замовлення = `MIS-{patientId}-{documentId}` → `matchDocument`.
- OVERDUE = факт > норма × K (`prosthetics.production.overdueMultiplier`, типово 1.5, межі 1.0–5.0); STALE = відкритий виріб без руху ≥ N днів (`prosthetics.production.staleDays`, типово 7, межі 1–30).

## База даних

- `core/008-prosthetics-production-permissions.sql` — 4 коди дозволів (каталог 24→28).
- Нових індексів не додано: EXPLAIN-аудит (#281) підтвердив достатність наявних (`idx_flow_instances_assignee/status/parent`, `idx_step_executions_instance`, `idx_brak_events_instance/new_instance`); read-модель — 1 запит інстансів + batch IN-агрегації, без per-row N+1.
- `prosth/010` не будувався: перепризначення виконавця визнано wont-do (#280 — операції перепризначення не існує).

## Відомий ризик фінального CI

E2E-спеки (`production-access/dashboard/team`) потребують живих документів MIS на придатних кандидатах. Проміжний CI `34530390354` (код #274) впав у сетапі: `need 2 MIS order documents, Received: 0`. Якщо живий MIS не поверне документів — ті самі сетапи впадуть у фінальному циклі; це перший кандидат на тріаж.
