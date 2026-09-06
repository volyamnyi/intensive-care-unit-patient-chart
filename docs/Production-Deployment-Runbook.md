# Production Deployment Runbook

ICU Patient Chart — пошаговий посібник із переведення застосунку в продакшн та подальшого вношення змін, які потребують зміни баз даних.

Останнє оновлення: 2026-08-20. Розглядає стан репозиторію станом на комміт, яким ви користуетесь (AGENTS.md описує стан на той момент).

Якщо ви читаєте цей документ **перший раз**, почніть з [Part 5 — FAQ](#part-5--faq-типові-питання).

---

## Зміст

- [Part 0 — Що стосується цього проєкту](#part-0--що-стосується-цього-проєкту)
- [Part 1 — Перший деплой в продакшн](#part-1--перший-деплой-в-продакшн)
- [Part 2 — Як вносити зміни, що потребують змін у БД](#part-2--як-вносити-зміни-що-потребують-змін-у-бд)
- [Part 3 — Запобігання втраті даних](#part-3--запобігання-втраті-даних)
- [Part 4 — Оперативні процедури (повсякденні)](#part-4--оперативні-процедури-повсякденні)
- [Part 5 — FAQ](#part-5--faq-типові-питання)
- [Appendix A — Шаблони змінних оточення](#appendix-a--шаблони-змінних-оточення)
- [Appendix B — systemd unit](#appendix-b--systemd-unit)
- [Appendix C — nginx reverse proxy](#appendix-c--nginx-reverse-proxy)
- [Appendix D — Щоденний backup job](#appendix-d--щоденний-backup-job)
- [Appendix E — Rollback runbook](#appendix-e--rollback-runbook)

---

## Part 0 — Що стосується цього проєкту

### 0.1 Архітектура БД, коротко

| БД | Модуль | Містить |
|---|---|---|
| `my_fullstack_core` | common | Users, RBAC matrix, audit log, system settings |
| `my_fullstack_icu` | icu-chart | Episodes, clinical days, hourly records, orders, scales, PDFs, signatures |
| `my_fullstack_med` | medication-sheet | Prescription lists/items/executions, vital signs, medicine cache |
| `my_fullstack_prosth` | prosthesis-manufacturing | Prosthesis patients, orders, templates, instances, evidence |

4 фізичні бази в PostgreSQL (CI використовує версію 16; продакшн — теж 16 або новішу). Кожна має **свій** Liquibase bean + **свій** changeset master + **свій** seed script:

| БД | Liquibase bean | Master changelog | Seed |
|---|---|---|---|
| core | `coreLiquibase` | `db/changelog/db.changelog-master-core.yaml` | `data-core.sql` |
| icu | `icuLiquibase` | `db/changelog/db.changelog-master-icu.yaml` | `data-icu.sql` |
| med | `medLiquibase` | `db/changelog/db.changelog-master-med.yaml` | `data-med.sql` |
| prosth | `prosthLiquibase` | `db/changelog/db.changelog-master-prosth.yaml` | `data-prosth.sql` |

Схема створюється **лише через Liquibase** — JPA `hibernate.hbm2ddl.auto` = `none` (`MultiDatabaseSupport.java:57`). Ніяких ручних DDL.

### 0.2 Дві речі, які легко зробити неправильно в продакшні

**1. Сід-дані (`app.seed-data`)** — `SeedDataInitializer` виконує `data-core.sql`, `data-icu.sql`, `data-med.sql`, `data-prosth.sql` **спочатку** на кожній БД. Вони містять `ON CONFLICT (id) DO NOTHING`, тобто на вже заселеній БД не додають нових рядків і не чіпають існуючих. **Але** — вони додають `9` демо-користувачів, `50` демо-епізодів, `360` листків призначень, тощо. **У продакшні це має бути `false`**:

```
APP_SEED_DATA_ENABLED=false     # критично для продакшну
```

В `application.yml:68-69` стоїть `true` — це dev/default. За замовчуванням у `SeedDataInitializer.java:18` (`matchIfMissing = true`) теж `true`. `prod`-профіль (`application.yml:174-176`) **автоматично** ставить `app.seed-data.enabled: false`. **Не забудьте перевизначити через змінну оточення, якщо деплоїте локально.**

**Boot-guard (A2 fix):** `SeedDataGuard` — `@Component`, що виконується перед `SeedDataInitializer` (через `@DependsOn`) і **не запускає застосунок** (`IllegalStateException`), якщо активний профіль `prod` і сід-дані ввімкнено. Це практично виключає випадок «випадково ввімкнули сід у проді».

**Паролі більше не перезаписуються:** у `data-core.sql` обидва `INSERT ... users` використовують `ON CONFLICT (login) DO NOTHING` (а не `DO UPDATE SET password_hash = EXCLUDED.password_hash`), тож рестарт проти наявної БД **ніколи** не відкатить операторський пароль до демо-значення.

> ⚠️ **Обов'язково до go-live:** перший старт (dev, або якщо прод ввімкнув сід один раз) створює `9` демо-користувачів з публічно відомими паролями (`doctor1/doctor123`, `admin/admin123`, …). Після першого старту **замініть або вимкніть** ці облікові записи — тепер жоден рестарт ніколи не поверне паролі, але самі демо-обліковки лишаються робочими, поки ви вручну їх не закриєте.

**2. RBAC seed (`PermissionService.seedIfEmpty`)** — виконується **лише** коли `role_permissions` порожня. На першому старті в продакшні — створює `25` рядків у `permissions` і default-grants з `PermissionCatalog.defaultMatrix()`. Це **бажано** і **коректно** — ви не будете «запускати» RBAC matrix вручну. Перевірте після старту:

```sql
SELECT count(*) FROM permissions;        -- 25
SELECT role, count(*) FROM role_permissions GROUP BY role ORDER BY role;
```

Якщо число не 25 — щось пішло не так при старті. **Не** запускайте `seedIfEmpty` вручну — воно і так спрацювало (або не спрацювало, якщо БД вже не порожня).

### 0.3 `spring.sql.init` і `spring.liquibase` у application.yml

`application.yml:14-16`:
```yaml
spring:
  sql.init.mode: never       # НЕ вмикайте — seed йде через SeedDataInitializer
  liquibase.enabled: false   # НЕ вмикайте — чотири beans в code вже створені
```
Ці два рядки мають залишатись в `false`/`never` і в продакшні — інакше Spring Boot спробує власне ініціалізувати SQL/liquibase і **конфліктує** з ручними config-ками `CoreDatabaseConfig`, `IcuDatabaseConfig`, `MedicationDatabaseConfig`, `ProstheticsDatabaseConfig`.

---

## Part 1 — Перший деплой в продакшн

Цей розділ виконується **один раз** — при першому переведенні застосунку в продакшн.

### 1.1 Вимоги

| Ресурс | Вимога | Примітка |
|---|---|---|
| ОС | Ubuntu 22.04+ / Debian 12+ / RHEL 9+ | systemd у базі |
| Java | Temurin JDK 25 | `apt install temurin-25-jdk` або [Adoptium](https://adoptium.net/) |
| Node.js | 22 LTS (лише для збірки frontend) | 1 раз на збірному сервері |
| PostgreSQL | 16 (рекомендовано) | 17+ теж ок, 15 — перевірте |
| Мережа | 0.0.0.0:8085 (або :443 через проксі) | + :5432 для БД (окремий хост/сеть) |
| Диск | ≥ 50 ГБ SSD | 4 БД + WAL + бекіпи |
| RAM | ≥ 8 ГБ | 4 Hikari pools + JVM |

### 1.2 Підготовка збірки (на збільному сервері)

```bash
# 1. Клонувати і встановити залежності
git clone <repo-url> /opt/ictc && cd /opt/ictc
git checkout <tag-or-commit>

# 2. Frontend — збірка у prod-режимі
cd frontend
npm ci                                   # точні версії з package-lock.json
# VITE_API_BASE — див. 1.3; за замовчуванням '/api' (relative) — ок, якщо nginx reverse-proxy
npm run build                            # → frontend/dist

# 3. Backend — збірка JAR
cd ../backend
mvn -B clean package -DskipTests         # → backend/app/target/app-*.jar
```

**Результат:**
- `/opt/ictc/frontend/dist/` — статика (index.html, assets/…)
- `/opt/ictc/backend/app/target/app-<ver>.jar` — виконуваний JAR

Якщо збірка йде щоразу в продакшні (без збірного сервера) — встановіть Node 22 + Temurin 25 на цю ж машину і повторюйте ті ж команди.

### 1.3 Змінні оточення (production-профіль)

Повний шаблон — [Appendix A](#appendix-a--шаблони-змінних-оточення). Коротко:

| Змінна | Значення | Чому |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | Вимикає Swagger (`springdoc.*.enabled: false` у `application.yml:163-166`), вмикає SSL-блок |
| `APP_DATASOURCE_CORE_URL` | `jdbc:postgresql://db:5432/my_fullstack_core?stringtype=unspecified&sslmode=require` | `@ConfigurationProperties("app.datasource.core")` → spring-мапінг `APP_DATASOURCE_CORE_URL` |
| `APP_DATASOURCE_CORE_USERNAME` | `ictc_app` | Не `postgres` — окремий непривілейований користувач |
| `APP_DATASOURCE_CORE_PASSWORD` | *(з vault)* | Не в yml, не в git |
| `APP_DATASOURCE_ICU_URL/USERNAME/PASSWORD` | … `my_fullstack_icu` | |
| `APP_DATASOURCE_MED_URL/USERNAME/PASSWORD` | … `my_fullstack_med` | |
| `APP_DATASOURCE_PROSTH_URL/USERNAME/PASSWORD` | … `my_fullstack_prosth` | |
| `APP_JWT_SECRET` | `openssl rand -base64 64` | Замінити дев-секрет з `application.yml:43`; `JwtSecretGuard` падає на старті в `prod`, якщо стоїть дев-дефолт (A1) |
| `APP_CORS_ALLOWED_ORIGINS` | `https://<домен>` (кома-список) | Точний allowlist для credentialed CORS; дефолт — лише localhost (#184/F5) |
| `APP_SEED_DATA_ENABLED` | **`false`** | Критично — інакше демо-дані |
| `APP_MIS_MOCK_ENABLED` | `false` | Не MockMisServiceImpl |
| `APP_MIS_WIREMOCK_ENABLED` | `true` | WireMockMisServiceImpl (див. 1.5) |
| `APP_MIS_WIREMOCK-URL` | `https://mis.internal/api` | Реальний endpoint MIS (з `MisApiClient.java:32` `app.mis.wiremock-url`) |
| `APP_MIS_INSTALLATION-GUID` | *(GUID інсталяції)* | `MisApiClient.java:35` |
| `APP_MIS_LOGIN` | *(integration user)* | `MisApiClient.java:38` |
| `SPRING_MAIL_HOST` / `PORT` / `USERNAME` / `PASSWORD` | реальний SMTP | `application.yml:21-24` має локальний демо |
| `LOGGING_LEVEL_ROOT` | `INFO` | `application.yml:99` = DEBUG — забагато для прод |
| `LOGGING_LEVEL_COM_SUPERHUMANS` | `INFO` | `application.yml:102` = TRACE |

**Правило:** жодний секрет не живе у репозиторії. Жоден секрет не живе у `application.yml` в продакшні. Усе — через `EnvironmentFile` / systemd / Vault.

### 1.4 Створення баз даних (один раз)

```sql
-- postgres.sql — виконати на продакшн-БД
CREATE ROLE ictc_app LOGIN PASSWORD '<з vault>';

CREATE DATABASE my_fullstack_core OWNER ictc_app;
CREATE DATABASE my_fullstack_icu  OWNER ictc_app;
CREATE DATABASE my_fullstack_med  OWNER ictc_app;
CREATE DATABASE my_fullstack_prosth OWNER ictc_app;

GRANT ALL PRIVILEGES ON DATABASE my_fullstack_core  TO ictc_app;
GRANT ALL PRIVILEGES ON DATABASE my_fullstack_icu   TO ictc_app;
GRANT ALL PRIVILEGES ON DATABASE my_fullstack_med   TO ictc_app;
GRANT ALL PRIVILEGES ON DATABASE my_fullstack_prosth TO ictc_app;

-- У кожній БД:
GRANT ALL ON SCHEMA public TO ictc_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO ictc_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO ictc_app;
```

**Безпека:** якщо хочеться обмежити застосунок від випадкового `DROP` — можна дати `SELECT/INSERT/UPDATE/DELETE` + `CREATE` (Liquibase створює таблиці) **без** `TRUNCATE`, `ALTER` (поза створенням), `REFERENCES` (поза CREATE). Але Liquibase іноді потребує `ALTER` (наприклад, `ALTER TABLE ... ADD COLUMN`) — тоді дайте `ALTER` теж. Не давайте `SUPERUSER`.

### 1.5 MIS-інтеграція

У репозиторії **лише два** варіанти `MisService`:
- `MockMisServiceImpl` — хардкод 5 пацієнтів (dev/test).
- `WireMockMisServiceImpl` — генерує mock-відповіді на основі `wiremock __files/*.json`. **Насправді це не WireMock** — це REST-клієнт до `MisApiClient`, який POSTить на `app.mis.wiremock-url/api/run`.

Обидва — **mock**. Для продакшну потрібен **новий** клас, що імплементує `MisService` і робить реальні виклики на ваш MIS API (з `MisApiClient.callMethod(methodName, params)`). **Без порушення read-only policy** (див. AGENTS.md, `MIS Data Policy`).

Що **можна** зараз (за 2 години):
1. Створити `RealMisServiceImpl` у `com.superhumans.mis` з `@ConditionalOnProperty(name="app.mis.real-enabled", havingValue="true")`.
2. Усередині — використовувати `MisApiClient.callMethod("spzIBPatientSearch", ...)` тощо.
3. `MockMisServiceImpl` залишається з `@ConditionalOnProperty(mock-enabled=true, matchIfMissing=false)` — коли `mock-enabled=false`, він не створюється.
4. Додати `RealMisServiceImpl` — новий `@ConditionalOnProperty(real-enabled=true, matchIfMissing=false)`.

Ця частина **залишається TODO для продакшну** — якщо MIS ще не готовий, працюйте на `WireMockMisServiceImpl` (`APP_MIS_WIREMOCK_ENABLED=true`) і не забудьте, що відповіді — від WireMock `__files/*.json`, не реальні.

### 1.6 Перший запуск

```bash
# 1. Перевірити, що БД доступні (із змінними з 1.3)
source /etc/environment-file-ictc       # або EnvironmentFile у systemd

# 2. Бек-ап ПОРОЖНІХ баз (базова точка відліку — ДО першого старту)
for db in my_fullstack_core my_fullstack_icu my_fullstack_med my_fullstack_prosth; do
  pg_dump -Fc --no-owner --no-privileges "$db" > /backup/$(date +%Y-%m-%d)-initial-${db}.dump
done

# 3. Запустити
java -Xms1g -Xmx2g -jar /opt/ictc/backend/app/target/app-*.jar \
     --spring.profiles.active=prod

# 4. Стежити вилів — очікуємо:
#   [main] ... Running Spring Boot ...
#   [main] Core liquibase: liquibase changelog db.changelog-master-core.yaml ...
#   [main] Liquibase: ChangeSet core/001-initial.sql::split-core:1::... ran successfully
#   ... (усі changeset-и)
#   [main] ... Started IcuPatientChartApplication in X.XXX seconds
```

**Час старту першого разу** — до 60-90 сек (Liquibase створює ~30 таблиць у 4 БД + індекси, `PermissionService` сіє RBAC). Наступні разів — ~15-25 сек.

### 1.7 Smoke-перевірка (перший день)

```bash
# 1. API доступний
curl -s https://domain/api/patients -H "Authorization: Bearer <token>" | head -c 200

# 2. RBAC засіяний
psql -d my_fullstack_core -c "SELECT code, category FROM permissions ORDER BY category, code;"
psql -d my_fullstack_core -c "SELECT role, count(*) FROM role_permissions GROUP BY role ORDER BY role;"

# 3. Користувачі — в проді має бути РЕАЛЬНИй список, не doctor1/nurse1
psql -d my_fullstack_core -c "SELECT login, role, full_name FROM users ORDER BY login;"

# 4. Базові сценарії
#   - Логін (через UI)
#   - Створення епізоду
#   - Введення vitals
#   - Підпис
#   - Генерація PDF
#   - Перегляд журналу аудиту (admin)

# 5. Перевірити, що сід NOT засіяний (якщо APP_SEED_DATA_ENABLED=false)
psql -d my_fullstack_icu -c "SELECT count(*) FROM episodes;"     # 0 або лише ваші
psql -d my_fullstack_core -c "SELECT login FROM users;"          # лише реальні
```

### 1.8 Реверс-проксі (nginx)

Шаблон — [Appendix C](#appendix-c--nginx-reverse-proxy). Ключові моменти:
- Тирингу на `:8085` + TLS (LE-сертифікат або комерційний).
- `proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;` — інакше `AuditLog.ipAddress` (§79 ТЗ) буде IP nginx, а не користувача.
- `X-Forwarded-Proto https`.
- `client_max_body_size` — для завантаження evidence-файлів у prosthetics (10 МБ ліміт у `EvidenceFileService`).

### 1.9 Перевірка з'єднання з MIS (якщо реалізовано)

Якщо ви написали `RealMisServiceImpl` (1.5):
```bash
# У логі бекенду — стежу:
#   INFO  c.s.mis.WireMockMisServiceImpl / RealMisServiceImpl  - MIS call: spzIBPatientSearch ...
#   INFO  ... response: 200 OK  (X ms)
```
Якщо MIS недоступний — застосунок не падає (всі `MisService` методи обороняють помилки → `Optional.empty()`), але UI не покаже пацієнтів. Діагностика через `POST /api/mis/error-mode?mode=none` (якщо ввімкнено).

---

## Part 2 — Як вносити зміни, що потребують змін у БД

**Це головний розділ.** Він описує повний цикл: від «потрібна нова колонка» до «в продакшні вже працює» і «як відкочитись, якщо пішла не так».

### 2.1 Бетонне правило: ніколи не редагуйте застосований changeset

**Чому це неможливо:**
1. Liquibase зберігає **checksum** кожного застосованого changeset у таблицю `DATABASECHANGELOG` кожної БД.
2. При наступному старті він повторює checksum. Якщо SQL-файл змінився — `ChecksumMismatchException` і **застосунок не стартує**.
3. Ручне правки `DATABASECHANGELOG` (видалити рядок, «перезастосувати») руйнує аудиторський слід і, в гіршому випадку, повторює DDL, яке містить `--rollback` — `DROP TABLE`. **Не робіть.**

**Єдиний легальний шлях — новий файл, новий changeset-ідентифікатор:**

```
db/changelog/core/006-<опис>.sql                              ← НОВИЙ ФАЙЛ
db/changelog/db.changelog-master-core.yaml                   ← +1 рядок include
```

### 2.2 Шаблон changeset-у (production-safe)

```sql
--liquibase formatted sql

--changeset split-core:6
--comment Опис: додаємо колонку group_code до permissions
--author: v.yamnyi
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS group_code VARCHAR(64);

--changeset split-core:7
--comment Задати group_code для існуючих рядків (backfill)
UPDATE permissions SET group_code = 'clinical'
WHERE category = 'Клінічні операції' AND group_code IS NULL;

UPDATE permissions SET group_code = 'admin'
WHERE category = 'Адміністрування' AND group_code IS NULL;

--changeset split-core:8
--comment Тепер можемо зробити NOT NULL
ALTER TABLE permissions ALTER COLUMN group_code SET DEFAULT 'clinical';
ALTER TABLE permissions ALTER COLUMN group_code SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_permissions_group_code ON permissions (group_code);

--rollback (для changeset:6)
-- ALTER TABLE permissions DROP COLUMN IF EXISTS group_code;
```

**Правила production-safe SQL:**

| Правило | Приклад | Чому |
|---|---|---|
| **Ідемпотентність** | `ADD COLUMN IF NOT EXISTS`, `ON CONFLICT DO NOTHING` | Може запуститись і на «свіжій» і на «живій» БД |
| **Один changeset = один логічний крок** | `ADD COLUMN` ≠ `UPDATE backfill` ≠ `SET NOT NULL` | `rollback` = один крок; простіше діагностика |
| **`UPDATE/DELETE` з обов'язковим `WHERE`** | `WHERE category='...' AND group_code IS NULL` | Без `WHERE` — вся таблиця |
| **Не `DROP COLUMN/TABLE` одразу** | Див. 2.4 | Backwards-compat з кодом |
| **`CONCURRENTLY` для великих індексів** | `CREATE INDEX CONCURRENTLY` | Не блокує записи (але не працює в транзакції) |
| **Обов'язковий `--rollback`** | Див. вище | Для dev + документаційне значення |
| **UTF-8** | Скрипт — UTF-8 без BOM | `file scripts/*.sql` має показати `UTF-8 Unicode text` |
| **`ON CONFLICT DO NOTHING` на INSERT** | Див. `core/003` | Сид-скріпти вже існують |

**Часті помилки, яких треба уникати:**

- ❌ Редагувати `core/003-role-permissions.sql`, додавши `('PRESCRIPTION_LIST_CREATE', ...)` в INSERT. Правильно — **новий** `core/006-*.sql` з INSERT (див. існуючий `core/005`).
- ❌ `UPDATE permissions SET category = 'Клінічні'` без `WHERE`.
- ❌ Двічі використати `split-core:5`. Правильно — 5, 6, 7.
- ❌ Забути додати `include` у `db.changelog-master-core.yaml` — liquibase просто не побачить новий файл (silent-fail). **Завжди перевіряйте:**
  ```bash
  ls backend/common/src/main/resources/db/changelog/core/   # к-сть файлів
  grep -c "include:" backend/common/src/main/resources/db/changelog/db.changelog-master-core.yaml
  # має бути однаково (кожний файл = один include)
  ```

### 2.3 Повний цикл «зміна БД → продакшн»

```
┌─────────────────────────────────────────────────────────┐
│ STEP 1  PRE-FLIGHT (локально, без тестів)               │
│   - mvn compile (backend)                               │
│   - npm run lint + npx tsc --noEmit (frontend)          │
│   - Огляд changeset: ідемпотентний? --rollback є?       │
│     WHERE є в UPDATE/DELETE?                             │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ STEP 2  РЕАЛІЗАЦІЯ (feature branch)                     │
│   a. db/changelog/<mod>/0NN-<опис>.sql  (новий файл)    │
│   b. db.changelog-master-<mod>.yaml     (+1 include)    │
│   c. Java-код, що використовує нову схему (entity/DTO)  │
│   d. Тести (CI) + інтеграційний тест на порожню БД      │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ STEP 3  CI (GitHub Actions)                             │
│   - format-check   : Checkstyle + oxlint + tsc          │
│   - backend-test   : mvn clean test (Postgres service)  │
│   - backend-int:   : mvn test -Pintegration-test        │
│   - frontend-test  : Vitest + build                     │
│   - e2e-test       : Playwright 59 spec / 11 проектів   │
│   Усі 6 джобів — green.                                  │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ STEP 4  MERGE → TAG / RELEASE                           │
│   - git tag v1.X.Y                                       │
│   - CI-джоб build створює jar + dist артефакти          │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ STEP 5  ВІДСТУП У ПРОДАКШН (див. Part 3)                │
│   1. pg_dump ×4 + changelog snapshot                     │
│   2. Drain traffic (демонстрація / rolling)              │
│   3. Запуск нового JAR                                   │
│   4. Liquibase: нові changeset-и SUCCESS                 │
│   5. Smoke-перевірки                                     │
│   6. Якщо fail → rollback (Appendix E)                   │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ STEP 6  ПІСЛЬ-ПЕРВІРКИ (через 1-24 год)                 │
│   - SELECT * FROM DATABASECHANGELOG WHERE id='<новий>';  │
│   - Бізнес-метрики: логини, помилки, latency            │
│   - Перевірити, що новий функціонал дійсно працює       │
└─────────────────────────────────────────────────────────┘
```

### 2.4 Backwards/forwards compatibility (коли не одночасний деплой)

| Сценарій | Покроково |
|---|---|
| **Додати колонку** | 1) `ADD COLUMN ... NULL` (або `DEFAULT`) 2) Код-реліз, який пише 3) Наступний реліз: `SET NOT NULL` |
| **Переіменувати колонку** | Не `ALTER RENAME`. Додати нову → `UPDATE` копію → код читає нову → **наступний** реліз: `DROP COLUMN old` |
| **Перевести тип** | Те саме: нова колонка → copy → переключити код → DROP old. `ALTER TYPE` на великій таблиці — дорогий + блокує |
| **Додати індекс** | `CREATE INDEX CONCURRENTLY` — не блокує записи |
| **Видалити колонку/таблицю** | Реліз N: код перестає читати. Реліз N+1: DROP. Між ними — спостереження (≥ 1 тиждень) + перевірка по `pg_stat_user_tables.n_tup_read` |
| **Новий RBAC permission** | Новий changeset `INSERT INTO permissions ... ON CONFLICT (code) DO NOTHING`; гранти — Java-сід (`PermissionService`) або changeset `INSERT INTO role_permissions ... ON CONFLICT DO NOTHING` |

### 2.5 Ролбек (коду, без відкату БД)

**Ключова річ:** лічильник у `DATABASECHANGELOG` **не повертається назад** сам по собі. Три шляхи:

| Шлях | Коли | Ризик |
|---|---|---|
| **`pg_restore` з бек-апу** (перед деплоєм) | Кратковчасна відправа + новий changeset «зламаний» | Низький — зберігає точну точку |
| **Forward-fix** (новий changeset, який «скасовує» зміну) | Зміни «не зламані», але неправильні | Низький — ідемпотентно |
| **`liquibase:rollback` / `rollbackCount`** | Тільки в dev, **не** в прод | Високий — `--rollback` SQL не повертає дані; `DROP TABLE` у rollback-скрипті |

**Продакшн-ролбек = `pg_restore` + старий JAR.** Ніяких `liquibase:rollback` у продакшні.

### 2.6 Приклад: повний workflow (конкретний кейс)

> **Задача:** додати колонку `assigned_to_login` у `prescription_items` (med-БД) і прив'язати до користувача, який додав предмет.

```
1. Feature branch
2. backend/common/src/main/resources/db/changelog/med/
     002-prescription-item-assigned-to.sql   (новий)
   --liquibase formatted sql
   --changeset split-med:2
   --comment Add assigned_to_login to prescription_items
   ALTER TABLE prescription_items
     ADD COLUMN IF NOT EXISTS assigned_to_login VARCHAR(64);

   --changeset split-med:3
   --comment Backfill: assign to the user who created the item (created_by → login)
   UPDATE prescription_items pi
      SET assigned_to_login = u.login
      FROM users u
     WHERE pi.created_by = u.id
       AND pi.assigned_to_login IS NULL;

   --changeset split-med:4
   --comment Enforce + index
   ALTER TABLE prescription_items ALTER COLUMN assigned_to_login SET NOT NULL;
   CREATE INDEX CONCURRENTLY IF NOT EXISTS
     idx_prescription_items_assigned_to
     ON prescription_items (assigned_to_login);

   --rollback (split-med:2)
   DROP INDEX IF EXISTS idx_prescription_items_assigned_to;
   ALTER TABLE prescription_items DROP COLUMN IF EXISTS assigned_to_login;
```

```
3. db.changelog-master-med.yaml: +1 include
4. Med entity: @Column(name="assigned_to_login") String assignedToLogin;
   + DTO + controller + frontend type
5. mvn compile OK, tsc OK, oxlint OK
6. git push → CI 6 jobs green
7. Merge to main, tag v1.x.y
8. pg_dump × 4 (backup-before-release-v1.x.y)
9. systemd: systemctl restart ictc
10. У логі:
     Liquibase: ChangeSet med/002-prescription-item-assigned-to.sql::split-med:2 ran successfully
     Liquibase: ChangeSet ... split-med:3 ran successfully
     Liquibase: ChangeSet ... split-med:4 ran successfully
11. Перевірка:
     psql -d my_fullstack_med -c "SELECT count(*) FROM prescription_items WHERE assigned_to_login IS NULL;"  -- 0
     psql -d my_fullstack_med -c "SELECT * FROM DATABASECHANGELOG WHERE id IN ('split-med:2','split-med:3','split-med:4');"
12. Smoke-тести: створити листок, додавати предмет — `assigned_to_login` заповнений
```

**Якщо пішла не так на кроці 10** (наприклад, `UPDATE` без `WHERE` через одрук):
```bash
systemctl stop ictc
pg_restore -c -d my_fullstack_med backup-before-release-v1.x.y.dump
# Відкотити changeset у DATABASECHANGELOG НІ (він не застосовувався — він у fail)
systemctl start ictc  # старий JAR — changeset не застосовувався, DATABASECHANGELOG чистий
# Або: відкотити код в git, зібрати старий JAR, pg_restore, запуск
```

---

## Part 3 — Запобігання втраті даних

### 3.1 Слой 1: резервні копії (обов'язково, до першого деплою)

**Політика 3-2-1:** 3 копії, 2 різні середовища, 1 offsite.

```
📍 Локально (prod-сервер)             — /var/lib/ictc/backups/   (7 днів)
📍 Інший сервер / NAS                 — rsync                   (4 місяці)
📍 Offsite (S3 / B2 / інша дата-центр) — minio / rclone          (1 рік)
```

**Щоденний `pg_dump -Fc`** — [Appendix D](#appendix-d--щоденний-backup-job). Коротко:

```bash
TS=$(date +%Y%m%d-%H%M%S)
for db in my_fullstack_core my_fullstack_icu my_fullstack_med my_fullstack_prosth; do
  pg_dump -Fc --no-owner --no-privileges "$db" > "${BAK}/${db}_${TS}.dump"
  gzip -f "${BAK}/${db}_${TS}.dump" && mv "${BAK}/${db}_${TS}.dump.gz" "${BAK}/${db}_${TS}.dump"
  # Ротація: 7 щоденних
  ls -1t "${BAK}/${db}_"* | tail -n +8 | xargs -r rm -f
done
```

**Чомусь `pg_dump -Fc` (custom format), не `-p` (plain):**
- `pg_restore` може відновити **окремі таблиці** — зручно для діагностики.
- Паралельне відновлення швидше.
- Не залежить від версії `psql` (plain-скрипти можуть містити `SET`-інструкції, які розбігаються).

### 3.2 Слой 2: PITR (Point-In-Time Recovery) — для медичного застосунку

**Карта інтенсивної терапії** — це медичний застосунок (§ТЗ 1.0). Втрати навіть 1 години даних = ризик для пацієнта.

**Рекомендація:** включіть WAL-архівування:

```
# postgresql.conf
wal_level = replica
archive_mode = on
archive_command = 'test ! -f /var/lib/pg-wal/%f && cp %p /var/lib/pg-wal/%f'
max_wal_senders = 4
```

Далі — `pg_basebackup` + `pg_rewind` для standby, або `pgBackRest` (рекомендовано) / `Barman` / `wal-g` для повноцінного PITR.

**Мінімум для продакшну на початку:** `pg_dump` × 4 щохвилини (cron 15-20 хв) **або** `pgBackRest` з повним щодня + інкрементальним щогодини.

### 3.3 Слой 3: перш ніж деплой (чек-лист)

Кожного разу перед деплоєм — **обов'язково**:

```bash
# 1. Бек-ап усіх 4 БД
TS=$(date +%Y%m%d-%H%M%S)
for db in my_fullstack_core my_fullstack_icu my_fullstack_med my_fullstack_prosth; do
  pg_dump -Fc --no-owner --no-privileges "$db" > "/backup/pre-deploy-${TS}-${db}.dump"
done

# 2. Перевірка інтегритету dumps
pg_dump --version
for f in /backup/pre-deploy-${TS}-*.dump; do
  pg_restore --list "$f" > /dev/null && echo "OK: $f" || echo "FAIL: $f"
done

# 3. Сніпшот DATABASECHANGELOG (для діагностики)
for db in my_fullstack_core my_fullstack_icu my_fullstack_med my_fullstack_prosth; do
  psql -d "$db" -c "\dt" > "/backup/pre-deploy-${TS}-${db}-tables.txt"
  psql -d "$db" -c "SELECT id, author, filename, EXTRACT(EPOCH FROM dateexecuted) AS ts FROM DATABASECHANGELOG ORDER BY dateexecuted;" \
        > "/backup/pre-deploy-${TS}-${db}-changelog.txt"
done

# 4. (Бажано) WAL-архів у safe-статусі
pg_basebackup --check > /dev/null && echo "WAL archive OK"
```

**Що робити **не** можна перед деплоєм:**
- ❌ «Ну, changeset-и ідемпотентні, бек-ап не потрібен» — **потрібен**.
- ❌ «Я просто перевірю `SELECT count(*)`» — **потрібен повний dump** (інакше не відновите).
- ❌ «Я зроблю бек-ап після успішного деплою» — тоді якщо деплой «поїхав», **нема** чого відновлювати.

### 3.4 Слой 4: в самому застосунку

| Механізм | Стан у коді | Продакшн |
|---|---|---|
| `@Transactional` + JPA optimistic locking (`@Version`) | ✅ | ✅ |
| `AuditLog` (кожна зміна) | ✅ | ✅ + ретеншн (див. 3.6) |
| `DocumentLockedException` (підписані дні) | ✅ | ✅ |
| `ConstraintViolationException` (JSR-380) → 400 | ✅ | ✅ |
| `VersionConflictException` → 409 | ✅ | ✅ |
| Слід «чию помилку ми зловили» | через `AuditLog.ipAddress` (§79) | + `X-Forwarded-For` у nginx |
| Soft-delete аудиту (§81) | `isDeleted` + `findAllActive()` | ✅ |
| RBAC seed only-once | `PermissionService.seedIfEmpty` | ✅ |
| Seed-data gate | `app.seed-data.enabled` | **`false`** |
| Mock MIS gate | `app.mis.mock-enabled` | **`false`** |
| WireMock MIS gate | `app.mis.wiremock-enabled` (matchIfMissing=true) | **`true`**, якщо real-MIS не готовий; **`false`**, коли готовий |
| `spring.liquibase.enabled` | `false` (у yml) | **`false`** |
| `spring.sql.init.mode` | `never` (у yml) | **`never`** |
| Hibernate DDL | `none` | **`none`** |

### 3.5 Слой 5: мережа / доступ

- **БД за вайфаями:** `listen_addresses = 'localhost'` + окремий хост у приватній мережі. Ніколи не :5432 з публічного IP.
- **TLS:** `?sslmode=require` в JDBC URL + `ssl = on` в `postgresql.conf`.
- **Окремий користувач:** `ictc_app` без `SUPERUSER`, без `TRUNCATE` (якщо можливе).
- **Вогон-стін:** лише :443 (reverse proxy) до застосунку; :8085 — всередині; :5432 — лише між серверами.
- **SSH-keys, не паролі;** `fail2ban` для SSH; `ufw` / `nftables`.

### 3.6 Ретеншн / archiving

| Що | Ретеншн | Примітка |
|---|---|---|
| `audit_logs` | 2 роки (ТЗ §81) | Щомісечний архів → `audit_logs_archive_YYYYMM` |
| `generated_pdfs` (fileData byte[]) | На скільки дозволяє ТЗ | Розглядати міграцію на S3/MinIO — `byte[]` у БД = дорогі бек-апи |
| `evidence_files` (prosth) | 10 МБ ліміт | Розділити на окрему БД / object storage |
| `hourly_records` | 5 років (медикаменти) | + archiving |
| `prescription_executions` | 5 років | |
| `WAL-архів` | 30 днів | Потім → cold storage |

### 3.7 Перевірка «а що, якщо…» (щомісячний drill)

```bash
# 1. Створити scratch-БД
CREATE DATABASE ictc_drill OWNER ictc_app;

# 2. Відновити останній dump
pg_restore -d ictc_drill /backup/last-good-my_fullstack_icu.dump

# 3. Перевірити ключові таблиці
psql -d ictc_drill -c "
  SELECT 'episodes' AS t, count(*) FROM episodes
  UNION ALL SELECT 'clinical_days', count(*) FROM clinical_days
  UNION ALL SELECT 'hourly_records', count(*) FROM hourly_records
  UNION ALL SELECT 'prescription_lists', count(*) FROM prescription_lists;"

# 4. Спробувати запуск застосунку на ЦЮ БД (перевірити, що Liquibase не валить)
APP_DATASOURCE_ICU_URL=jdbc:postgresql://localhost:5432/ictc_drill java -jar app.jar

# 5. Прикрити
DROP DATABASE ictc_drill;
```

**Якщо drill fail** — у вас немає бек-апів, навіть якщо файли є. Файл ≠ бек-ап.

### 3.8 Що **не** робити в продакшні

- ❌ Ручну `psql`-DDL поза changeset-ами («швидко додамо колонку, завтра допишемо changeset»).
- ❌ Редагувати застосований changeset.
- ❌ Дуже важкі `UPDATE` / `DELETE` без `WHERE`.
- ❌ `DROP COLUMN` / `DROP TABLE` в першій релізії, де її ще читає код.
- ❌ `TRUNCATE` (навіть «пусту таблицю») — втратите WAL-позицію.
- ❌ `VACUUM FULL` у робочий час — блокує все, тривалий час.
- ❌ `REINDEX` без `CONCURRENTLY`.
- ❌ Запускати з `APP_SEED_DATA_ENABLED=true` (навіть «разок»).
- ❌ `--liquibase.rollback` у продакшні.
- ❌ Ручне правки `DATABASECHANGELOG` таблиці.
- ❌ Додавати `@SpringBootTest`-тести, які **не** transactional (вони пишуть у БД).

---

## Part 4 — Оперативні процедури (повсякденні)

### 4.1 Щоденно (автоматизуються)

| Час | Що | Хто |
|---|---|---|
| 02:00 | `pg_dump -Fc` ×4 | cron/systemd-timer |
| 02:30 | rsync → NAS | cron |
| 03:00 | rclone → S3/B2 | cron |
| 04:00 | WAL-архів (якщо PITR) | postgres |
| 06:00 | Перевірка диска (df -h) | cron + алерт |
| 06:00 | Перевірка `pg_stat_activity` (длугі запити) | cron + алерт |
| 12:00 | Перевірка `DATABASECHANGELOG` (нові changeset-и?) | cron |
| 18:00 | Перевірка `audit_logs` (аномальна активність) | analyst |

### 4.2 Щотижня (вручну)

- [ ] Огляд alert-ів (Prometheus / Grafana — якщо є)
- [ ] `EXPLAIN ANALYZE` топ-10 запитів — де-нормалізувати / індексувати
- [ ] Перевірка, що `audit_logs` не «з'їдає» диск
- [ ] Огляд `pg_stat_user_tables` — які таблиці пишуться, які читаються (для майбутнього archiving)

### 4.3 Щомісяця (вручну)

- [ ] **Restore drill** (3.7) — **обов'язково**
- [ ] Огляд RBAC matrix — хто що має
- [ ] `pg_basebackup --check` (якщо PITR)
- [ ] Огляд ліцензій / сертифікатів (TLS, Java)
- [ ] Огляд `users` — хтось новий? хтось пішов?

### 4.4 Щоквартали (вручну)

- [ ] Security-audit: `pg_hba.conf`, `ufw`, `fail2ban`, `openssl s_client` на TLS
- [ ] `pg_upgrade` (якщо PostgreSQL оновлюється) — **test first on a clone**
- [ ] Огляд ТЗ — чи не з'явились нові вимоги до ретеншну
- [ ] Chaos drill: вимкнення prod-БД на 30 сек → чи правильно поводиться застосунок (Hikari pool? reconnect? retry?)

### 4.5 Навіщо потрібен «runbook за 5 хвилин» (для deжурного)

Якщо система «пішла» — перші 5 хвилин:

```
1. Перевірити health: curl -f https://domain/api/health
2. Перевірити лог: journalctl -u ictc --since "5 min ago" | tail -50
3. Перевірити БД: psql -d my_fullstack_core -c "SELECT 1" → OK?
4. Перевірити CPU/RAM: top, free -h
5. Якщо БД «заклинила» → pg_stat_activity — які сесії? KILL longest.
6. Якщо застосунок «заклинило» → systemctl restart ictc (без бек-апу — це код, не дані)
7. Якщо дані «зламано» → НЕ restart! → див. Appendix E (rollback)
```

---

## Part 5 — FAQ (типові питання)

**Q: Я можу просто зробити `pg_dump` і закинути в S3?**
A: Так для **копії**. Для **PITR** — ні, потрібен WAL-архів (3.2). Для медичного застосунку — мінімум PITR.

**Q: А якщо я «просто» додам колонку вручну через psql, а changeset допиши завтра?**
A: Ні. Liquibase на наступному старті **не** побачить цю зміну (checksum збігається, бо changeset не застосовувався). Але при наступному changeset-і (наприклад, `ADD INDEX ON new_column`) — liquibase **не знає** про колонку, бо вона не у changeset-і. Результат: непередбачувані стану на різних серверах (хто який changeset застосував). **Завжди changeset → git → CI → деплой.**

**Q: Я змінив `003-role-permissions.sql`, додавши новий рядок у INSERT. Чому це «погано»?**
A: Чому це «погано», пояснено в 2.1. Коротко: (1) checksum зламається, (2) ви не знаєте, хто і коли додав, (3) ви порушуєте аудиторський слід, (4) `ON CONFLICT DO UPDATE` (який у `003`) перезапише label/description, які хтось вручну змінив — втрата даних.

**Q: А `ON CONFLICT DO UPDATE` у `003` — це безпечно?**
A: У dev-середовищі — так (це як «auto-heal» від кириличного break в `data-med.sql`, див. AGENTS.md). У продакшні — **небезпечно**, бо: (1) перезаписує `label`/`description`, які оператор міг вручну відредагувати, (2) `ON CONFLICT DO UPDATE` = «ти не знав, що там уже було». Нові changeset-и **завжди** `ON CONFLICT DO NOTHING`.

**Q: Я хочу повернути стару версію застосунку. Як?**
A: `systemctl stop ictc; pg_restore -c -d ...; systemctl start ictc` (старий JAR). Детальніше — [Appendix E](#appendix-e--rollback-runbook).

**Q: Я хочу додати RBAC permission. Через UI чи через SQL?**
A: **Через UI** (admin → «Доступи та ролі» → toggle). UI викликає `PUT /api/admin/permissions`, який пише у `role_permissions` + аудитує. **Поза UI** — тільки якщо ви сидите за `psql` і точно знаєте, що робите (наприклад, при першому сіді). За замовчуванням `PermissionService.seedIfEmpty` створює matrix — не чіпайте його вручну, якщо не розбираєтесь.

**Q: А якщо я «просто» зроблю `ALTER TABLE ... ADD COLUMN` через psql у продакшні — і все?**
A: Так — і **втрата** синхронізації з іншими серверами (якщо є) + втрата аудиторського слід + checksum-конфлікт при наступному changeset-і. Правильний шлях — changeset (2.2).

**Q: Лічильник `DATABASECHANGELOG` — я можу видалити рядок і «перезастосувати»?**
A: Ні. Це те саме, що «видалити рядок з журналу аудиту». Правильно — forward-fix (2.5).

**Q: А якщо changeset-файл «зламаний» через одрук (UPDATE без WHERE)?**
A: (1) Не запускати знову. (2) `pg_restore` з 3.3. (3) Поправити в git. (4) Знову CI → деплой. **Не** правити `DATABASECHANGELOG`.

**Q: Як перевірити, що всі changeset-и застосовано?**
A: `SELECT id, author, filename, dateexecuted FROM DATABASECHANGELOG ORDER BY dateexecuted;` — має бути рівна к-сть з `grep -c "changeset" db/changelog/<mod>/*.sql`.

**Q: А якщо в продакшні вже є дані, а в новому changeset-і `INSERT INTO permissions VALUES ('NEW_CODE', ...)` — і я запущу з `APP_SEED_DATA_ENABLED=true`?**
A: `data-core.sql` має `ON CONFLICT (id) DO NOTHING` (для `users`) + `ON CONFLICT (code) DO UPDATE` (для `permissions`) → перезапише label/description. **Вимкніть seed в проді**, інакше — втрата вручних змін.

**Q: Скільки часу тримає перший запуск з liquibase?**
A: 30-90 сек на порожній БД (4 БД × ~6 changeset-ів + index creation). Наступні — 15-25 сек (лише checksum-перевірки).

**Q: А якщо я «просто» видалю файл `data-*.sql` — і все?**
A: Ні. `SeedDataInitializer` — `@ConditionalOnProperty(app.seed-data.enabled=true, matchIfMissing=true)`. Вимкніть через змінну, не видаляйте файл (dev-середовище ще потребує).

**Q: Я хочу зробити PITR, але PostgreSQL 16 вже «старий»?**
A: PostgreSQL 16 — LTS, підтримується до 2028. Питання не «старий», а «чи є WAL-архів». Див. 3.2.

**Q: А `liquibase.enabled: false` у `application.yml` — це безпечно?**
A: Так. Spring Boot **не** автоматично запускає liquibase — 4 ручні beans (`coreLiquibase` тощо) роблять це. `spring.liquibase.enabled` стосується **автоматичного** запуску, якого ми не використовуємо.

**Q: А `spring.sql.init.mode: never`?**
A: Те саме — ми не використовують `schema.sql` / `data.sql` (Spring Boot SQL init). Seed — через `SeedDataInitializer`.

**Q: А «multi-datasource» — це надійно?**
A: Так — кожен модуль має свій EMF, свій transaction manager, свої repository. Не перемішуйте — ArchUnit (`ModuleBoundaryTest`) стежить, що `medication-sheet` не імпортує `icu-chart`.

**Q: А якщо я «просто» запущу два JAR (rolling) — і один на старій схемі, один на новій?**
A: Це «двостені» (two-tier) деплой. **Можна**, ТІЛЬКИ якщо зміна схемі backwards-compatible (2.4). Коли змінюється структура (наприклад, `DROP COLUMN`), **не** можна — один JAR читає колонку, якої немає в БД іншого.

**Q: А якщо я хочу зробити blue-green?**
A: Дві повні інстанси (JAR + БД). DNS-переключення. Довго + дорого, але найнадійніше. Для медичного застосунку — розгляньте.

**Q: А якщо я хочу zero-downtime?**
A: Backwards-compatible changes (2.4) + rolling + health-check. **Не** `DROP COLUMN` у першій релізії.

**Q: А якщо я хочу `pg_upgrade`?**
A: 1. Backup. 2. `pg_basebackup` на новий сервер. 3. `pg_upgrade` (offline) / `pglogical` (online). 4. Переключення DNS. **Test first.**

**Q: А якщо я хочу вимкнути Swagger в проді?**
A: `SPRING_PROFILES_ACTIVE=prod` вже вимикає (див. `application.yml:163-166`). Якщо не хочете — `app.mis.wiremock-enabled=false` + власна `prod`-профіль.

**Q: А `MockMisServiceImpl` у проді — це «так»?**
A: Ні. `APP_MIS_MOCK_ENABLED=false`. `WireMockMisServiceImpl` — теж mock (попри назву). `RealMisServiceImpl` — TODO.

---

## Appendix A — Шаблони змінних оточення

### A.1 `/etc/environment-file-ictc` (systemd `EnvironmentFile=`)

```bash
# === ICU Patient Chart — PRODUCTION ===
# Вимірювання: APP_DATASOURCE_CORE_URL → app.datasource.core.url
# (spring boot relaxed binding: _ → ., uppercase)

# --- Profile ---
SPRING_PROFILES_ACTIVE=prod

# --- Datasources (4 БД) ---
APP_DATASOURCE_CORE_URL=jdbc:postgresql://db.internal:5432/my_fullstack_core?stringtype=unspecified&sslmode=require
APP_DATASOURCE_CORE_USERNAME=ictc_app
APP_DATASOURCE_CORE_PASSWORD=<з vault>

APP_DATASOURCE_ICU_URL=jdbc:postgresql://db.internal:5432/my_fullstack_icu?stringtype=unspecified&sslmode=require
APP_DATASOURCE_ICU_USERNAME=ictc_app
APP_DATASOURCE_ICU_PASSWORD=<з vault>

APP_DATASOURCE_MED_URL=jdbc:postgresql://db.internal:5432/my_fullstack_med?stringtype=unspecified&sslmode=require
APP_DATASOURCE_MED_USERNAME=ictc_app
APP_DATASOURCE_MED_PASSWORD=<з vault>

APP_DATASOURCE_PROSTH_URL=jdbc:postgresql://db.internal:5432/my_fullstack_prosth?stringtype=unspecified&sslmode=require
APP_DATASOURCE_PROSTH_USERNAME=ictc_app
APP_DATASOURCE_PROSTH_PASSWORD=<з vault>

# --- JWT ---
# openssl rand -base64 64
APP_JWT_SECRET=<64-byte base64>
APP_JWT_EXPIRATION-MS=86400000

# --- Datasource pool (Hikari) ---
SPRING_DATASOURCE_HIKARI_MAXIMUM-POOL-SIZE=20
SPRING_DATASOURCE_HIKARI_MINIMUM-IDIKE-CONNECTIONS=5
SPRING_DATASOURCE_HIKARI_CONNECTION-TIMEOUT=30000

# --- Seed-data (КРИТИЧНО: false в проді) ---
APP_SEED_DATA_ENABLED=false

# --- MIS (якщо real-не готовий) ---
APP_MIS_MOCK_ENABLED=false
APP_MIS_WIREMOCK_ENABLED=true
APP_MIS_WIREMOCK-URL=https://mis.internal/api
APP_MIS_INSTALLATION-GUID=<GUID>
APP_MIS_LOGIN=integration

# --- LDAP / Active Directory (read-only bind; вимкнено за замовчуванням) ---
# Увімкнути тільки якщо застосунок повинен приймати корпоративні обліковки.
# Каталог використовується лише для bind/search/read; записів у AD немає.
# TLS — через системний JVM truststore; service account потребує лише прав
# на пошук і bind (без прав на запис). Значення — тільки з vault.
APP_LDAP_ENABLED=true
APP_LDAP_URLS=ldaps://ad.corp:636
APP_LDAP_BASE=dc=corp,dc=local
APP_LDAP_USERNAME=<bind-DN з vault>
APP_LDAP_PASSWORD=<з vault>

# --- Mail ---
SPRING_MAIL_HOST=smtp.internal
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=<user>
SPRING_MAIL_PASSWORD=<pass>
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true

# --- Logging ---
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_SUPERHUMANS=INFO
LOGGING_LEVEL_ORG_HIBERNATE=INFO
SPRING_LIQUIBASE_LOG_LEVEL=WARN

# --- Swagger (вимкнений у prod-proфілі, але надійно) ---
SPRINGDOC_API-DOCS_ENABLED=false
SPRINGDOC_SWAGGER-UI_ENABLED=false

# --- Misc ---
SERVER_PORT=8085
SERVER_COMPRESSION_ENABLED=true
SERVER_COMPRESSION_MIME-TYPES=application/json,text/html,text/xml
```

### A.2 `.env` (docker-compose, якщо є)

```bash
# .env
COMPOSE_PROJECT_NAME=ictc
POSTGRES_VERSION=16
POSTGRES_USER=ictc_app
POSTGRES_PASSWORD=<з vault>
POSTGRES_DB_INIT=true

# БД (docker-compose створює 4)
DB_PORT=5432
JVM_XMS=1g
JVM_XMX=2g

# Frontend
VITE_API_BASE=/api
```

### A.3 `docker-compose.yml` (опційно)

```yaml
services:
  db-core:
    image: postgres:16
    environment:
      POSTGRES_USER: ictc_app
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      POSTGRES_DB: my_fullstack_core
    volumes: [db-core-data:/var/lib/postgresql/data]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ictc_app -d my_fullstack_core"]

  # (analogously: db-icu, db-med, db-prosth)

  backend:
    build: ./backend
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - APP_DATASOURCE_CORE_URL=jdbc:postgresql://db-core:5432/my_fullstack_core
      - APP_DATASOURCE_CORE_USERNAME=ictc_app
      - APP_DATASOURCE_CORE_PASSWORD=${POSTGRES_PASSWORD}
      # ... (4 джерел)
      - APP_SEED_DATA_ENABLED=false
    depends_on:
      db-core: { condition: service_healthy }
      # ...
    restart: unless-stopped

  frontend:
    image: nginx:alpine
    volumes:
      - ./frontend/dist:/usr/share/nginx/html:ro
      - ./nginx/ictc.conf:/etc/nginx/conf.d/ictc.conf:ro
    depends_on: [backend]

volumes:
  db-core-data:
  # ...
```

---

## Appendix B — systemd unit

```ini
# /etc/systemd/system/ictc.service
[Unit]
Description=ICU Patient Chart (Spring Boot)
After=network.target postgresql.service
Wants=postgresql.service

[Service]
Type=simple
User=ictc
Group=ictc

# Secrets / config — не у command line (видно в `ps`)
EnvironmentFile=/etc/environment-file-ictc

# JVM
ExecStart=/usr/bin/java \
  -Xms1g -Xmx2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/lib/ictc/logs/heapdump.hprof \
  -Djava.security.egd=/dev/urandom \
  -jar /opt/ictc/app.jar \
  --spring.profiles.active=prod

# Limits
LimitNOFILE=65536
LimitNPROC=32768

# Auto-restart (з backoff)
Restart=on-failure
RestartSec=5
StartLimitIntervalSec=60
StartLimitBurst=5

# Hardening (не «over-do»)
NoNewPrivileges=true
ProtectSystem=strict
ProtectHome=true
PrivateTmp=true
PrivateDevices=true
RestrictSUIDSGID=true
RestrictRealtime=true
LockPersonality=true
MemoryDenyWriteExecute=true
SystemCallArchitectures=native

# Logs
StandardOutput=journal
StandardError=journal
SyslogIdentifier=ictc

# Working dir (для relative paths)
WorkingDirectory=/var/lib/ictc

# Hard timeout на start
TimeoutStartSec=120
TimeoutStopSec=60

[Install]
WantedBy=multi-user.target
```

**Керування:**
```bash
sudo systemctl daemon-reload
sudo systemctl enable --now ictc
sudo systemctl status ictc
journalctl -u ictc -f
systemctl restart ictc
systemctl stop ictc
```

**Health-check (systemd 240+):**
```bash
# /etc/systemd/system/ictc.service.d/override.conf
[Service]
ExecStartPost=/usr/bin/curl -fsS http://localhost:8085/api/health || true
```

---

## Appendix C — nginx reverse proxy

```nginx
# /etc/nginx/conf.d/ictc.conf
upstream ictc_backend {
    server 127.0.0.1:8085;
    # Для multi-instance:
    # server 10.0.0.11:8085;
    # server 10.0.0.12:8085;
    keepalive 32;
}

server {
    listen 80;
    server_name ictc.example.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name ictc.example.com;

    # TLS
    ssl_certificate     /etc/letsencrypt/live/ictc.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/ictc.example.com/privkey.pem;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;
    ssl_session_cache   shared:SSL:10m;
    ssl_session_timeout 10m;

    # Headers
    add_header Strict-Transport-Security "max-age=63072000; includeSubDomains" always;
    add_header X-Content-Type-Options nosniff;
    add_header X-Frame-Options DENY;
    add_header Referrer-Policy strict-origin-when-cross-origin;

    # Frontend (статика)
    location / {
        root /var/www/ictc;
        try_files $uri $uri/ /index.html;
        index index.html;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # API
    location /api/ {
        proxy_pass http://ictc_backend;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;   # ← КРИТИЧНО для AuditLog.ipAddress
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
        client_max_body_size 20m;   # evidence files (10 MB) + PDF
    }

    # Swagger — вимкнений у prod, але на всяк випадок
    location ~ ^/(swagger-ui|api-docs) {
        return 404;
    }

    # Rate-limit (опційно)
    # limit_req zone=api burst=20 nodelay;
}
```

**Перевірка:**
```bash
nginx -t
sudo systemctl reload nginx
curl -I https://ictc.example.com/api/patients
```

---

## Appendix D — Щоденний backup job

### D.1 `pg_dump` скрипт

```bash
#!/usr/bin/env bash
# /usr/local/bin/ictc-backup.sh
set -euo pipefail

LOG=/var/log/ictc-backup.log
BAK_LOCAL=/var/lib/ictc/backups
BAK_REMOTE=nas:  # rsync destination
BAK_S3=ictc-prod  # rclone remote

TS=$(date +%Y%m%d-%H%M%S)
DBS=(my_fullstack_core my_fullstack_icu my_fullstack_med my_fullstack_prosth)

mkdir -p "${BAK_LOCAL}"

log() { echo "[$(date -Is)] $*" | tee -a "${LOG}"; }

log "=== Starting backup ${TS} ==="

for db in "${DBS[@]}"; do
  f="${BAK_LOCAL}/${db}_${TS}.dump"
  if ! pg_dump -Fc --no-owner --no-privileges "$db" > "$f" 2>> "${LOG}"; then
    log "ERROR: pg_dump failed for ${db}"
    exit 1
  fi
  log "OK: ${db} → ${f} ($(du -h "$f" | cut -f1))"
done

# Ротація: 7 щоденних
for db in "${DBS[@]}"; do
  ls -1t "${BAK_LOCAL}/${db}_"*.dump 2>/dev/null | tail -n +8 | xargs -r rm -f
done

# → NAS (rsync)
rsync -a --delete "${BAK_LOCAL}/" "nas:/backups/ictc/" 2>> "${LOG}" \
  && log "OK: rsync → NAS" \
  || { log "ERROR: rsync failed"; exit 1; }

# → S3 (rclone)
rclone copy "${BAK_LOCAL}" "${BAK_S3}/daily" --transfers 4 2>> "${LOG}" \
  && log "OK: rclone → S3" \
  || { log "ERROR: rclone failed"; exit 1; }

# Перевірка інтегритети (тилового) — на останньому
LAST=$(ls -1t "${BAK_LOCAL}/${DBS[0]}"_*.dump | head -1)
pg_restore --list "$LAST" > /dev/null 2>&1 \
  && log "OK: integrity check passed" \
  || { log "ERROR: dump corrupt"; exit 1; }

log "=== Backup complete ${TS} ==="
```

### D.2 `systemd timer`

```ini
# /etc/systemd/system/ictc-backup.service
[Unit]
Description=ICU Patient Chart Daily Backup

[Service]
Type=oneshot
User=ictc
ExecStart=/usr/local/bin/ictc-backup.sh
```

```ini
# /etc/systemd/system/ictc-backup.timer
[Unit]
Description=Run ICU backup daily at 02:00

[Timer]
OnCalendar=*-*-* 02:00:00
Persistent=true

[Install]
WantedBy=timers.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now ictc-backup.timer
systemctl list-timers ictc-backup.timer
```

### D.3 `pgBackRest` (для PITR — рекомендовано)

```ini
# /etc/pgbackrest.conf
[ictc-prod]
pg1-path = /var/lib/postgresql/16/main
pg1-port = 5432
repo1-path = /var/lib/pgbackrest
repo1-retention-full = 4
repo1-retention-diff = 14
repo1-retention-arch = 1
archive-timestamp = y
process-max = 4
```

```xml
<!-- PostgreSQL 16 postgresql.conf -->
wal_level = replica
archive_mode = on
archive_command = 'pgbackrest archive-push %p'
```

**Щоденний full + щогодинний diff:**
```bash
pgbackrest --stanza=ictc-prod backup --type=full
pgbackrest --stanza=ictc-prod backup --type=diff   # щогодини через cron
pgbackrest --stanza=ictc-prod verify                # щонеділі
```

**Restore:**
```bash
pgbackrest --stanza=ictc-prod restore \
  --type=time --target="2026-08-20 12:34:00+03" \
  --target-tie=latest
```

---

## Appendix E — Rollback runbook

### E.1 Коли rollback?

- ❌ Liquibase changeset **зламав** старт (`ChecksumMismatchException`, `SQLSyntaxError`).
- ❌ Бізнес-логіка «поїхала» (наприклад, `UPDATE` без `WHERE` через одрук).
- ❌ Регресія в коді (новий JAR).
- ❌ Performance regression (новий changeset додати slow-запити).
- ✅ Не rollback: ліцензійні питання, security-patch (виправте forward).

### E.2 Кроки (покроково)

```bash
# 0. ОЦІНИ: що саме пішло не так? (див. 5.x FAQ)
#    - Код? (→ E.2, E.3)
#    - Data? (→ E.3, E.4)
#    - Обидва? (→ E.4)

# ========================================================================
# E.2. CODE ROLLBACK (дані не пошкоджені)
# ========================================================================
systemctl stop ictc

# Старий JAR має бути на диску (версія v1.x.y-1)
ls -la /opt/ictc/releases/
#   icu-1.10.0.jar
#   icu-1.10.1.jar   ← поточний (зламаний)
#   icu-1.9.9.jar    ← останній відомий «good»
#
# Якщо є «last good» — просто поверніть:
ln -sfn /opt/ictc/releases/ictc-1.9.9.jar /opt/ictc/app.jar

systemctl start ictc
journalctl -u ictc -f
# Перевірити health + 3-5 бізнес-флу

# ========================================================================
# E.3. DATA ROLLBACK (дані пошкоджені, код неважливий)
# ========================================================================
systemctl stop ictc

# Знайти останній «good» dump
ls -la /backup/pre-deploy-*-my_fullstack_*.dump
#   pre-deploy-20260820-090000-my_fullstack_core.dump   ← перед деплоєм
#   pre-deploy-20260820-090000-my_fullstack_icu.dump
#   pre-deploy-20260820-090000-my_fullstack_med.dump
#   pre-deploy-20260820-090000-my_fullstack_prosth.dump

# Перевірити інтегритет
for f in /backup/pre-deploy-20260820-090000-*.dump; do
  pg_restore --list "$f" > /dev/null && echo "OK: $f"
done

# Відновити (POSSIBLE: це НЕ атомарно для 4 БД — тримайте в mind)
for db in my_fullstack_core my_fullstack_icu my_fullstack_med my_fullstack_prosth; do
  psql -d postgres -c "DROP DATABASE IF EXISTS ${db};"
  createdb -O ictc_app "${db}"
  pg_restore --clean --if-exists --no-owner --no-privileges \
    -d "${db}" "/backup/pre-deploy-20260820-090000-${db}.dump"
done

# ========================================================================
# E.4. CODE + DATA ROLLBACK (обидва)
# ========================================================================
# = E.2 + E.3 в одному порядку:
systemctl stop ictc
# → E.3 (data)
# → E.2 (code: ln -sfn last-good-jar)
systemctl start ictc
```

### E.3 Часті помилки при rollback

| Помилка | Наслідок | Фікс |
|---|---|---|
| `pg_restore -c` на «займаний» БД | `ERROR: database is being accessed by other users` | `systemctl stop ictc` + `pg_terminate_backend` |
| Втрачений `--clean --if-exists` | `ERROR: table already exists` | Додайте `--clean --if-exists` |
| `pg_restore` без `--no-owner` | `ERROR: must be owner` | `--no-owner --no-privileges` |
| Забули відновити `DATABASECHANGELOG` | Старий changeset «застосований» →ChecksumMismatch на старті | **Не забувайте** — `DATABASECHANGELOG` теж у dump |
| `pg_restore` 4 БД по черзі | Між БД — застосунок «не бачить» повну картину | Зупинити застосунок, відновити, запускати |
| Rollback у «робочий час» | Пацієнти / лікарі чують downtime | Планувати на low-traffic (ноч/вечір) |

### E.4 Перевірка після rollback

```bash
# 1. Health
curl -fsS https://domain/api/health
# 2. Бізнес-флу (5-10 хвилин)
# 3. Перевірити, що DATABASECHANGELOG «чистий» від зламаних changeset-ів
for db in my_fullstack_core my_fullstack_icu my_fullstack_med my_fullstack_prosth; do
  psql -d "$db" -c "SELECT id, author, filename, dateexecuted FROM DATABASECHANGELOG ORDER BY dateexecuted DESC LIMIT 5;"
done
# 4. Аудит — хтось «бачив» помилку?
psql -d my_fullstack_core -c "SELECT * FROM audit_logs WHERE action LIKE '%ERROR%' ORDER BY timestamp DESC LIMIT 10;"
```

### E.5 Forward-fix (замість rollback)

Якщо rollback **неможливий** (наприклад, дані вже «записані» в новий формат), **forward-fix** — новий changeset, який «скасовує» зміну:

```sql
--liquibase formatted sql
--changeset split-core:7
--comment Revert split-core:6 (group_code was a mistake)
ALTER TABLE permissions DROP COLUMN IF EXISTS group_code;
--rollback ALTER TABLE permissions ADD COLUMN IF NOT EXISTS group_code VARCHAR(64);
```

Це «правильно» з точки зору лічильника: 1 → 2 → 3 → … → 6 (зламано) → **7 (скасовує 6)** → 8 → …

---

## Зв'язок з AGENTS.md

Цей runbook **доповнює** AGENTS.md, а не замінює його:
- AGENTS.md — **як розробляти** (локальний цикл, CI, тести).
- Цей runbook — **як експлуатувати** (продакшн, БД, безпека).

Якщо є конфлікт — AGENTS.md має пріоритет для коду, цей runbook — для эксплуатации.

## Зв'язок з ТЗ

- §30 (SOFA inputs) — див. AGENTS.md «2026-07-31».
- §46-§53 ( clinical validation) — див. AGENTS.md «Compliance Fixes».
- §79-§81 (audit, IP, soft-delete) — див. AGENTS.md.
- §94, §98 (PDF transfer, MIS audit) — див. AGENTS.md.

## Зв'язок з CI

CI pipeline (`.github/workflows/playwright.yml`) — **не** деплоює. Це **лише** CI (6 джобів: format-check, backend-test, backend-integration, frontend-test, e2e-test, build). Для продакшну потрібен **окремий** pipeline (GitHub Actions / GitLab CI / self-hosted), який:
1. Збірає JAR + dist (як `build` джоб).
2. Деплоює (через systemd / docker / k8s).
3. Health-check.
4. Smoke-тести.
5. Алерт (Telegram / email / PagerDuty).

Див. AGENTS.md «Repeatable CI Development Workflow (THE Loop)» — це **dev-loop**, не deployment.

## Додаткові ресурси

- [Liquibase docs](https://docs.liquibase.com/) — changeset, checksum, rollback
- [PostgreSQL docs: pg_dump](https://www.postgresql.org/docs/16/app-pgdump.html)
- [PostgreSQL docs: PITR](https://www.postgresql.org/docs/16/continuous-archiving.html)
- [Spring Boot externalized config](https://docs.spring.io/spring-boot/docs/current/reference/html/externalized-configuration.html)
- [systemd service](https://www.freedesktop.org/software/systemd/man/systemd.service.html)
- [nginx reverse proxy](https://docs.nginx.com/nginx/admin-guide/web-server/reverse-proxy/)

---

*Кінець документа.*
