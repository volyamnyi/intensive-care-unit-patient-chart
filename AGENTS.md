# ICU Patient Chart — AI Agent Guide

## Current Session

**2026-08-25: Responsive/ShadCN Phase 3 — forms & dialogs adaptive layout at tablet widths (issue #177)** — M1–M3 code + tests complete, pre-flight green (oxlint 0 errors / 8 pre-existing warnings, `tsc --noEmit` clean). **M1 scale-form verification**: `ApacheIiForm` (sm:2/md:3/lg:4), `BradenForm` (sm:2/md:3), `RassSelector` trigger already had full tier ladders + `pointer-coarse:min-h-11` — no change; **`SofaForm` was the only gap** → its 13-field grid is now `grid grid-cols-1 gap-1.5 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4`. **M2**: `VitalSignsForm` grid `sm:grid-cols-4` → `sm:grid-cols-3 md:grid-cols-4` + textarea `min-h-[60px] md:min-h-[72px]` (component currently has no production importer — test-only, kept); `PatientStatePanel` six select fields wrapped in `grid grid-cols-1 gap-1.5 md:grid-cols-2`, all inputs/triggers `pointer-coarse:min-h-11`, notes textarea `min-h-[2.5rem] md:min-h-[72px]`; `MedicalNotesPanel` fragment → outer `grid grid-cols-1 gap-2 lg:grid-cols-2` with the list wrapped in `flex flex-col` (editor | list side-by-side ≥1024px). **M3**: `ui/dialog.tsx` `DialogContent` base policy `sm:max-w-sm` → `sm:max-w-sm md:max-w-md` (384→448px ≥768), single source; narrow consumers keep intentional caps via cn()/tailwind-merge (`max-w-xs` on dose-execution popovers survives unconflicted — documented exception); `ui/sheet.tsx` unchanged — right-side patient sidebar sheet capped by consumer (`w-full p-0 sm:max-w-sm` = 384px @768). **Tests**: new `src/test/responsive/tabletForms.test.tsx` (9 ?raw contracts incl. dialog policy + sheet cap + max-w-xs retention), new `components/ui/dialog.test.tsx` (7 jsdom: closed-hidden, labelled open dialog, `sm:max-w-sm`/`md:max-w-md` classes, default no `data-fullscreen`, `mobileFullscreen`→`data-fullscreen="mobile"`, Escape onOpenChange, controlled open), new E2E `tests/specs/responsive/tablet-forms.spec.ts` (4 serial tests in `responsive-tablet-chromium`; testMatch extended) targeting **episode `a3333333` (Сидоренко) whose ONLY day `b3333333` stays OPEN suite-wide** (no spec signs it; doctor-project signoff specs touch a1111111/a2222222 only) so sidebar forms are editable by default — inline sidebar visible (`div.hidden.md\:block` hosting the Шкала combobox) + sheet trigger «Панель пацієнта» hidden at 768; SOFA form row-alignment via boundingBox y (items 1–2 same row, Тромбоцити wraps) + `pointer-coarse:min-h-11` class contract + calculate round-trip asserting the SOFA card flips to «Результат:»; patient-state Свідомість/Шкіра pair on one row with Набряки below; `/prescriptions/doctor` delete-dialog width asserted in (384, 450] then «Скасувати» (drawer flow: search «1002» → row «Відкрити» → drawer «Видалити»). Plan doc Phase 3 outcome paragraph + component-map rows updated (dialog ✅, sheet ✅ verified).

**2026-08-25: Responsive/ShadCN Phase 2 — popover primitive + consumer migration (issue #176, CLOSED, CI GREEN ×2)** — runs `32841155977` (head `5bdea20`) + `32843143495` (head `af46395`, empty-commit flake check) both all 6 jobs green — the required double-green passed. **M1**: new `frontend/src/components/ui/popover.tsx` (Base UI `Root/Trigger/Portal/Positioner/Popup/Title/Description`, `data-slot` per part, repo floating-surface recipe on `PopoverContent`: `z-50 rounded-lg bg-popover p-1 text-sm text-popover-foreground shadow-md outline-none ring-1 ring-foreground/10 duration-100` + `data-open/closed:animate-in/out`, content-sized, no fixed `w-72`). `PopoverTitle`/`PopoverDescription` wrap Base UI `PopoverPrimitive.Title/Description` — required so the popup store syncs `titleElementId`/`descriptionElementId` and the open popup is a properly labelled `role=dialog` (`aria-labelledby`). ⚠️ gotcha: `PopoverPopup` THROWS without `PopoverPositionerContext` — the popup can only render inside `PopoverPositioner`; the render test harness must include a real anchor (first CI run `32840166571` failed 5 tests on this). Consumers migrated off hand-rolled `getBoundingClientRect` overlays: `prescription/DeleteConfirmPopover.tsx` and `prescription/ExecuteDosePopover.tsx` (dose-entry half; its 2FA half already uses `ui/dialog`) now import `@/components/ui/popover` only. **M2**: `ui/alert-dialog.tsx` deliberately SKIPPED — `ui/dialog.tsx` + `ui/popover.tsx` already cover destructive-confirm/context-menu patterns (decision + rationale in the plan's component map). **M3**: `index.css` `prefers-reduced-motion` gate extended with `popover-content` + previously un-gated `dropdown-menu-content`/`dropdown-menu-sub-content`/`tooltip-content`. **Tests**: `ui/popover.test.tsx` (5: closed-hidden, trigger `aria-expanded`, open, labelled-dialog role+name, Escape close, controlled `open`) + `test/responsive/popoverPrimitive.test.tsx` (3 contract: exports present; both consumers import via `ui/popover` and contain no `@base-ui/react` / `getBoundingClientRect`); local Vitest 83 files / 687 tests green. **Docs**: `docs/Responsive-UI-ShadCN-Implementation-Plan.md` — component map popover row done, alert-dialog skip decision, Phase 2 outcome paragraph. Issue #176 closed.

**2026-08-24: MIS Single Source - WireMock is the only MisService implementation (issues #191-#194, CI GREEN)** - 4-phase series replacing dual Mock+WireMock with WireMock as single source of truth. **Phase #191**: docs/MIS-Integration-Audit.md divergence table; MisErrorSimulator + MisDictionaries shared helpers; WireMockMisServiceImplTest (~37 unit cases); MisParityTest fixture validation. **Phase #192**: closed WireMock gaps - spzIBMedicineDictionary (20 items), spzIBPatientAllergy (per-patient filter), patients extended 52 to 92 (2001-2040 surgery/rehab), getDepartmentUsers filters by userDepartmentID, getDepartments prefers stable companyID, UserMisDTO gains departmentId; wiremock dep moved icu-chart to common; MisWireMockIntegrationTest (~15 cases, embedded server + real HTTP). **Phase #193**: matchIfMissing=false on WireMock; MisMutualExclusionGuard fail-fast; MisEmbeddedWireMockConfig boots WireMockServer on port 9090 serving classpath fixtures programmatically (JAR-safe); application.yml defaults changed to wiremock-enabled=true + embedded-wiremock-enabled=true + mock-enabled removed; prod profile disables embedded. **Phase #194**: MockMisServiceImpl deleted (~700 lines); MockMisServiceImplTest/MockMisServiceTest coverage migrated to WireMock unit tests + parity tests; pom hygiene across all 5 modules. **Config**: app.mis.wiremock-enabled=true (only flag), embedded-wiremock-enabled=true for dev/CI, prod disables embedded.

**2026-08-22: Security Phase A red-gate (issue #170, CI GREEN)** — run `32571933791` all 6 jobs green (E2E 15m30s). Closed the highest-value security regressions + paired fixes. **F1 (CRITICAL, A1)**: `application.yml` `app.jwt.secret` → `${APP_JWT_SECRET:...}` env override (+ new `JwtSecretGuard` fails startup under `prod` if the committed default is in effect; `APP_JWT_SECRET` added to the 3 DB CI jobs). **F2 (HIGH, A5)**: `FlowInstanceService.requireOwner` returns the instance only for the assigned owner or an allow-all (prosthetics admin) caller, else 404; `EvidenceFileService.download` binds the file's owning instance to the same owner check. **F4 (A3)**: `@JsonIgnore` on `User.passwordHash` (raw `User` entities returned by `/api/users/*`, `/api/admin/users` no longer leak BCrypt hashes). **F6 (A6)**: every mutating ICU endpoint now carries `@PreAuthorize` reusing the 25 existing codes — episode PATCH/close/archive→`EPISODE_CREATE`; clinical-day PATCH/close-early→`CLINICAL_DAY_CREATE`; medical-order PATCH→`PRESCRIPTION_CREATE`; scales create/update→`hasAny('SCALE_APACHE_SOFA','SCALE_CAMICU_BRADEN_RASS')`; notes/PDF/fluid-recalc/labs/ventilation/patient-state→`hasAny('SCALE_APACHE_SOFA','SCALE_CAMICU_BRADEN_RASS','VITALS_ENTER')` (excludes PROSTHETIST/ADJACENT_SPECIALIST). **Tests**: `JwtTokenProviderTest` (round-trip/expired/tampered/wrong-key/env-source tripwire), `FlowInstanceServiceTest.requireOwner_*` + `EvidenceFileServiceTest.download_*` (owner/allowAll/non-owner/unknown, 404), `SecurityTokenIntegrationTest` (SEC-B01..B05 JWT rejection + cookie parity, B24 PROSTHETIST episode PATCH→403, passwordHash-absence), ArchUnit rule #1 (only `com.superhumans.mis` may call `MisApiClient.callMethod`) + rule #2 (mutating feature/ICU controllers carry `@PreAuthorize`, excluding platform auth/admin/mock). **Docs**: runbook/README A1/A3/A6 notes. Commits `80662cd`, `79a4895` (JWT test assertion fix), `409c00` (evidence-download test corrected to expect 404). Note: `FlowInstanceService.start` still bypasses `requireOwner` (uses `findByIdForUpdate` directly) — a deeper pre-existing ownership gap left for a follow-up; `startByAnotherProsthetistIsAllowed` retained.

**2026-08-22: Seed-data password reset fix (issue #182, [Security] CRITICAL, CI GREEN)** — CI run `32565243912` all 6 jobs green; the 2 previously-failing... actually all green. **Bug (audit A2, CWE-798)**: the `users` seed INSERTs in `data-core.sql` used `ON CONFLICT (login) DO UPDATE SET password_hash = EXCLUDED.password_hash`, so any operator-rotated password was silently reverted to the documented demo value on every boot (exploitable: `doctor1/doctor123`, `admin/admin123` stay valid in prod). **Fixes**: (1) `application.yml` base block wired `app.seed-data.enabled: ${APP_SEED_DATA_ENABLED:true}` + a `prod`-profile block sets `app.seed-data.enabled: false`; (2) new `SeedDataGuard` `@Component` fails startup (`IllegalStateException`) under `prod` if seeding is enabled, wired via `@DependsOn` before `SeedDataInitializer`; (3) both `data-core.sql` user INSERTs → `ON CONFLICT (login) DO NOTHING` (kept the intentional `prescription_lists`/`system_settings` auto-heal upserts). **Tests** (all pass in CI): `SeedDataSqlTripwireTest` (static: `data-core.sql` must not contain `EXCLUDED.password_hash`), `SeedDataGuardTest` (prod+enabled throws / prod+disabled-ok / non-prod-ok), `SeedDataPasswordIdempotencyIntegrationTest` (`backend-integration`: re-runs only `data-core.sql` via `ScriptUtils` on `coreDataSource` after mutating `doctor1`'s hash → unchanged). **Docs**: README `Seed Data` A2 notice, AGENTS.md, `docs/Production-Deployment-Runbook.md` section 0.2 note the demo credentials must be rotated/disabled before go-live + the new boot guard. Commit `eeaf97c`. Note: the working tree carried unrelated uncommitted prior-session changes (frontend prosthetics + README RBAC/Security rewrite) that were preserved and NOT included.

**2026-08-21: Per-item prescription day management — Phase 4 E2E + docs (issue #169, PHASE COMPLETE, CI GREEN)** — CI run `32523845039` all 6 jobs green (`format-check`, `backend-test`, `backend-integration`, `frontend-test`, `e2e-test`, `build`); the 2 previously-failing doctor day tests pass after scoping them to the item grid. `tests/specs/doctor/prescription-day-management.spec.ts` root cause of the 2 CI failures: the prescription DETAIL page renders **two** tables (item grid `PrescriptionGrid` + vital-sign grid `VitalSignGrid`), so page-wide `tbody tr` / `th` locators were ambiguous (row count 12 ≠ item count 5) and the non-retrying `.count()` raced the item-grid loading spinner (read 0). Fix `eec7349`: scope all grid assertions to the items table (uniquely identified by its «Препарат / Метод» header vs the vital grid's «Показник»), wait for a «Додати день» button before reading any count, and assert the day context menu **unmounts** after a successful removal (the old `th hasText dateLabel toHaveCount(0)` was a false-flake risk — header dates are the union across ALL items, so a sibling item sharing the date would keep the `<th>`). Prior red runs `32517818622`/`32521267980` (`.first()` TypeError + these). Phase 4 of the 4-phase per-item day feature closes the milestone (#166 backend service `PrescriptionItemService.addDay`/`removeDay` + 422 guard DONE, #167 controller endpoints DONE, #168 frontend UI DONE, **#169 E2E + docs DONE**). **Endpoints** (both `@PreAuthorize PRESCRIPTION_CREATE` — DOCTOR/HOD only, nurses 403): `POST /api/prescriptions/items/{itemId}/days` → 201, adds the next day (max `dayDate` + 1) with 4 unplanned day parts (morning/day/evening/night, one new `dayId`); returns the FULL item (all `dayParts` for all days — controller: `itemMapper.toResponse(itemService.getListItem(itemId))`). `DELETE /api/prescriptions/items/{itemId}/days/{dayId}` → 204; **422 `BUSINESS_RULE`** «День містить виконані призначення, видалення неможливе» if any day part of the day is completed (`PrescriptionItemService.removeDay` guard). **3 new E2E specs — 9 tests** (`--list` green): `tests/specs/doctor/prescription-day-management.spec.ts` (2 tests — patient 1003 / Сидоренко, 5 items: per-row «Додати день» (+) button appears on every row; add-then-remove round-trip via the dose-cell right-click context menu «Видалити цей день»; card navigation via `card.getByRole('button', { name: /Листок/ }).first()` — the card has TWO navigate buttons: the big document-name button + the small ExternalLink «Відкрити»); `tests/specs/nurse/prescription-day-management.spec.ts` (2 tests — «Додати день» not rendered for any row, right-click opens no context menu; nurse gating); `tests/specs/api/prescription-day-access.spec.ts` (5 tests — patient 1001 morfin seed item `40f40760-4807-997e-d706-7293273f0769`: nurse POST 403, nurse DELETE 403, doctor POST 201 with the new day identified via `max(dayDate)` (full-item response) having exactly 4 unplanned parts one `dayId`, doctor DELETE completed-day `138b0217-…` → 422 `BUSINESS_RULE`, doctor DELETE open-day `13e71c36-…` → 204). **Suite totals after Phase 4**: E2E 292 tests / 64 spec files / 11 projects; Vitest 667 tests / 79 files; backend 348 main sources / 115 test files. **Docs**: README.md API table gains both endpoints; AGENTS.md endpoints + counts updated.

**2026-08-20: Wizard checkbox whole-surface clickability** — every parent checkbox row in the «Операційна карта» wizard (`frontend/src/pages/prosthetics/process/WizardScreen.tsx`) is now clickable across its whole surface. **Refactor**: new `CheckboxRow` component — the row itself is `<label htmlFor={id}>` (native label activation covers padding/gap/text; no JS onClick), inner `Label` replaced with `span data-slot="label"` (nested `<label>` invalid; clicking the Base UI checkbox span stays a single toggle — the HTML spec suppresses label activation for interactive content, verified in jsdom's `HTMLLabelElement._activationBehavior`); 3 variants `card`/`muted`/`plain` (plain used by the `ElementField` CHECKBOX branch incl. its `border-destructive ring-1 ring-destructive` error class). All ~70 checkbox rows converted: PPE groups (PpeChecklistGroup + MeasurementForms gloves rows + thermoforming/inner-sleeve PPE), kit-formed, plaster negative/quality/positive, thermoforming latex/thermal + sleeve formed/edges polished, inner-sleeve latex/thermal + formed/edges, inner-fitting nitrile + sleeve/axes, outer-sleeve model-ready, lamination + confirmed, processing + confirmed, assembly/fastening confirmed, final assembly, final-fitting nitrile + fastening/cables, handover nitrile + passive rotation/soft tissues, marking nitrile + final inspection/applied, fitting nitrile + sleeve, prototype-fitting nitrile + tested, ElementField CHECKBOX. `Label htmlFor` remains only for non-checkbox controls (kit inputs, radios, dialogs). Base UI CheckboxRoot verified: the `id` prop lands on the hidden INPUT (label `for` matches), `aria-checked` lives on the `span[role=checkbox]`, span onClick dispatches a PointerEvent click on the input (single toggle). **Vitest**: `WizardScreen.test.tsx` new test «toggles a checkbox from anywhere on its parent row surface» — text click (label activation), row click, control click (single toggle, no double-fire). **E2E**: `tests/specs/prosthetics/wizard-checkbox-surface.spec.ts` (prosthetics-chromium, serial) — creates a fresh instance via API (order-candidate retry loop against the concurrent mobile-wizard-smoke creator), then walks ALL template steps (loop breaks on instance COMPLETED): on every step clicks EACH checkbox row at 5 surface points (4 corners + center, `page.mouse.click` after centering the row clear of the sticky top/bottom bars) asserting `aria-checked` flips exactly once per click; steps without checkbox rows skipped via new `completeCurrentStepViaApi` helper in `tests/helpers/prosthetics-flow.ts` (`buildValues` now exported); afterAll drives the instance to COMPLETED (keeps the "new process" review screen unblocked). Pre-flight green (oxlint 0 errors, tsc clean, build OK).

**2026-08-19: Responsive QA phase (issue #165)** — 2 new Playwright projects + 5 responsive specs (10 mobile + 26 tablet tests); commits `b7ed366` (projects + specs), `e71f5f6` (prosthetics dashboard overflow fix + spec alignment), `f56f448` (strict-mode locators); CI runs `32305140094` (failed: 3 test bugs + 2 REAL responsive bugs) → `32307157147` (failed: 2 strict-mode locators) → `32308478131` all 6 jobs green (E2E 14m6s); issue #165 closed. **Projects**: `responsive-mobile-chromium` (iPhone 13 emulation, chromium, `.auth/doctor.json`, testMatch `mobile-nav`/`touch-targets`/`mobile-wizard-smoke`, `fullyParallel: false`) + `responsive-tablet-chromium` (Desktop Chrome 768×1024 `hasTouch`, testMatch `no-horizontal-scroll`/`tablet-dashboard`, `fullyParallel: false`), both after `prosthetics-chromium`, deps `['setup']`. **Specs** (`tests/specs/responsive/`): mobile-nav (hamburger → sheet → route → auto-close), touch-targets (44px CTAs incl. create-card patient search «Ткачук Андрій Вікторович» — mock MIS has 3 Ткачуків — and episode «Панель пацієнта»), mobile-wizard-smoke (API-created instances auto-start IN_PROGRESS → wizard opens directly on stage 1, no «Розпочати процес» screen; completes stage 1 at 360px via `WizardExecutionPage`; strict-mode: stage texts match both title + step chip → `.first()`), no-horizontal-scroll (docWidth audit at 360+768; offenders inside `overflow-x-auto` ancestors skipped — tables/scroll containers legitimately extend), tablet-dashboard (rail `w-[60px]`→`w-[220px]` expansion polled past the CSS width transition; stat cards «Активні»/«Призупинені» share a row in the 2-col grid). **REAL bugs found + fixed** in `pages/prosthetics/DashboardPage.tsx`: header row `flex-wrap` (5px overflow at 360px) + instances table wrapped in `overflow-x-auto touch-pan-x` (1026px at 768px). Episode page needs the full seed UUID (`a3333333-3333-3333-3333-333333333333` — short form 404s «Епізод не знайдено»). Suite now 268 tests / 59 files / 11 projects.

**2026-08-19: Responsive UI Phase 5 — touch targets, safe-area sheets, WCAG focus rings (issue #164)** — commits `cc0c0f0` + `992a662` (Vitest fixes), CI run `32300494295` all 6 jobs green (E2E 10m48s), issue #164 closed. First run `32299674854` failed 3 new Vitest assertions → fixed in `992a662`. **Touch targets** (`pointer-coarse:` variant — fires only on coarse pointers, inert in desktop E2E and jsdom): `button` base `pointer-coarse:min-h-11`; `icon`/`icon-xs`/`icon-sm`/`icon-lg` sizes `pointer-coarse:size-11` (incl. `HourlyGrid` cell buttons — grid rows grow to 44px on touch by design); `checkbox`/`switch` enlarge the hit area via invisible `after:` pseudo-element (`after:-inset-x-3.5 after:-inset-y-3.5`); `radio` same via `relative` + `pointer-coarse:after:absolute after:-inset-3.5 after:rounded-full after:content-['']`; `SelectTrigger`/`Input`/`TabsList`/`TabsTrigger` `pointer-coarse:min-h-11`; textarea skipped (min-h-16 ≥44). **Safe areas**: `SheetContent` per-side `env(safe-area-inset-*)` with 1.5rem fallback via `pt-[max(1.5rem,env(safe-area-inset-top))]`-style longhands in `sideClasses` (base `p-6` kept — longhand wins; `pb-` fallback matches the `bottom` side). **WCAG 1.4.11 focus rings**: all `ring-ring/50` → `ring-ring` full opacity in ui primitives AND the 5 feature copies (`PatientDayPage:303`, `MedicalNotesPanel:27`, `VitalSignsForm:189`, `PatientStatePanel:103`, `ScaleResultsPanel:59`) + destructive variants; radio `ring-1`→`ring-2`. Rationale: `ring/50` ≈1.7:1 fails; full `#FF5F33` vs light bg ≈3.0:1 + border change = composite ≥3:1 (same pattern as the `index.css` fullscreen override from #137). **Touch feedback**: `active:translate-y-px` on `NavLink` + interactive Stepper `StepIndicator`; `touch-pan-x` on 15 horizontal scrollers (tabs list, wizard chips, ProcessLayout rail, tables, Breadcrumbs, AdminPage matrix, AuditLogTable, HourlyGrid…). **Contrast**: tabs inactive `text-foreground/60` → `/70` (4.26→5.99:1); `muted-foreground` audited — passes AA everywhere (light `#5A5A5A` 6.6/6.6/5.6; dark `#A0A0A0` 7.4/6.2/5.4) → unchanged; primary white-on-orange 3.0:1 kept as brand token (documented out of scope). **Reduced motion**: media gate extended with `fade-in-up`, `fade-in`, `slide-in-left`, `scale-in`, `pulse`, `skeleton`, `[data-slot=stepper-loading]`, `dialog-content`, `dialog-overlay`, `select-content`. **Perf**: `AppNavList` links memoized via `useMemo` (no re-render on every route change); no JS resize listeners anywhere. **Tests**: new `ui/a11y.test.tsx` (roles + coarse-pointer class contract) + `sheet`/`stepper`/`GlobalLayout` extensions. Base UI render reality (lessons for future tests): Select accessible name must go on `SelectTrigger` (Root renders no DOM); Base UI Input emits NO `type` attribute when `type` omitted → `input[type="text"]` selectors return null, use `[data-slot="input"]`; Base UI Drawer does NOT set `aria-modal` → sheet test asserts `role="dialog"` + accessible name only.

**2026-08-19: Responsive UI Phase 4 — wizard steppers, sticky CTAs, mobile dialogs (issue #163)** — commit `f73bc58`, CI run `32286616343` all 6 jobs green (E2E 10m28s), issue #163 closed. Layout/interaction markup only; no Vitest spec updates were needed (all assertions are role/text-based; responsive classes are inert in jsdom). `SetupSteps.tsx` rewritten on the `Stepper` primitive (`size="md"`, `gap-1.5`): each step title is `hidden md:inline` except the active one → mobile shows compact dots + active label only (labels verified rendered exactly once in `SetupSteps.test.tsx`); App.tsx routes the `*Page.tsx` wrappers, the `*Step.tsx` components are Vitest-only. Setup pages (`PatientSearchPage`, `OrderSelectPage`, `OrderReviewPage`, `TemplateSelectPage`) + `WizardScreen`: sticky bottom action bars `sticky bottom-0 z-10 -mx-4 sm:-mx-6 border-t bg-background/95 backdrop-blur` with `pb-[max(0.75rem,env(safe-area-inset-bottom))]` (safe-area) and stacked `flex-col sm:flex-row` buttons (`w-full sm:w-auto`); `WizardScreen` also fixed the top-bar `-mx-6` overflow in the `p-4` mobile `ProcessLayout` main, progress row `flex-wrap`, stage chips `overflow-x-auto` + `shrink-0 whitespace-nowrap`. **Mobile fullscreen dialogs**: `DialogContent` gains `mobileFullscreen?: boolean` → `data-fullscreen="mobile"`; new `index.css` rules under `@media (max-width: 639.98px)` — `inset: 0`, full width/height, `border-radius: 0`, `translate: none` (Tailwind v4 translate utilities use the `translate` property, not `transform`), overlay `backdrop-filter: none` + dark tint; existing `[data-fullscreen]` modalMorph animations apply automatically. Used by `ClosePrescriptionDialog` + WizardScreen pause/fail dialogs; `HourlyGridDialog` passes `data-fullscreen="true"` via `{...props}` spread AFTER the new attribute → unaffected. Touch targets: all wizard secondary buttons + CTAs, `QualityGatePanel` criteria rows (`min-h-11`) and decision buttons (`flex-col sm:flex-row`, `w-full sm:w-auto`), `MedicineSearchInput`/`PrescriptionItemForm` dropdown rows + add buttons, `VitalSignForm` save, `DayPartPlanner` plan/complete buttons, `ScaleResultsPanel` select/input/Додати. ICU scale forms: `ApacheIiForm` grid → `grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4`, `SofaForm`/`BradenForm` → `grid-cols-1 sm:grid-cols-2 md:grid-cols-3`; all four `h-7 text-xs` inputs replaced with `pointer-coarse:min-h-11` (touch-only sizing); `CamIcuForm` labels + `RassSelector` trigger likewise. Verified already-compliant (no edits): `PrescriptionSpreadsheet` horizontal `overflow-auto` + sticky first column (Task 7), `MeasurementForms` `inputMode="decimal"` + `flex-col md:flex-row` (Task 4).

**2026-08-19: Responsive UI Phase 3 — dashboard grids, sticky matrix, mobile ICU sidebar sheet (issue #162)** — commit `199e837`, CI run `32283844103` all 6 jobs green, issue #162 closed. Dashboard column flows: `ProstheticsDashboard` stats `grid-cols-1 sm:grid-cols-2 lg:grid-cols-3`; doctor/nurse dashboards full-width search + wrapping action rows; `DepartmentDashboard` 1/2/3 card columns + metric wrap. Tables: `EpisodeTable`/`PrescriptionTable` `min-w` + `overflow-x-auto`, 44px rows. `AdminPage`: stats 1/2/4 columns, RBAC matrix sticky first column (`sticky left-0` + bg) with horizontal tab scroll. **ICU**: `IntensiveCareCard` restructured — `PatientSidebar` now `hidden` below 1024px (was always-visible with `isMobile` single-column); new `lg:hidden` «Панель пацієнта» icon button (`PanelRightOpen`, top bar, aria-label) opens the SAME shared `sidebarContent` element inside a `Sheet` (no markup duplication); `HourlyGridDialog` mobile bottom action bar + safe-area confirmations.

**2026-08-19: Responsive UI Phase 2 — responsive navigation & shell (issue #161)** — commit `e91d396`, CI run `32274123606` all 6 jobs green, issue #161 closed. `AppSidebar`: extracted `useNavItems` + `AppNavList` (single source of truth shared by desktop rail and mobile Sheet nav); tablet band defaults to a collapsed icon rail (tooltips on icons); sidebar unmounts on mobile — `GlobalLayout` hamburger opens a right-side `Sheet` with the shared `AppNavList` + user footer, auto-closes on route change; header gains safe-area padding, title truncation, 44px touch targets. `ThemeToggle` `size-11` → `sm:size-8` (44px on mobile). `ProcessLayout`: mobile top-tab bar (Огляд/Історія) with touch-target items; desktop right rail unchanged (touch targets added). `Breadcrumbs` `overflow-x-auto`. `index.html` gains `viewport-fit=cover` for safe-area support. Tests: `src/test/setup.ts` matchMedia mock made mutable + `setMatchMediaQuery` helper (reset in afterEach); `AppSidebar.test.tsx` AuthContext mock path fixed (was one level too deep — real hook was being used, failing with 'useAuth must be used within AuthProvider'); new `GlobalLayout` mobile sheet flow test (open, nav, close-on-navigate).

**2026-08-19: Responsive UI Phase 1 — audit & design foundation (issue #160)** — commits `0f6369c` + `2f623ba` (test fix) + `8402293` (docs), CI run `32230291689` all 6 jobs green (E2E 14m41s, Vitest 627/627), issue #160 closed. Audit doc `docs/Responsive-UI-Audit-Phase1.md` — screen map of all 28 routed pages with responsive status + Phase 2 plan. Breakpoint strategy locked: mobile <640px / tablet 640–1024px (`sm:`…`lg:`, PRIMARY target) / desktop >1024px (`lg:`+); CSS-first (variants, no JS layout logic); tokens in `index.css` `@theme`: `--breakpoint-sm: 40rem`, `--breakpoint-md: 48rem`, `--breakpoint-lg: 64rem`, `--breakpoint-xl: 80rem`. New shared hooks `hooks/useMediaQuery.ts`: `useMediaQuery(query)` (SSR-safe via `useSyncExternalStore`) + `useIsMobile()` = `(max-width:639.98px)`; `src/test/setup.ts` static matchMedia mock. New Base UI primitives (only runtime dep added: `@base-ui/react`): `ui/sheet.tsx` (Drawer-based slide-over, `side` prop, plain-function Root — popup renders no DOM; `swipeDirection` default `"down"`), `ui/stepper.tsx` (custom shadcn-style: `Stepper`/`Step`/`StepIndicator`/`StepSeparator`/`StepTitle`/`StepDescription`/`useStepper`, 1-based `step`, `nonLinear` + `onStepClick`, `Check`/`Loader2` icons), `ui/scroll-area.tsx` (Base UI ScrollArea with `keepMounted` scrollbars so both rails render in jsdom). Inline matchMedia copies removed from `ui/Sidebar.tsx` (`SidebarProvider` offcanvas) + `monitoring/IntensiveCareCard.tsx` (single-column) — both use shared `useMediaQuery('(max-width:1023.98px)')` (desktop detection moved 1200px → 1024px). First CI run failed on a jsdom ScrollArea `Corner` assertion (Corner is null while hidden) → dropped in `2f623ba`.

**2026-08-18: Distinct `PRESCRIPTION_LIST_CREATE` permission (create a prescription LIST, separate from `PRESCRIPTION_CREATE` planning)** — new RBAC code `PRESCRIPTION_LIST_CREATE` «Створення листка лікарських призначень» (catalog 24→25, `PermissionCatalog` — new constant + `Def` + DOCTOR/HOD default-grant rows; `PRESCRIPTION_CREATE` «Створення призначень» label kept, its description narrowed to "Планування призначень: додавання позицій у листок, планування та скасування доз"). `POST /api/prescriptions` (`PrescriptionController:70`) now gated on `PRESCRIPTION_LIST_CREATE`; item/plan/execute endpoints keep `PRESCRIPTION_CREATE`. Seeding: definition row added as a NEW Liquibase changeset `core/005-role-permissions-add-prescription-list-create.sql` (registered in `db.changelog-master-core.yaml`; `003` left untouched to preserve applied-changeset checksums); DEFAULT GRANTS are Java-seeded by `PermissionService.seedIfEmpty()` from `PermissionCatalog.defaultMatrix()` (which now includes `PRESCRIPTION_LIST_CREATE` for DOCTOR + HOD, not NURSE) — so `005` intentionally adds only the definition row, never grants (pre-inserting grants would flip `rolePermissionRepository.count()` non-zero and suppress the full default-matrix seed on a fresh install). Frontend: `PrescriptionPage.tsx` gates the drawer «Створити листок» button on `hasPermission('PRESCRIPTION_LIST_CREATE')` (destructure `hasPermission` from `useAuth`). `PermissionServiceTest` updated: catalog `hasSize(25)`, `PRESCRIPTION_LIST_CREATE` in `allCodes()`. Pre-flight green (mvn compile, oxlint 0 errors, tsc clean).

**2026-08-18: Phase 8 — documentation update (issues #157/#158 milestone complete)** — AGENTS.md + README.md rewritten (commit `0f015b6`) to reflect the post-refactor reality: backend Maven module layout with dependency direction (`common` leaf ← `icu-chart`/`medication-sheet`/`prosthesis-manufacturing` ← `app` shell), real frontend layout (`pages/`, per-feature `components/`, `api/` + `types/` modules, isolated `prosthetics/`), new «Module Boundaries» section (ArchUnit allowlist + oxlint `no-restricted-imports`), route prefixes `/icu/doctor/*`/`/icu/nurse/*`, Playwright 9 projects / 6 roles, UserRole 7 values, services grouped by module (common 4, icu-chart 16, med 11, prosth 11), Liquibase 15 changesets (core 4, icu 6, med 1, prosth 4), file counts (Java 348 main/112 test; TS 127 sources/69 test files; E2E 55 specs/~228 tests). README: MUI/Emotion → Base UI + Tailwind CSS (badges, Tech Stack, Project Structure tree), JDK 25, stale test counts replaced with file-based numbers, Module Boundaries paragraph. UseManual.md unchanged (no user-facing change). CI run `32119777622` all 6 jobs green (Code Quality 49s, E2E 11m55s); issue #158 closed. Docs-only commits — Phase 7 (import boundaries, `e7aab6f` + `cf874b2`, run `32116832684` green, #157 closed) and Phase 8 close the Module Separation milestone; only the historical removal notes mention `PRESCRIBER`/`user.permissions`/`src/medication-sheet` — no live references remain.

## CI RULE (EXECUTE TEST SUITE (UNIT, INTEGRATION, PLAYWRIGHT E2E TESTS) ONLY LOCALLY)

**TESTS RUNNING.** The only valid testing workflow is:

```
ASK USER RUN LOCALLY OR execute workflow: EXECUTE TEST SUITE (UNIT, INTEGRATION, PLAYWRIGHT E2E TESTS) -> FIX FAILURES -> RUN TESTS AGAIN IF FAILS -> COMMIT AND PUSH IF ALL TESTS ARE GREEN
```

ALL test suites: unit tests, integration tests, Playwright E2E.
Local `mvn test` is FORBIDDEN. Local `mvn compile` is permitted for verifying compilation only.
This rule is documented in AGENTS.md, README.md, and checked by CI pipeline.

---

## Current Session

**2026-08-18: Phase 7 — import boundaries enforced with oxlint `no-restricted-imports`** — `.oxlintrc.json` `overrides` (per-directory rule sets, oxlint 1.73): `pages/prescription` → forbid `components/icu` + `components/monitoring`; `pages/prosthetics` → forbid `components/icu` + `components/prescription`; `components/icu` → forbid `components/prescription` + `components/monitoring`; `components/prescription` → forbid `components/icu` + `components/monitoring`; `components/common` → forbid all feature components (`icu`, `monitoring`, `prescription`, `prosthetics`). Patterns are `regex: "(^|/)icu/"`-style — they match BOTH relative specifiers (`../monitoring/criticalRanges`) and the `@/` alias form (`@/components/icu/...`) at any depth. Only straggler fixed: `clinicalRanges.ts` (shared validation bounds, single importer `components/icu/VitalSignsForm.tsx`) moved `components/monitoring/` → `lib/` (its doc comment still warns against merging with `components/monitoring/criticalRanges.ts` — alarm thresholds, distinct concept). Acceptance verified locally: deliberate cross-feature imports (relative + `@/` alias) fail `npm run lint`; full lint 0 errors; build green. CI `format-check` runs the same `npm run lint` with the project config, so the rules are enforced automatically.

**2026-08-18: Phase 6 — ICU-only frontend components/helpers moved out of shared** — commits `a0e654f` (docs) + `1238830` (refactor: 16 components + 6 scales + 3 helpers + 18 tests moved from `components/common/`, `hooks/`, `utils/`, `constants/` into `components/icu/`, `components/prescription/`, `components/monitoring/`; `common/` keeps only PatientSearch/ThemeToggle/AuditLogTable; empty `src/medication-sheet/` deleted). **E2E flake #2 root cause (run `32109411749`, nurse tests at 07:06Z) — NOT a Phase 6 regression**: the med-day is 08:00→07:59, and `HourlyGrid.isPastMedDay(h, realClockHour)` renders every hour before the current real hour as `✓` (non-clickable — `clickable` false in `TherapyCell`). In the 07:00–07:59 UTC window the ONLY clickable hour is the current one (7:00). `nurse-day-flow.spec.ts` planned `realHour+1+retry` → 8/9/10 → all past → click opened nothing → `planInput` never appeared (line 114). The snapshot also showed hour 7:00 already planned with `1000` — that's `modal-therapy.spec.ts`'s leftover (it plans the CURRENT real hour on the same day/order with DOSE `1000` and restores the plan; a doctor can re-plan a planned cell, so `order-execution.spec.ts` — also current-hour — passed). Fix: both nurse specs plan at the CURRENT real hour (never past); `nurse-day-flow` scans up to 24 cells from the current hour for the first non-`✓`/`✕` cell (free `➚` or already-planned dose — re-plannable); `order-execution` switched from the contended `Glucose 5%` (`d3333001`) to the runtime-created `Dobutamine` order (doctor-day-flow creates it earlier in the same run; free in every hour). Verified green for all 24 real hours; residual edge (accepted): a retry after a completed attempt in the 07:00–07:59Z window finds no free Glucose cell.

**2026-08-18: Phase 5 — frontend API clients & DTO types split into per-feature modules**

`frontend/src/api/endpoints.ts` + `frontend/src/types/index.ts` deleted (no barrels). New modules: `api/platform.ts` (authApi, patientApi, userApi, settingsApi, auditApi, adminApi), `api/icu.ts` (episodeApi, clinicalDayApi, hourlyRecordApi, medicalOrderApi, orderExecutionApi, medicalNoteApi, clinicalScaleApi, fluidBalanceApi, pdfApi, patientStateApi, ventilationApi, labResultApi, departmentApi), `api/medication.ts` (prescriptionApi, vitalSignApi); types likewise split into `types/{core,icu,medication}.ts` (core holds shared `PatientDto` + auth/RBAC/audit DTOs). `patientApi`/`PatientDto` live in platform/core because medication-sheet pages consume them (mirrors backend MIS-in-common). `api/client.ts` + `api/prosthetics.ts` + `prosthetics/types.ts` unchanged (prosthetics was already isolated). Every importer migrated to direct relative imports (`AuthContext.tsx` now imports `../api/platform`); no feature file imports another feature's API/types module; `endpoints.test.ts` and all test `vi.mock` factories retargeted (PatientDayPage.test.tsx mocks icu + platform separately). Automated via temp script `migrate_imports.py` (statement-aware, symbol→module map; prosthetics `./types`/`@/prosthetics/types` untouched).

Phase 5 shipped: commits `871951f` (migration + barrel deletion, 78 files) + `6116206` (E2E fix), CI run `32106534596` all 6 jobs green, issue #155 closed. **E2E flake root cause (found during Phase 5 CI runs `32104710009`/`32105901291`, 05:54/06:11Z) — NOT a Phase 5 regression**: `security-rules.spec.ts` planned a FIXED hour 6 on seed order `d3333001`, but `tests/specs/nurse/order-execution.spec.ts:31` plans+completes the REAL current hour (`new Date().getHours()`) on the same order — at CI times 05:00–06:59 UTC real hour = 6 → `OrderExecutionService.plan` throws `DocumentLockedException` → 422 `DOCUMENT_LOCKED` "Execution for this hour is already completed" → `expect(doctorPlan.ok()).toBeTruthy()` false. Confirmed from the trace artifact (retry1 `trace.zip`, response body sha1 `16c436ce…`). Fix (`6116206`): plan hour is now dynamic `new Date(Date.now() + 2 * 3600_000).getHours()` — offset from the real hour, so it never collides at any CI time (also distinct from `nurse-day-flow`'s `realHour+1`).

**2026-08-17: Phase 4 — legacy `users.permissions` CSV removed from the auth flow**

- `LoginResponse` no longer carries the legacy CSV permissions field; `AuthService` no longer populates it; `AuthController` mints JWTs via the 3-arg `JwtTokenProvider.generateToken(login, role, userId)`; the JWT `permissions` claim, the 4-arg `generateToken` overload and `getPermissionsFromToken` are deleted (no consumers existed — enforcement is matrix-based via `PermissionService`). `AuthContext.tsx` + TS `User`/`LoginResponse` types drop the legacy field; effective permissions come solely from `GET /api/users/me/permissions`. The `users.permissions` DB column, entity field and `002-user-permissions.sql` changeset remain untouched (schema never edited); `User.hasPermission/addPermission/removePermission` stay as entity utilities.

**2026-08-07: Module-routing permissions (RBAC matrix)**

- **Module-routing RBAC**: 4 new permission codes in category «Модулі» — `MODULE_ICU_ACCESS`, `MODULE_MEDICATION_ACCESS`, `MODULE_PROSTHETICS_ACCESS`, `MODULE_ADMIN_ACCESS` (catalog 20 → 24 codes; SQL-seeded in `012-role-permissions.sql`). Defaults: DOCTOR/NURSE/HOD → ICU+MEDICATION, PROSTHETIST/PROSTHETICS_ADMIN → PROSTHETICS, ADMINISTRATOR/AUDITOR → ADMIN. `AppSidebar` + `AppSelectorPage` render a module only when the role holds its permission; `App.tsx` `Guard` accepts `permissions` (access = role **OR** permission — revoking a module permission never locks the role out). Prosthetics **read** endpoints widened to `hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')` — a doctor granted the module can navigate and view (read-only); writes still require the specific `PROSTHETICS_*` codes. E2E `permissions.spec.ts` grants DOCTOR `MODULE_PROSTHETICS_ACCESS` → sidebar link appears → navigation works.

**2026-08-07: Dynamic RBAC — role-permission matrix managed from the admin UI**

Full role-based access control with an admin-editable matrix. `permissions` + `role_permissions` tables (Liquibase `012-role-permissions.sql`), seeded by `PermissionService` on first boot when empty (definitions also SQL-seeded with `ON CONFLICT DO UPDATE`; grants are Java-seeded — edits persist across restarts since seeding only fires when the tables are empty).

- **Enforcement**: URL ceilings in `ClinicalSecurityRules` widened to `CLINICAL_ROLES` (write endpoints), precise enforcement via `@PreAuthorize("@permissionService.has('CODE')")` on controllers (icu-chart, medication-sheet, prosthesis-manufacturing). 403 (not 500) via the existing `AuthorizationDeniedException` handler.
- **Module path operations**: the `MODULE_*_ACCESS` checkboxes in the matrix grant the ability to VISIT a module end to end. Frontend: the `Guard` accepts `permissions` (role OR permission); role-scoped sibling sub-views stay exclusive via `excludeRoles` (a DOCTOR holding `MODULE_ICU_ACCESS` still cannot land on `/icu/nurse`; a NURSE cannot land on `/icu/doctor`; a non-clinical role granted `MODULE_ICU_ACCESS`/`MODULE_MEDICATION_ACCESS` enters via `/icu/doctor`/`/prescriptions/doctor`). Backend: `ClinicalSecurityRules` read rules accept the module permission for the module's GET paths (`access("hasAnyRole(CLINICAL_CORE) or @permissionService.has('MODULE_*_ACCESS')")`) — `CLINICAL_CORE` = DOCTOR/NURSE/HEAD_OF_DEPARTMENT/ADJACENT_SPECIALIST, so ADMINISTRATOR/PROSTHETIST/PROSTHETICS_ADMINISTRATOR read clinical modules only when the checkbox is checked; `GET /api/users/me/**` is `authenticated()` (AuthContext), `GET /api/users/**` needs clinical core or `MODULE_ADMIN_ACCESS`; writes stay ceiling+`@PreAuthorize` gated.
- **Matrix** (defaults): DOCTOR/HOD — episode, clinical day, prescriptions, sign doctor, APACHE II/SOFA + CAM-ICU/Браден/RASS, patient view, modules ICU + medication; NURSE — sign nurse, execute prescriptions, vitals, CAM-ICU/Браден/RASS, patient view, modules ICU + medication; HOD — reopen day (only); ADMIN — patient view + audit access + module «Адміністрування» only (clinical modules are NOT granted by default — the checkbox opens them); PROSTHETIST — dashboard/instance/step/pause + module prosthetics; PROSTHETICS_ADMIN — prosthetics + gate + templates + orders + module prosthetics. HOD removed from prosthetics guards (backend + `App.tsx` routes).
- **Admin UI**: `AdminPage` new tab «Доступи та ролі» — matrix editor (checkbox grid grouped by category, dirty tracking, «Зберегти зміни» diff-saves via `PUT /api/admin/permissions`). Audit tab «Переглянути» gated by `AUDIT_ACCESS` permission. Role dropdown extended with PROSTHETIST / PROSTHETICS_ADMINISTRATOR.
- **API**: `GET /api/admin/permissions` (matrix), `PUT /api/admin/permissions` (grant/revoke, body `{role, permissionCode, granted}`), `GET /api/users/me/permissions` (effective codes for current role). `AuthContext` loads effective permissions and `hasPermission` is matrix-based.
- **Tests**: `PermissionServiceTest` (common unit), `AdminPermissionsIntegrationTest` (grant → 403→400→403 enforcement cycle), E2E `tests/specs/admin/permissions.spec.ts` (UI matrix view + grant/revoke enforcement, serialized).
- The legacy per-user `PRESCRIBER` CSV (`user.permissions` column) is no longer part of the auth flow (removed 2026-08-17, see Current Session); the column and entity field remain for schema/data compatibility; `ScaleAuthorizationService` is now permission-driven (DOCTOR may create Браден per matrix).

### Previous sessions (condensed):

**2026-08-06: Prosthetics E2E verification — quality-gate flow fixed (gate guards stage entry)**

Full headed-browser E2E of the prosthetics flow verified end-to-end (prosthetist1 → patient Сніжко → order PR-2026-0001 → template TP-UL-01 → steps 1–4 → admin gate PASS → steps 5–6 → done → PDF → history), instance COMPLETED, 0 errors.

- **Quality gate semantics (backend bug)**: `FlowInstanceService.advance()` previously checked the CURRENT stage's gate (guards EXIT) — with the seed gate on the last stage (d0000004 «Контроль якості») the gate never fired before stage 4 and its steps auto-started; the wizard CTA («Контроль якості →», `WizardScreen.tsx:228–234`) and `QualityGateService.decide()` (gate must be on `currentStageId`) both expect the gate to guard ENTRY to its stage. Fixed `advance()`: completing the last step of a stage now looks at the NEXT stage — if it has a gate → `WAITING_REVIEW` + `currentStageId` = gated stage (no step auto-created); added `enterStage()`; `QualityGateService.pass()` now **enters** the gated stage's steps (was `moveToNextStage`, which skipped them); `rework()` also sets `currentStageId` to the rework stage. Gate flow now: stage 3 → WAITING_REVIEW → admin PASS → stage 4 steps → COMPLETED.
- **Order access for admins**: `ProstheticsOrderController` `GET /orders`, `/{id}`, `/{id}/document` were `hasAnyRole('PROSTHETIST')` — the admin wizard 403'd fetching the order. Widened to `hasAnyRole('PROSTHETIST', 'PROSTHETICS_ADMINISTRATOR')`.
- **403 vs 500**: `GlobalExceptionHandler` had no `AuthorizationDeniedException` handler → `@PreAuthorize` denials surfaced as 500; added a handler → 403 (`ErrorCode.FORBIDDEN`).
- **E2E spec aligned** (`tests/specs/prosthetics/prosthetics-workflow.spec.ts`): gate buttons are «Прийнято (Pass)»/«На доопрацювання»/«Брак (Fail)» (not «Схвалити|Пройдено»); wizard CTAs «Готово →»/«Завершити процес» (not «Завершити крок»); signature element is a toggle button «Область для електронного підпису»/«Підпис отримано» (clicked in `fillFields`). Since the gate requires an admin and the spec runs as prosthetist1, PASS is issued via API: `request` login as prosthetics_admin1 → GET snapshot → find gated stage → `POST /instances/{id}/gates/{gateId}/decision` with `{decision:'PASS', criteriaConfirmed:[...], comment:''}` → reload → continue steps 5–6 → `/done`.
- **UI labels (verified)**: wizard CTAs «Готово →» / «Контроль якості →» / «Завершити процес»; gate «Прийнято (Pass)» (disabled until all criteria checked), «На доопрацювання» (needs comment), «Брак (Fail)»; signature button «Область для електронного підпису» (signed: «Підпис отримано»); DoneScreen «Процес успішно завершено»; PDF `report_{id}.pdf`.

### Previous sessions (condensed):

**2026-08-04: Fullscreen Grid Modal Phase 8–9 — E2E tests + docs (Issues #141, #142, master #133)** — All 6 CI jobs green (run `30911073343`). Commit `35dac79`: modal-therapy E2E (locked day + open-day plan/cancel/restore), modal-grid E2E (nurse edit persistence, a11y, sticky panels), `getNextHourISO` timezone fix (`HourlyGrid.tsx:333–338`), plan-mode ✕ race fix (`onMouseDown preventDefault`). Vitest 419/419, lint 0, tsc clean.

- **Critical ranges extraction**: `CRITICAL_RANGES` + `isCritical()` moved from `HourlyGrid.tsx` into `frontend/src/components/monitoring/criticalRanges.ts` — single source of truth (8 inclusive ranges) shared by rail, chip and cell flash; `''`/NaN/unknown values non-critical; `countCriticalByHour`/`countCriticalTotal` + `pluralCritical` (1 → «критичне значення», 2–4 → «критичні значення», else «критичних значень»; 21/12 plural edges); unit tests in `criticalRanges.test.ts`.
- **Rail & chip**: OutlierRail (pin rail on mobile, highlight in main grid) shows violation cells from the same ranges as cell highlighting and updates after save; alarm chip counter matches critical cells, click focuses the first visible one; no animated/blinking elements in rail/chip (criteria 3); status span must NOT have `role="status"`/`aria-live`; dialog tests use fixture `realClockHour:10`.
- **E2E flake root cause (order-execution.spec.ts)** — NOT a parallel race (CI `workers: 1`): `HourlyGrid.isPastMedDay(h, realClockHour)` marks hours below the current real hour non-clickable (they render '✓'); CI runs ~05:00–08:00Z, so only hours {real, real+1} are ever clickable. The `+13` shift (`aad1568`) targeted hours 20–23 → always past → guaranteed failure (retry2 `Запланувати Glucose 5% 23:00`); fixed in `688398d` to `new Date().getHours()` (the real hour is never past; disjoint from `nurse-day-flow` at real+1; CI uses fresh ephemeral Postgres per run, so no cross-run depletion). Green on first attempt.

### Previous sessions (condensed):

**2026-08-04 (earlier): Fullscreen Grid Modal Phase 5 — edge-case hardening (Issue #138, master #133)** — 409 conflict banner («Оновити дані»/«Залишити мій варіант»), day-lock banner, print CSS for dialog overlay, rapid-toggle guards, `dayLoading` spinner, mobile 44px touch targets + `PatientSidebar` hidden, safe-area insets; 404 capture scoped to episode GET. Commits `6598efd`, `272d654`, `aab6700`. E2E 183 tests.

**2026-07-31: ТЗ v1.2 — SOFA input parameters formalized (docs only)**

Updated `docs/Технічне завдання карта Інтенсивної терапії.md` to v1.2 (2839 → 3026 lines). The ТЗ was brought in line with the existing implementation — verified `SofaCalculator` (all 13 inputs incl. epinephrine), `SofaForm` (4 vasopressors, GCS, creatinine, 24h urine output), `HourlyRecord.meanArterialPressure`; no code changes needed:

- §29/§30: GCS (3–15) field added to general state + hourly monitoring; FiO₂ defined (%); MAP marked auto-calculated with formula `MAP = (2 × ДАТ + САТ) / 3`; new block «Вазопресорна та інотропна підтримка» (допамін, добутамін, норадреналін, адреналін у мкг/кг/хв); діурез мл/год + сумарний за 24 години для SOFA
- §36: одиниці вимірювання (тромбоцити ×10⁹/л; креатинін/білірубін мкмоль/л або мг/дл); `pO₂` → `PaO₂` (визначення); автозапис PaO₂/FiO₂
- §53.2: серцево-судинна оцінка SOFA доповнена адреналіном (≤0.1 → 3, >0.1 → 4), дози у мкг/кг/хв

- 2026-07-30: Clinical scales — episode-level binding, calculator algorithms, E2E tests (Issues #1-#6). `ScaleResult` episodeId/rawData(jsonb); pure-static calculators (ApacheIi, Sofa, CamIcu, Braden); `ScaleAuthorizationService` per-scale roles (APACHE II/SOFA → DOCTOR); episode-level endpoints `GET/POST /episodes/{id}/scales` + `POST .../calculate`; `ScaleFormFactory` + forms (ApacheIiForm, SofaForm, CamIcuForm, BradenForm, RassSelector); PDF episode scales via `findByEpisodeId()`; 13 E2E tests (`scales-episode.spec.ts`, `scales-access.spec.ts`)
- 2026-07-29: 177/177 Playwright tests passing. Pattern A-D (20 fixes), Pattern E-H (additional fixes). DB reset script.
- 2026-07-29 (earlier): Issue #87 prescription list dropdown. Issue #84 GlobalLayout nav. Issue #83 nurse patient list. Cyrillic encoding fix (Issue #82). Global theme. PrescriptionGrid.
- 2026-07-26: Medication Sheet backend — Phase 3-6 complete (EmailService, PrescriptionSchedulerService, controllers + security, integration tests, docs). 422 backend tests.
- 2026-07-25: Exploratory testing — 5 bugs fixed. Model QA audit — grade B, all gaps fixed. 27 validation tests.

## MIS Data Policy (DO NOT VIOLATE)

**The ICU Chart module is a READ-ONLY client of MIS.** Only data retrieval from MIS is permitted. The sole exception is sending generated PDF documents to the patient's document repository.

| Operation | Status | MIS method |
|---|---|---|
| Search patients | ✅ ALLOWED (read) | `spzIBPatientSearch` |
| Get patient by ID | ✅ ALLOWED (read) | `spzIBPatientSearch` |
| Get hospitalization / schedule | ✅ ALLOWED (read) | `spzIBPatientScheduleList` |
| Get user profile | ✅ ALLOWED (read) | `spzIBUserDetails` |
| Get department users | ✅ ALLOWED (read) | `spzIBUserDetails` |
| Get departments | ✅ ALLOWED (read) | `spzIBCompanyDetails` |
| Get dictionaries | ✅ ALLOWED (read) | `spzIB*Dictionary` |
| Send PDF to MIS | ✅ ALLOWED (exception) | `sendPdf()` (transfers immutable PDF, no record modification) |
| Create patient | ❌ FORBIDDEN | `spzIBPatientCreate` — must never be called |
| Create schedule/appointment | ❌ FORBIDDEN | `spzIBScheduleCreate` — must never be called |
| Save agent/insurance | ❌ FORBIDDEN | `spzIBAgentSave` — must never be called |
| Save institution/venue | ❌ FORBIDDEN | `spzIBInstitutionSave` — must never be called |
| Any other MIS mutation | ❌ FORBIDDEN | All `*Save`, `*Create`, `*Update`, `*Delete` methods |

**Rule:** The `MisApiClient` only supports GET-style calls to `/api/run`. Any MIS write endpoint must never be implemented or called. Violating this policy will corrupt MIS data integrity.

## Architecture

```
frontend/  (React 19 + TypeScript 6 + Vite 8 + Tailwind CSS 4 + Base UI, single app — no feature subfolder roots; pages/components live at `src/` root)
  src/pages/               ← route pages: LoginPage, AppSelectorPage, doctor/, nurse/, prescription/, prosthetics/, admin/
  src/components/          ← per-feature components: icu/, monitoring/, prescription/, common/, prosthetics/, navigation/, ui/ (shadcn-style Base UI components)
  src/api/                 ← per-feature API clients: client.ts (axios instance), platform.ts, icu.ts, medication.ts, prosthetics.ts
  src/types/               ← shared DTO types: core.ts, icu.ts, medication.ts (prosthetics types live in src/prosthetics/types.ts)
  src/prosthetics/         ← prosthetics feature root: ProstheticsContext, types, validation, failureCategories
  src/services/, src/layouts/, src/lib/, src/utils/  ← AuthContext, Doctor/Nurse/Global layouts, shared helpers
backend/   (Spring Boot 4.1.0 + Java 25 + Maven, multi-module; dependency direction: common ← feature modules ← app)
  pom.xml                   ← parent POM (pom packaging, 5 modules)
  common/                   ← shared platform leaf (no internal deps; 119 main sources): `@SpringBootApplication` main class `com.superhumans.IcuPatientChartApplication` (mainClass of the runnable JAR), platform controllers (auth/user/patient/admin/audit/settings/mock-MIS), `entity/base` (BaseEntity) + `entity/core` (User, UserRole, Permission, RolePermission, AuditLog, SystemSettings, ReferenceValue), `repository/core`, auth (JWT), config (security, CORS, multi-DB wiring, SpringContext), exception, mapper, mis, service (AuthService, AuditService, PermissionService, PermissionCatalog), util
  icu-chart/                ← ICU chart feature (84 main sources): `com.superhumans.icu.*` (entities + repositories) + ICU domain root packages (controller ×13, service ×20, dto, mapper); depends on common
  medication-sheet/         ← medication sheet feature (61 main sources): `com.superhumans.medicationsheet.*` (entity/dto/repository/service/controller/mapper/config); depends on common
  prosthesis-manufacturing/ ← prosthetics manufacturing feature (84 main sources): `com.superhumans.prosthesismanufacturing.*` (entity/dto/repository/service/controller/mapper/config); depends on common
  app/                      ← deployable shell (no production code): depends on common + 3 features; the spring-boot plugin repackages the runnable JAR (mainClass in common); hosts the ArchUnit boundary test (`app/src/test/java/com/superhumans/architecture/ModuleBoundaryTest.java`)
tests/     (Playwright 1.61)
```

After login, user lands on `/select` (AppSelectorPage) and picks a sub-app. Routes are prefixed per sub-app:
- `/icu/doctor/*`, `/icu/nurse/*` → ICU chart
- `/prescriptions/doctor/*`, `/prescriptions/nurse/*` → Medication sheet
- `/prosthetics/*` → Prosthetics manufacturing
- `/admin/*` → Admin

- JWT auth delivered via an **httpOnly `jwt` cookie** (SameSite=Lax) set on login and cleared on logout; axios uses `withCredentials: true` with no Authorization header. `localStorage` holds only a lightweight `auth:session` flag - never the token.
- Backend port: **8085** (`application.yml`).
- **Databases (PostgreSQL 16, one per module)** — 4 physical DBs, `ddl-auto: none`, schema per DB managed by its own Liquibase changelog (15 SQL changesets total: core 4, icu 6, med 1, prosth 4 — all in `common/src/main/resources/db/changelog/{core,icu,med,prosth}/`):

  | Database | Module | Purpose / contents |
  |---|---|---|
  | `my_fullstack_core` | COMMON (single-deployment core) | Users & authentication, dynamic RBAC (`permissions` + `role_permissions` matrix), audit log (`audit_logs`), system settings and reference values |
  | `my_fullstack_icu` | ICU Chart | Episodes, clinical days, hourly records, medical orders & executions, notes, clinical scale results, signatures, generated PDFs, labs, ventilation, patient state, fluid balance |
  | `my_fullstack_med` | Medication Sheet | Prescription lists/items/days/parts/executions/signatures, vital sign lists, medicine/allergy/drug-interaction caches, telegram subscriptions |
  | `my_fullstack_prosth` | Prosthetics Manufacturing | Patients, orders, flow templates, flow instances & step executions, quality gates & decisions, failure snapshots, evidence files |
  | `my_fullstack_db` | — (bootstrap only, **not used by the app**) | Default database auto-created by the PostgreSQL Docker service container in CI (`POSTGRES_DB` env var, required by the image); the application never connects to it — all CI DB-using jobs create the 4 real databases above inside that container (`CREATE DATABASE` ×4) |

  - Datasources configured in `application.yml` under `app.datasource.{core,icu,med,prosth}.{url,username,password}` (env override: `APP_DATASOURCE_*_URL/USERNAME/PASSWORD`); multi-DB bootstrap in `com.superhumans.config.multidb` (per-DB `DataSource`/EMF/`SpringLiquibase`/`JpaTransactionManager`, chained `transactionManager`).
- Seed data: `SeedDataInitializer` (COMMON) runs `data-{core,icu,med,prosth}.sql` on the matching datasource at boot (gated by `app.seed-data.enabled=${APP_SEED_DATA_ENABLED:true}`; tests disable it). The `prod` profile sets `app.seed-data.enabled: false`; `SeedDataGuard` fails startup if `prod` + seeding are both on. Counts: 9 users (6 core roles + 3 prosthetics), 50 episodes, 90 clinical days, 360 prescription lists, 90 vital sign lists, prosthetics 2 patients/2 orders/2 templates.
- CI: `.github/workflows/playwright.yml` — Postgres service, JDK 25, Node 22, Playwright chromium, 40min timeout. Every DB-using job creates the 4 DBs (`CREATE DATABASE` ×4) and passes the 12 `APP_DATASOURCE_*` env vars.
- MIS data served by embedded WireMock server (classpath `mis-wiremock/` fixtures); `MisApiClient` → POST `/api/run`.

## Module Boundaries (enforced)

**Backend — ArchUnit** (`app/src/test/java/com/superhumans/architecture/ModuleBoundaryTest.java`, runs in `backend-test`): the feature namespaces `com.superhumans.medicationsheet..` and `com.superhumans.prosthesismanufacturing..` may depend ONLY on:
- their own namespace,
- the shared platform allowlist: `entity.base`, `entity.core`, `repository.core`, `exception`, `mis`, `util` (com.superhumans),
- exact classes `AuditService`, `PermissionService`, `SpringContext`,
- third-party runtime packages (java/jakarta/lombok/org.springframework/org.mapstruct/org.hibernate/org.slf4j/com.fasterxml.jackson/com.itextpdf/io.swagger).

Everything else under `com.superhumans` — the ICU domain root packages (`controller`, `service`, `dto`, `entity`, `mapper`, `repository`) and `com.superhumans.icu.*` — is off-limits to features. Features must not depend on each other, and platform code must not depend on feature packages. The ICU feature is NOT subject to the allowlist: it lives in the platform packages by design (episodes, clinical days, orders, notes, scales, PDF, audit).

**Frontend — oxlint** (`frontend/.oxlintrc.json` `overrides` with `no-restricted-imports`, enforced by CI `format-check` via `npm run lint`): `pages/prescription` → forbid `components/icu` + `components/monitoring`; `pages/prosthetics` → forbid `components/icu` + `components/prescription`; `components/icu` → forbid `components/prescription` + `components/monitoring`; `components/prescription` → forbid `components/icu` + `components/monitoring`; `components/common` → forbid all feature components (`icu`, `monitoring`, `prescription`, `prosthetics`). Patterns are regex-based and match both relative specifiers and the `@/` alias form; shared code (api/, types/, lib/, utils/, ui/, navigation/) is importable from everywhere. `src/prosthetics/` is a fully isolated feature root (own API client, types, context).

## Repeatable CI Development Workflow (THE Loop)

**All tests run exclusively via GitHub Actions CI — never locally (may be violated if the user explicitly states so).** Local `mvn test` / `npm test` / Playwright are FORBIDDEN; `mvn compile`, `npm run lint`, `npx tsc --noEmit` are permitted for pre-flight only.

The complete development loop:

```
1. PRE-FLIGHT   → local checks only (fast feedback, no tests)
2. IMPLEMENT    → make code changes for the issue
3. STAGE/COMMIT → git add intended files only + Conventional Commits message
4. PUSH/TRIGGER → git push → GitHub Actions starts automatically
5. POLL         → gh run watch / periodic gh run list until all jobs finish
6. TRIAGE       → failing jobs: view logs, download artifacts, root-cause, fix code or tests
7. REPEAT       → steps 3–6 until every check passes
```

### Phase 0 — Pre-flight (local, test-free)
| Check | Command | Catches |
|---|---|---|
| Backend compiles | `mvn compile` (in `backend/`) | Compile errors |
| Frontend lint | `npm run lint` (in `frontend/`) | Oxlint violations |
| Frontend types | `npx tsc --noEmit` (in `frontend/`) | TypeScript errors |
| Frontend build | `npm run build` (in `frontend/`) | Production build breaks |

### Phase 1 — Implement
- Scope changes to the GitHub issue; do not touch unrelated files (medication-sheet module is off-limits except clinical scales).
- Fix defects in a NEW commit on failure — never amend or force-push.

### Phase 2 — Stage & commit
- `git status` / `git diff` first; stage ONLY intended files (never secrets, never `playwright-results/`).
- Conventional Commits: `feat:` / `fix:` / `refactor:` / `docs:` / `chore:` / `test:`.

### Phase 3 — Push & trigger
- `git push origin main` (or push a feature branch and open a PR to `main`).
- Workflow `.github/workflows/playwright.yml` triggers on push to `main`/`develop` and PR to `main`; the push output prints the run URL.

### Phase 4 — Poll
- `gh run list --limit 5` → find the run ID; `gh run watch <run-id>` blocks until completion, or poll with periodic `gh run list`.
- `gh run view <run-id>` → per-job status; `gh run view <run-id> --job <job-id> --log` → failed-job logs.

### Phase 5 — Triage failures
| Job (actual ID) | What it runs | Failure artifacts (`gh run download <run-id> -n <name> -D <dir>`) |
|---|---|---|
| `format-check` | Checkstyle + oxlint + `tsc --noEmit` | — |
| `backend-test` | `mvn clean test` (unit, PostgreSQL service) | `backend-test-results` (surefire-reports) |
| `backend-integration` | `mvn test -Pintegration-test` | `backend-integration-results` |
| `frontend-test` | Vitest + production build | `vitest-coverage` |
| `e2e-test` | Playwright (67 spec files, chromium, 40-min timeout; `needs: backend-test, frontend-test`) | `playwright-report`, `playwright-test-results` |
| `build` | JAR + frontend dist artifacts (main push only; needs all 5 jobs) | — |

### Exit criteria
All checks pass: `format-check`, `backend-test`, `backend-integration`, `frontend-test`, `e2e-test` (plus `build` on `main`). Green run = done; start the next issue at Phase 1.

## Commands

### Backend (`cd backend`)
| Command | Action |
|---|---|
| `mvn -pl app spring-boot:run` | Dev server on `:8085` |
| `mvn clean package -DskipTests` | Build JAR |
| `mvn compile` | Compile only |
| `mvn test` | Run unit tests (excludes integration) |
| `mvn test -Pintegration-test` | Run 79 integration tests (requires Docker/PostgreSQL) |
| `mvn verify` | Run all + JaCoCo coverage check + Checkstyle |

### Frontend (`cd frontend`)
| Command | Action |
|---|---|
| `npm run dev` | Vite dev server on `:5173` |
| `npm run build` | `tsc -b && vite build` |
| `npm run lint` | Oxlint |
| `npx tsc --noEmit` | Type-check without build |
| `npm t` or `npx vitest run` | Run Vitest tests (674 tests across 80 files) |

### Playwright (`cd tests`)
| Command | Action |
|---|---|
| `npx playwright test` | Run all E2E tests (67 spec files) |
| `npx playwright test --list` | List tests without running |
| `npx playwright show-report` | View HTML report |

## Testing

- **Backend**: 348 main sources / 115 test files across the multi-module reactor (common 119/10, icu-chart 84/62, medication-sheet 61/20, prosthesis-manufacturing 84/22, app 0/1 — the app test is the ArchUnit `ModuleBoundaryTest`). JaCoCo 60% instruction / 50% branch minimum. Checkstyle Google checks.
- **Frontend**: 674 Vitest tests across 80 test files (131 TS/TSX sources). Run with `npm t`. Security-contract suite: `src/test/services/authSecurityContract.test.tsx`.
- **E2E**: 67 Playwright spec files (304 tests) across 11 projects (setup, login, api-error-mode, doctor, nurse, hod, admin, api, prosthetics, responsive-mobile, responsive-tablet).

## Playwright Projects

| Project | Depends On | storageState | Tests |
|---|---|---|---|
| setup | — | — | Auth setup (6 roles) |
| login-chromium | — | none | Login/logout flow |
| api-error-mode-chromium | — | none | Mock MIS error scenarios |
| doctor-chromium | setup, api-error-mode | `.auth/doctor.json` | Dashboard, create card, prescriptions, notes, sign-off |
| nurse-chromium | setup, api-error-mode | `.auth/nurse.json` | Dashboard, vitals, fluid balance, order execution |
| hod-chromium | setup, api-error-mode | `.auth/hod.json` | Dashboard, clinical day reopen |
| admin-chromium | setup, api-error-mode | `.auth/admin.json` | User tables, RBAC matrix, audit log |
| api-chromium | api-error-mode | none | Patient search API, error handling, scales access control |
| prosthetics-chromium | setup | `.auth/prosthetist.json` | Prosthetics workflow, quality gates |
| responsive-mobile-chromium | setup | `.auth/doctor.json` | Mobile 360px smoke (iPhone 13 emulation): nav sheet, touch targets, wizard smoke; `fullyParallel: false` |
| responsive-tablet-chromium | setup | `.auth/doctor.json` | Tablet 768×1024 (`hasTouch`): no-horizontal-scroll audits, sidebar rail expand, 2-col stats; `fullyParallel: false` |

## Seed Data

| Login | Password | Role |
|---|---|---|
| `doctor1` / `doctor2` | `doctor123` | DOCTOR |
| `nurse1` / `nurse2` | `nurse123` | NURSE |
| `head1` | `head123` | HEAD_OF_DEPARTMENT |
| `admin` | `admin123` | ADMINISTRATOR |
| `prosthetist1` / `prosthetist2` | `doctor123` | PROSTHETIST |
| `prosthetics_admin1` | `doctor123` | PROSTHETICS_ADMINISTRATOR |
| *(backend-only)* | — | AUDITOR |

Mock MIS provides 5 test patients: Петренко, Коваленко, Сидоренко, Бондаренко, Ткачук.

Prosthetics seed patients (demographics served by the MIS Integration Layer wiremock `__files/patients_52.json`; clinical fields in local tables):

| Patient | ID | Order | Template |
|---|---|---|---|
| Сніжко Іван Петрович | `900001` | ПВ-26-0413 (upper_limb) | TP-UL-01 (ACTIVE) |
| Гаврилюк Олена Миколаївна | `900002` | ПВ-26-0414 (lower_limb) | TP-LL-01 (DRAFT) |

3 seed episodes with 4 clinical days:

| Episode | Clinical Days |
|---|---|
| `a1111111` (Петренко) | `b1111111` OPEN, `b1111112` NURSE_SIGNED |
| `a2222222` (Коваленко) | `b2222222` OPEN, `b4444444` NURSE_SIGNED |
| `a3333333` (Сидоренко) | `b3333333` OPEN |

**E2E test data isolation** (each spec targets a specific episode, no `.first()` race):
- `a1111111`: `signoff-full-chain` (signs `b1111111` + `b1111112`), `signoff`
- `a2222222`: `clinical-day-reopen` (reopens `b4444444`), `pdf-generation` (signs `b2222222`)
- `a3333333`: `notes`, `notes-full`, `prescriptions`, `prescription-cancel`, `scales-episode`

**Prosthetics E2E isolation** (separate mock tables, no cross-module interference):
- `prosthetist1` → owns `Сніжко` / `ПВ-26-0413` / instance from `TP-UL-01`
- `prosthetist2` → owns `Гаврилюк` / `ПВ-26-0414` / instance from `TP-LL-01`
- `prosthetics_admin1` → quality gate decisions, template admin
- Each spec uses fixed seed IDs (no `.first()`)

## Data Model

```
BaseEntity (abstract)
  ├── id: UUID (PK)
  ├── createdAt, createdBy, updatedAt, updatedBy
  └── version: Integer (@Version, optimistic locking)

Episode (1) ──── (N) ClinicalDay
                         │
               ┌────────┼────────┬────────┬────────┬────────┐
           Hourly  Medical  Medical  Scale   FluidBalance  Signature
           Record  Order    Note    Result

AuditLog (standalone, no BaseEntity)
  id, timestamp, userId, entity, entityId, action,
  oldValue, newValue, correlationId, details,
  ipAddress, userRole, isDeleted
```

### Entity Details

| Entity | Extends | Key Fields | Constraints |
|---|---|---|---|
| `User` | BaseEntity | login(unique), passwordHash, fullName, role(UserRole), email, specialityCode/Name, phone | Role: DOCTOR/NURSE/HEAD_OF_DEPARTMENT/ADMINISTRATOR/AUDITOR |
| `Episode` | BaseEntity | patientId, hospitalizationId, departmentId, admissionDate, dischargeDate, status(EpisodeStatus) | Status: DRAFT → ACTIVE → COMPLETED/ARCHIVED |
| `ClinicalDay` | BaseEntity | episode(M→1), dayNumber, startDateTime, endDateTime, status(ClinicalDayStatus), doctorSigned, nurseSigned, closedAt | Status: OPEN → NURSE_SIGNED → DOCTOR_SIGNED → CLOSED/REOPENED |
| `HourlyRecord` | BaseEntity | clinicalDay(M→1), recordTime, recordHour, consciousness, temperature(34-42), heartRate(0-300), respiratoryRate(0-60), systolicBP(50-250), diastolicBP(30-150), meanArterialPressure, spo2(50-100), glucose(1-30), etco2, fio2, cvp, urineOutput, drainOutput, stool, vomit, painScore, notes | UNIQUE(clinical_day_id, record_hour); ranges validated in @PrePersist/@PreUpdate |
| `MedicalOrder` | BaseEntity | clinicalDay(M→1), category, drugName, dose, unit, route, frequency, startTime, endTime, status(MedicalOrderStatus) | Status: DRAFT/ACTIVE/COMPLETED/CANCELLED |
| `OrderExecution` | BaseEntity | order(M→1), executedBy, executedAt, actualDose, status(OrderExecutionStatus), comment | Status: PLANNED/IN_PROGRESS/COMPLETED/PARTIALLY_COMPLETED/CANCELLED |
| `MedicalNote` | BaseEntity | clinicalDay(M→1), authorId, role, noteType, text(TEXT) | — |
| `ScaleResult` | BaseEntity | clinicalDay(M→1)(nullable), scale(M→1), result(text), episodeId(UUID), rawData(jsonb), calculatedAt, calculatedBy | Auto-calculates GCS/RASS from consciousness |
| `FluidBalance` | BaseEntity | clinicalDay(M→1), hour, intake, output, balance, cumulativeBalance | Recalculated on HourlyRecord changes |
| `Signature` | BaseEntity | clinicalDay(M→1), userId, role, signedAt, hash, status | — |
| `Permission` | — | code(PK), label, description, category | Dictionary of the RBAC catalog (25 codes) |
| `RolePermission` | — | role(PK, UserRole), permissionCode(PK→Permission) | Default-deny grants; presence = granted |
| `GeneratedPdf` | BaseEntity | clinicalDay(M→1), fileName, fileVersion, generatedAt, generatedBy, checksum, fileData(byte[]), transferStatus(TransferStatus), transferError, transferredAt | TransferStatus: PENDING/SENT/FAILED |
| `SystemSettings` | BaseEntity | key(unique), value(TEXT), description(TEXT) | — |

### Enums

| Enum | Values |
|---|---|
| `UserRole` | DOCTOR, NURSE, HEAD_OF_DEPARTMENT, ADMINISTRATOR, AUDITOR, ADJACENT_SPECIALIST, PROSTHETIST, PROSTHETICS_ADMINISTRATOR |
| `EpisodeStatus` | DRAFT, ACTIVE, COMPLETED, ARCHIVED |
| `ClinicalDayStatus` | OPEN, NURSE_SIGNED, DOCTOR_SIGNED, CLOSED, REOPENED |
| `MedicalOrderStatus` | DRAFT, ACTIVE, COMPLETED, CANCELLED |
| `OrderExecutionStatus` | PLANNED, IN_PROGRESS, COMPLETED, PARTIALLY_COMPLETED, CANCELLED |
| `TransferStatus` | PENDING, SENT, FAILED |

## API Endpoints

All endpoints prefixed with `/api`.

### Auth
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/login` | No | Authenticate, returns JWT |

### Episodes
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/episodes` | Yes | Search (patientId, status query params) |
| GET | `/api/episodes/{id}` | Yes | Get by ID |
| GET | `/api/episodes/{id}/clinical-days` | Yes | Get clinical days for episode |
| POST | `/api/episodes` | Yes | Create episode |
| PATCH | `/api/episodes/{id}` | Yes | Update episode fields (with version) |
| POST | `/api/episodes/{id}/close` | Yes | Close episode |
| PUT | `/api/episodes/{id}/archive` | Yes | Archive episode (→ 204) |

### Clinical Days
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/clinical-days/{id}` | Yes | Get by ID |
| POST | `/api/clinical-days` | Yes | Create (with episodeId) |
| PATCH | `/api/clinical-days/{id}` | Yes | Update endDateTime (with version) |
| POST | `/api/clinical-days/{id}/sign/nurse` | Yes | Nurse sign (→ 204 No Content) |
| POST | `/api/clinical-days/{id}/sign/doctor` | Yes | Doctor sign (→ 204 No Content) |
| POST | `/api/clinical-days/{id}/reopen` | Yes | Reopen (with version) |

### Hourly Records
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/clinical-days/{id}/hourly-records` | Yes | List for clinical day |
| POST | `/api/clinical-days/{id}/hourly-records` | Yes | Create (recordHour auto-set) |
| PATCH | `/api/hourly-records/{id}` | Yes | Update fields (with version) |

### Medical Orders
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/clinical-days/{id}/orders` | Yes | List for clinical day |
| POST | `/api/clinical-days/{id}/orders` | Yes | Create |
| PATCH | `/api/orders/{id}` | Yes | Update (with version) |
| POST | `/api/orders/{id}/cancel` | Yes | Cancel (with version) |

### Order Executions
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/orders/{id}/executions` | Yes | List for order |
| POST | `/api/orders/{id}/execute` | Yes | Create execution |
| PATCH | `/api/executions/{id}` | Yes | Update (with version) |

### Medical Notes
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/clinical-days/{id}/notes` | Yes | List for clinical day |
| POST | `/api/clinical-days/{id}/notes` | Yes | Create |
| PATCH | `/api/notes/{id}` | Yes | Update (with version) |

### Clinical Scales
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/scales` | Yes | List available scales |
| GET | `/api/clinical-days/{id}/scales` | Yes | Get scale results for clinical day |
| POST | `/api/clinical-days/{id}/scales` | Yes | Create scale result |
| PATCH | `/api/scales/{id}` | Yes | Update (with version) |
| GET | `/api/episodes/{episodeId}/scales` | Yes | Get episode-level scale results |
| POST | `/api/episodes/{episodeId}/scales` | Yes | Create episode-level scale result |
| POST | `/api/episodes/{episodeId}/scales/calculate` | Yes | Calculate and save scale from raw data |

### Fluid Balance
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/clinical-days/{id}/fluid-balance` | Yes | Get fluid balance entries |
| POST | `/api/clinical-days/{id}/fluid-balance/recalculate` | Yes | Recalculate from scratch |

### Patients (Mock MIS)
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/patients` | Yes | Search (query param) |
| GET | `/api/patients/{id}` | Yes | Get by ID |

### Users
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/users/me` | Yes | Current user profile |
| GET | `/api/users/doctors` | Yes | List all doctors |
| GET | `/api/users/nurses` | Yes | List all nurses |

### PDF
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/clinical-days/{id}/pdf` | Yes | Get latest generated PDF |
| POST | `/api/clinical-days/{id}/pdf` | Yes | Generate PDF for clinical day |
| GET | `/api/clinical-days/{id}/pdf/status` | Yes | Get PDF transfer status |

### Audit
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/audit` | Yes | List (paginated, filters: userId, entity, entityId, action, dateFrom, dateTo). Requires `AUDIT_ACCESS` or AUDITOR |
| GET | `/api/audit/{id}` | Yes | Get single audit log entry. Requires `AUDIT_ACCESS` or AUDITOR |

### RBAC (Admin role-permission matrix)
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/admin/permissions` | ADMINISTRATOR | Full matrix: roles, permission catalog, grants |
| PUT | `/api/admin/permissions` | ADMINISTRATOR | Grant/revoke: `{role, permissionCode, granted}` |
| GET | `/api/users/me/permissions` | Any authenticated | Effective permission codes of the current user's role |

### Mock MIS Controls
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/mis/error-mode?mode=timeout\|not_found\|unavailable\|none` | Yes | Set mock MIS error simulation |

### Prosthetics Manufacturing
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/prosthesis-manufacturing/patients` | Yes (PROSTHETIST, PROSTHETICS_ADMINISTRATOR) | Search patients |
| GET | `/api/prosthesis-manufacturing/patients/{id}` | Yes | Get patient by ID |
| POST | `/api/prosthesis-manufacturing/patients` | Yes (PROSTHETICS_ADMINISTRATOR) | Create patient |
| GET | `/api/prosthesis-manufacturing/orders` | Yes | List orders |
| GET | `/api/prosthesis-manufacturing/orders/{id}` | Yes | Get order by ID |
| POST | `/api/prosthesis-manufacturing/orders` | Yes (PROSTHETICS_ADMINISTRATOR) | Create order |
| GET | `/api/prosthesis-manufacturing/templates` | Yes | List flow templates |
| GET | `/api/prosthesis-manufacturing/templates/{id}` | Yes | Get template by ID |
| POST | `/api/prosthesis-manufacturing/templates` | Yes (PROSTHETICS_ADMINISTRATOR) | Create template |
| PATCH | `/api/prosthesis-manufacturing/templates/{id}` | Yes (PROSTHETICS_ADMINISTRATOR) | Update template |
| GET | `/api/prosthesis-manufacturing/instances` | Yes | List flow instances |
| GET | `/api/prosthesis-manufacturing/instances/{id}` | Yes | Get instance by ID |
| POST | `/api/prosthesis-manufacturing/instances` | Yes (PROSTHETIST) | Create instance from order + template |
| GET | `/api/prosthesis-manufacturing/instances/{id}/step-executions` | Yes | Get step executions for instance |
| POST | `/api/prosthesis-manufacturing/step-executions/{id}/complete` | Yes (PROSTHETIST) | Complete step execution |
| GET | `/api/prosthesis-manufacturing/instances/{id}/quality-gates` | Yes | Get quality gates for instance |
| POST | `/api/prosthesis-manufacturing/gate-decisions` | Yes (PROSTHETICS_ADMINISTRATOR) | Make gate decision (PASS/REWORK/FAIL) |
| POST | `/api/prosthesis-manufacturing/instances/{id}/pause` | Yes (PROSTHETIST) | Pause instance |
| POST | `/api/prosthesis-manufacturing/instances/{id}/resume` | Yes (PROSTHETIST) | Resume instance |
| POST | `/api/prosthesis-manufacturing/instances/{id}/replacement` | Yes (PROSTHETIST) | Create replacement after FAIL |
| GET | `/api/prosthesis-manufacturing/instances/{id}/failure-snapshot` | Yes | Get failure snapshot |
| GET | `/api/prosthesis-manufacturing/instances/{id}/pdf` | Yes | Generate PDF report for instance |
| POST | `/api/prosthesis-manufacturing/evidence-files` | Yes (PROSTHETIST) | Upload evidence file |

## Frontend Routes

| Path | Component | Guard (roles / permissions) |
|---|---|---|
| `/login` | `LoginRoute` → `LoginPage` | Redirects to `/` if authenticated |
| `/` | `RoleRedirect` | Authenticated → `/select` |
| `/select` | `AppSelectorPage` | Any authenticated |
| `/icu/doctor` | `DoctorLayout` > `DashboardPage` | DOCTOR, HEAD_OF_DEPARTMENT (or `MODULE_ICU_ACCESS`, excl. NURSE) |
| `/icu/doctor/department` | `DepartmentDashboardPage` | HEAD_OF_DEPARTMENT |
| `/icu/doctor/create-card` | `CreateCardPage` | DOCTOR, HEAD_OF_DEPARTMENT (or module perm) |
| `/icu/doctor/episode/:episodeId` | `PatientDayPage` | DOCTOR, HEAD_OF_DEPARTMENT (or module perm) |
| `/icu/nurse` | `NurseLayout` > `NurseDashboardPage` | NURSE (or `MODULE_ICU_ACCESS`, excl. DOCTOR/HOD) |
| `/icu/nurse/episode/:episodeId` | `PatientDayPage` | NURSE (or module perm) |
| `/prescriptions/doctor` | `PrescriptionPage` | DOCTOR, HEAD_OF_DEPARTMENT (or `MODULE_MEDICATION_ACCESS`, excl. NURSE) |
| `/prescriptions/doctor/:id` | `PrescriptionDetailPage` | DOCTOR, HEAD_OF_DEPARTMENT (or module perm) |
| `/prescriptions/nurse` | `NursePrescriptionPage` | NURSE (or `MODULE_MEDICATION_ACCESS`, excl. DOCTOR/HOD) |
| `/prescriptions/nurse/:id` | `PrescriptionDetailPage` | NURSE (or module perm) |
| `/prosthetics` | `ProstheticsDashboard` | PROSTHETIST, PROSTHETICS_ADMINISTRATOR (or `MODULE_PROSTHETICS_ACCESS`) |
| `/prosthetics/new/select-patient` | `PatientSearchPage` | same |
| `/prosthetics/new/select-order` | `OrderSelectPage` | same |
| `/prosthetics/new/review-order` | `OrderReviewPage` | same |
| `/prosthetics/new/select-template` | `TemplateSelectPage` | same |
| `/prosthetics/process/:id` | `ProcessLayout` > `ProcessDetail` | same |
| `/prosthetics/process/:id/history` | `ProcessHistoryPage` | same |
| `/prosthetics/process/:id/wizard` | `WizardScreen` | same (backend enforces PROSTHETIST writes) |
| `/prosthetics/process/:id/done` | `DoneScreen` | same |
| `/prosthetics/process/:id/failed` | `FailedScreen` | same |
| `/admin` | `AdminPage` | ADMINISTRATOR, AUDITOR (or `MODULE_ADMIN_ACCESS`) |

## Frontend Components

### Pages (28)
| Area | Files |
|---|---|
| root | `LoginPage.tsx`, `AppSelectorPage.tsx` |
| `doctor/` | `DashboardPage.tsx`, `DepartmentDashboardPage.tsx` (HOD), `CreateCardPage.tsx`, `PatientDayPage.tsx` |
| `nurse/` | `NurseDashboardPage.tsx` |
| `prescription/` | `PrescriptionPage.tsx`, `PrescriptionDetailPage.tsx`, `NursePrescriptionPage.tsx` |
| `prosthetics/` | `ProstheticsDashboard.tsx`, `DashboardPage.tsx`; `setup/` — `PatientSearchPage`, `OrderSelectPage`, `OrderReviewPage`, `TemplateSelectPage` + steps (`OrderStep`, `PatientStep`, `ReviewStep`, `TemplateStep`); `process/` — `ProcessDetail`, `ProcessHistoryPage`, `ProcessLayout`, `ProcessOverview`, `WizardScreen`, `DoneScreen`, `FailedScreen`, `MeasurementForms` |
| `admin/` | `AdminPage.tsx` (users, RBAC matrix «Доступи та ролі», audit log gated by `AUDIT_ACCESS`, stats) |

### Components by feature
| Directory | Contents |
|---|---|
| `components/icu/` | ICU chart feature: `ClinicalDayTimeline`, `DepartmentPatientCard`, `DocumentHeader`, `EpisodeTable`, `FluidBalancePanel`, `HourlyRecordTable`, `HourSelector`, `LabResultsPanel`, `MedicalNotesPanel`, `MedicalOrdersPanel`, `PatientStatePanel`, `ScaleResultsPanel`, `SignDialog`, `VentilationPanel`, `VitalSignsForm`, `useAutoSave`; `scales/` — `ApacheIiForm` (20 parameters), `SofaForm`, `CamIcuForm`, `BradenForm`, `RassSelector`, `ScaleFormFactory` |
| `components/monitoring/` | `HourlyGrid` (24-h grid, therapy cells, plan/execute, critical flash), `HourlyGridDialog` (fullscreen modal with undo/status/critical chip), `IntensiveCareCard` (central ICU card), `DoctorDashboard`, `NurseDashboard`, `PatientSidebar`, `criticalRanges.ts` (alarm thresholds), `dashboardTypes.ts` |
| `components/prescription/` | `PrescriptionGrid`, `PrescriptionSpreadsheet`, `PrescriptionTable`, `PrescriptionItemTable`, `PrescriptionItemForm`, `PrescriptionExecutionPanel`, `VitalSignGrid`, `VitalSignForm`, `DayPartPlanner`, `MedicineSearchInput`, `AllergyWarning`, `ClosePrescriptionDialog`, `DeleteConfirmPopover`, `ExecuteDosePopover`, `prescriptionDayParts.ts` |
| `components/common/` | `PatientSearch.tsx`, `ThemeToggle.tsx`, `AuditLogTable.tsx` (shared, feature-free) |
| `components/prosthetics/` | `StatusBadge`, `SetupSteps`, `QualityGatePanel`, `ProcessStat` |
| `components/navigation/` | `AppSidebar.tsx`, `Breadcrumbs.tsx` |
| `components/ui/` | shadcn-style Base UI primitives: `button`, `input`, `card`, `dialog`, `table`, `select`, `tabs`, `switch`, `checkbox`, `radio-group`, `dropdown-menu`, `popover`, `tooltip`, `progress`, `skeleton`, `sonner`, … |

### API Client (`frontend/src/api/`)
- **`client.ts`**: Axios instance → `http://localhost:8085/api`, JWT interceptor
- **Per-feature modules** (no barrel): `platform.ts` (auth, patient, user, settings, audit, admin), `icu.ts` (episode, clinicalDay, hourlyRecord, medicalOrder, orderExecution, medicalNote, clinicalScale, fluidBalance, pdf, patientState, ventilation, labResult, department), `medication.ts` (prescription, vitalSign); prosthetics APIs in `prosthetics.ts` (isolated). Shared DTO types live in `types/core.ts`, ICU types in `types/icu.ts`, medication types in `types/medication.ts`.

### Auth (`frontend/src/services/AuthContext.tsx`)
- `AuthProvider` with user/token state, login/logout, role checking
- Session flag persisted in `localStorage` (`auth:session`; the JWT itself lives in the httpOnly cookie)
- Guards in `App.tsx` via `Guard` component and `LoginRoute`/`RoleRedirect`

## DTOs

### Request DTOs
`LoginRequest`, `EpisodeCreateRequest`, `EpisodePatchRequest`, `EpisodeCloseRequest`, `ClinicalDayCreateRequest`, `ClinicalDayPatchRequest`, `HourlyRecordCreateRequest`, `HourlyRecordPatchRequest`, `MedicalOrderCreateRequest`, `MedicalOrderPatchRequest`, `MedicalNoteCreateRequest`, `MedicalNotePatchRequest`, `ScaleResultCreateRequest`, `ScaleResultPatchRequest`, `ScaleResultCalculateRequest`, `OrderExecutionCreateRequest`, `OrderExecutionPatchRequest`, `SignRequest`, `ReopenRequest` (19 total)

### Response DTOs
`LoginResponse`, `EpisodeResponse`, `ClinicalDayResponse`, `HourlyRecordResponse`, `MedicalOrderResponse`, `OrderExecutionResponse`, `MedicalNoteResponse`, `ScaleResultResponse`, `FluidBalanceResponse`, `SignResponse`, `PdfResponse`, `UserResponse`, `AuditLogResponse`, `ErrorResponse` (14 total)

## Backend Services (by module)

### Platform (`common`, 4)
| Service | Responsibility |
|---|---|
| `AuthService` | Login with password verification + JWT generation |
| `AuditService` | Create/query audit log entries with pagination |
| `PermissionService` | Dynamic RBAC: `has/hasAny/hasForRole` (SpEL for `@PreAuthorize`), matrix read, grant/revoke with cache invalidation + audit, first-boot seeding of defaults |
| `PermissionCatalog` | RBAC catalog: 25 permission codes across 8 categories, role defaults for seeding |

### ICU chart (`icu-chart`, 16)
| Service | Responsibility |
|---|---|
| `EpisodeService` | CRUD + search + close/archive with optimistic locking |
| `ClinicalDayService` | CRUD + signing workflow + reopen with signature revocation + next-day gating |
| `HourlyRecordService` | CRUD with clinical day lock checking |
| `MedicalOrderService` | CRUD + cancel with status validation |
| `OrderExecutionService` | CRUD with order status validation |
| `MedicalNoteService` | CRUD with author role assignment |
| `ClinicalScaleService` | Scale results + automatic GCS/RASS from consciousness + episode-level results + algorithm-based calculation (APACHE II, SOFA, CAM-ICU, Braden) |
| `FluidBalanceService` | Recalculation from HourlyRecord + OrderExecution |
| `ScaleAuthorizationService` | Per-scale role-based access control (APACHE II/SOFA → DOCTOR, others → NURSE) |
| `SignatureService` | Create/revoke signatures, check existing signatures |
| `PdfGeneratorService` | Generate PDF (iText) with all clinical day sections |
| `VentilationSettingsService`, `LabResultService`, `PatientStateAssessmentService`, `DepartmentService`, `EmailService` | Ventilation settings, lab results, patient state, department data, e-mail notifications |

### Medication sheet (`medication-sheet`, 11)
| Service | Responsibility |
|---|---|
| `PrescriptionListService`, `PrescriptionItemService`, `PrescriptionExecutionService` | Prescription lists/items/day-part executions (21-day grid) |
| `PrescriptionSchedulerService` | Scheduled plan/complete processing for day parts |
| `VitalSignService` | Vital sign days/entries |
| `MedicineCatalogService`, `DrugInteractionService` | Medicine catalog + allergy/drug-interaction checks |
| `NotificationService`, `LogNotificationService`, `LogEmailService`, `EmailService` | Telegram/e-mail notifications (logging fallbacks in tests) |

### Prosthetics manufacturing (`prosthesis-manufacturing`, 11)
| Service | Responsibility |
|---|---|
| `ProstheticsPatientService`, `ProstheticsOrderService`, `ProstheticsPdfService` | Patient/order CRUD + PDF generation |
| `FlowTemplateService` | Template CRUD + stages/steps/elements |
| `FlowInstanceService` | Instance lifecycle (create, pause, resume, complete steps) |
| `QualityGateService` | Gate decisions (PASS/REWORK/FAIL), rework loops |
| `FailureSnapshotService` | Failure capture + PDF report |
| `EvidenceFileService` | File upload (images/PDFs, 10MB limit) |
| `MisOrderTemplateDataService`, `TemplateSnapshotParser`, `MisOrderTemplateData` | Template parsing from MIS order data |

## Compliance Fixes Applied

| § | Description | Implementation |
|---|---|---|
| §46 | Unique constraint on (clinical_day_id, record_hour) | `recordHour` field + `@UniqueConstraint` on `HourlyRecord` |
| §47 | Next clinical day gating | `canAdvanceToNextDay()` checks status + hourly record count |
| §49 | Clinical range validation | JSR-380 annotations + `@PrePersist/@PreUpdate` validation |
| §52 | Auto fluid balance recalculation | `@PostPersist/@PostUpdate` triggers `FluidBalanceService.recalculate()` |
| §53 | Archive episode endpoint | `PUT /api/episodes/{id}/archive` → 204 No Content |
| §79 | Audit log IP address + user role + login events | `ipAddress`, `userRole` fields; `JwtAuthenticationFilter` logs login |
| §80 | HTTP 204 for sign endpoints | Sign endpoints return `ResponseEntity.noContent()` |
| §81 | Soft delete on AuditLog | `isDeleted` field + `findAllActive()` JPQL query |
| §84 | AUDITOR role | Added to `UserRole` enum |
| §86 | Integration tests in CI | New `integration-tests` job with PostgreSQL 16 service |
| §87 | Mock MIS error scenarios | Error modes: timeout, not_found, unavailable via `POST /api/mis/error-mode` |
| §88 | JaCoCo coverage | 60% instruction / 50% branch minimums |
| §89 | Checkstyle analysis | Google checks with console output |
| §94 | PDF transfer status tracking | `GeneratedPdf.transferStatus` + `TransferStatus` enum (PENDING/SENT/FAILED) + `GET /clinical-days/{id}/pdf/status` |
| §98 | MIS calls audited | `WireMockMisServiceImpl` methods call `auditService.logAction()` including `sendPdf()` |
| §— | Liquibase schema management | `ddl-auto: none`, schema per DB via `db/changelog/db.changelog-master-{core,icu,med,prosth}.yaml` (15 SQL changesets: core 4, icu 6, med 1, prosth 4); seed data via `SeedDataInitializer` (`data-{core,icu,med,prosth}.sql`, gated by `app.seed-data.enabled`) |

## Key Patterns

- **Locators** (Playwright): prefer `getByRole`, `getByLabel`, row-specific filters over `.first()`
- **Seed data references**: 3 episodes — use `filter({ hasText })` with patient names (Петренко, Коваленко, Сидоренко)
- **Auth**: storageState per role, projects depend on `setup`
- **Parallelism**: `fullyParallel: true` — tests can race; use specific locators, not `.first()`, for shared data
- **CI retries**: 2 retries per test
- **Backend tests**: `@SpringBootTest` are `@Transactional` (rollback); `@DataJpaTest` uses `@AutoConfigureTestDatabase(replace = NONE)` (real PostgreSQL)
- **IDs**: All UUID strings (`string` type in TS, `UUID` type in Java)
- **Optimistic Locking**: `@Version version` on all entities → `VersionConflictException` → HTTP 409
- **Audit**: Every create/update/delete operation creates an `AuditLog` entry
- **Error response**: `ErrorResponse` DTO with `code`, `message`, `correlationId`
- **ClinicalDay locking**: Signed/closed days cannot be modified (throws `DocumentLockedException`)
- **Signing flow**: Nurse must sign before doctor; signatures can be revoked on reopen
- **Frontend error display**: All API catch blocks in `IntensiveCareCard.tsx` use `getErrorMessage(err, fallback)` which extracts `err.response?.data?.message` from Axios errors (shows the backend validation message instead of generic "Request failed with status code 400")
- **Backend validation exceptions**: `ConstraintViolationException` (JSR-380) and `InvalidDataAccessApiUsageException` (wraps `IllegalArgumentException` from `@PrePersist`) are both caught in `GlobalExceptionHandler` and return 400 with the validation message
- **Validation runs before method security**: `@Valid @RequestBody` argument binding happens *before* the `@PreAuthorize` interceptor — a request with an invalid body returns 400 (validation) even for roles lacking the permission. Tests that assert 403 must send a **valid** request body; an invalid body only proves the role *passed* security when it yields 201/200 (or 400 with a valid body in the denied case). Same applies to E2E specs (`security-rules.spec.ts`, `permissions.spec.ts`)
- **Fullscreen modal keyboard model** (`HourlyGridDialog.tsx`): initial focus on the close button (✕), not the first editable cell, to prevent accidental data change before reviewing patient state (WCAG 2.4.3). `Tab`/`Shift+Tab` cycles within the dialog (focus trap). `Esc` closes; `Alt+Enter` toggles. `Escape` in a dirty cell reverts the draft; `Enter` commits.
- **Custom keyframes** (`frontend/src/index.css`): non-Tailwind keyframes (`scale-in`, `fade-in`, `slide-in-from-left`, `content-show`) live in `index.css` under `@layer base`, scoped via `[data-fullscreen="true"]` so they don't affect non-modal use. `animate-in` utilities (`duration-…`, `fade-in`, `slide-in-from-…`, `zoom-in-…`) from a motion library are inert — the dialog overrides them with its own `style={{ animation: 'none' }}` on initial render to avoid double-animation. On `prefers-reduced-motion`, all animations are gated by a media-query guard.

## Responsive UI

- **Breakpoint strategy**: mobile < 640px (base styles), tablet 640–1024px (`sm:`…`lg:`), desktop > 1024px (`lg:`+). Explicit tokens in `index.css` `@theme`: `--breakpoint-sm: 40rem`, `--breakpoint-md: 48rem`, `--breakpoint-lg: 64rem`, `--breakpoint-xl: 80rem`. CSS-first: base styles target mobile, `sm:`/`md:`/`lg:` variants progressively enhance for tablet/desktop.
- **Shared hooks** (`frontend/src/hooks/useMediaQuery.ts`): `useMediaQuery(query)` (SSR-safe, `useSyncExternalStore`, subscribes via `addEventListener('change')`) and `useIsMobile()` = `useMediaQuery('(max-width: 639.98px)')`. MatchMedia is not implemented in jsdom — `src/test/setup.ts` provides a static mock (always `matches: false`); tests that need real behavior must install their own mock and restore `window.matchMedia` afterwards.
- **Desktop/tablet detection**: the ICU layout treats viewports ≤ 1023.98px as mobile (`(max-width: 1023.98px)` via the shared `useMediaQuery`) — used by `SidebarProvider` (offcanvas behavior) and `IntensiveCareCard` (single-column layout). Never duplicate inline matchMedia `useState`/`useEffect` blocks — use the shared hook.
- **Primitives** (Base UI ports, no new runtime deps): `components/ui/sheet.tsx` (Drawer-based Sheet — `SheetContent` `side` prop `left|right|top|bottom`, default `right`; popup renders NO DOM element on `Sheet` Root so it is a plain function, `swipeDirection` default `"down"`; animations via `index.css` `sheet-*` keyframes gated on `[data-slot="sheet-content"][data-side=…][data-open|data-closed]` + `prefers-reduced-motion`), `components/ui/stepper.tsx` (custom shadcn-style stepper — `Stepper`/`Step`/`StepIndicator`/`StepSeparator`/`StepTitle`/`StepDescription`/`useStepper`; 1-based `step`, `orientation`, `size`, `nonLinear` + `onStepClick` makes indicators `role="button"`; completed steps render a `Check` icon, `loading` renders `Loader2`), `components/ui/scroll-area.tsx` (Base UI ScrollArea with `keepMounted` scrollbars so both rails render in jsdom tests).
- **Touch targets & focus** (Phase 5): `pointer-coarse:` variants for touch-only sizing — `button` base `pointer-coarse:min-h-11`, icon sizes `pointer-coarse:size-11`, `checkbox`/`switch`/`radio` hit areas via invisible `after:` pseudo-elements (`after:-inset-x/y-3.5` / `after:-inset-3.5`), `SelectTrigger`/`Input`/`TabsList`/`TabsTrigger` `pointer-coarse:min-h-11`. Variant is inert in jsdom and desktop E2E (fine pointers) — class-only changes need no test updates. Focus rings must be full-opacity `ring-ring` (never `ring-ring/50` — ≈1.7:1 fails WCAG 1.4.11; full brand-orange + 1px border change composites ≥3:1); destructive = full `ring-destructive`; radio `ring-2`. Horizontal scrollers get `touch-pan-x`. `SheetContent` uses per-side `env(safe-area-inset-*)` with 1.5rem fallback (`pt-[max(1.5rem,env(safe-area-inset-top))]` longhands in `sideClasses` — base `p-6` kept, longhand wins). Tabs inactive text `text-foreground/70`. Interactive elements get `active:translate-y-px` for touch feedback.

## Conventions

- **Commits**: Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`, `chore:`)
- **TypeScript**: `erasableSyntaxOnly: true` — no enums, no namespaces
- **Roles**: Gate in backend (Spring Security `@PreAuthorize`) and frontend (`Guard` component)
- **Routing**: `/icu/doctor/*` for DOCTOR/HOD, `/icu/nurse/*` for NURSE, `/prescriptions/*` for medication sheet, `/prosthetics/*` for prosthetics, `/admin/*` for ADMINISTRATOR
- **DB**: `ddl-auto: none` — schema per DB managed by the Liquibase changelogs in `db/changelog/{core,icu,med,prosth}/` (master yamls + 15 SQL changesets); never write manual DDL
- **Data seeding**: Only via `SeedDataInitializer` — one script per module: `data-core.sql`, `data-icu.sql`, `data-med.sql`, `data-prosth.sql` (in `backend/common/src/main/resources/`), executed on the matching datasource; gated by `app.seed-data.enabled: true`. Never write manual seed DDL.
- **Test seed data**: Integration tests use `data-test-core.sql` / `data-test-icu.sql` / `data-test-med.sql` (in `backend/icu-chart/src/test/resources/`) with plain INSERTs, routed per-datasource via `@Sql` + `@SqlConfig(dataSource = ...)` (plus `data-prescription.sql` with `@SqlConfig(dataSource = "medDataSource", separator = "GO")`) on a fresh PostgreSQL database. The production seed files keep `ON CONFLICT (id) DO NOTHING` for local dev resilience (exception: `prescription_lists` uses `ON CONFLICT (id) DO UPDATE SET document_name = EXCLUDED.document_name` to auto-heal Cyrillic encoding corruption). The `users` inserts in `data-core.sql` use `ON CONFLICT (login) DO NOTHING` — a demo password is **never** overwritten on restart (A2, CWE-798). Modified data may persist across restarts. Reset each DB with `DROP SCHEMA public CASCADE; CREATE SCHEMA public;` in PostgreSQL before the next run.

## Encoding Policy

**All SQL seed files and generated SQL must be UTF-8** — never UTF-16LE, never Windows-1251.

- **Generator scripts** (`scripts/*.cjs`): Use `fs.writeFileSync(path, content, 'utf8')` for file output and `process.stdout.write(content, 'utf8')` for stdout. **Never use `console.log()`** to generate file content — on Windows PowerShell, `console.log` pipes through `process.stdout` which defaults to UTF-16LE, producing a UTF-16LE BOM and null-byte interleaved ASCII that PostgreSQL cannot decode.
- **Verification commands**:
  - `file scripts/*.sql` should report "UTF-8 Unicode text", never "Little-endian UTF-16 Unicode text"
  - `hexdump -C scripts/*.sql | head -3` should show no BOM (`FF FE`) and single-byte (not zero-interleaved) ASCII
- **`data-*.sql`**: Must be UTF-8. Any seed SQL file must be explicitly written as UTF-8. If a corrupted file was already concatenated, convert it with `Set-Content -Encoding UTF8` or `iconv -f UTF-16 -t UTF-8` and re-insert.
- **Auto-heal**: If corrupted `document_name` values already exist in the database, the `ON CONFLICT (id) DO UPDATE SET document_name = EXCLUDED.document_name` clause on `prescription_lists` INSERTs will overwrite them with clean UTF-8 text on the next `data-med.sql` execution.
- **Seed splitting**: `data.sql`/`data-test.sql` are generated per module with the `split-seed.cjs` statement-aware splitter (temp tool, not in repo). Re-run it after changing any seed content — the split files carry "DO NOT EDIT BY HAND" headers.

## Project Files (kept in repo)

```
AGENTS.md              ← This file — agent guide
README.md              ← Project README with badges and usage
UseManual.md           ← User manual (Ukrainian)
.gitignore             ← Global ignore rules
backend/
  pom.xml              ← Maven build with JaCoCo, Checkstyle, surefire (5 modules: common, icu-chart, medication-sheet, prosthesis-manufacturing, app)
  src/main/java/       ← 348 Java source files
  src/main/resources/  ← application.yml, data-{core,icu,med,prosth}.sql, PDF template, db/changelog/ (Liquibase)
  src/test/java/       ← 115 test files
frontend/
  package.json         ← Dependencies
  vite.config.ts       ← Vite build config
  tsconfig*.json       ← TypeScript configs
  index.html           ← App entry HTML
  public/              ← Static assets
  src/                 ← 131 TS/TSX source + 79 test files
tests/
  playwright.config.ts ← Playwright config with 11 projects
  package.json         ← Test dependencies
  specs/               ← 64 spec files
  pages/               ← Page Object Model (7 files)
  fixtures/            ← Test fixtures
docs/
  Технічне завдання карта Інтенсивної терапії.md  ← Full technical specification (3026 lines)
.github/
  workflows/playwright.yml  ← CI pipeline (3 jobs: integration-tests, test, format-check)
```
