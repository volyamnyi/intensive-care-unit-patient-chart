# Карта інтенсивної терапії (ICU Patient Chart)

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk)](https://jdk.java.net/25/)
[![React](https://img.shields.io/badge/React-19-61DAFB?logo=react)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-6.0-3178C6?logo=typescript)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-8.1-646CFF?logo=vite)](https://vite.dev/)
[![Base UI](https://img.shields.io/badge/Base%20UI-1.6-0054FF?logo=baseui)](https://base-ui.com/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-4.3-38BDF8?logo=tailwindcss)](https://tailwindcss.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> A full-stack electronic medical record system for Intensive Care Units (ICU). Implements the Ukrainian standard form **003-15/о «Карта інтенсивної терапії»**. Enables doctors and nurses to digitally manage patient ICU charts — including hourly vital sign tracking, fluid balance monitoring, prescription management, clinical scale assessments, PDF generation (A4 landscape, Times New Roman), and digital signing workflows.

**Also includes: Prosthetics Manufacturing Module** — A workflow management system for prosthetics production (from order selection → template selection → step-by-step wizard → quality gate decisions → PDF reports), with role-based access (PROSTHETIST / PROSTHETICS_ADMINISTRATOR).

---

## Features

- **Form 003-15/о compliant** — Electronic ICU chart matching the Ukrainian paper standard
- **PDF Generation** — A4 landscape with tabular layout, Times New Roman font, all card sections
- **PDF Transfer to MIS** — PDF is stored as binary, transmitted to MIS, with transfer status tracking (PENDING/SENT/FAILED)
- **Single-Page Layout** — Two-column design (table + resizable sidebar via left-edge drag), all sections always visible, no tabs/accordions

### For AUDITOR
- **Audit Log Viewer** — Read-only access to paginated audit logs with filters
- **Patient Data** — Read-only view of episodes and clinical data

### For Doctors / HOD
- **Episode Dashboard** — View all active ICU episodes with patient names and status
- **Episode Creation** — Search patients from the hospital information system (MIS) and create new episodes
- **Clinical Day Timeline** — Visual timeline of all clinical days per episode with status (OPEN, NURSE_SIGNED, DOCTOR_SIGNED, REOPENED)
- **Hourly Vital Signs** — Full 24-hour vital sign tables with color-coded completion status
- **Prescription Management** — Create and cancel medication/lab orders with dose, route, frequency
- **Prescription Dashboard** — Department toggle (Хірургія/Реабілітація), sortable patient table, search filter, 40 seed patients
- **Prescription Grid** — Inline 21-day spreadsheet with 7-day scroll window, color-coded cells (blue=planned, green=completed, purple=cancelled), click-to-edit dose editing
- **Clinical Scale Assessments** — Record and view APACHE II, SOFA, RASS, CAM-ICU, Braden scores
- **Medical Notes** — Add typed clinical notes per day
- **Digital Sign-Off** — Two-stage signing workflow (nurse → doctor/HOD), triggers PDF generation
- **PDF Export** — Auto-generated professional PDF for each signed day
- **Fluid Balance** — Calculated intake/output per hour with cumulative totals
- **Reopen Workflow** — HOD can reopen a signed day for corrections

### For Nurses
- **Episode List** — View all active episodes with patient names
- **Hourly Vital Sign Entry** — Enter vitals for each hour with HTML5 validation (min/max ranges)
- **Hour Selector** — Visual 24-hour strip with color-coded completed/missed/current hours
- **Fluid Balance** — View intake, output, daily and cumulative balance
- **Nurse Sign-Off** — First stage of the two-stage signing workflow
- **Prescription Execution** — Execute medication doses inline with 2-factor authorization popover

### For Prosthetists / Prosthetics Administrators
- **Prosthetics Dashboard** — View own flow instances with status filters (Active, Paused, Completed, Failed)
- **Setup Wizard** — Patient search (from local mock DB) → Order selection → Order review (recipe PDF) → Template selection → Instance creation
- **Execution Wizard** — Step-by-step guided workflow with validation (measurements, anamnesis, manufacturing, file uploads)
- **Pause/Resume** — Timer-aware pausing with categorized reasons (Materials, Approval, Technical, Other)
- **Quality Gate** — PASS / REWORK / FAIL decisions (PROSTHETICS_ADMINISTRATOR only)
- **Rework Loop** — Automatic rollback to target step/stage with max attempt limits
- **Failure Handling** — Failure snapshot capture (reason, description, attachments) + replacement instance creation
- **PDF Reports** — Recipe PDF (order review), Instance PDF (Done screen), Failure PDF (Failed screen)
- **Evidence Upload** — Image/PDF uploads (10 MB limit) per step

### Global UI
- **GlobalLayout** — Unified AppBar header with dynamic route-based titles for all pages
- **Dark/Light Theme** — Default light mode with global toggle in header
- **App Selector** — Choose between ICU Chart, Prescription Sheet, and Prosthetics Manufacturing modules

### Automated
- **Audit Logging** — All entity operations are logged with user, timestamp, and diff
- **Optimistic Locking** — JPA `@Version` prevents concurrent edit conflicts

---

## Tech Stack

### Backend
| Technology | Version | Purpose |
|---|---|---|
| Java | 25 | Runtime |
| Spring Boot | 4.1.0 | Application framework |
| Spring Data JPA | — | ORM / database access |
| Spring Security | — | JWT-based authentication |
| PostgreSQL | 16 | Relational database |
| Hibernate | — | JPA implementation |
| jjwt | 0.12.5 | JWT token handling |
| iText 7 | 8.0.4 | PDF generation |
| Lombok | — | Boilerplate reduction |
| Maven | — | Build tool |

### Frontend
| Technology | Version | Purpose |
|---|---|---|
| React | 19.2.7 | UI framework |
| TypeScript | 6.0 | Type-safe JavaScript |
| Vite | 8.1 | Build tool / dev server |
| Base UI | 1.6 | Headless UI primitives (shadcn-style components) |
| Tailwind CSS | 4.3 | Utility-first styling |
| Axios | 1.18 | HTTP client |
| React Router DOM | 7.18 | Client-side routing |
| Sonner | 2.0 | Toast notifications |
| Day.js | 1.11 | Date manipulation |
| Oxlint | 1.73 | Linter |
| Vitest | 3.2 | Unit testing |

### E2E Testing
| Technology | Version | Purpose |
|---|---|---|
| Playwright | 1.61 | Browser automation |


---

## Architecture

```
┌──────────────┐     HTTP/JSON      ┌──────────────┐     JDBC      ┌────────────┐
│   Frontend   │ ◄──────────────────► │   Backend    │ ◄────────────► │ PostgreSQL │
│  (React 19)  │   localhost:5173     │ (Spring Boot)│  localhost:5432│    16      │
│              │                      │ localhost:8085│               │            │
│  Vite Dev    │    JWT Bearer Auth   │              │               │            │
│  Server      │                      │  JWT Filter  │               │            │
└──────────────┘                      └──────┬───────┘               └────────────┘
                                             │
                                     ┌───────┴───────┐
                                     │  WireMockMisService│
                                     │  (or real MIS) │
                                     └───────────────┘
```

- Frontend communicates via RESTful JSON APIs with JWT Bearer auth
- Backend integrates with MIS via a pluggable `MisService` interface (mock implementation by default)
- Scheduled tasks handle day transitions and escalation checks
- **Module boundaries are enforced**: backend ArchUnit test (`backend/app/src/test/java/com/superhumans/architecture/ModuleBoundaryTest.java`) restricts the feature modules (`medication-sheet`, `prosthesis-manufacturing`) to a shared platform allowlist; frontend oxlint rules (`frontend/.oxlintrc.json` `no-restricted-imports`) forbid cross-feature imports
- **Prosthetics Manufacturing** is a separate backend module (`prosthesis-manufacturing`) with its own entities, services, and REST endpoints under `/api/prosthesis-manufacturing`, using local mock tables (not MIS)

---

## Prerequisites

- **JDK** 25
- **Node.js** 20 or later, **npm** 10+
- **PostgreSQL** 16
- **Maven** 3.9+ (or use `mvnw`)
- **Docker** (not required — integration tests run against a local/CI PostgreSQL 16)

---

## Getting Started

### 1. Database

The application uses **4 separate PostgreSQL databases** (one per module), each with its own schema, managed by its own Liquibase changelog:

| Database | Module | Purpose / contents |
|---|---|---|
| `my_fullstack_core` | COMMON (single-deployment core) | Users & authentication, dynamic RBAC (`permissions` + `role_permissions` matrix), audit log (`audit_logs`), system settings and reference values |
| `my_fullstack_icu` | ICU Chart | Episodes, clinical days, hourly records, medical orders & executions, notes, clinical scale results, signatures, generated PDFs, labs, ventilation, patient state, fluid balance |
| `my_fullstack_med` | Medication Sheet | Prescription lists/items/days/parts/executions/signatures, vital sign lists, medicine/allergy/drug-interaction caches, telegram subscriptions |
| `my_fullstack_prosth` | Prosthetics Manufacturing | Patients, orders, flow templates, flow instances & step executions, quality gates & decisions, failure snapshots, evidence files |
| `my_fullstack_db` | — (bootstrap only, **not used by the app**) | Default database auto-created by the PostgreSQL Docker service container in CI (`POSTGRES_DB` env var, required by the image); the application never connects to it — all CI jobs create the 4 real databases above inside that container |

Create the 4 application databases:

```bash
psql -U postgres -c "CREATE DATABASE my_fullstack_core;"
psql -U postgres -c "CREATE DATABASE my_fullstack_icu;"
psql -U postgres -c "CREATE DATABASE my_fullstack_med;"
psql -U postgres -c "CREATE DATABASE my_fullstack_prosth;"
```

`run-test.ps1` / `run-full-test.bat` create any missing databases automatically before starting the servers.

### 2. Backend

```bash
cd backend
mvn clean package -DskipTests
java -jar app/target/app-*.jar
# Starts on http://localhost:8085
```

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
# Starts on http://localhost:5173
```

Open **http://localhost:5173** and log in with seed credentials.

---

## Configuration

Backend settings in `backend/common/src/main/resources/application.yml`:

```yaml
server:
  port: 8085

app:
  datasource:
    core:
      url: jdbc:postgresql://localhost:5432/my_fullstack_core
      username: postgres
      password: admin
    icu:
      url: jdbc:postgresql://localhost:5432/my_fullstack_icu
      username: postgres
      password: admin
    med:
      url: jdbc:postgresql://localhost:5432/my_fullstack_med
      username: postgres
      password: admin
    prosth:
      url: jdbc:postgresql://localhost:5432/my_fullstack_prosth
      username: postgres
      password: admin
  seed-data:
    enabled: true        # run data-{core,icu,med,prosth}.sql at boot
  jwt:
    secret: <base64-secret>
    expiration-ms: 86400000
  mis:
    wiremock-enabled: true
    embedded-wiremock-enabled: true
```

Every datasource can be overridden with environment variables: `APP_DATASOURCE_<CORE|ICU|MED|PROSTH>_<URL|USERNAME|PASSWORD>`. Schema is managed per DB by Liquibase (`db/changelog/db.changelog-master-{core,icu,med,prosth}.yaml`).

Frontend API URL in `frontend/src/api/client.ts`:

```typescript
const API_BASE = 'http://localhost:8085/api';
```

---

## Running the Application

### Development

```bash
# Terminal 1 — Backend
cd backend && mvn -pl app spring-boot:run

# Terminal 2 — Frontend
cd frontend && npm run dev
```

### API Documentation (Swagger UI)

Once the backend is running, access the interactive API documentation:

- **Swagger UI**: http://localhost:8085/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8085/api-docs
- **OpenAPI YAML**: http://localhost:8085/api-docs.yaml

> Note: API docs are disabled in the `prod` profile for security.

### Production

```bash
cd frontend && npm run build          # outputs to frontend/dist/
cd ../backend && mvn clean package -DskipTests
java -jar app/target/app-*.jar
```

---

## API Endpoints

### Authentication
| Method | URL | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/login` | No | Login, returns JWT + user info |

### Episodes (replaces legacy ICU cards)
| Method | URL | Auth | Description |
|---|---|---|---|
| `GET` | `/api/episodes` | Yes | Search episodes by patientId / status |
| `GET` | `/api/episodes/{id}` | Yes | Get episode by ID |
| `POST` | `/api/episodes` | Yes | Create episode |
| `PATCH` | `/api/episodes/{id}` | Yes | Update episode |
| `POST` | `/api/episodes/{id}/close` | Yes | Close episode |
| `GET` | `/api/episodes/{id}/clinical-days` | Yes | List clinical days for episode |

### Patients (MIS)
| Method | URL | Auth | Description |
|---|---|---|---|
| `GET` | `/api/patients` | Yes | Search patients by query (name, card#, phone) |

### Clinical Days
| Method | URL | Auth | Description |
|---|---|---|---|
| `GET` | `/api/clinical-days/{id}` | Yes | Get clinical day |
| `POST` | `/api/clinical-days` | Yes | Create clinical day |
| `PATCH` | `/api/clinical-days/{id}` | Yes | Update clinical day |
| `POST` | `/api/clinical-days/{id}/sign/nurse` | Nurse | Nurse sign-off |
| `POST` | `/api/clinical-days/{id}/sign/doctor` | Doctor/HOD | Doctor sign-off |
| `POST` | `/api/clinical-days/{id}/reopen` | HOD | Reopen signed day |

### Hourly Records
| Method | URL | Auth | Description |
|---|---|---|---|
| `GET` | `/api/clinical-days/{id}/hourly-records` | Yes | List hourly records |
| `POST` | `/api/clinical-days/{id}/hourly-records` | Yes | Create hourly record |
| `PATCH` | `/api/hourly-records/{id}` | Yes | Update hourly record |

### Medical Orders
| Method | URL | Auth | Description |
|---|---|---|---|
| `GET` | `/api/clinical-days/{id}/orders` | Yes | List orders |
| `POST` | `/api/clinical-days/{id}/orders` | Doctor | Create order |
| `PATCH` | `/api/orders/{id}` | Doctor | Update order |
| `POST` | `/api/orders/{id}/cancel` | Doctor | Cancel order |

### Order Executions
| Method | URL | Auth | Description |
|---|---|---|---|
| `GET` | `/api/orders/{id}/executions` | Yes | List executions |
| `POST` | `/api/orders/{id}/execute` | Nurse | Execute order |
| `PATCH` | `/api/executions/{id}` | Yes | Update execution |

### Medical Notes
| Method | URL | Auth | Description |
|---|---|---|---|
| `GET` | `/api/clinical-days/{id}/notes` | Yes | List notes |
| `POST` | `/api/clinical-days/{id}/notes` | Yes | Create note |
| `PATCH` | `/api/notes/{id}` | Yes | Update note |

### Clinical Scales
| Method | URL | Auth | Description |
|---|---|---|---|
| `GET` | `/api/scales` | Yes | List available scales |
| `GET` | `/api/clinical-days/{id}/scales` | Yes | Get scale results for clinical day |
| `POST` | `/api/clinical-days/{id}/scales` | Yes | Create scale result |
| `PATCH` | `/api/scales/{id}` | Yes | Update scale result |
| `GET` | `/api/episodes/{episodeId}/scales` | Yes | Get episode-level scale results |
| `POST` | `/api/episodes/{episodeId}/scales` | Yes | Create episode-level scale result |
| `POST` | `/api/episodes/{episodeId}/scales/calculate` | Yes | Calculate and save scale from raw data |

### Fluid Balance
| Method | URL | Auth | Description |
|---|---|---|---|
| `GET` | `/api/clinical-days/{id}/fluid-balance` | Yes | Get balance items |
| `POST` | `/api/clinical-days/{id}/fluid-balance/recalculate` | Yes | Recalculate balance |

### PDF
| Method | URL | Auth | Description |
|---|---|---|---|
| `GET` | `/api/clinical-days/{id}/pdf` | Yes | Get latest generated PDF |
| `POST` | `/api/clinical-days/{id}/pdf` | Yes | Generate PDF |
| `GET` | `/api/clinical-days/{id}/pdf/status` | Yes | Get PDF transfer status |

### Users
| Method | URL | Auth | Description |
|---|---|---|---|
| `GET` | `/api/users/me` | Yes | Current user |
| `GET` | `/api/users/doctors` | Admin | List doctors |
| `GET` | `/api/users/nurses` | Admin | List nurses |

### Prescriptions — Medication Sheet (Листок лікарських призначень)
| Method | URL | Auth | Description |
|---|---|---|---|
| `GET` | `/api/prescriptions?patientId=` | Yes | List prescriptions for patient |
| `GET` | `/api/prescriptions/{id}` | Yes | Get prescription by ID |
| `POST` | `/api/prescriptions` | Doctor/HOD | Create prescription list |
| `DELETE` | `/api/prescriptions/{id}` | Doctor/HOD | Delete prescription |
| `POST` | `/api/prescriptions/{id}/close` | Doctor/HOD | Close prescription |
| `GET` | `/api/prescriptions/{listId}/items` | Yes | List prescription items |
| `POST` | `/api/prescriptions/{listId}/items` | Doctor/HOD | Add medicine item (auto-creates 21-day grid) |
| `DELETE` | `/api/prescriptions/items/{itemId}` | Doctor/HOD | Remove item |
| `POST` | `/api/prescriptions/items/{itemId}/days` | Doctor/HOD | Add next day (max day date + 1) with 4 unplanned day parts |
| `DELETE` | `/api/prescriptions/items/{itemId}/days/{dayId}` | Doctor/HOD | Remove a day (422 if any day part is completed) |
| `PUT` | `/api/prescriptions/day-parts/{id}/plan` | Doctor/HOD | Plan dose for day part |
| `PUT` | `/api/prescriptions/day-parts/{id}/complete` | Nurse/HOD | Complete day part |
| `POST` | `/api/prescriptions/day-parts/{id}/execute` | Nurse/HOD | Execute dose (with optional 2P auth) |
| `GET` | `/api/prescriptions/allergies?patientId=` | Yes | Patient allergies (from MIS) |
| `GET` | `/api/prescriptions/medicine-catalog?keyword=` | Yes | Medicine catalog search |

### Prosthetics Manufacturing (Виробництво протезів)
| Method | URL | Auth | Description |
|---|---|---|---|
| `GET` | `/api/prosthesis-manufacturing/patients` | PROSTHETIST, PROSTHETICS_ADMIN | Search patients |
| `GET` | `/api/prosthesis-manufacturing/patients/{id}` | PROSTHETIST, PROSTHETICS_ADMIN | Get patient by ID |
| `POST` | `/api/prosthesis-manufacturing/patients` | PROSTHETICS_ADMIN | Create patient |
| `GET` | `/api/prosthesis-manufacturing/orders` | PROSTHETIST, PROSTHETICS_ADMIN | List orders |
| `GET` | `/api/prosthesis-manufacturing/orders/{id}` | PROSTHETIST, PROSTHETICS_ADMIN | Get order by ID |
| `POST` | `/api/prosthesis-manufacturing/orders` | PROSTHETICS_ADMIN | Create order |
| `GET` | `/api/prosthesis-manufacturing/templates` | PROSTHETIST, PROSTHETICS_ADMIN | List flow templates |
| `GET` | `/api/prosthesis-manufacturing/templates/{id}` | PROSTHETIST, PROSTHETICS_ADMIN | Get template by ID |
| `POST` | `/api/prosthesis-manufacturing/templates` | PROSTHETICS_ADMIN | Create template |
| `PATCH` | `/api/prosthesis-manufacturing/templates/{id}` | PROSTHETICS_ADMIN | Update template |
| `GET` | `/api/prosthesis-manufacturing/instances` | PROSTHETIST, PROSTHETICS_ADMIN | List flow instances |
| `GET` | `/api/prosthesis-manufacturing/instances/{id}` | PROSTHETIST, PROSTHETICS_ADMIN | Get instance by ID |
| `POST` | `/api/prosthesis-manufacturing/instances` | PROSTHETIST | Create instance from order + template |
| `GET` | `/api/prosthesis-manufacturing/instances/{id}/step-executions` | PROSTHETIST, PROSTHETICS_ADMIN | Get step executions for instance |
| `POST` | `/api/prosthesis-manufacturing/step-executions/{id}/complete` | PROSTHETIST | Complete step execution |
| `GET` | `/api/prosthesis-manufacturing/instances/{id}/quality-gates` | PROSTHETIST, PROSTHETICS_ADMIN | Get quality gates for instance |
| `POST` | `/api/prosthesis-manufacturing/gate-decisions` | PROSTHETICS_ADMIN | Make gate decision (PASS/REWORK/FAIL) |
| `POST` | `/api/prosthesis-manufacturing/instances/{id}/pause` | PROSTHETIST | Pause instance |
| `POST` | `/api/prosthesis-manufacturing/instances/{id}/resume` | PROSTHETIST | Resume instance |
| `POST` | `/api/prosthesis-manufacturing/instances/{id}/replacement` | PROSTHETIST | Create replacement after FAIL |
| `GET` | `/api/prosthesis-manufacturing/instances/{id}/failure-snapshot` | PROSTHETIST, PROSTHETICS_ADMIN | Get failure snapshot |
| `GET` | `/api/prosthesis-manufacturing/instances/{id}/pdf` | PROSTHETIST, PROSTHETICS_ADMIN | Generate PDF report for instance |
| `POST` | `/api/prosthesis-manufacturing/evidence-files` | PROSTHETIST | Upload evidence file |

### Vital Signs
| Method | URL | Auth | Description |
|---|---|---|---|
| `GET` | `/api/vital-signs?prescriptionListId=` | Yes | Get vital sign days |
| `GET` | `/api/vital-signs/days/{dayId}/entries` | Yes | Get entries for day |
| `POST` | `/api/vital-signs` | Yes | Create vital sign entry |

### Audit
| Method | URL | Auth | Description |
|---|---|---|---|
| `GET` | `/api/audit` | Admin/AUDITOR | List audit logs |
| `GET` | `/api/audit/{id}` | Admin/AUDITOR | Get audit log entry |

---

## Seed Data

9 users seeded via `backend/common/src/main/resources/data-{core,icu,med,prosth}.sql` (executed per-datasource by `SeedDataInitializer` at boot):

| Login | Password | Role |
|---|---|---|
| `doctor1` / `doctor2` | `doctor123` | DOCTOR |
| `nurse1` / `nurse2` | `nurse123` | NURSE |
| `head1` | `head123` | HEAD_OF_DEPARTMENT |
| `admin` | `admin123` | ADMINISTRATOR |
| `prosthetist1` / `prosthetist2` | `doctor123` | PROSTHETIST |
| `prosthetics_admin1` | `doctor123` | PROSTHETICS_ADMINISTRATOR |
| *(backend-only)* | — | AUDITOR |

> ⚠️ **Production (A2):** first-boot seeding creates these well-known demo credentials. They must be **rotated or disabled before go-live**. Seeding is disabled under the `prod` profile (`app.seed-data.enabled: false`) and the `SeedDataGuard` boot guard refuses to start if `prod` + seeding are somehow enabled. `data-core.sql` user inserts are `ON CONFLICT (login) DO NOTHING`, so a restart never reverts an operator-rotated password to the demo value.

5 mock patients (from MIS mock):

| Full Name | Card # | Year |
|---|---|---|
| Петренко Іван Сергійович | МК-001234 | 1978 |
| Коваленко Олена Вікторівна | МК-005678 | 1985 |
| Сидоренко Василь Петрович | МК-009012 | 1962 |
| Бондаренко Наталія Петрівна | МК-003456 | 1990 |
| Ткачук Андрій Миколайович | МК-007890 | 1975 |

Prosthetics seed patients (demographics served by the MIS Integration Layer wiremock `__files/patients_52.json`; clinical fields in local tables):

| Patient | ID | Order | Template |
|---|---|---|---|
| Сніжко Іван Петрович | `900001` | ПВ-26-0413 (upper_limb) | TP-UL-01 (ACTIVE) |
| Гаврилюк Олена Миколаївна | `900002` | ПВ-26-0414 (lower_limb) | TP-LL-01 (DRAFT) |

3 seed episodes with 4 seed clinical days (3 OPEN, 1 NURSE_SIGNED) with fixed UUIDs (used in integration tests).

Prosthetics E2E isolation uses fixed seed IDs per spec (no `.first()` race).

---

## Project Structure

```
icu-patient-chart/
├── backend/                    ← Maven multi-module reactor (parent POM + 5 modules: common, icu-chart, medication-sheet, prosthesis-manufacturing, app)
│   ├── common/                 ← shared platform leaf (no internal deps): `@SpringBootApplication` main class, auth/JWT,
│   │   │                         security config, multi-DB wiring, platform controllers (auth/user/patient/admin/audit/
│   │   │                         settings/mock-MIS), `entity/base` + `entity/core` (User, Permission, RolePermission,
│   │   │                         AuditLog, ...), `repository/core`, exception handlers, MIS client, services (Auth,
│   │   │                         Audit, PermissionService/PermissionCatalog), Liquibase changelogs (master yamls + 15
│   │   │                         SQL changesets in `db/changelog/{core,icu,med,prosth}/`)
│   ├── icu-chart/              ← ICU chart feature: `com.superhumans.icu.*` (entities + repositories) + ICU domain
│   │                             packages (controller ×13, service ×16, dto, mapper); depends on common
│   ├── medication-sheet/       ← medication sheet feature (`com.superhumans.medicationsheet.*`); depends on common
│   ├── prosthesis-manufacturing/ ← prosthetics feature (`com.superhumans.prosthesismanufacturing.*`); depends on common
│   └── app/                    ← deployable shell (no production code): repackages the runnable JAR (mainClass in
│                                 common); hosts the ArchUnit `ModuleBoundaryTest`
├── frontend/                   ← React 19 + TypeScript + Vite + Tailwind CSS 4 + Base UI (single app, no feature subfolder roots)
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig*.json
│   ├── index.html
│   └── src/
│       ├── App.tsx             # Root component + routing (Guard / LoginRoute / RoleRedirect)
│       ├── pages/              # LoginPage, AppSelectorPage, doctor/, nurse/, prescription/, prosthetics/, admin/
│       ├── components/         # per-feature: icu/, monitoring/, prescription/, common/, prosthetics/, navigation/, ui/
│       ├── api/                # client.ts + per-feature modules (platform, icu, medication, prosthetics)
│       ├── types/              # shared DTO types: core.ts, icu.ts, medication.ts
│       ├── prosthetics/        # isolated feature root (ProstheticsContext, types, validation, failureCategories)
│       ├── services/           # AuthContext
│       ├── layouts/            # Doctor, Nurse, Global layouts
│       ├── lib/ utils/         # shared helpers (clinicalRanges, errorMessage)
│       └── test/               # Vitest tests (87 files)
├── tests/                      # Playwright E2E (80 spec files, 11 projects)
│   ├── playwright.config.ts
│   ├── pages/                  # Page objects (7)
│   ├── fixtures/               # Role-based test fixtures
│   └── specs/                  # Test specs
├── docs/                       # Технічне завдання (3026 lines), TESTING_GUIDE
└── README.md
```

---

## Development

### Commands

#### Backend
| Command | Action |
|---|---|
| `mvn -pl app spring-boot:run` | Dev server on `:8085` |
| `mvn clean package -DskipTests` | Build JAR |
| `mvn compile` | Compile only |
| `mvn test` | Run unit tests (137 test files) |
| `mvn test -Pintegration-test` | Run integration tests (79) — requires Docker/PostgreSQL |

#### Frontend
| Command | Action |
|---|---|
| `npm run dev` | Vite dev server on `:5173` |
| `npm run build` | `tsc -b && vite build` |
| `npm run lint` | Oxlint |
| `npx tsc --noEmit` | Type-check without build |
| `npm t` | Run Vitest tests (699 across 87 files) |

#### E2E Tests (`cd tests`)
| Command | Action |
|---|---|
| `npx playwright test` | Run all E2E tests (80 spec files, 360 tests) |
| `npx playwright test --project=doctor-chromium --project=hod-chromium --workers=1` | Run only doctor + HOD tests |
| `npx playwright test --ui` | Run with Playwright UI mode |
| `npx playwright test --list` | List tests |
| `npx playwright show-report` | View HTML report |

### Repeatable CI Development Workflow

**All tests run exclusively via GitHub Actions CI — never locally.** The loop: pre-flight local checks → implement → stage/commit (Conventional Commits) → `git push origin main` → GitHub Actions auto-triggers → poll with `gh run watch <run-id>` (or `gh run list`) → triage failures via `gh run view <run-id> --job <job-id> --log` and `gh run download <run-id>` → fix in a new commit → repeat until green.

| Job | What it runs | Trigger |
|---|---|---|
| `format-check` | Checkstyle + oxlint + `tsc --noEmit` | Push to `main` / `develop` or PR to `main` |
| `backend-test` | `mvn clean test` (unit, PostgreSQL service) | Same |
| `backend-integration` | `mvn test -Pintegration-test` | Same |
| `frontend-test` | Vitest + production build | Same |
| `e2e-test` | Playwright (80 spec files; `needs: backend-test, frontend-test`) | Same |
| `build` | JAR + frontend dist artifacts | Main push only; needs all 5 jobs |

Push → CI runs jobs in parallel → if any fails, fix and repeat until every check passes.

### Testing Summary
- **Backend tests**: 137 test files across the multi-module reactor — common (19) + icu-chart (68) + medication-sheet (17) + prosthesis-manufacturing (32) + app (1, ArchUnit `ModuleBoundaryTest`) — `mvn test`
- **Backend integration tests**: 84 tests — `mvn test -Pintegration-test`
- **Frontend Vitest tests**: 699 tests (87 files) — includes responsive + prosthetics suites
- **E2E Playwright tests**: 80 spec files (360 tests), 11 projects (setup, login, api-error-mode, doctor, nurse, hod, admin, api, prosthetics, responsive-mobile, responsive-tablet)
- **CI**: GitHub Actions — PostgreSQL service, JDK 25, Node 22, Playwright chromium, 40min timeout

### Resolved Issues (from exploratory testing — #71-#74)
- [#71](https://github.com/volyamnyi/intensive-care-unit-patient-chart/issues/71) **RESOLVED** — Cyrillic encoding: `data-med.sql` now uses explicit `ON CONFLICT` auto-heal clause
- [#72](https://github.com/volyamnyi/intensive-care-unit-patient-chart/issues/72) **RESOLVED** — MockMIS patient name prefix cleaned
- [#73](https://github.com/volyamnyi/intensive-care-unit-patient-chart/issues/73) **RESOLVED** — Nurse detail view fixed
- [#74](https://github.com/volyamnyi/intensive-care-unit-patient-chart/issues/74) **RESOLVED** — Ghost button removed

> **Note:** All E2E tests require fresh PostgreSQL databases between full runs because seed `data-{core,icu,med,prosth}.sql` uses `ON CONFLICT (id) DO NOTHING`. CI always starts with clean DBs. For local development, run `DROP SCHEMA public CASCADE; CREATE SCHEMA public;` in each of the 4 databases before each test run.

> **Testing Guide:** See [docs/TESTING_GUIDE.md](docs/TESTING_GUIDE.md) for comprehensive testing documentation.

### Commit Conventions

[Conventional Commits](https://www.conventionalcommits.org/):
```
feat: add new feature
fix: correct a bug
refactor: restructure code
docs: update documentation
chore: maintenance tasks
```

### Role Permissions

Access is enforced by a **dynamic role-permission matrix** (25 permission codes across 8 categories in `PermissionCatalog`). The table below lists each permission (code and label) and the roles **granted it by default** via `PermissionCatalog.defaultMatrix()`. Administrators can change these grants at runtime through the admin UI («Доступи та ролі»), and the changes take effect immediately.

| Permission (code → label) | DOCTOR | NURSE | HOD | ADMIN | AUDITOR | ADJ. SPECIALIST | PROSTHETIST | PROSTH. ADMIN |
|---|---|---|---|---|---|---|---|---|
| `EPISODE_CREATE` — Створення епізоду | ✓ | — | ✓ | — | — | — | — | — |
| `CLINICAL_DAY_CREATE` — Створення клінічного дня | ✓ | — | ✓ | — | — | — | — | — |
| `SIGN_NURSE` — Підпис медсестрою | — | ✓ | — | — | — | — | — | — |
| `SIGN_DOCTOR` — Підпис лікарем | ✓ | — | ✓ | — | — | — | — | — |
| `REOPEN_DAY` — Перевідкриття дня | — | — | ✓ | — | — | — | — | — |
| `PRESCRIPTION_CREATE` — Планування призначень | ✓ | — | ✓ | — | — | — | — | — |
| `PRESCRIPTION_LIST_CREATE` — Створення листка призначень | ✓ | — | ✓ | — | — | — | — | — |
| `PRESCRIPTION_EXECUTE` — Виконання призначень | — | ✓ | — | — | — | — | — | — |
| `VITALS_ENTER` — Введення показників | — | ✓ | — | — | — | — | — | — |
| `PATIENT_VIEW` — Перегляд даних пацієнта | ✓ | ✓ | ✓ | ✓ | — | ✓ | — | — |
| `SCALE_APACHE_SOFA` — Шкали APACHE II / SOFA | ✓ | — | ✓ | — | — | — | — | — |
| `SCALE_CAMICU_BRADEN_RASS` — Шкали CAM-ICU / Браден / RASS | ✓ | ✓ | ✓ | — | — | — | — | — |
| `AUDIT_ACCESS` — Журнал аудиту | — | — | — | ✓ | — | — | — | — |
| `AUDITOR_VIEW` — Read-only доступ аудитора | — | — | — | — | ✓ | — | — | — |
| `PROSTHETICS_DASHBOARD` — Дашборд протезування | — | — | — | — | — | — | ✓ | ✓ |
| `PROSTHETICS_INSTANCE_CREATE` — Створення процесу (Wizard) | — | — | — | — | — | — | ✓ | ✓ |
| `PROSTHETICS_STEP_COMPLETE` — Виконання кроків / файли | — | — | — | — | — | — | ✓ | ✓ |
| `PROSTHETICS_PAUSE_RESUME` — Пауза / відновлення | — | — | — | — | — | — | ✓ | ✓ |
| `PROSTHETICS_GATE_DECISION` — Рішення quality gate | — | — | — | — | — | — | — | ✓ |
| `PROSTHETICS_TEMPLATE_MANAGE` — Керування шаблонами | — | — | — | — | — | — | — | ✓ |
| `PROSTHETICS_ORDER_MANAGE` — Пацієнти та замовлення | — | — | — | — | — | — | — | ✓ |
| `MODULE_ICU_ACCESS` — Модуль: Карта інтенсивної терапії | ✓ | ✓ | ✓ | — | — | — | — | — |
| `MODULE_MEDICATION_ACCESS` — Модуль: Листок призначень | ✓ | ✓ | ✓ | — | — | — | — | — |
| `MODULE_PROSTHETICS_ACCESS` — Модуль: Виробництво протезів | — | — | — | — | — | — | ✓ | ✓ |
| `MODULE_ADMIN_ACCESS` — Модуль: Адміністрування | — | — | — | ✓ | ✓ | — | — | — |

> **Notes**
> - `HOD` = `HEAD_OF_DEPARTMENT`; `ADJ. SPECIALIST` = `ADJACENT_SPECIALIST`; `PROSTH. ADMIN` = `PROSTHETICS_ADMINISTRATOR`.
> - Module-visit permissions (`MODULE_*_ACCESS`) control navigation to a sub-app. Access to a module is `role (if clinical core or granted) OR permission` — revoking a module permission never locks a role out of its own module.
> - A role granted a `MODULE_*_ACCESS` for a clinical module enters via `/icu/doctor` or `/prescriptions/doctor` and can view it read-only; writes still require the specific operation permission.
> - A granted permission is **additive** to the role's default set — the role-permission matrix is `default-deny`, and presence of a grant row means the role holds that permission.

---

## Security

- **Authentication**: JWT stored in an **httpOnly cookie** named `jwt`, issued via a `Set-Cookie` header on login (and cleared on logout). The Axios client is configured with `withCredentials: true`, and the JWT filter also accepts a `Bearer` Authorization header. `localStorage` holds only a lightweight session flag (`auth:session`), never the token.
- **Password Storage**: BCrypt hashing
- **Authorization**: Spring Security **method + URL-based** enforcement, layered on a **dynamic permission matrix** — precise rules are `@PreAuthorize("@permissionService.has('CODE')")` (so admin edits to role permissions take effect immediately), module-visit gates via `MODULE_*_ACCESS`, and URL ceilings in `ClinicalSecurityRules`
- **CORS**: REST API allows `*` origin patterns (with credentials); the **WebSocket** endpoint (`WebSocketConfig`) is restricted to `http://localhost:5173` and `http://localhost:3000`
- **CSRF**: Disabled (stateless API)
- **Audit**: `AuditService` logs all entity operations (plus non-GET API calls through the `JwtAuthenticationFilter`)
- **Optimistic Locking**: `@Version` field on all `BaseEntity` subclasses prevents concurrent overwrites

---

## License

MIT License. See [LICENSE](LICENSE).
