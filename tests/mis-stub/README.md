# MIS sidecar-stub (issue #297)

Локальний HTTP-стаб реальної MIS для Playwright E2E. Говорить живий envelope-контракт
(`MisApiClient` + `MisAuthService`; еталон — `MisRealHttpChainTest.java`), дані —
тільки `fixtures.json` (рукотворні, без PII).

## Запуск

```bash
node tests/mis-stub/server.cjs            # http://127.0.0.1:9099
MIS_STUB_PORT=9100 node tests/mis-stub/server.cjs
```

Healthcheck (використовує CI): `GET /health` → `200 {"status":"ok"}`.

Backend проти стабу:

```bash
APP_MIS_API_BASE_URL=http://127.0.0.1:9099 \
APP_MIS_API_TOKEN_PATH=/token \
APP_MIS_API_RUN_PATH=/api/run \
APP_MIS_API_LOGIN=e2e-stub \
APP_MIS_API_PASSWORD=e2e-stub \
APP_MIS_API_INSTALLATION_GUID=e2e-stub-guid \
java -jar backend/app/target/app-*.jar
```

## Контракт

| Метод/шлях | Вхід | Вихід |
|---|---|---|
| `POST /token` | form: `grant_type=password`, `username={login}@@@{guid}`, `password=*` | `{access_token, token_type, expires_in, login}` |
| `POST /api/run` | JSON `{name, params:[{name,value}], installationId}` + `Authorization: Bearer *` | `{spiPatientProsthesCheck:[...]}` / `{spiDocumentProsthesCheck:[...]}` / `{medicineItemKindDetails:[...]}` |
| `GET/HEAD /doc/<id>` | відомі id з фікстур | `200` плейсхолдер (probe `DocumentUrlAvailability` + `iframe` review-сторінки); невідомі → `404` |
| `GET /health` | — | `200 {"status":"ok"}` |

Суворість навмисна (ловить дрейф клієнта): без Bearer → `401`, невідома
процедура / відсутні `Login`-параметр чи `installationId` / битий JSON → `400`.

`documentUrl` у фікстурах містить плейсхолдер `{STUB_BASE}` — сервер підставляє
фактичну адресу (`http://127.0.0.1:<port>`) під час віддачі, тож фікстури
незалежні від порту.

## Дані (`fixtures.json`)

- Ростер: `900001`/`900002` (dept 19 — дзеркало сід-рядків `data-prosth.sql`, щоб
  стикувалися сід-ордери `PR-2026-0001/0002` і `candidates` був непорожнім на
  чистій БД), `13373` (якір `setup-order-documents`), `10101` (dept 37),
  `10102` (dept 19), `10401` (статус `MOV` — виключення з лікувальних в'ю),
  `10501` (dept 2 — поза eligibility).
- Документи: `900001` → 120; `900002` → 121 (багаті поля для префілу TP-LL-02);
  `13373` → 120 + 121; решта → порожньо.
- Каталог: 5 позицій, одна `itemKindIsDisabled=true`.

## Тести

```bash
node --test tests/mis-stub/stub-contract-test.cjs
```

Файл тесту навмисно названо `stub-contract-test.cjs` (без `.test.`), щоб його
не підхопив Playwright (`testDir '.'`). `node --test` знаходить його за
патерном `*-test.cjs`.

## Правила

1. Жодного PII: вигадані ПІБ/телефони (`+38000000*`)/пошти (`*.stub.invalid`).
2. Нові поля — лише ті, що парсить `MisServiceImpl` (див. `textOrNull`/`longOrNull`
   ключі); невідомі ключі бекенд ігнорує.
3. Зміна процедур/полів реальної MIS → оновити тут + `MisStubContractTest` (issue D).
4. Стаб слухає тільки `127.0.0.1`; ніколи не стартує в prod (CI-степ + E2E-оточення лише).
