# ICU Patient Chart — Agent Guide

## Architecture

```
frontend/  (React 19 + TS 6 + Vite 8 + MUI 9)
backend/   (Spring Boot 3.2.5 + Java 17 + Maven)
```

- Monorepo with two independent packages (no workspace orchestration).
- JWT auth stored in `localStorage`. API base URL: `frontend/src/api/client.ts`.
- Backend port: **8085** (`application.yml`, not 8080 as root README claims).
- DB: PostgreSQL 16, `ddl-auto: update` — schema auto-created by Hibernate.
- Seed data: `backend/src/main/resources/data.sql` (6 users, 3 ICU cards + active days, `spring.sql.init.mode: always`).
- CI: `.github/workflows/playwright.yml` — Postgres service, JDK 17, Node 22, Playwright with chromium, 40min timeout.

## Commands

### Frontend (`cd frontend`)
| Command | Action |
|---|---|
| `npm run dev` | Vite dev server on `:5173` |
| `npm run build` | `tsc -b && vite build` (type-check then bundle) |
| `npm run lint` | Oxlint (not ESLint) |
| `npx tsc --noEmit` | Type-check without build |

### Backend (`cd backend`)
| Command | Action |
|---|---|
| `mvn spring-boot:run` | Dev server on `:8085` |
| `mvn clean package -DskipTests` | Build JAR |
| `mvn compile` | Compile only |
| `mvn test` | Run tests |

### Playwright (`cd tests`)
| Command | Action |
|---|---|
| `npx playwright test` | Run all E2E tests |
| `npx playwright test --list` | List tests without running |
| `npx playwright show-report` | View HTML report |

## Testing

- **Backend**: 150 tests (21 files — 12 service, 6 controller, 3 repository @DataJpaTest, 3 integration @SpringBootTest, 1 security, 2 auth). Run with `mvn test`.
- **Frontend**: 23 Vitest tests (3 files — LoginPage, endpoints, AuthContext). Run with `npm t` or `npx vitest run`.
- **E2E**: 43 Playwright tests in 10 spec files across 7 projects (setup, login, doctor, nurse, hod, admin, api). Run with `npx playwright test` from `tests/`.

## Test Architecture

### Playwright Projects

| Project | Depends On | storageState | Tests |
|---|---|---|---|
| setup | — | — | Auth setup (4 roles) |
| login-chromium | — | none | Login flow (5) |
| doctor-chromium | setup | `.auth/doctor.json` | Dashboard, cards, prescriptions, notes, sign-off |
| nurse-chromium | setup | `.auth/nurse.json` | Dashboard, vitals, fluid balance |
| hod-chromium | setup | `.auth/hod.json` | Dashboard, create card, sign-off |
| admin-chromium | setup | `.auth/admin.json` | User tables |
| api-chromium | — | none | Patient search API |

### Key Patterns
- **Locators**: prefer `getByRole`, `getByLabel`, row-specific filters over `.first()` for determinism
- **Seed data**: 3 ICU cards (Петренко, Коваленко, Сидоренко) — use `filter({ hasText })` to target them
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

Mock MIS provides 3 test patients: Петренко, Коваленко, Сидоренко.

## Conventions

- **Commits**: Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`, `chore:`).
- **TypeScript**: `erasableSyntaxOnly: true` — no enums, no namespaces.
- **Roles**: DOCTOR, NURSE, HEAD_OF_DEPARTMENT, ADMINISTRATOR — gating in `backend/` (Spring Security) and `frontend/` (`Guard` component in `App.tsx`).
- **Routing**: `/doctor/*` for DOCTOR/HOD, `/nurse/*` for NURSE, `/` redirects by role.
