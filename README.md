# Карта інтенсивної терапії (ICU Patient Chart)

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk)](https://jdk.java.net/17/)
[![React](https://img.shields.io/badge/React-19-61DAFB?logo=react)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-6.0-3178C6?logo=typescript)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-8.1-646CFF?logo=vite)](https://vite.dev/)
[![MUI](https://img.shields.io/badge/MUI-9.2-007FFF?logo=mui)](https://mui.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> A full-stack electronic medical record system for Intensive Care Units (ICU). Implements the Ukrainian standard form **003-15/о «Карта інтенсивної терапії»**. Enables doctors and nurses to digitally manage patient ICU charts — including hourly vital sign tracking, fluid balance monitoring, prescription management, clinical scale assessments, PDF generation (A4 landscape, Times New Roman), and digital signing workflows.

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

### Automated
- **Audit Logging** — All entity operations are logged with user, timestamp, and diff
- **Optimistic Locking** — JPA `@Version` prevents concurrent edit conflicts

---

## Tech Stack

### Backend
| Technology | Version | Purpose |
|---|---|---|
| Java | 17 | Runtime |
| Spring Boot | 3.2.5 | Application framework |
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

### E2E Testing
| Technology | Version | Purpose |
|---|---|---|
| Playwright | 1.61 | Browser automation |
| Allure | 3.2 | CI test reporting |

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
      ddl-auto: update       # Auto-creates schema from entities

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
| `GET` | `/api/clinical-days/{id}/scales` | Yes | Get scale results |
| `POST` | `/api/clinical-days/{id}/scales` | Yes | Create scale result |
| `PATCH` | `/api/scales/{id}` | Yes | Update scale result |

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

### Prescriptions (Листок лікарських призначень)
| Method | URL | Auth | Description |
|---|---|---|---|
| `GET` | `/api/prescriptions?patientId=` | Yes | List prescriptions for patient |
| `GET` | `/api/prescriptions/{id}` | Yes | Get prescription by ID |
| `POST` | `/api/prescriptions` | Doctor/HOD | Create prescription list |
| `DELETE` | `/api/prescriptions/{id}` | Doctor/HOD | Delete prescription |
| `POST` | `/api/prescriptions/{id}/close` | Doctor/HOD | Close prescription |
| `GET` | `/api/prescriptions/{listId}/items` | Yes | List prescription items |
| `POST` | `/api/prescriptions/{listId}/items` | Doctor/HOD | Add medicine item |
| `DELETE` | `/api/prescriptions/items/{itemId}` | Doctor/HOD | Remove item |
| `PUT` | `/api/prescriptions/day-parts/{id}/plan` | Doctor/HOD | Plan dose for day part |
| `PUT` | `/api/prescriptions/day-parts/{id}/complete` | Nurse/HOD | Complete day part |
| `POST` | `/api/prescriptions/day-parts/{id}/execute` | Nurse/HOD | Execute dose |
| `GET` | `/api/prescriptions/allergies?patientId=` | Yes | Patient allergies (from MIS) |
| `GET` | `/api/prescriptions/medicine-catalog?keyword=` | Yes | Medicine catalog search |

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
| *(backend-only)* | — | AUDITOR |

5 mock patients (from MIS mock):

| Full Name | Card # | Year |
|---|---|---|
| Петренко Іван Сергійович | МК-001234 | 1978 |
| Коваленко Олена Вікторівна | МК-005678 | 1985 |
| Сидоренко Василь Петрович | МК-009012 | 1962 |
| Бондаренко Наталія Петрівна | МК-003456 | 1990 |
| Ткачук Андрій Миколайович | МК-007890 | 1975 |

3 seed episodes with 4 seed clinical days (3 OPEN, 1 NURSE_SIGNED) with fixed UUIDs (used in integration tests).

---

## Project Structure

```
icu-patient-chart/
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/superhumans/
│       ├── SuperhumansApplication.java
│       ├── auth/             # JWT authentication (filter + token provider)
│       ├── config/           # Security, CORS
│       ├── controller/       # REST controllers (18)
│       ├── dto/              # Request/response DTOs (48 total)
│       ├── entity/           # JPA entities (25 including enums)
│       ├── exception/        # Domain exceptions + global handler
│       ├── mapper/           # Entity ↔ DTO mappers
│       ├── mis/              # MIS integration (mock + interface)
│       ├── repository/       # Spring Data repositories (18)
│       └── service/          # Business logic services (17 implementation + 2 interfaces)
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig*.json
│   └── src/
│       ├── App.tsx           # Root component + routing
│       ├── api/              # Axios client + endpoint definitions
│       ├── components/       # Shared UI components (12)
│       ├── layouts/          # Doctor & Nurse layouts
│       ├── pages/            # LoginPage, doctor/, nurse/, admin/
│       ├── services/         # AuthContext
│       ├── styles/           # MUI theme + animations
│       └── types/            # TypeScript interfaces
├── tests/                    # Playwright E2E tests
│   ├── playwright.config.ts
│   ├── pages/                # Page objects (7)
│   ├── fixtures/             # Role-based test fixtures
│   └── specs/                # Test specs (28 files, 79 tests)
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
| `mvn test` | Run unit tests (151) |
| `mvn test -Pintegration-test` | Run integration tests (79) — requires Docker |

#### Frontend
| Command | Action |
|---|---|
| `npm run dev` | Vite dev server on `:5173` |
| `npm run build` | `tsc -b && vite build` |
| `npm run lint` | Oxlint |
| `npx tsc --noEmit` | Type-check without build |
| `npm t` | Run Vitest tests (~190 across 22 files) |

#### E2E Tests (`cd tests`)
| Command | Action |
|---|---|
| `npx playwright test` | Run all E2E tests (38 spec files) |
| `npx playwright test --project=doctor-chromium --project=hod-chromium --workers=1` | Run only doctor + HOD tests |
| `npx playwright test --ui` | Run with Playwright UI mode |
| `npx playwright test --list` | List tests |
| `npx playwright show-report` | View HTML report |

### Main Test Scenario

**All tests run exclusively via GitHub Actions CI — never locally.**

| Test type | CI job | Trigger |
|---|---|---|
| Backend unit (151) | `test` → `mvn clean verify` | Push to `main` / `develop` or PR to `main` |
| Backend integration (79) | `integration-tests` → `mvn test -Pintegration-test` | Same |
| Frontend Vitest (~190) | `test` → `npm test` | Same |
| Playwright E2E (38 spec files) | `test` → `npx playwright test` | Same |
| Format / Checkstyle | `format-check` → `mvn compile checkstyle:check` | Same |

Push → CI runs all 3 jobs in parallel → if any fails, fix and repeat until green.

### Testing Summary
- **Backend unit tests**: 319 tests (22 classes) — `mvn test`
- **Backend integration tests**: 79 tests via Testcontainers (13 classes) — `mvn test -Pintegration-test`
- **Frontend Vitest tests**: 316 tests (38 files)
- **E2E Playwright tests**: 40 spec files, 7 projects
- **Total**: 754+ tests
- **CI**: GitHub Actions — PostgreSQL service, JDK 17, Node 22, Playwright chromium, 40min timeout

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

| Operation | DOCTOR | NURSE | HOD | ADMIN |
|---|---|---|---|---|
| Create episode | ✓ | ✗ | ✓ | ✗ |
| Create clinical day | ✓ | ✗ | ✓ | ✗ |
| Sign off (nurse stage) | ✗ | ✓ | ✗ | ✗ |
| Sign off (doctor stage) | ✓ | ✗ | ✓ | ✗ |
| Reopen signed day | ✗ | ✗ | ✓ | ✗ |
| Create prescriptions | ✓ | ✗ | ✓ | ✗ |
| Execute prescriptions | ✗ | ✓ | ✗ | ✗ |
| Enter vitals | ✗ | ✓ | ✗ | ✗ |
| View patient data | ✓ | ✓ | ✓ | ✓ |
| Audit log access | ✗ | ✗ | ✗ | ✓ |
| AUDITOR read-only view | ✗ | ✗ | ✗ | ✗ |

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
