# ICU Patient Chart — Agent Guide

## Architecture

```
frontend/  (React 19 + TS 6 + Vite 8 + MUI 9)
backend/   (Spring Boot 3.2.5 + Java 17 + Maven)
tests/     (Playwright 1.61)
```

- Monorepo with three independent packages (no workspace orchestration).
- JWT auth stored in `localStorage`. API base URL: `frontend/src/api/client.ts` → `http://localhost:8085/api`.
- Backend port: **8085** (`application.yml`).
- DB: PostgreSQL 16, `ddl-auto: update` — schema auto-created by Hibernate.
- Seed data: `backend/src/main/resources/data.sql` (6 users, 3 episodes + 3 open clinical days, `spring.sql.init.mode: always`).
- CI: `.github/workflows/playwright.yml` — Postgres service, JDK 17, Node 22, Playwright chromium, 40min timeout.

## Commands

### Backend (`cd backend`)
| Command | Action |
|---|---|
| `mvn spring-boot:run` | Dev server on `:8085` |
| `mvn clean package -DskipTests` | Build JAR |
| `mvn compile` | Compile only |
| `mvn test` | Run 106 unit tests (excludes integration) |
| `mvn test -Pintegration-test` | Run 7 integration tests (requires Docker) |

### Frontend (`cd frontend`)
| Command | Action |
|---|---|
| `npm run dev` | Vite dev server on `:5173` |
| `npm run build` | `tsc -b && vite build` |
| `npm run lint` | Oxlint |
| `npx tsc --noEmit` | Type-check without build |
| `npm t` or `npx vitest run` | Run 35 Vitest tests |

### Playwright (`cd tests`)
| Command | Action |
|---|---|
| `npx playwright test` | Run all 37 E2E tests |
| `npx playwright test --list` | List tests without running |
| `npx playwright show-report` | View HTML report |

## Testing

- **Backend**: 106 unit tests + 7 integration tests (Testcontainers). Unit tests: `mvn test`. Integration tests: `mvn test -Pintegration-test`.
- **Frontend**: 35 Vitest tests (5 files — LoginPage, DashboardPage, NurseDashboardPage, endpoints, AuthContext). Run with `npm t`.
- **E2E**: 37 Playwright tests in 13 spec files across 7 projects (setup, login, doctor, nurse, hod, admin, api).

## Playwright Projects

| Project | Depends On | storageState | Tests |
|---|---|---|---|
| setup | — | — | Auth setup (4 roles) |
| login-chromium | — | none | Login flow (5) |
| doctor-chromium | setup | `.auth/doctor.json` | Dashboard, create card, prescriptions, notes, sign-off |
| nurse-chromium | setup | `.auth/nurse.json` | Dashboard, vitals, fluid balance |
| hod-chromium | setup | `.auth/hod.json` | Dashboard, create card, sign-off |
| admin-chromium | setup | `.auth/admin.json` | User tables |
| api-chromium | — | none | Patient search API |

## Key Patterns
- **Locators**: prefer `getByRole`, `getByLabel`, row-specific filters over `.first()` for determinism
- **Seed data**: 3 episodes — use `filter({ hasText })` with patient names (Петренко, Коваленко, Сидоренко)
- **Auth**: storageState per role, projects depend on `setup`
- **Parallelism**: `fullyParallel: true` — tests can race; use specific locators not `.first()` for shared data
- **CI retries**: 2 retries per test
- **Backend tests**: `@SpringBootTest` tests are `@Transactional` (rollback); `@DataJpaTest` uses `@AutoConfigureTestDatabase(replace = NONE)` (real PostgreSQL)

## Seed Data

| Login | Password | Role |
|---|---|---|
| `doctor1` / `doctor2` | `doctor123` | DOCTOR |
| `nurse1` / `nurse2` | `nurse123` | NURSE |
| `head1` | `head123` | HEAD_OF_DEPARTMENT |
| `admin` | `admin123` | ADMINISTRATOR |

Mock MIS provides 5 test patients: Петренко, Коваленко, Сидоренко, Бондаренко, Ткачук.

## Data Model (New Architecture)

```
Episode (1) ──── (N) ClinicalDay
                         │
               ┌────────┼────────┬────────┬────────┬────────┐
           Hourly  Medical  Medical  Scale   FluidBalance  Signature
           Record  Order    Note    Result     Item
```

- **Episode**: ICU stay (DRAFT → ACTIVE → COMPLETED/ARCHIVED)
- **ClinicalDay**: A 24h period within an episode (OPEN → NURSE_SIGNED → DOCTOR_SIGNED → CLOSED, or REOPENED)
- **HourlyRecord**: Vital signs for a specific hour
- **MedicalOrder**: Doctor's prescription/order
- **OrderExecution**: Nurse's execution of an order
- **MedicalNote**: Clinical note entry
- **ScaleResult**: Scale assessment score
- **FluidBalanceItem**: Hourly fluid balance calculation

## Conventions

- **Commits**: Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`, `chore:`).
- **TypeScript**: `erasableSyntaxOnly: true` — no enums, no namespaces.
- **Roles**: DOCTOR, NURSE, HEAD_OF_DEPARTMENT, ADMINISTRATOR — gating in `backend/` (Spring Security method security) and `frontend/` (`Guard` component in `App.tsx`).
- **Routing**: `/doctor/*` for DOCTOR/HOD, `/nurse/*` for NURSE, `/admin/*` for ADMINISTRATOR, `/` redirects by role.
- **IDs**: All UUID strings (`string` type in TS).
- **Optimistic Locking**: Every entity has `@Version version` field; conflict → `VersionConflictException` → HTTP 409.
