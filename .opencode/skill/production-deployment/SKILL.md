---
name: production-deployment
description: Production deployment and database-migration skill for the ICU Patient Chart project. Use whenever the user mentions deploying or releasing to production (продакшн, деплой, реліз), first deploy, adding or changing a Liquibase changeset or migration (міграція), changing the DB schema of a live system, pg_dump backups or pg_restore (бекап, відновлення), point-in-time recovery, rolling back a failed release, DATABASECHANGELOG checksums or lock, seed data or demo users in prod, or safe SQL (idempotent changesets, CONCURRENTLY indexes, forward-fix vs rollback). Use it even when the user does not say "production" but is touching schema or migrations that will reach a live system. Detail lives in docs/Production-Deployment-Runbook.md.
---

# Production Deployment Skill

Governs **operating** the ICU Patient Chart in production and evolving its schema after
go-live. AGENTS.md continues to govern repository and development work; use this skill
with the runbook for production operations. Higher-level instructions always take
precedence.

## Project status (read first)

- **There is no production yet.** The runbook is written for the *first* deploy. Nothing
  below describes a running system.
- **No production operations are currently established:** there are no configured backup
  jobs, WAL archiving, deploy pipeline or staging environment documented. Backups,
  retention tiers and WAL settings are recommendations to set up and verify. Local
  development may be running; do not confuse that with production. Never write "daily
  dumps run at 02:00" or similar present-tense claims.
- **Intended first-deploy model in the runbook:** bare-metal/VM + systemd + JAR swap
  (`systemctl stop ictc` / `systemctl start ictc`, `ln -sfn` to a release JAR). Docker
  Compose is an optional template only (runbook A.2/A.3). Kubernetes is out of scope; do
  not add K8s content.
- CI (`.github/workflows/playwright.yml`) **does not deploy**. It is test-only.

## Agent guardrails

- **Draft-only.** Claude has no prod shell, no vault, no prod `psql`. It drafts changesets,
  commands and checklists; **a human operator runs them.** There is no autonomous prod
  execution path.
- Before any destructive step (`pg_restore`, `DROP DATABASE`, `DROP`, `DELETE`,
  `TRUNCATE`, lock release), state exactly which data it affects and ask the human to
  confirm. Approver roles are not defined in the project; do not invent them. Ask who
  has authority.
- **Secrets:** refer to env variable *names* or `${...}` references only. Never read,
  print or paste values (passwords, JWT secret, MIS/LDAP credentials) into chat, code,
  logs, tests, fixtures, screenshots, Issues or PRs. Patient data and database dumps get
  the same protection: do not expose record contents in agent output or artifacts; use
  counts, status codes or appropriately anonymized evidence where possible.
- Claude may run `psql`/`mvn` against **local dev/test databases** to verify a changeset.

## Non-negotiables

1. **Liquibase is the ONLY legal schema-change path.** Four databases
   (`my_fullstack_core`, `my_fullstack_icu`, `my_fullstack_med`, `my_fullstack_prosth`),
   each with its own `SpringLiquibase` bean and master yaml
   (`db/changelog/db.changelog-master-<mod>.yaml`). `ddl-auto` is `none`
   (`MultiDatabaseSupport`). No manual `psql` DDL in production, ever.
   `spring.liquibase.enabled` and `spring.sql.init` stay disabled.
2. **Never edit an applied changeset.** The checksum in `DATABASECHANGELOG` makes startup
   fail with `ChecksumMismatchException`. Fix forward with a NEW file + NEW changeset id +
   one `include` line. Canonical example: `core/005-role-permissions-add-prescription-list-create.sql`.
3. **Never hand-edit or delete rows of `DATABASECHANGELOG`.**
4. **MIS is absolutely read-only** (never implement MIS write methods; a tripwire test
   enforces it) and **AD/LDAP is read/verify-only** (bind/search/read, no writes).
5. **Production env flags** (names only; values come from `EnvironmentFile`/vault):
   - `APP_SEED_DATA_ENABLED=false`. The `prod` profile also sets it, and `SeedDataGuard`
     aborts startup if `prod` is active with seeding on. Still verify it explicitly. When
     seeding is enabled, `SeedDataInitializer` loads SQL fixtures and calls
     `UserSeedService`; user accounts are created only when matching
     `APP_TEST_USERNAME*`/`APP_TEST_PASSWORD*` values are configured. No public default
     user passwords are embedded in production SQL. Keep test-seeding variables out of
     production. If seeding ran, have the operator review and remove inappropriate fixture
     data and any environment-seeded test accounts; never assume their credentials.
   - `APP_JWT_SECRET`: a fresh value (`openssl rand -base64 64`). `JwtSecretGuard` aborts
     startup in `prod` if the dev default is present.
   - `APP_DATASOURCE_{CORE,ICU,MED,PROSTH}_{URL,USERNAME,PASSWORD}`, non-superuser app role.
   - `APP_MIS_API_BASE_URL/LOGIN/PASSWORD/INSTALLATION_GUID` (+ optional
     `APP_MIS_API_TOKEN_PATH/RUN_PATH`). No mock mode; missing keys fail startup.
   - `APP_CORS_ALLOWED_ORIGINS` (exact allowlist), `SPRING_PROFILES_ACTIVE=prod`
     (Swagger off), `LOGGING_LEVEL_ROOT=INFO`, `LOGGING_LEVEL_COM_SUPERHUMANS=INFO`.
6. **Before ANY deploy:** `pg_dump -Fc` all 4 DBs and snapshot `DATABASECHANGELOG`.
7. **For an upgrade, rollback may require the previous JAR and a database restore, only
   when forward-fix is not viable** (see "Restore vs forward-fix"). Never
   `liquibase:rollback` in production. A first deployment has no previous JAR; define and
   approve its recovery plan before go-live rather than implying a code rollback exists.
8. Use Conventional Commits if a commit is explicitly requested; SQL files must be UTF-8
   without BOM; module boundaries (ArchUnit, oxlint) apply to code accompanying a migration.

## When a schema change is requested

1. Identify the module (core / icu / med / prosth) → DB, master yaml, author
   (`split-core`, `split-icu`, `split-med`, `split-prosth`).
2. Find the highest number:
   `grep -rhE '^--changeset' backend/common/src/main/resources/db/changelog/<mod>/`
   Convention: new id = max+1 within that module (Liquibase identity is id + author +
   file, so this is project policy, not a Liquibase rule). Some historical files reuse
   numbers across filenames; do not renumber or edit them. For every new changeset,
   allocate a new module-wide number, including for a "revert" changeset.
3. Create `db/changelog/<mod>/NNN-<short-name>.sql`:

   ```sql
   --liquibase formatted sql

   -- Replace NN with the next module ID before use; this is a template, not a literal ID.
   --changeset split-med:NN
   --comment Short human description of the change
   ALTER TABLE prescription_items ADD COLUMN assigned_to_login VARCHAR(64);
   --rollback ALTER TABLE prescription_items DROP COLUMN assigned_to_login;
   ```

   `--rollback` blocks are for dev use and documentation only; they never run in prod.
4. Append exactly one `include` to `db.changelog-master-<mod>.yaml`. Every `.sql` file in
   the module folder must be included exactly once (a missed include fails silently).
5. **SQL rules:**
   - Prefer safe retry behavior where appropriate, but do not add `IF NOT EXISTS` / `IF
     EXISTS` blindly: these can hide schema drift (for example, an existing column with the
     wrong type). Inspect the existing definition or use an explicit Liquibase precondition.
     For static definition inserts, use `ON CONFLICT DO NOTHING` rather than `DO UPDATE`,
     which can overwrite operator-edited values.
   - **One logical step per changeset.** `ADD COLUMN`, backfill `UPDATE` and `SET NOT NULL`
     are separate changesets; do not copy runbook examples that bundle steps.
   - Every `UPDATE`/`DELETE` has a `WHERE`.
   - No `DROP COLUMN`/`DROP TABLE` in the release where code still reads it. Release N
     stops using it, release N+1 drops it, with at least a week of observation between.
   - Rename or type change: new column → backfill → switch code → drop old (next release).
   - New RBAC permission: `INSERT INTO permissions ... ON CONFLICT (code) DO NOTHING`.
     Definitions are SQL-seeded; grants come from `PermissionService.seedIfEmpty` or a
     separate changeset. Pre-inserting grants in SQL suppresses the default-matrix seed on
     fresh installs, so follow `core/005`.
   - Never write a changeset that deletes or archives data younger than its retention:
     `audit_logs` 2 years (ТЗ §81), `hourly_records` and `prescription_executions` 5 years.
     Confirm with the owner whether these are binding or proposed.
6. **Large-table precautions** (apply to `hourly_records`, `prescription_items`/`day_parts`,
   `audit_logs`, `generated_pdfs`, prosthetics `evidence_files`, or any table whose size is
   unknown; production sizes are not yet known):
   - `CREATE INDEX CONCURRENTLY` must use a changeset header with
     `runInTransaction:false`. Do not put transaction-dependent statements in that
     changeset. A failed build can leave an `INVALID` index; `CREATE INDEX CONCURRENTLY IF
     NOT EXISTS` can silently skip it. Prefer an explicitly reviewed restart-safe rebuild
     changeset (`DROP INDEX CONCURRENTLY IF EXISTS <name>` followed by `CREATE INDEX
     CONCURRENTLY <name> ...`) when the target index is expected to be absent or is
     intentionally being rebuilt. Check the index definition/state first; do not blindly
     drop an existing valid index. Test failure and retry behavior on a scratch database.
   - For transactional DDL changesets, use transaction-local limits such as
     `SET LOCAL lock_timeout = '5s'` and a suitable `SET LOCAL statement_timeout` at the
     start of the changeset. For `runInTransaction:false` changesets, do not add session
     `SET` commands that could leak settings through a pooled connection; configure and
     verify timeout handling separately, and ensure any session settings are reset on both
     success and failure.
   - `SET NOT NULL`: `CHECK (col IS NOT NULL) NOT VALID` → `VALIDATE CONSTRAINT` (own
     changeset) → `SET NOT NULL`.
7. **Verify locally (no staging exists):** `mvn clean package` (never trust a stale fat
   JAR), `npm run lint`, `npx tsc --noEmit`, run the relevant tests locally, and start the
   app against a scratch/local DB to see the new changeset `ran successfully`. Once prod
   data exists, also apply it to a scratch DB restored from the latest dump.
8. Use Conventional Commits if a commit is explicitly requested; do not stage or commit
   without that request. **CI runs only if the user explicitly asks**: do not push just to
   trigger it and do not poll `gh run watch` unprompted (AGENTS.md).

## Before deploying to production

- [ ] Local verification above is green; build was `clean package`.
- [ ] For an upgrade: previous release JAR is on disk (`/opt/ictc/releases/`) and the
      symlink target is known. For the first deploy: record that no prior JAR exists and
      confirm the approved initial-release recovery plan with the operator.
- [ ] App stopped/drained so no writes occur while the four database dumps and changelog
      snapshots are taken. Stop-the-app is the normal path; rolling deploy only for
      backwards-compatible changes. Prefer low-traffic hours; no maintenance window is
      defined yet.
- [ ] `pg_dump -Fc --no-owner --no-privileges` for all 4 DBs, using one recorded backup
      set/timestamp.
- [ ] `pg_restore --list <dump> > /dev/null` for each dump. This checks the archive listing,
      not recoverability; recoverability requires a successful scratch restore drill.
- [ ] `DATABASECHANGELOG` snapshot per DB (`SELECT id, author, filename, dateexecuted ...`)
      saved with the same backup set.
- [ ] Dumps are encrypted, stored off-host, access-restricted (patient data).
- [ ] Env checklist: `APP_SEED_DATA_ENABLED=false`, non-dev `APP_JWT_SECRET`, MIS vars set,
      and no `APP_TEST_USERNAME*`/`APP_TEST_PASSWORD*` test-user seed credentials; verify
      names/configuration without exposing values to the agent.
- [ ] A restore drill on a scratch DB (runbook 3.7) has been done at least once and its
      duration recorded.

## After starting the new JAR

- Log: each new changeset `ran successfully` (or `previously ran`), then
  `Started IcuPatientChartApplication`. Check all four modules.
- `SELECT id, filename, md5sum, dateexecuted FROM DATABASECHANGELOG ORDER BY dateexecuted DESC LIMIT 10;`
- `SELECT count(*) FROM permissions;` equals the size of `PermissionCatalog` for this
  release (check the code; do not rely on a remembered number).
- Read-only smoke by default: health endpoint, login, required read endpoints, all four
  module startup/migration logs, and a successful MIS read. Do not create or modify real
  clinical records as a smoke test. If a write test is required, use only an explicitly
  approved controlled test record and a documented cleanup procedure; never use a real
  patient's chart for synthetic testing.
- Have the human operator verify the production user roster against the approved roster.
  Do not assume demo accounts or expose user credentials in the agent session. Confirm no
  test-user seed variables are configured.
- Confirm expected audit behavior using the approved smoke procedure; do not create a
  clinical write solely to manufacture an audit event.
- Confirm an MIS read succeeded using status/latency metadata only; do not expose patient
  identifiers or response payloads in agent output, logs shared outside the operator team,
  or artifacts.

## Restore vs forward-fix

A restore **discards every write after the dump**, which here means clinical data.

- **Forward-fix (preferred)** when the app is up or can start, data is intact, and a new
  changeset or code fix can correct it.
- **Restore** only when the release corrupted or lost data, or the schema is in a state a
  forward-fix cannot safely repair. Quantify the write window that will be lost and get
  explicit human sign-off first.
- Recovery targets (RPO/RTO) are **not defined**. The runbook implies low RPO for a
  medical system, but that is a recommendation. Ask the owner before recommending a
  restore on timing grounds.

For an upgrade, mechanics follow runbook Appendix E (a human runs them): stop the app; for
each DB drop/recreate and `pg_restore --clean --if-exists --no-owner --no-privileges`;
restore the previous JAR; start; verify. `DATABASECHANGELOG` is inside the dump, so
restore all four databases from the **same** pre-deploy set. The four restores are not
atomic. A first deployment has no prior JAR; use only its separately approved initial-
release recovery plan.

**PITR:** the runbook includes WAL-archiving settings (3.2), a pgBackRest template (D.3),
and an abbreviated time-target restore example (D.3). It does not provide a complete,
validated end-to-end PITR recovery procedure. Do not improvise one or assume WAL/PITR is
configured. Until an operator verifies the infrastructure and a complete recovery
procedure, use the documented dump-based recovery path only when the required dumps and a
successful restore drill exist.

## Failure triage

For the on-call quick procedure see runbook 4.5 and Appendix E. No escalation contact is
defined yet.

| Symptom | First action |
|---|---|
| `ChecksumMismatchException` | STOP. Never change the meaning of an applied changeset or edit `DATABASECHANGELOG`. Compare the deployed/source version and recorded checksums; verify all four DBs for changes already applied by this release. On an upgrade, use the previous JAR only if it exists and is compatible with every DB's schema; otherwise escalate and assess the approved recovery plan. If the source file was accidentally altered, recover the exact version matching the recorded checksum from trusted history/release artifacts, then put any intended schema change in a new changeset. Restore only if there is evidence of data change and after quantifying lost writes. |
| Error inside a changeset at startup | Postgres DDL is transactional, so the failed changeset normally rolls back and is not recorded. Fix the source in a new change; commit only if requested, and never amend. Exception: `runInTransaction:false` changesets can apply partially; inspect the index state and use only the reviewed Liquibase rebuild/recovery path. Do not manually clean up with `psql` DDL. |
| Startup hangs on Liquibase / cannot acquire change log lock | A crash mid-migration can leave `DATABASECHANGELOGLOCK` held. Stop and escalate to the responsible database operator using the organization's incident path. The project docs contain no reviewed lock-release procedure or named approver; do not draft or run unlock SQL until the operator confirms the instance is inactive and supplies an approved procedure. |
| One DB migrated, another failed | Decide explicitly: restore all four from one dump set, or confirm the running version tolerates the mix. |
| Startup aborts in `prod` naming seed or JWT | The guards worked. Fix the env value (name only); do not weaken the guard. |
| Change does not seem to apply / old behavior after deploy | Suspect a stale fat JAR (nested Maven modules). Rebuild with `clean package`. Also check the `include` line exists. |
| Business regression (data wrong) | Apply "Restore vs forward-fix"; record the forward-fix as new changesets. |
| Slow query after deploy | `pg_stat_statements`; check no `INVALID` index (`pg_index.indisvalid`). |

## Runbook reference and precedence

Read `docs/Production-Deployment-Runbook.md` for detail, by trigger:
- First deploy, env template, DB creation, MIS check: Part 1, Appendix A.
- systemd unit: Appendix B. nginx: Appendix C. Backup job/timer/pgBackRest: Appendix D.
- Rollback steps: Appendix E. Backup layers, retention, monthly drill: Part 3.

**Use the stricter safety rule when this skill and the runbook disagree; do not treat this
skill as overriding higher-level instructions.** Known runbook defects: example
changesets in 2.2 bundle several steps in one changeset; E.5 reuses `split-core:7`; 2.5
calls `pg_restore` "low risk" without mentioning the lost writes; §3.2 recommends WAL
archiving but gives no PITR procedure; 1.3 lists the `APP_MIS_API_*`
variables twice; and it says nothing on `DATABASECHANGELOGLOCK`, `runInTransaction:false`
or `lock_timeout`. Appendix D.3 has an abbreviated pgBackRest time-target restore command,
but not a validated end-to-end PITR procedure. There is also a first-deploy TLS mismatch:
the `prod` profile enables HTTPS on the app (`application.yml`), while the nginx template
proxies to the backend over plain HTTP (Appendix C). Treat this as a deployment blocker:
the system owner must choose/verify the TLS termination design and align both configs
before deployment. Retention (7d local / 4m NAS / 1y offsite, WAL 30d) is a proposal.

## Anti-patterns (reject these)

- Editing `core/003-role-permissions.sql` (or any applied file) to add a row.
- `psql` DDL "quickly" and "writing the changeset tomorrow".
- Leaving seeding on, or the dev JWT secret, in prod (the guards are a backstop, not a plan).
- `ON CONFLICT DO UPDATE` in migration INSERTs.
- `liquibase:rollback` against a production database.
- `DROP COLUMN` in the same release as code that still reads it.
- `CREATE INDEX CONCURRENTLY` without `runInTransaction:false`.
- `TRUNCATE`, `VACUUM FULL` (holds `ACCESS EXCLUSIVE` for the whole run), plain `REINDEX`
  (use `REINDEX CONCURRENTLY`) during business hours.
- Restoring a dump without stating which writes will be lost.
- Implementing any MIS write method or any LDAP write.
- Shipping a build that was not `clean package`.
