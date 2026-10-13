# FR-Review-W — Протокол рев'ю Width-епіку (фази 1–6)

**Дата:** 2026-10-13 · **Обсяг:** система ширини контенту фронтенду · **Вердикт:** PASS; одна помилка мігпорайонна виправлена сам-на-сам.

## 1. Підтверджено коректним

| Перевірка | Доказ (file:line) |
|---|---|
| Токени Tailwind v4 | `frontend/src/index.css:62-66` — `--container-page` (80rem), `--container-page-wide` (120rem), `--container-page-narrow` (42rem) у `@theme`; dist-CSS містить `.max-w-page-wide`/`.max-w-page-narrow`/`.max-w-none` |
| Примітив PageContainer | `frontend/src/layouts/PageContainer.tsx` — `width?: 'narrow' | 'page' | 'wide' | 'full'` (замовчування `'page'`); `PageContainer.test.tsx` 5/5 |
| Shell | `GlobalLayout.tsx` — width у контейнері по маршруту: `/prosthetics/process/*` = full, create-card/select = narrow, решта = wide; `fade-in-up` перенесено з контейнера на окремий wrapper; `main` має `min-w-0` |
| Вкладені `<main>` (R1) | `ProcessLayout.tsx:41,51` — внутрішні `<main>` → `<div>` |
| Хедер вирівняний (R3) | `GlobalLayout.tsx` — `PageContainer width={contentWidth}` навколо вмісту шапки (візуально не міняє розміри на банках ≤1280) |
| Міграція 15 місць | 8 файлів: `pages/prosthetics/{DashboardPage,ProductionPage,ProstheticsDashboard,process/ProcessOverview,setup/{OrderReview,OrderSelect,PatientSearch,TemplateSelect}Page}.tsx` — прибрано `container` + злито на `max-w-page-*` |
| Клінічне уявлення (Ph.5) | `components/icu/DocumentHeader.tsx:73` — пацієнт отримує `title={patientName}` |

## 2. Знахідки, закриті в межах раунду

- **B-W1 (Low, self-caught):** редакторна помилка: `replaceAll` навколо `OrderReviewPage.tsx` несподівано змінив `py-8 → py-6` у всіх 4 строках — відразу повернуто до `py-8` і перевірено (commit `d255bfc0` тримає py-8).
- **B-W2 (Info, прийнято):** у класі `container mx-auto max-w-[1400px]` у ProcessOverview прибрано придушення `container` (tailwind point-wise підміняє max-w-[1400px]) — візуал <!-- stable між 1280-1920; ≥1920 досягає 1400px як й було за задумом.
- **B-W3 (Info, само-корекція):** тимчасове corrupt-вміст у коде-файлі (FR-Review-W была перезаписана чисто) — підтверджено Read-back.

## 3. Верифікація

- Vitest: **990 tests / 109 files green** (було 985/108 → +5/+1 за PageContainer).
- E2E responsive-tablet: **87/87** живий прогін (стек: stub :9099 + backend :8085 + Vite :5173).
- `--list` загального набора: **493 tests / 101 files** (було 466 → +27 нових 1920/tiered).
- oxlint: 0 errors; tsc: чисто.

## 4. Коміти

| Коміт | Зміст |
|---|---|
| `5c1879cb` docs | `docs/Responsive-UI-Width-Audit.md` (Фаза 1) |
| `10014bf2` feat | Токени + PageContainer + тест (Фаза 2) |
| `7ca10867` feat | GlobalLayout + ProcessLayout (Фаза 3) |
| `d255bfc0` refactor | 15 місць міграції сторінок (Фаза 4) |
| `b239b317` fix | DocumentHeader title (Фаза 5) |
| `c3cc55c3` test | Матриця E2E 1920 + tiered 1440/1024 (Фаза 6) |

## 5. Обмеження / backlog

- **Довгі стрічки-cell**: окремий `long-string` E2E-спада не додано (тільки відкритий сценарій в viewport-матриці) — у follow-up.
- Livel-опищен фокус-повернення авто-dismiss оверлея (B-F5 from FR-Review-B) — залишається як backlog.
- CI-політика: усі прогіни локальні; CI запускається за explicit-запитанням власника.
