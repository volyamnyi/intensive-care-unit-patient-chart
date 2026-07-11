# ICU Patient Chart — E2E Test Plan & Framework

This document describes the Playwright-based functional test framework for the
ICU Patient Chart application (`https://github.com/volyamnyi/intensive-care-unit-patient-chart`).

## 1. Objectives

- Validate **every page and feature** of the SPA across **all four user roles**
  (DOCTOR, NURSE, HEAD_OF_DEPARTMENT, ADMINISTRATOR).
- Validate the **REST API contract** and **role-based access control (RBAC)**.
- Drive the **full clinical workflow** end-to-end (create card → enter vitals →
  prescribe → execute → sign off → PDF).
- Surface, report, and fix defects; iterate until the suite is green.

## 2. Architecture

```
tests/
├── package.json            # ESM project ("type": "module")
├── playwright.config.js    # runner config (1 worker, 30s timeout, html+json reports)
├── fixtures/
│   ├── env.js              # base URLs, seed users, MIS patients, raw API helpers
│   └── auth.js             # loginViaApi, gotoAsRole, createTestCard helpers
├── specs/
│   ├── api.spec.js         # API contract + RBAC matrix (41 tests)
│   ├── auth.spec.js        # login form, redirects, logout (8 tests)
│   ├── doctor.spec.js      # dashboard, create card, patient day, sign-off (8 tests)
│   ├── nurse.spec.js       # vitals entry, prescription execution, balance (5 tests)
│   ├── hod.spec.js         # HOD on doctor routes, sign-off, prescriptions (5 tests)
│   ├── admin.spec.js       # admin panel, user listing, guards (5 tests)
│   └── roles.spec.js       # role → route mapping matrix (8 tests)
├── TEST_PLAN.md
├── BUG_REPORTS.md
└── test-results/          # generated reports / traces on failure
```

### Conventions
- API-level tests use Playwright's `request` fixture and talk directly to
  `http://localhost:8085/api`.
- UI tests authenticate by injecting a JWT into `localStorage` (via the API login)
  and reloading, which is far faster and more stable than driving the login form.
  The login *form itself* is still covered by `auth.spec.js`.
- Mutating tests create their **own** ICU card (and day / prescription) so runs are
  repeatable and do not depend on shared seed state.

## 3. Environment

| Component | URL | Notes |
|---|---|---|
| Frontend (Vite) | `http://localhost:5173` | `frontend/` – `npm run dev` |
| Backend (Spring Boot) | `http://localhost:8085/api` | `backend/` – `mvn spring-boot:run` |
| Database | `postgres://localhost:5432/my_fullstack_db` | `ddl-auto: validate` (schema pre-created) |

Seed users: `doctor1`/`doctor2` (`doctor123`), `nurse1`/`nurse2` (`nurse123`),
`head1` (`head123`), `admin` (`admin123`).

## 4. Coverage Matrix

### 4.1 Authentication & Authorization
| # | Area | Covered by |
|---|---|---|
| AUTH-01..06 | Login form, valid/invalid creds, role redirect | `auth.spec.js` |
| AUTH-07..08 | Unauthenticated guard, logout clears token | `auth.spec.js` |
| API-01..08 | Login contract, 401 on bad creds, 403 on missing/invalid token | `api.spec.js` |
| ROLE-01..08 | Role→route mapping for every seed account | `roles.spec.js` |

### 4.2 Doctor
| # | Area | Covered by |
|---|---|---|
| DOC-01..02 | Dashboard heading + table + search filter | `doctor.spec.js` |
| DOC-03..04 | Navigate to create-card; full create flow via MIS search | `doctor.spec.js` |
| DOC-05 | Open existing patient day (vitals/scales/prescriptions tabs) | `doctor.spec.js` |
| DOC-06 | Sign off an active day via UI → SIGNED | `doctor.spec.js` |
| DOC-07 | Doctor blocked from `/nurse` | `doctor.spec.js` |
| DOC-08 | Create prescription from patient day | `doctor.spec.js` |

### 4.3 Nurse
| # | Area | Covered by |
|---|---|---|
| NURSE-01 | Nurse layout/header + patient selector | `nurse.spec.js` |
| NURSE-02 | Save hourly vitals → persisted (verified via API) | `nurse.spec.js` |
| NURSE-03 | Execute a prescription → fluid intake recorded | `nurse.spec.js` |
| NURSE-04 | Fluid balance panel visible | `nurse.spec.js` |
| NURSE-05 | Nurse blocked from `/doctor` | `nurse.spec.js` |

### 4.4 Head of Department
| # | Area | Covered by |
|---|---|---|
| HOD-01..05 | HOD on doctor routes, create card, sign-off, prescribe, blocked from `/nurse` | `hod.spec.js` |

### 4.5 Administrator
| # | Area | Covered by |
|---|---|---|
| ADM-01..05 | Admin panel, doctor/nurse listing, guards, logout | `admin.spec.js` |

### 4.6 API / RBAC matrix
| Endpoint | DOCTOR | NURSE | HOD | ADMIN | Covered |
|---|---|---|---|---|---|
| `POST /api/auth/login` | ✓ | ✓ | ✓ | ✓ | API-01/02 |
| `POST /api/auth/login` (bad) | 401 | 401 | 401 | 401 | API-03/04/05 |
| `GET /api/patients/search` | ✓ | ✓ | ✓ | ✓ | API-09..13 |
| `GET /api/patients/{id}` | 200 | 200 | 200 | 200 | API-14/15 |
| `POST /api/icu-cards` | ✓ | 403 | ✓ | 403 | API-16..19 |
| `GET /api/icu-cards/**` | ✓ | ✓ | ✓ | ✓ | API-20/21 |
| `GET /api/icu-days/**` | ✓ | ✓ | ✓ | ✓ | API-22 |
| `PUT /api/icu-days/{id}/vitals` | 200 | 200 | 200 | 200 | API-23/24 |
| `POST /api/icu-days/{id}/scales` | 200 | 200 | 200 | 200 | API-25 |
| `GET /api/icu-days/{id}/balance` | 200 | 200 | 200 | 200 | API-26 |
| `POST /api/icu-days/*/sign-off` | ✓ | **403** | ✓ | **403** | API-27..31 |
| `POST /api/prescriptions/by-card` | ✓ | **403** | ✓ | **403** | API-32..34 |
| `GET /api/prescriptions/**` | ✓ | ✓ | ✓ | ✓ | API-35 |
| `POST /api/prescriptions/*/execute` | ✓ | ✓ | ✓ | (n/a) | API-36/37 |
| `POST /api/prescriptions/*/stop` | ✓ | (n/a) | ✓ | (n/a) | API-38 |
| `GET /api/users/**` | ✓ | ✓ | ✓ | ✓ | API-39..41 |

## 5. How to run

```bash
# Terminal 1 — backend
cd backend && mvn spring-boot:run
# Terminal 2 — frontend
cd frontend && npm run dev
# Terminal 3 — tests
cd tests && npx playwright test            # all specs
npx playwright test api.spec.js --reporter=list
npx playwright show-report                 # open HTML report
```

CI: `.github/workflows/playwright.yml` spins up Postgres, builds the backend JAR,
installs Playwright Chromium, starts both servers, and runs the suite on every push/PR.

## 6. Defects

See `BUG_REPORTS.md` for the full list of issues found during the Test → Fix →
Analyse cycle and their resolutions.
