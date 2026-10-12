# FR-Review-B — Протокол ревью Воркстріму B (дофікс оверлея #350–#355)

**Дата:** 2026-10-12 · **Скоуп:** диф епіка #350–#355 (центроване сповіщення) + підсумковий документ сесії · **Вердикт:** PASS із двома Low-фіксами, виконаними в межах раунду

## 1. Підтверджено коректним (no action)

| Перевірка | Доказ |
|---|---|
| B1: export-контракт `interactionPairFocus.ts` | 4 чисті DOM-хелпери (`pairCells`/`setActivePair`/`clearActivePair`/`showPairInGrid`), `CSS.escape` на селекторі (:12), жодного React-стану; `InteractionAlert.tsx:9` — повна заміна локальних копій на імпорт |
| Portal + translate-архітектура | `index.css:232–242`: wrapper центрується виключно через `translate` property → нуль конфліктів із `transform`-keyframes картки (`:244–251`); портал у `document.body` (`InteractionAlertOverlay.tsx:109`) обходить known containing-bloсk gotcha `fade-in-up` передків |
| Reduced-motion | `index.css:432–433` — `.interaction-alert-center-enter` у гейті разом із `.interaction-alert-enter` |
| Forced-colors | `index.css:253–258` — системний `Mark` outline на оверлеї; `:210–215` на severity-комірках |
| `max-height` | `index.css:239–240` — коректний фолбек-ланцюг (`100vh` → `100dvh`) + `overflow-y: auto` |
| Тестова гігієна оверлея | 8/8 тестів: fake timers + `useRealTimers` в `afterEach` (:40–42), прототип `scrollIntoView` відновлено у `finally` (:120–122), ім'я тесту — template literal із константою (:88) → B2 безпечне |
| Page-test скоупінг | `PrescriptionDetailPage.test.tsx:455` — `within(screen.getByTestId('interaction-alert'))` (банер vs оверлей-dedup), :456 overlay presence |
| E2E харденинг | `suiteCompleted`-гейт (:95/135/500), memoized `currentListId` (:102), `dismissOverlay` з waitFor (:113), усі 9 `toHaveScreenshot` з `periodMask` (:126) |
| `.gitignore:50` | Виняток звужено до `*-linux.png`; обидва docs-файли з підсумку на диску |
| Mount | `PrescriptionDetailPage.tsx:310–312` — банер з `listId`, оверлей поруч, один спільний запит |

## 2. Знахідки (усі закриті/офіційно прийняті)

| ID | Severity | Місце | Рішення |
|---|---|---|---|
| B-F1 | Low | `InteractionAlert.tsx:13`, `useInteractionAlertSignal.ts:66`, `InteractionAlertOverlay.tsx:21` | **Три ідентичні копії `formatShortDate`** → витягнуто в `interactionAlertModel.ts` (chore `f2479ca6`) |
| B-F2 | Info, прийнято | `index.css:237` vs `dialog.tsx:59` | Паритет z-50: оверлей перекриває діалог лише за нова signature під час відкритого діалогу; non-modal картка, діалог тримає backdrop/keyboard. **Прийнято, без змін.** |
| B-F3 | Info, прийнято | `Overlay.tsx:91` window-Escape | Крос-закриття з day-menu (`PrescriptionSpreadsheet.tsx:371`) та Base UI popover/dialog — прийнято за конвенцією «Escape закриває все» |
| B-F4 | Info, прийнято (рішення власника B8) | `Overlay.tsx:47` | in-memory `dismissed` → re-show при навігації; затверджено як дизайн «анонс на кожен вхід» |
| B-F5 | Low → backlog | `Overlay.tsx:51` | render-time DOM-read `focusInside`; jsdom-покриття є, live E2E-на доказ фокус-повернення — залишено як окремий backlog (за межами раунду) |
| B-F6 | Info | spec L477–485 | `setTimeout`-поллінг ibuprofen — канонічніше було б `expect.poll`, працює; не блокує |

## 3. Виконання раунду

| # | Дія | Коміт | Гейт |
|---|---|---|---|
| 1 | B2: `INTERACTION_OVERLAY_AUTODISMISS_MS` 10_000 → 6_000 (`InteractionAlertOverlay.tsx:22`) | `4343e6a1` fix | Vitest 26/26 (оверлей 8 + DetailPage 18) |
| 2 | B-F1: `formatShortDate` → `interactionAlertModel.ts`, видалено 3 копії | `f2479ca6` chore | Vitest 42/42 (prescription-пакет), `tsc` чисто, lint 0 errors (16 warnings — бейзлайн) |
| 3 | R8: `page.clock.install({time: new Date()})` у тесті «overlay screenshots» — 6с-таймер не гасить картку посеред capture; заодно оновлено застарілий коментар про 10с timer-restart | `098075ac` test | Спека скомпільована (`--list` 30/30) + **живий прогін 30/30 green** |
| 4 | Docs: AGENTS top-entry — 10s→6s + review-round донотація; README — hardcoded-згадок немає (перевірено grep) | docs-коміт разом з п.5 | — |
| 5 | Цей протокол | docs-коміт разом з п.4 | — |
| 6 | B10: evidence-коментарі + закриття #350–#355 | — | ⏳ чекає згоди власника (зовнішня GitHub-мутація) |

## 4. Живий доказ E2E

- Стек піднятий локально під раунд: MIS stub :9099 (PID 10108, `/health` ok), backend :8085 (з існуючого JAR 05.10 — backend main-код не змінювався після збірки, перевірено `git log -- backend/`), Vite :5173.
- `npx playwright test prescription-interactions.spec.ts --project=doctor-chromium` → **30/30 passed (2.5 min)**: `page.clock` у скріншот-тесті оверлея зелений, весь ланцюжок (banner 1280/360, medium-downgrade @768 + restore, reduced-motion/forced-colors/zoom200, delete-unmount) — без регресій.

## 5. Зафіксовані рішення власника (для evidence-коментарів #350–#355)

1. Оверлей сповіщає на кожен вхід на сторінку (in-memory dismiss) — дизайн-рішення.
2. Автозакриття 6с (було 10с) — зафіксовано у коді + AGENTS.
3. `role="group"` у оверлея — прийнято (банер монопольно тримає `role="alert"` з decay 600мс; подвійний assertive-анонс — гірше).
4. Звук — не потрібен.
5. Escape-крос-закриття з поповерами/діалогами — прийнято (конвенція).

## 6. Відкладене (backlog)

- **B-F5:** живий E2E фокус-повернення при авто-dismiss (Tab у кнопку → 6с → фокус на `h1[tabIndex=-1]`, fallback blur) — невеликий тест, окремим PR.
- **B-F6:** заміна `setTimeout`-поллінгу на `expect.poll` у spec L477–485 — косметика.
