# ICU Patient Chart — Agent Guide

## Architecture

```
frontend/  (React 19 + TS 6 + Vite 8 + MUI 9)
backend/   (Spring Boot 3.2.5 + Java 17 + Maven)
```

- Monorepo with two independent packages (no workspace orchestration).
- JWT auth stored in `localStorage`. API base URL: `frontend/src/api/client.ts`.
- Backend port: **8085** (`application.yml`, not 8080 as root README claims).
- DB: PostgreSQL 16, `ddl-auto: validate` — schema must be pre-created.
- Seed data: `backend/src/main/resources/data.sql` (6 users, `spring.sql.init.mode: always`).

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

## Testing

- **Backend**: Test deps configured but **no test sources exist yet**.
- **Frontend**: No test framework configured.

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
