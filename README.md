# Карта інтенсивної терапії (ICU Patient Chart)

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk)](https://jdk.java.net/25/)
[![React](https://img.shields.io/badge/React-19-61DAFB?logo=react)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-6.0-3178C6?logo=typescript)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-8.1-646CFF?logo=vite)](https://vite.dev/)
[![MUI](https://img.shields.io/badge/MUI-9.2-007FFF?logo=mui)](https://mui.com/)
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
| Testcontainers | 1.19.8 | Integration test containers |

### Frontend
| Technology | Version | Purpose |
|---|---|---|
| React | 19.2.7 | UI framework |
| TypeScript | 6.0 | Type-safe JavaScript |
| Vite | 8.1 | Build tool / dev server |
| MUI (Material UI) | 9.2 | Component library |
| Axios | 1.18 | HTTP client |
| React Router DOM | 7.18 | Client-side routing |
| Emotion | 11.14 | CSS-in-JS styling |
| Day.js | 1.11 | Date manipulation |
| Oxlint | 1.71 | Linter |
| Vitest | 3.2 | Unit testing |

### E2E Testing (only locally)
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
                                     │  MockMisService│
                                     │  (or real MIS) │
                                     └───────────────┘
```

- Frontend communicates via RESTful JSON APIs with JWT Bearer auth
- Backend integrates with MIS via a pluggable `MisService` interface (mock implementation by default)
- Scheduled tasks handle day transitions and escalation checks
- **Prosthetics Manufacturing** is a separate backend module (`prosthesis-manufacturing`) with its own entities, services, and REST endpoints under `/api/prosthesis-manufacturing`, using local mock tables (not MIS)

---

## Prerequisites

- **JDK** 17 or later
- **Node.js** 20 or later, **npm** 10+
- **PostgreSQL** 16
- **Maven** 3.9+ (or use `mvnw`)
- **Docker** (only for integration tests via Testcontainers)

---

## Getting Started

### 1. Database

```bash
psql -U postgres -c "CREATE DATABASE my_fullstack_db;"
```

### 2. Backend

```bash
cd backend
mvn clean package -DskipTests
java -jar target/patient-chart-backend-*.jar
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

Backend settings in `backend/src/main/resources/application.yml`:

```yaml
server:
  port: 8085

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/my_fullstack_db
    username: postgres
    password: admin
  jpa:
    hibernate:
      ddl-auto: none            # schema managed by Liquibase
  liquibase:
    enabled: true
    change-log: classpath:/db/changelog/db.changelog-master.yaml

app:
  jwt:
    secret: <base64-secret>
    expiration-ms: 86400000
  mis:
    mock-enabled: true
```

Frontend API URL in `frontend/src/api/client.ts`:

```typescript
const API_BASE = 'http://localhost:8085/api';
```

---

## Running the Application

### Development

```bash
# Terminal 1 — Backend
cd backend && mvn spring-boot:run

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
java -jar backend/target/patient-chart-backend-*.jar
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

6 users seeded via `backend/src/main/resources/data.sql`:

| Login | Password | Role |
|---|---|---|
| `doctor1` / `doctor2` | `doctor123` | DOCTOR |
| `nurse1` / `nurse2` | `nurse123` | NURSE |
| `head1` | `head123` | HEAD_OF_DEPARTMENT |
| `admin` | `admin123` | ADMINISTRATOR |
| `prosthetist1` / `prosthetist2` | `doctor123` | PROSTHETIST |
| `prosthetics_admin1` | `doctor123` | PROSTHETICS_ADMINISTRATOR |
| *(backend-only)* | — | AUDITOR |

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
├── backend/
│   ├── pom.xml               ← parent POM (4 modules: common, medication-sheet, icu-chart, prosthesis-manufacturing)
│   ├── common/               ← shared entities, JWT/security, base classes
│   ├── medication-sheet/     ← prescriptions module (entities, services, controllers)
│   ├── icu-chart/
│   │   ├── pom.xml
│   │   ├── src/main/resources/
│   │   │   └── db/changelog/   # Liquibase migrations (6 changesets)
│   │   └── src/main/java/com/superhumans/
│   │       ├── SuperhumansApplication.java
│   │       ├── auth/             # JWT authentication (filter + token provider)
│   │       ├── config/           # Security, CORS
│   │       ├── controller/       # REST controllers (19)
│   │       ├── dto/              # Request/response DTOs (48 total)
│   │       ├── entity/           # JPA entities (25 including enums)
│   │       ├── exception/        # Domain exceptions + global handler
│   │       ├── mapper/           # Entity ↔ DTO mappers
│   │       ├── mis/              # MIS integration (mock + interface)
│   │       ├── repository/       # Spring Data repositories (18)
│   │       └── service/          # Business logic services (17 implementation + 2 interfaces)
│   └── prosthesis-manufacturing/
│       ├── pom.xml
│       ├── src/main/resources/
│       │   └── db/changelog/   # Liquibase migrations (1 changeset)
│       └── src/main/java/com/superhumans/prosthesismanufacturing/
│           ├── controller/       # REST controllers (5)
│           ├── dto/              # Request/response DTOs (24)
│           ├── entity/           # JPA entities (26 including enums)
│           ├── mapper/           # Entity ↔ DTO mappers (5)
│           ├── repository/       # Spring Data repositories (12)
│           └── service/          # Business logic services (7)
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig*.json
│   └── src/
│       ├── App.tsx           # Root component + routing
│       ├── api/              # Axios client + endpoint definitions (prosthetics.ts added)
│       ├── components/       # Shared UI components (12) + prosthetics/ (3)
│       ├── layouts/          # Doctor, Nurse, Prosthetics layouts
│       ├── pages/            # LoginPage, doctor/, nurse/, admin/, prosthetics/
│       ├── services/         # AuthContext
│       ├── styles/           # MUI theme + animations
│       └── types/            # TypeScript interfaces (prosthetics types added)
├── tests/                    # Playwright E2E tests
│   ├── playwright.config.ts
│   ├── pages/                # Page objects (7)
│   ├── fixtures/             # Role-based test fixtures
│   └── specs/                # Test specs (48 files, 186 tests)
└── README.md
```

---

## Development

### Commands

#### Backend
| Command | Action |
|---|---|
| `mvn spring-boot:run` | Dev server on `:8085` |
| `mvn clean package -DskipTests` | Build JAR |
| `mvn compile` | Compile only |
| `mvn test` | Run unit tests (526) |
| `mvn test -Pintegration-test` | Run integration tests (163) — requires Docker |

#### Frontend
| Command | Action |
|---|---|
| `npm run dev` | Vite dev server on `:5173` |
| `npm run build` | `tsc -b && vite build` |
| `npm run lint` | Oxlint |
| `npx tsc --noEmit` | Type-check without build |
| `npm t` | Run Vitest tests (~350 across 44 files) |

#### E2E Tests (`cd tests`)
| Command | Action |
|---|---|
| `npx playwright test` | Run all E2E tests (45 spec files) |
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
| `e2e-test` | Playwright (45 spec files; `needs: backend-test, frontend-test`) | Same |
| `build` | JAR + frontend dist artifacts | Main push only; needs all 5 jobs |

Push → CI runs jobs in parallel → if any fails, fix and repeat until every check passes.

### Testing Summary
- **Backend unit tests**: 557 tests — common (22 skippable) + medication-sheet (88) + icu-chart (416) + prosthesis-manufacturing (31) — `mvn test`
- **Backend integration tests**: 163 tests — medication-sheet (22) + icu-chart (141) — `mvn test -Pintegration-test`
- **Frontend Vitest tests**: 419 tests (47 files, 0 failures) — includes prosthetics tests
- **E2E Playwright tests**: 48 spec files, 9 projects (setup, login, doctor, nurse, hod, admin, prosthetist, prosthetadmin, api)
- **Total**: ~1,187 tests
- **CI**: GitHub Actions — PostgreSQL service, JDK 17, Node 22, Playwright chromium, 40min timeout

### Resolved Issues (from exploratory testing — #71-#74)
- [#71](https://github.com/volyamnyi/intensive-care-unit-patient-chart/issues/71) **RESOLVED** — Cyrillic encoding: `data.sql` now uses explicit `ON CONFLICT` auto-heal clause
- [#72](https://github.com/volyamnyi/intensive-care-unit-patient-chart/issues/72) **RESOLVED** — MockMIS patient name prefix cleaned
- [#73](https://github.com/volyamnyi/intensive-care-unit-patient-chart/issues/73) **RESOLVED** — Nurse detail view fixed
- [#74](https://github.com/volyamnyi/intensive-care-unit-patient-chart/issues/74) **RESOLVED** — Ghost button removed

> **Note:** All E2E tests require a fresh PostgreSQL database between full runs because seed `data.sql` uses `ON CONFLICT (id) DO NOTHING`. CI always starts with a clean DB. For local development, run `DROP SCHEMA public CASCADE; CREATE SCHEMA public;` before each test run.

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

| Operation | DOCTOR | NURSE | HOD | ADMIN | PROSTHETIST | PROSTHETICS_ADMIN |
|---|---|---|---|---|---|---|
| Create episode | ✓ | ✗ | ✓ | ✗ | ✗ | ✗ |
| Create clinical day | ✓ | ✗ | ✓ | ✗ | ✗ | ✗ |
| Sign off (nurse stage) | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ |
| Sign off (doctor stage) | ✓ | ✗ | ✓ | ✗ | ✗ | ✗ |
| Reopen signed day | ✗ | ✗ | ✓ | ✗ | ✗ | ✗ |
| Create prescriptions | ✓ | ✗ | ✓ | ✗ | ✗ | ✗ |
| Execute prescriptions | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ |
| Enter vitals | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ |
| View patient data | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ |
| Create clinical scale (APACHE II/SOFA) | ✓ | ✗ | ✓ | ✗ | ✗ | ✗ |
| Create clinical scale (CAM-ICU/Braden/RASS) | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ |
| Audit log access | ✗ | ✗ | ✗ | ✓ | ✗ | ✗ |
| AUDITOR read-only view | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |
| Prosthetics Dashboard (own instances) | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ |
| Create prosthetics instance (Wizard) | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ |
| Complete wizard steps / upload files | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ |
| Pause/Resume instance | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ |
| Quality Gate decision (PASS/REWORK/FAIL) | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ |
| Create prosthetics templates | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ |
| Create prosthetics patients/orders | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ |

---

## Security

- **Authentication**: JWT Bearer tokens stored in `localStorage`
- **Password Storage**: BCrypt hashing
- **Authorization**: Spring Security method + URL-based RBAC
- **CORS**: Restricted to `http://localhost:5173` and `http://localhost:3000`
- **CSRF**: Disabled (stateless API)
- **Audit**: `AuditService` logs all entity operations
- **Optimistic Locking**: `@Version` field on all entities prevents concurrent overwrites

---

## License

MIT License. See [LICENSE](LICENSE).
