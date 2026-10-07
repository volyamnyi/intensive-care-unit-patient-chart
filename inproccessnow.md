# HTTPS для supercare.superhumans.com — опис GitHub Issues

Мета: `https://supercare.superhumans.com` працює **виключно у внутрішній корпоративній мережі**.
Без зовнішнього розгортання, без публічного DNS, без Let's Encrypt.

Зафіксовані рішення:
- TLS термінує **nginx** корпоративним сертифікатом (внутрішній CA / wildcard `*.superhumans.com`).
- Вбудований SSL Spring Boot вимкнено.
- Frontend — same-origin через nginx (`dist/` + проксі `/api`), JWT cookie `HttpOnly; Secure; SameSite=Lax`.
- Перший деплой: VM + systemd + JAR за `docs/Production-Deployment-Runbook.md`. Без Docker/K8s. CI не деплоїть.
- Жодних секретів у Issues/PR/логах — лише імена змінних (`APP_JWT_SECRET`, `APP_DATASOURCE_*`, `APP_MIS_API_*`, `APP_LDAP_*`).

## Карта issues

| Issue | Зміст | Виконавець | Стан |
|---|---|---|---|
| Epic #344 | https://supercare.superhumans.com, тільки внутрішня мережа | координація | OPEN |
| P0 #345 | Кодова готовність до nginx-термінації | локальна розробка (@volyamnyi) | OPEN, код готовий локально |
| P1 #346 | Внутрішній DNS + корпоративний сертифікат | IT-відділ + прод | Перевірка прод — DONE, лишилось слово оператора |
| P2 #347 | Артефакти збірки всередину мережі | локальна розробка готує → прод приймає | Локальна збірка DONE, чекає передача + прийом |
| P3 #348 | Деплой і smoke на прод-VM | прод | OPEN |
| P4 #349 | Бекапи, drill, доки, закриття epic | локальна розробка (@volyamnyi) | OPEN |

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

Стан (07.10.2026, статус зафіксовано в #346): перевірка прод — DONE. DNS: `supercare.superhumans.com → 192.168.24.49` зсередини. Сертифікат (`/etc/nginx/tls/supercare/fullchain` + `privkey`, ключ відповідає, SAN покриває домен): serial `28314EEEF4C2BC17A75CCC0F43F5536AA4707C92`, issuer Cloudflare Origin SSL CA, термін 08.10.2026 → 05.01.2027 (90 днів — закласти поновлення у P4/runbook). Рішення власника: Cloudflare Origin прийнято як ПОСТІЙНИЙ варіант. Наслідок: браузери не довірятимуть напряму — корінь `origin_ca_rsa_root.pem` на клієнти через групові політики. Дрібниці для P3: файли без розширення `.pem`, права зараз `root:root 0600`. Відкрито лише підтвердження недоступності ззовні (оператор).

## P2 #347 — Артефакти збірки всередину мережі

https://github.com/volyamnyi/intensive-care-unit-patient-chart/issues/347

Виконавець: локальна розробка готує (assigned @volyamnyi) → прод приймає. Мітки: `backend`, `documentation`.

Зміст: на прод-VM може не бути виходу в інтернет, тому збірка — зовні, доставка — артефактами (JAR за зразком CI `build`-job + `frontend/dist/` з `VITE_API_BASE=/api`). Локальна розробка фіксує коміт-SHA та SHA256 обох артефактів; прод звіряє SHA, розкладає за канонічними шляхами (`/opt/ictc/releases/`, `/var/www/ictc`). Бінарники в git не комітити. Канал передачі (VPN-scp / внутрішній файлообмінник) організує власник.

Стан (07.10.2026, докази — коментарем у #347): локальна збірка DONE з чистого worktree на `c769c3e` (робоче дерево репо мало чужі незакомічені зміни, їх не чіпали; worktree прибрано). `mvn -B clean package -DskipTests`: BUILD SUCCESS (`app-1.0.0.jar`, 81.5 MB); `npm ci + npm run build`: BUILD OK. JAR перевірено зсередини (prod `ssl.enabled=false` + `forward-headers-strategy`, `APP_CORS`/`APP_WEBSOCKET_ALLOWED_ORIGINS`, 4× порожні DB-паролі). SHA256: JAR `2B4437E2…AD465B6`, `frontend-dist.zip` `2E6F949C…F259A2B21F` (+ `SHA256SUMS.txt` у бандлі). Чекає: передача власником → прийом і SHA-звірка прод → коментар-підтвердження в #347.

## P3 #348 — Деплой і smoke на прод-VM

https://github.com/volyamnyi/intensive-care-unit-patient-chart/issues/348

Виконавець: прод. Мітки: `backend`, `database`, `security`.

Зміст: інвентаризація; firewall лише для LAN/VPN; PostgreSQL 16 (роль `ictc_app` + 4 БД, `pg_dump` порожніх БД до старту); env `/etc/environment-file-ictc` (значення — з vault, без `APP_TEST_*`/e2e-stub); systemd `ictc.service`; nginx (`server_name supercare.superhumans.com`, корпоративний сертифікат з P1, `X-Forwarded-For/Proto/Host`, swagger-шляхи в 404). Smoke з LAN-машини без PII: `https://` → 200 + HSTS, корпоративний ланцюжок, `Secure`-cookie як доказ `X-Forwarded-Proto`, реальний аудит-IP, `permissions`/`users` за очікуваннями, недоступність ззовні словом оператора. Стоп-умови: `ChecksumMismatchException`, `DATABASECHANGELOGLOCK`, розбіжність міграцій, падіння guard seed/JWT, відсутність `Secure`, недоступний MIS. Жодних `liquibase:rollback` / ручного DDL / правок `DATABASECHANGELOG`.

## P4 #349 — Бекапи, drill, доки, закриття epic

https://github.com/volyamnyi/intensive-care-unit-patient-chart/issues/349

Виконавець: локальна розробка (assigned @volyamnyi). Мітки: `documentation`, `database`.

Зміст: коміт доків (runbook Appendix C під домен + Cloudflare Origin без certbot/LE, Appendix A — `APP_WEBSOCKET_ALLOWED_ORIGINS`, запис у `AGENTS.md`, перевірка README без обіцянок публічного доступу); координація щоденних `pg_dump -Fc` ×4 + внутрішній offsite + перший restore-drill на scratch-БД (дата, тривалість, OK — коментарем); перевірка exit criteria epic; закриття P0–P4, потім epic.

Стан (07.10.2026): доки DONE (коміт нижче). README перевірено — обіцянок LE/публічного доступу нема, правити нічого. Бекапи + drill ЗАБЛОКОВАНО на P3 (деплой ще не виконано — нема чого бекапити/відновлювати); чекліст для прод — коментарем у #349. Закриття issues + epic — після P3 + drill.
