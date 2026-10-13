# Responsive-UI Width Audit — Phase 1 deliverable

**Дата:** 2026-10-13 · **Обсяг:** фронтенд width-епік (фази 1–7) · **Джерело:** фактичні інвентаризації по коду під час сесії

Цей документ — контракт для per-phase рев'ю: жодна зміна поза переліком нижче.

---

## 1. Фактична карта акторів ширини (layout map)

### Shell
| Місце | Факт | Дія |
|---|---|---|
| `layouts/GlobalLayout.tsx:153` | `mx-auto w-full max-w-7xl px-4 mt-3 mb-4 fade-in-up sm:px-6` — єдиний глобальний контейнер (80rem ≈ 1280px) | замінити на `PageContainer` (Ph.3) |
| `layouts/GlobalLayout.tsx:152` | `<main className="flex-1">` (без `min-w-0`; батько :103 — `flex-col flex-1 min-w-0`) | додати `min-w-0` (Ph.3) |
| `layouts/GlobalLayout.tsx:104–148` | Хедер **full-bleed** (поза контейнером) з внутрішнім `flex h-16 px-[max(1rem,env())] sm:px-6` | вирівняти вміст шапки з контейнером (Ph.3, R3) |
| `layouts/GlobalLayout.tsx:153` | `fade-in-up` на самому контейнері | перенести клас на окремий wrapper — портали (`InteractionAlertOverlay`) не чіпати (A6) |
| `layouts/DoctorLayout.tsx:5` / `layouts/NurseLayout.tsx:5` | лише `fade-in-up` wrapper (без ширини) | без змін |
| `pages/prosthetics/process/ProcessLayout.tsx:41,51` | рендерить **власний** `<main>` всередині `<main>` оболонки (R1, підтверджено у коді — 2 лендмарки) + власні gutter-и `p-4 sm:p-6` | демонтувати внутрішні `<main>` → `<div>`; маршрут `/prosthetics/process/*` = `full` (Ph.3) |
| `App.tsx:132` | GlobalLayout огортає ВСІ авторизовані маршрути | підтверджено → width-map тут |

### Сторінкові самоконтейнери — 14 місць з `container mx-auto`
| Файл:рядки | Поточне | Дія (Ph.4) |
|---|---|---|
| `pages/prosthetics/setup/TemplateSelectPage.tsx:97,115` | `container mx-auto max-w-2xl py-8` ×2 | прибрати клас `container`; критерій width=narrow (42rem) |
| `pages/prosthetics/setup/PatientSearchPage.tsx:98` | `container mx-auto max-w-4xl py-8` | прибрати `container`; max-w залишити (56rem) |
| `pages/prosthetics/setup/OrderSelectPage.tsx:102` | `container mx-auto max-w-2xl py-8` | те саме |
| `pages/prosthetics/setup/OrderReviewPage.tsx:144,159,174,191` | `container mx-auto max-w-2xl py-8` ×4 | те саме |
| `pages/prosthetics/ProstheticsDashboard.tsx:22` | `container mx-auto py-6` | прибрати `container` (під shell wide) |
| `pages/prosthetics/ProductionPage.tsx:280` | те саме | те саме |
| `pages/prosthetics/DashboardPage.tsx:127` | те саме | те саме |
| `pages/prosthetics/process/ProcessOverview.tsx:58,74,86` | `container mx-auto max-w-[1400px] px-6 py-8` ×3 | прибрати `container`, власний max-w лишити (окремий маршрут `/prosthetics/process/*` — full) |

### Сторінкові власні малі контейнери (не `container`-клас, залишаємо)
- `CreateCardPage.tsx:36` (`mx-auto max-w-2xl`), `ProcessHistoryPage.tsx:263` (`max-w-3xl`), `DoneScreen.tsx:141`, `FailedScreen.tsx:141`, `WizardScreen.tsx:1847` (`max-w-xl`), `LowerLimbMeasurementForm.tsx:238` (`max-w-5xl`), `AppSelectorPage.tsx:70,93` (`max-w-[700px]`) — **без змін**.

## 2. Місця, де зміна небезпечна (контракти, тести)

### ?raw-contract tests (контроль зміни з phase-by-phase)
| Тест | Що асертить | Вмикає |
|---|---|---|
| `tabletPhase4.test.tsx` | `sticky left-0 z-10 min-w-[260px] bg-card` + `min-w-[72px] text-center` (AdminPage) | не чіпати стіки |
| `tabletForms.test.tsx` | діалог `sm:max-w-sm md:max-w-md`; sheet cap; `max-w-xs` у dose-поповерах | діалог-політика — **не чіпати** |
| `tabletDensity.test.tsx` | `sm:max-w-[95vw]`, `md:max-w-[92vw]`, `md:min-w-[140px]` (HourlyGridDialog) | — |
| `popoverPrimitive.test.tsx` | експорти popover, відсутність `getBoundingClientRect` | — |
| `interactionAlertMotion.test.tsx` | кейфрейми оверлея + reduced-motion gate | — |
| `frontend/src/components/ui/dialog.test.tsx` | `sm:max-w-sm`/`md:max-w-md` | — |
| `focusMotion.test.ts` | присутність `.fade-in-up` серед reduced-motion-гейтів | перенос класу з контейнера на wrapper — правило лишається |

### Свідомо широкі клінічні сітки (не чіпати)
- `HourlyGrid.tsx:15,22` — `min-w-[1100px]` у власному `overflow-x:auto` (легальна внутрішня прокрутка);
- `PrescriptionSpreadsheet.tsx:460,499,537` + `VitalSignGrid.tsx:129,168` — sticky колонки + scroll-container;
- `AdminPage.tsx:427` — RBAC матриця sticky `min-w-[260px]`.

## 3. Клінічне усікання (A11 / R7) — Phase 5

| Місце | Поточне | Дія |
|---|---|---|
| `components/icu/DocumentHeader.tsx:73` | `ml-auto max-w-[320px] truncate`, **без `title`** на клінічному значенні | додати `title={…}` (Ph.5, низькоризикова зміна) |
| `components/icu/EpisodeTable.tsx:136` | `hidden max-w-[200px] truncate md:table-cell` на episode id | дизайн — залишаємо |
| `components/prosthetics/StepNoteAttachments.tsx:161` | `truncate` + `title={f.fileName}` | OK |

## 4. Фіксед/portal-споживачі shell
- `components/prescription/InteractionAlertOverlay.tsx` — `createPortal(document.body)` (:109); коментар :35–38 містить containing-block-through-fade-in-up gotcha. **Заборонено чіпати** (A6).
- `components/ui/dialog.tsx:59` — `fixed top-1/2 left-1/2 z-50` (sm:max-w-sm md:max-w-md) — інша політика, поза скоупом.

## 5. E2E-базис
- **Поточний** `tests/specs/responsive/no-horizontal-scroll.spec.ts` — 3 вьюпорти (360, 768, 1280) × маршрути: doctor ×4, prosthetist ×3 (+3 draft-seeded), nurse ×3, hod ×1, admin ×1.
- **Додати (Ph.6):**
  - 1920 always-on для всіх чинних маршрутів;
  - 1440 + 1024 reduced set (6–8 ключових маршрутів);
  - нерозривний рядок 60 символів у місцях: заголовок h1, комірка таблиці, input value, badge (overflow-кейси);
  - окремий хелпер `assertNoHorizontalOverlap` у обмеженій зоні (header / breadcrumbs / картки статистики) — уникнути флейху щодо загального скану всього дерева.
- Локально (не в CI): 320px WCAG reflow — на періодичній перевірці.

## 6. Токени, які додамо у Phase 2
- `--container-page: 80rem`, `--container-page-wide: 120rem`, `--container-page-narrow: 42rem` у `@theme`
- Утиліти `max-w-page{-wide,-narrow}` — Tailwind v4 повинен згенерувати (перевірити через build → dist CSS)
- `--breakpoint-2xl` залишається дефолтним (96rem) — фактично вже доступний

## 7. R1–R6 з даного циклу
- **R1** вкладені `<main>` — фікс у Фазі 3.
- **R2** Escape-крос-закриття оверлея/BaseUI — прийнято (дизайн).
- **R3** хедер full-bleed vs контейнер — фікс у Фазі 3 (вирівняти вміст шапки з контейнером).
- **R6** емісія `max-w-page*` з Tailwind v4 — довести dist-перевіркою в Фазі 2.

## 8. Квартальні контрольні цифри (коли переходимо до Phase 7)
- 15 рядків `container mx-auto` у 8 файлах (14 унікальних місць; ProcessOverview — 3 рядки)
- +1 коміт width-епіку (всі фази разом)
- E2E-зростання у Ph.6 ≈ +27 тестів (1920 × 15, 1440/1024 × 12)