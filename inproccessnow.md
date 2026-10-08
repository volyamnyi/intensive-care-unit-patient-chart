# HTTPS для supercare.superhumans.com — опис GitHub Issues

Мета: `https://supercare.superhumans.com` працює **виключно у внутрішній корпоративній мережі**.
Без зовнішнього розгортання, без публічного DNS, без Let's Encrypt.

Зафіксовані рішення (корекція власника 07.10.2026 — див. #346, тіло #344):
- TLS термінує **nginx** публічним **Let's Encrypt**-сертифікатом (ECDSA, до 05.01.2027; випуск/поновлення DNS-01 через Cloudflare API, `certbot.timer` увімкнено).
- Cloudflare Origin для прямого використання ВІДХИЛЕНО (клієнти `ERR_CERT_AUTHORITY_INVALID`, встановлення кореня заборонено); файли лишаються на диску як резерв.
- Публічного DNS нема (NXDOMAIN зовні — підтверджено, це і є доказ недоступності ззовні).
- Вбудований SSL Spring Boot вимкнено.
- Frontend — same-origin через nginx (`dist/` + проксі `/api`), JWT cookie `HttpOnly; Secure; SameSite=Lax`.
- Перший деплой: VM + systemd + JAR за `docs/Production-Deployment-Runbook.md`. Без Docker/K8s. CI не деплоїть.
- Жодних секретів у Issues/PR/логах — лише імена змінних (`APP_JWT_SECRET`, `APP_DATASOURCE_*`, `APP_MIS_API_*`, `APP_LDAP_*`).

## Карта issues

| Issue | Зміст | Виконавець | Стан |
|---|---|---|---|
| Epic #344 | https://supercare.superhumans.com, тільки внутрішня мережа | координація | OPEN |
| P0 #345 | Кодова готовність до nginx-термінації | локальна розробка (@volyamnyi) | OPEN, код готовий локально |
| P1 #346 | Внутрішній DNS + TLS-сертифікат (LE, корекція 07.10.2026) | IT-відділ + прод | DONE (закривається) |
| P2 #347 | Артефакти збірки (суперседовано прод-збіркою з `5456a2ee`) | локальна розробка готує → прод приймає | DONE (закривається) |
| P3 #348 | Деплой і smoke на прод-VM | прод | OPEN |
| P4 #349 | Бекапи, drill, доки, закриття epic | локальна розробка (@volyamnyi) | Доки DONE; drill/щоденні дампи → follow-up (закривається) |

Порядок: #345 → #346 → #347 → #348 → #349. P3 не стартує без закритих P0–P2.

## Epic #344

https://github.com/volyamnyi/intensive-care-unit-patient-chart/issues/344

Групуючий issue: рішення, фази, наскрізні правила (Liquibase — єдиний шлях змін схеми; MIS тільки читання; LDAP тільки bind/search/read; `APP_SEED_DATA_ENABLED=false`; деструктивні дії лише після явного «так» оператора), exit criteria (https зсередини 200 + HSTS, `Secure` cookie, реальний аудит-IP, недоступність ззовні, бекапи + drill, оновлений runbook).

## P0 #345 — Кодова готовність до nginx-термінації

https://github.com/volyamnyi/intensive-care-unit-patient-chart/issues/345

Виконавець: локальна розробка. Мітки: `backend`, `security`, `testing`.

Зміст:
- `prod`-профіль `application.yml`: вбудований SSL вимкнено, `server.forward-headers-strategy: framework` (щоб `request.isSecure()` бачив зовнішній https через `X-Forwarded-Proto`).
- `CorsConfig`: точний allowlist з `APP_CORS_ALLOWED_ORIGINS` замість wildcard `*`.
- `/ws` origins через `APP_WEBSOCKET_ALLOWED_ORIGINS`.
- Logout clear-cookie дзеркалить `Secure`/`SameSite=Lax`.
- `CorsConfigTest` переписано під allowlist-контракт. Frontend перевірено (відносний `/api`, без змін).

Стан: код виконано локально (коміти `d241fa6` — прибрані `admin`-паролі БД med/prosth, env-only; `c769c3e` — готовність до nginx-термінації; `e322b86b` — tripwire-сумісний фейк-хост у тесті). Запушено; CI: ран `37613980713` — 5/6 зелено, E2E 420/421 (один флейк `prescription-medication-cancel.spec.ts:149`, доведено флейком); перепрогон `37617949060` на тому ж head — ALL GREEN. P0 CI-гейт пройдено.

## P1 #346 — Внутрішній DNS + корпоративний сертифікат

https://github.com/volyamnyi/intensive-care-unit-patient-chart/issues/346

Виконавці: IT-відділ + прод. Мітки: `security`, `documentation`.

Зміст:
- IT-відділ: внутрішній `A supercare.superhumans.com → <внутрішній IP>` (без публічного запису); fullchain + key від корпоративного CA (бажано wildcard, інакше окремий сертифікат); за потреби — корінь CA на клієнти через групові політики.
- Прод: перевірка (`nslookup` зсередини, `openssl x509` — subject/issuer/строки, фіксація серійника та джерела без ключа).
- Certbot / Let's Encrypt не ставити взагалі. Самопідпис — лише крайній тимчасовий варіант за письмовою згодою власника.

Прийняття: DNS резолвиться всередині, ззовні — ні (слово оператора); сертифікат валідний, шляхи файлів зафіксовано. Без цього P3 не стартує.

Стан (07.10.2026, статус зафіксовано в #346 + корекція власника там же): перевірка прод — DONE. DNS: `supercare.superhumans.com → 192.168.24.49` зсередини; зовні — NXDOMAIN (підтверджено власником, закриває пункт «слово оператора»). Сертифікат: Let's Encrypt ECDSA до 05.01.2027, DNS-01 через Cloudflare API, `certbot.timer` увімкнено; curl без `--insecure` → 200 (звіт P3). Origin-файли — лише резерв на диску. Початковий Origin-звіт (serial/issuer/90-денний термін) — історичний, див. #346.

## P2 #347 — Артефакти збірки всередину мережі

https://github.com/volyamnyi/intensive-care-unit-patient-chart/issues/347

Виконавець: локальна розробка готує (assigned @volyamnyi) → прод приймає. Мітки: `backend`, `documentation`.

Зміст: на прод-VM може не бути виходу в інтернет, тому збірка — зовні, доставка — артефактами (JAR за зразком CI `build`-job + `frontend/dist/` з `VITE_API_BASE=/api`). Локальна розробка фіксує коміт-SHA та SHA256 обох артефактів; прод звіряє SHA, розкладає за канонічними шляхами (`/opt/ictc/releases/`, `/var/www/ictc`). Бінарники в git не комітити. Канал передачі (VPN-scp / внутрішній файлообмінник) організує власник.

Стан (07–08.10.2026): локальна збірка DONE (деталі — коментар у #347), АЛЕ прод зібрав власні артефакти з `5456a2ee` і задеплоїв їх (звіт P3) — локальний бандл не передавався і більше не потрібен. Намір фази (розгортні артефакти на проді) виконано прод-збіркою. Закривається як superseded.

## P3 #348 — Деплой і smoke на прод-VM

https://github.com/volyamnyi/intensive-care-unit-patient-chart/issues/348

Виконавець: прод. Мітки: `backend`, `database`, `security`.

Зміст: інвентаризація; firewall лише для LAN/VPN; PostgreSQL 16 (роль `ictc_app` + 4 БД, `pg_dump` порожніх БД до старту); env `/etc/environment-file-ictc` (значення — з vault, без `APP_TEST_*`/e2e-stub); systemd `ictc.service`; nginx (`server_name supercare.superhumans.com`, корпоративний сертифікат з P1, `X-Forwarded-For/Proto/Host`, swagger-шляхи в 404). Smoke з LAN-машини без PII: `https://` → 200 + HSTS, корпоративний ланцюжок, `Secure`-cookie як доказ `X-Forwarded-Proto`, реальний аудит-IP, `permissions`/`users` за очікуваннями, недоступність ззовні словом оператора. Стоп-умови: `ChecksumMismatchException`, `DATABASECHANGELOGLOCK`, розбіжність міграцій, падіння guard seed/JWT, відсутність `Secure`, недоступний MIS. Жодних `liquibase:rollback` / ручного DDL / правок `DATABASECHANGELOG`.

## P4 #349 — Бекапи, drill, доки, закриття epic

https://github.com/volyamnyi/intensive-care-unit-patient-chart/issues/349

Виконавець: локальна розробка (assigned @volyamnyi). Мітки: `documentation`, `database`.

Зміст: коміт доків (runbook Appendix C під домен + Cloudflare Origin без certbot/LE, Appendix A — `APP_WEBSOCKET_ALLOWED_ORIGINS`, запис у `AGENTS.md`, перевірка README без обіцянок публічного доступу); координація щоденних `pg_dump -Fc` ×4 + внутрішній offsite + перший restore-drill на scratch-БД (дата, тривалість, OK — коментарем); перевірка exit criteria epic; закриття P0–P4, потім epic.

Стан (07–08.10.2026): доки DONE (runbook Appendix C + §1.8 + Appendix A, AGENTS.md, цей файл; 08.10 скориговано з Origin на LE DNS-01). README чистий. Бекапи: pre-P3 дампи ×4 зроблено прод (звіт P3, `pg_restore --list` OK) — щоденні дампи + перший drill НЕ підтверджено записами → винесено у follow-up. Закривається з цим застереженням.
