---
name: production-deployment
description: Production deployment and database-migration runbook for the ICU Patient Chart project. Use when the user asks to deploy the app to production (продакшн), add or change a Liquibase changeset, modify database schema after the app is live, create or restore pg_dump backups, perform point-in-time recovery, roll back a failed release, or asks about DATABASECHANGELOG checksums, seed data in prod, seedIfEmpty RBAC seeding, or safe SQL (idempotent changesets, CONCURRENTLY indexes, forward-fix vs rollback). References docs/Production-Deployment-Runbook.md as the source of truth.
---

# Production Deployment Skill

This skill governs **operating** the ICU Patient Chart in production and evolving its
database schema after the app is live. It complements AGENTS.md (which covers the dev
loop); for anything "how do we change the DB without losing data", this skill + the
runbook win.

## Non-negotiables (read these first)

1. **Liquibase is the ONLY legal schema-change path.** Four databases
   (`my_fullstack_core`, `my_fullstack_icu`, `my_fullstack_med`, `my_fullstack_prosth`),
   each with its own `SpringLiquibase` bean and master yaml
   (`db/changelog/db.changelog-master-<mod>.yaml`). `ddl-auto: none`
   (`MultiDatabaseSupport.java:57`). No manual `psql` DDL in production, ever.
2. **Never edit an applied changeset.** Liquibase stores a checksum in
   `DATABASECHANGELOG` of each DB; editing the SQL file breaks startup with
   `ChecksumMismatchException`. The only move forward is a NEW file + NEW changeset id
   (+ `include` line in the master yaml). See `core/005-role-permissions-add-prescription-list-create.sql`
   as the canonical example of this pattern.
3. **Never hand-edit or delete rows of `DATABASECHANGELOG`.**
4. **Production environment flags (override `application.yml` defaults via env vars):**
   - `APP_SEED_DATA_ENABLED=false` — CRITICAL. `SeedDataInitializer` (`matchIfMissing = true`)
     otherwise injects 9 demo users, 50 episodes, 360 prescription lists into prod.
   - `APP_MIS_API_BASE_URL/LOGIN/PASSWORD/INSTALLATION_GUID` (+ optional
     `APP_MIS_API_TOKEN_PATH/RUN_PATH`) — the single real `MisServiceImpl`
     has no mocks/modes; missing keys fail startup fast (Phase 11, #264).
   - `APP_JWT_SECRET=<new value>` — the value in `application.yml:43` is a dev secret.
   - `APP_DATASOURCE_{CORE,ICU,MED,PROSTH}_{URL,USERNAME,PASSWORD}` — passwords from
     vault, never in repo.
   - `SPRING_PROFILES_ACTIVE=prod` (disables Swagger per `application.yml:163-166`).
   - `LOGGING_LEVEL_ROOT=INFO` (yaml default is DEBUG — too verbose for prod).
5. **Before ANY deploy: `pg_dump -Fc` all 4 DBs + snapshot `DATABASECHANGELOG`.**
6. **Production rollback = `pg_restore` from the pre-deploy dump + previous JAR.**
   Never `liquibase:rollback` in production; a schema change is reverted with a
   forward-fix changeset, not by rewinding the changelog.

## When a schema change is requested

1. Identify the module: core / icu / med / prosth → the DB + master yaml + changeset
   id namespace (`split-core:N`, `split-icu:N`, `split-med:N`, `split-prosth:N`).
2. Check the highest existing id: `grep -r "changeset" backend/common/src/main/resources/db/changelog/<mod>/`.
   New id = max+1. Reusing an id is a hard error.
3. Write a new file `db/changelog/<mod>/NNN-<short-name>.sql` (zero-padded numeric
   prefix, kebab-case name, UTF-8 without BOM):

   ```sql
   --liquibase formatted sql

   --changeset split-med:5
   --comment Short human description of the change
   ALTER TABLE prescription_items ADD COLUMN IF NOT EXISTS assigned_to_login VARCHAR(64);
   --rollback ALTER TABLE prescription_items DROP COLUMN IF EXISTS assigned_to_login;
   ```
4. Append exactly one `include` to `db.changelog-master-<mod>.yaml`.
   Verify: `ls db/changelog/<mod>/ | wc -l` == number of `include:` lines in that yaml.
5. Production-safe SQL rules:
   - Idempotent: `IF NOT EXISTS` / `IF EXISTS`, `ON CONFLICT DO NOTHING` on INSERTs.
   - One logical step per changeset: `ADD COLUMN` / backfill `UPDATE` / `SET NOT NULL`
     are THREE changesets.
   - Every `UPDATE`/`DELETE` MUST have a `WHERE`.
   - Never `DROP COLUMN`/`DROP TABLE` in the same release where code still reads it —
     two-release deprecation (release N stops using it, release N+1 drops it).
   - Large indexes: `CREATE INDEX CONCURRENTLY` (cannot run inside a transaction).
   - Type migration: add new column → backfill → switch code → drop old (next release).
   - New RBAC permission: `INSERT INTO permissions ... ON CONFLICT (code) DO NOTHING`
     (definitions are SQL-seeded; grants are Java-seeded by `PermissionService.seedIfEmpty`
     or a separate changeset — pre-inserting grants into SQL suppresses the full
     default-matrix seed on fresh installs; follow the `core/005` precedent).
6. Local pre-flight (tests are CI-only per AGENTS.md CI RULE):
   `mvn compile` (backend), `npm run lint` + `npx tsc --noEmit` (frontend).
7. Commit with Conventional Commits, push, and poll the 6 CI jobs
   (`gh run watch`); a green run before any prod step.

## Before deploying to production

- [ ] `pg_dump -Fc --no-owner --no-privileges` for all 4 DBs (retention 7d/4m/1y).
- [ ] Integrity-check the dumps: `pg_restore --list <dump> > /dev/null`.
- [ ] `SELECT id, author, filename, dateexecuted FROM DATABASECHANGELOG ORDER BY dateexecuted`
      saved to file, per DB.
- [ ] App drained/stopped (or rolling deploy only if the change is backwards-compatible).
- [ ] Verify `APP_SEED_DATA_ENABLED=false` in the target `EnvironmentFile`.

## After starting the new JAR

- Confirm in the log: each new changeset `ran successfully` (or `previously ran` for
  already-applied ones), then `Started IcuPatientChartApplication`.
- `SELECT id, filename, md5sum, dateexecuted FROM DATABASECHANGELOG ORDER BY dateexecuted DESC LIMIT 10;`
- Smoke: login, one business write per module, audit log entry present,
  `permissions` count still 25 (or expected count).

## Failure triage

| Symptom | First action |
|---|---|
| App won't start, `ChecksumMismatchException` | STOP. Do not edit the changeset file. Restore pre-deploy dumps (or revert to the JAR that carried the previous changelog set). |
| `SQLSyntaxError` inside a changeset at startup | App is down but the failed changeset is NOT recorded in `DATABASECHANGELOG` (Liquibase marks it only on success). Fix the SQL in git (new commit — never amend), re-run CI, redeploy; or `pg_restore` if data was mutated by a prior statement in the same changeset (avoid — split changesets so a failure cannot partially apply). |
| Business regression (data wrong) | `pg_restore` pre-deploy dumps + prior JAR. Log the forward-fix needed and implement it as NEW changesets afterward. |
| Slow query after deploy | Check `pg_stat_statements` top; if a new index was added, verify `CREATE INDEX CONCURRENTLY` finished (no `INVALID` index left). |

## Files this skill depends on

- `docs/Production-Deployment-Runbook.md` — the full runbook (env-var templates,
  systemd unit, nginx config, backup scripts, rollback runbook, FAQ). **Read it**
  before non-trivial production work.
- `backend/common/src/main/resources/db/changelog/` — changelog sources of truth.
- `backend/common/src/main/java/com/superhumans/config/multidb/` — per-DB
  `SpringLiquibase` wiring (`CoreDatabaseConfig`, `IcuDatabaseConfig`,
  `MedicationDatabaseConfig`, `ProstheticsDatabaseConfig`, `SeedDataInitializer`).
- `.github/workflows/playwright.yml` — CI (build/deploy is a separate pipeline, see
  runbook "Зв'язок з CI").
- `AGENTS.md` — dev loop, MIS read-only policy (never implement MIS write methods),
  module boundaries.

## Anti-patterns (reject these when you see them)

- Editing `core/003-role-permissions.sql` (or any applied file) to add a row.
- Running `psql` DDL "quickly" and "writing the changeset tomorrow".
- `APP_SEED_DATA_ENABLED` left `true` in prod.
- `ON CONFLICT DO UPDATE` in migration INSERTs (re-writes operator-edited labels).
- `liquibase:rollback` against a production database.
- Deploying `DROP COLUMN` in the same release as the code that still reads it.
- `TRUNCATE`, `VACUUM FULL` (non-concurrent), or `REINDEX` without `CONCURRENTLY`
  during business hours.
