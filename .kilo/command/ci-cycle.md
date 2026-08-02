---
description: "Commit, push to main, poll GitHub Actions, fix failures, repeat until all CI checks pass"
---

# CI Cycle Workflow

Run the development loop until the latest push to `main` is green across all 3 CI jobs (`test`, `integration-tests`, `format-check`). CI is the ONLY place tests run — never locally. This rule is enforced by AGENTS.md and the pipeline itself.

## Rules

- NEVER run tests locally: no `mvn test`, `mvn verify`, `npm t`, `npx vitest run`, `npx playwright test`.
- Permitted local checks: `mvn compile` (backend), `npm run lint`, `npx tsc --noEmit` (frontend).
- Stage only intended files; never commit secrets; keep `playwright-results/` untracked.
- Never amend a pushed commit — always add new commits.
- Don't retry blindly — read the logs and fix the root cause.

## Steps

### 1. Make code changes

- First inspect the failing job logs (`gh run view <run-id> --log-failed`), then edit code/tests accordingly.
- Debug by reading code, CI logs, and seed data (`backend/src/main/resources/data.sql`) — never by running tests locally.
- E2E debugging hints (CI-specific):
  - CI runs Playwright with 1 worker (`workers: CI ? 1 : undefined`) and spec files execute alphabetically within each project — cross-spec state on shared seed episodes (`a1111111-…`, `a2222222-…`, `a3333333-…`) matters; use specific locators (`getByRole`/`getByLabel`, row filters with patient names), never `.first()`.
  - Selector strictness: disambiguate when multiple elements match (e.g. GCS appears in hourly grid and scale forms).
  - Seed clinical days: `b1111111` OPEN, `b1111112` NURSE_SIGNED, `b2222222` OPEN, `b4444444` NURSE_SIGNED, `b3333333` OPEN. A doctor can only sign a `NURSE_SIGNED` day, so sign-flow specs that share an episode must not depend on each other's side effects.
  - Prefer `GET /api/…` fixtures/guards over assuming a clean DB state.

### 2. Stage and commit

- `git status` / `git diff --stat` to review; `git add <intended files only>`.
- Conventional Commits (`feat:`, `fix:`, `test:`, `refactor:`, `docs:`, `chore:`) with a concise imperative summary, matching repo style (e.g. `fix: pre-existing CI defects — E2E URLs + backend schema idempotency`).

### 3. Push to main (triggers CI)

- `git push origin main` — `.github/workflows/playwright.yml` runs automatically: `test`, `integration-tests`, `format-check` (parallel; 40 min timeout; Playwright retries 2 per test).

### 4. Poll for results

- Locate the run: `gh run list --branch main --limit 1 --json databaseId,status,conclusion,displayTitle,headSha`
- Block until done: `gh run watch <databaseId> --exit-status`, or poll `gh run list` every 60–120 s.
- On failure: `gh run view <databaseId> --log-failed` (failed steps only); per job: `gh run view --job <jobId> --log-failed`; browser: `gh run view <databaseId> --web`.

### 5. Address failures

| Job | Content | Fix path |
|---|---|---|
| `format-check` | `mvn compile checkstyle:check` | Fix formatting/style in source; no test runs needed |
| `test` | 557 backend unit, ~350 Vitest, 45 Playwright E2E specs | Read failed logs → fix code or tests → commit → push |
| `integration-tests` | 79 backend integration tests (PostgreSQL 16 service) | Same |

- Stuck >40 min: `gh run cancel <databaseId>` after verifying no progress.

### 6. Repeat until green

- Loop steps 1–5 until the run for the latest `main` push has `conclusion: success` on all 3 jobs.
- Finish: report per-job results and the final commit SHA.
