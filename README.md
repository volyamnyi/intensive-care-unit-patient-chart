# Карта інтенсивної терапії (ICU Patient Chart)

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk)](https://jdk.java.net/17/)
[![React](https://img.shields.io/badge/React-19-61DAFB?logo=react)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-6.0-3178C6?logo=typescript)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-8.1-646CFF?logo=vite)](https://vite.dev/)
[![MUI](https://img.shields.io/badge/MUI-9.2-007FFF?logo=mui)](https://mui.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> A full-stack electronic medical record system for Intensive Care Units (ICU). Enables doctors and nurses to digitally manage patient ICU charts — including hourly vital sign tracking, fluid balance monitoring, prescription management, clinical scale assessments, PDF generation, and automated day transitions.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture Overview](#architecture-overview)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
  - [1. Clone the Repository](#1-clone-the-repository)
  - [2. Database Setup](#2-database-setup)
  - [3. Backend Setup](#3-backend-setup)
  - [4. Frontend Setup](#4-frontend-setup)
- [Configuration](#configuration)
  - [Backend Configuration](#backend-configuration)
  - [Frontend Configuration](#frontend-configuration)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Seed Data](#seed-data)
- [Project Structure](#project-structure)
- [Development](#development)
- [Deployment](#deployment)
- [Security](#security)
- [Roadmap](#roadmap)
- [License](#license)

---

## Features

### For Doctors
- **Patient Dashboard** — View all active ICU patients with key metrics (APACHE II, SOFA scores)
- **ICU Card Creation** — Search patients from the hospital information system (MIS) and create new ICU cards with diagnosis, APACHE II, and SOFA scores
- **Hourly Vital Signs** — Review complete 24-hour vital sign tables (systolic/diastolic BP, heart rate, SpO₂, temperature, CVP, respiratory rate) with color-coded completion status
- **Prescription Management** — Create, view, and stop medication prescriptions with dose, route, and scheduling
- **Clinical Scale Assessments** — View APACHE II, SOFA, RASS, CAM-ICU, and Braden scale evaluations
- **Day Sign-Off** — Sign off completed ICU days, triggering automatic PDF generation and MIS submission
- **PDF Export** — Generate professional PDF documents for each signed ICU day

### For Nurses
- **Patient Selection** — Switch between active patients via dropdown
- **Hourly Vital Sign Entry** — Enter and save vital signs for each hour of the current ICU day
- **Hour Strip Navigation** — Visual 24-hour strip with color-coded completed, missed, and current hours
- **Fluid Output Tracking** — Record urine, tube drainage, and stool output
- **Prescription Execution** — One-click administration of active medication prescriptions
- **Fluid Balance Panel** — Real-time view of total intake, output, daily balance, and cumulative balance

### Automated
- **Daily Day Transitions** — Automatic closing of previous ICU day and creation of a new day at 07:00
- **Escalation Notifications** — Automatic email alerts to the Head of Department if a day remains unsigned after 09:00
- **Audit Logging** — All actions are logged for compliance and traceability

---

## Tech Stack

### Backend
| Technology | Version | Purpose |
|---|---|---|
| Java | 17 | Runtime |
| Spring Boot | 3.2.5 | Application framework |
| Spring Data JPA | — | ORM / database access |
| Spring Security | — | JWT-based authentication |
| Spring WebSocket | — | STOMP real-time updates |
| Spring Mail | — | Email notifications |
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
| MUI (Material UI) | 9.2 | Component library |
| MUI Icons | 9.2 | Icon set |
| Axios | 1.18 | HTTP client |
| React Router DOM | 7.18 | Client-side routing |
| Emotion | 11.14 | CSS-in-JS styling |
| Day.js | 1.11 | Date manipulation |
| Rubik + Mulish | — | Typography |
| Oxlint | 1.71 | Linter |

---

## Architecture Overview

```
┌──────────────┐     HTTP/JSON      ┌──────────────┐     JDBC      ┌────────────┐
│   Frontend   │ ◄──────────────────► │   Backend    │ ◄────────────► │ PostgreSQL │
│  (React 19)  │   localhost:5173     │ (Spring Boot)│  localhost:5432│            │
│              │     ┌─────────┐     │ localhost:8080│               │            │
│  Vite Dev    │     │  Axios  │     │              │               │            │
│  Server      │     │ JWT Auth│     │  JWT Filter  │               │            │
└──────────────┘     └─────────┘     └──────┬───────┘               └────────────┘
                                            │
                                    ┌───────┴───────┐
                                    │  MockMISService│
                                    │  (or real MIS) │
                                    └───────────────┘
```

- The frontend communicates with the backend via RESTful JSON APIs over HTTP
- Authentication is stateless using JWT tokens stored in `localStorage`
- The backend integrates with the Hospital Information System (MIS) via a pluggable service interface (currently using a mock implementation)
- Scheduled tasks handle day transitions and escalation checks
- WebSocket support is configured for future real-time updates

---

## Prerequisites

Before you begin, ensure you have the following installed:

- **Java Development Kit (JDK)** 17 or later — [Download](https://adoptium.net/)
- **Node.js** 20 or later — [Download](https://nodejs.org/)
- **npm** 10 or later (ships with Node.js)
- **PostgreSQL** 16 or later — [Download](https://www.postgresql.org/download/)
- **Maven** 3.9 or later — [Download](https://maven.apache.org/download.cgi) (or use the included `mvnw` wrapper)

---

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/your-organization/icu-patient-chart.git
cd icu-patient-chart
```

### 2. Database Setup

```bash
# Connect to PostgreSQL
psql -U postgres

# Create the database
CREATE DATABASE my_fullstack_db;

# Verify
\l    # list databases
\q    # quit
```

The schema is validated on application startup (`ddl-auto: validate`). If you are running for the first time, either:
- Change `ddl-auto` to `update` in `backend/src/main/resources/application.yml` temporarily, or
- Execute the schema manually using a tool like pgAdmin or DBeaver against your entity model

### 3. Backend Setup

```bash
cd backend

# Build the project
mvn clean package -DskipTests

# The JAR will be in: target/patient-chart-backend-1.0.0.jar
```

### 4. Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Verify TypeScript compilation
npx tsc --noEmit
```

---

## Configuration

### Backend Configuration

All backend settings are in `backend/src/main/resources/application.yml`:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/my_fullstack_db
    username: postgres
    password: admin

app:
  jwt:
    secret: <base64-encoded-secret>
    expiration-ms: 86400000       # 24 hours
  mis:
    mock-enabled: true            # Set false in production with real MIS
  scheduling:
    day-close-hour: 7             # 07:00 daily auto-close
    escalation-hour: 9            # 09:00 escalation email check
```

| Property | Description | Default |
|---|---|---|
| `server.port` | Backend server port | `8080` |
| `spring.datasource.url` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/my_fullstack_db` |
| `app.jwt.secret` | Base64-encoded JWT signing secret | (see file) |
| `app.jwt.expiration-ms` | JWT token validity period | `86400000` (24h) |
| `app.mis.mock-enabled` | Use mock MIS (no real integration) | `true` |
| `app.scheduling.day-close-hour` | Hour for automatic day close | `7` |
| `app.scheduling.escalation-hour` | Hour for escalation email check | `9` |

> **⚠️ Production**: Replace the JWT secret with a strong randomly generated value. Disable mock mode and implement a real `MISService`. Configure SMTP settings for email notifications.

### Frontend Configuration

The API base URL is defined in `frontend/src/api/client.ts`:

```typescript
const API_BASE = 'http://localhost:8080/api';
```

For production deployment, update this to your backend URL or configure a reverse proxy.

---

## Running the Application

### Development Mode

**Terminal 1 — Backend:**

```bash
cd backend
mvn spring-boot:run
```

The backend starts on `http://localhost:8080`.

**Terminal 2 — Frontend:**

```bash
cd frontend
npm run dev
```

The frontend starts on `http://localhost:5173`.

Open your browser and navigate to **http://localhost:5173**.

### Production Mode

```bash
# Build backend
cd backend && mvn clean package -DskipTests

# Build frontend
cd ../frontend && npm run build

# Serve frontend build via backend or a web server
# The frontend build output is in frontend/dist/

# Run backend JAR
java -jar backend/target/patient-chart-backend-1.0.0.jar
```

For production, it is recommended to serve the frontend build through a reverse proxy (e.g., Nginx) that forwards `/api/*` requests to the backend.

---

## API Documentation

### Authentication

All API endpoints except `/api/auth/login` require a JWT token in the `Authorization` header:

```
Authorization: Bearer <token>
```

### Endpoints

| Method | URL | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/login` | No | Login, returns JWT token |
| `GET` | `/api/patients/search?query=` | Yes | Search patients in MIS |
| `GET` | `/api/patients/{id}` | Yes | Get patient by MIS ID |
| `POST` | `/api/icu-cards` | Yes | Create new ICU card |
| `GET` | `/api/icu-cards/{id}` | Yes | Get ICU card by ID |
| `GET` | `/api/icu-cards/active` | Yes | List all active ICU cards |
| `GET` | `/api/icu-cards/by-patient/{patientId}` | Yes | Get cards for a patient |
| `GET` | `/api/icu-days/by-card/{cardId}` | Yes | List all days for a card |
| `GET` | `/api/icu-days/{id}` | Yes | Get ICU day by ID |
| `PUT` | `/api/icu-days/{dayId}/vitals/{hour}` | Yes | Save hourly vitals |
| `GET` | `/api/icu-days/{dayId}/vitals` | Yes | Get all vitals for a day |
| `POST` | `/api/icu-days/{dayId}/intake/{hour}` | Yes | Record fluid intake |
| `POST` | `/api/icu-days/{dayId}/output/{hour}` | Yes | Record fluid output |
| `GET` | `/api/icu-days/{dayId}/balance` | Yes | Get fluid balance |
| `POST` | `/api/icu-days/{dayId}/scales` | Yes | Save scale assessment |
| `GET` | `/api/icu-days/{dayId}/scales` | Yes | Get scale assessments |
| `POST` | `/api/icu-days/{dayId}/sign-off` | Doctor | Sign off an ICU day |
| `GET` | `/api/icu-days/{dayId}/pdf` | Yes | Download PDF summary |
| `POST` | `/api/prescriptions/by-card/{cardId}` | Yes | Create prescription |
| `GET` | `/api/prescriptions/by-card/{cardId}` | Yes | List prescriptions |
| `POST` | `/api/prescriptions/{id}/stop` | Yes | Stop a prescription |
| `POST` | `/api/prescriptions/{id}/execute` | Yes | Execute prescription |
| `GET` | `/api/users/me` | Yes | Get current user |
| `GET` | `/api/users/doctors` | Yes | List doctors |
| `GET` | `/api/users/nurses` | Yes | List nurses |

---

## Seed Data

The application seeds 6 users on startup via `backend/src/main/resources/data.sql`:

| Login | Password | Role | Full Name |
|---|---|---|---|
| `doctor1` | `doctor123` | DOCTOR | Олександр Мельник |
| `doctor2` | `doctor123` | DOCTOR | Наталія Бойко |
| `nurse1` | `nurse123` | NURSE | Олена Ткаченко |
| `nurse2` | `nurse123` | NURSE | Марія Кравчук |
| `head1` | `head123` | HEAD_OF_DEPARTMENT | Василь Гончарук |
| `admin` | `admin123` | ADMINISTRATOR | Адмін Системи |

The mock MIS service provides three test patients for searching and card creation:

| Patient Name | Medical Card # | Year of Birth |
|---|---|---|
| Петренко Іван Сергійович | МК-001234 | 1978 |
| Коваленко Олена Вікторівна | МК-005678 | 1985 |
| Сидоренко Василь Петрович | МК-009012 | 1962 |

---

## Project Structure

```
icu-patient-chart/
├── backend/
│   ├── pom.xml
│   └── src/main/
│   ├── java/com/superhumans/
│   │   ├── SuperhumansApplication.java
│       │   ├── auth/             # JWT authentication
│       │   ├── config/           # Security, CORS, WebSocket config
│       │   ├── controller/       # REST controllers (6)
│       │   ├── dto/              # Request/response DTOs
│       │   ├── entity/           # JPA entities (12)
│       │   ├── mis/              # MIS integration (mock + interface)
│       │   ├── repository/       # Spring Data JPA repositories (10)
│       │   └── service/          # Business logic services (10)
│       └── resources/
│           ├── application.yml
│           └── data.sql          # Seed data
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig*.json
│   └── src/
│       ├── App.tsx               # Root component / routing
│       ├── api/                  # Axios client & endpoint definitions
│       ├── styles/               # MUI theme & CSS animations
│       ├── services/             # Auth context
│       ├── layouts/              # Doctor & Nurse layouts
│       ├── pages/                # Page components
│       │   ├── LoginPage.tsx
│       │   ├── doctor/           # Dashboard, CreateCard, PatientDay
│       │   └── nurse/            # NurseDashboard
│       ├── components/           # Shared component stubs
│       └── types/                # TypeScript interfaces
├── docs/                         # Project documentation (Ukrainian)
└── README.md
```

---

## Development

### Code Quality

```bash
# Frontend linting
cd frontend && npm run lint

# Frontend type checking
cd frontend && npx tsc --noEmit

# Backend compilation
cd backend && mvn compile
```

### Commit Conventions

This project follows [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add new feature
fix: correct a bug
refactor: restructure code without changing behavior
docs: update documentation
chore: maintenance tasks
```

### Available Scripts

#### Frontend

| Command | Description |
|---|---|
| `npm run dev` | Start Vite development server on `:5173` |
| `npm run build` | Type-check and build for production |
| `npm run preview` | Preview production build locally |
| `npm run lint` | Run Oxlint static analysis |

#### Backend

| Command | Description |
|---|---|
| `mvn spring-boot:run` | Run backend in development mode |
| `mvn clean package` | Build production JAR |
| `mvn compile` | Compile without running tests |
| `mvn test` | Run tests |

---

## Deployment

### Traditional Deployment

1. **Build the frontend** — `npm run build` produces static files in `frontend/dist/`
2. **Build the backend** — `mvn clean package -DskipTests` produces a JAR
3. **Serve the frontend** via Nginx, Apache, or the backend itself
4. **Run the backend** — `java -jar patient-chart-backend-1.0.0.jar`
5. **Configure a reverse proxy** to route `/api/*` to the backend

### Docker Deployment (Recommended)

*Docker support coming soon — a `Dockerfile` and `docker-compose.yml` are planned.*

Until then, a manual setup guide:

```bash
# Build
cd frontend && npm run build
cd ../backend && mvn clean package -DskipTests

# Run with PostgreSQL
java -jar backend/target/patient-chart-backend-1.0.0.jar
```

### Environment Variables (Production)

> **Note**: Currently, configuration is file-based. For production, consider externalizing:
> - `SPRING_DATASOURCE_URL`
> - `SPRING_DATASOURCE_USERNAME`
> - `SPRING_DATASOURCE_PASSWORD`
> - `APP_JWT_SECRET`
> - `APP_MIS_MOCK_ENABLED`

---

## Security

- **Authentication**: JWT-based, stateless authentication
- **Password Storage**: BCrypt hashing
- **Authorization**: Role-based access control (DOCTOR, NURSE, HEAD_OF_DEPARTMENT, ADMINISTRATOR)
- **CORS**: Restricted to `http://localhost:5173` and `http://localhost:3000` in development
- **CSRF**: Disabled (stateless API)
- **Audit Trail**: All operations are logged via `AuditService`

### Role Permissions

| Operation | DOCTOR | NURSE | HOD | ADMIN |
|---|---|---|---|---|
| Create ICU card | ✓ | ✗ | ✓ | ✗ |
| Sign off day | ✓ | ✗ | ✓ | ✗ |
| Create prescriptions | ✓ | ✗ | ✓ | ✗ |
| Enter vitals | ✗ | ✓ | ✗ | ✗ |
| Execute prescriptions | ✗ | ✓ | ✗ | ✗ |
| View patient data | ✓ | ✓ | ✓ | ✓ |

---

## Roadmap

- [x] Core ICU chart management (CRUD)
- [x] Hourly vital sign tracking
- [x] Prescription management & execution
- [x] Fluid balance calculations
- [x] Clinical scale assessments
- [x] JWT authentication & RBAC
- [x] PDF generation (iText 7)
- [x] Automated day transitions & escalation
- [x] MIS integration (mock)
- [ ] Real-time updates via WebSocket
- [ ] Docker containerization & docker-compose
- [ ] Internationalization (i18n) support
- [ ] Dark mode theme
- [ ] Reporting & analytics dashboard
- [ ] Integration with real HIS/MIS systems
- [ ] Mobile-responsive layout

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

## Support

For questions, feature requests, or bug reports, please open an issue on the [GitHub repository](https://github.com/your-organization/icu-patient-chart/issues).
