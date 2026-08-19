# Responsive UI Audit — Phase 1 (2026-08-19, Issue #160)

> Scope: audit of every routed screen, breakpoint/token foundation, shared hooks, and the
> three Base UI primitives (Sheet, Stepper, ScrollArea). Companion doc to the
> «Responsive UI» section of `AGENTS.md`.

## 1. Breakpoint strategy (locked)

| Band | Range | Tailwind | Role |
|---|---|---|---|
| Mobile | < 640px | base styles (no prefix) | single column, offcanvas Sheet nav, 44px touch targets |
| Tablet | 640–1024px | `sm:` / `md:` | **PRIMARY target** — dashboard cards flow to 2–3 columns |
| Desktop | > 1024px | `lg:` / `xl:` | fixed sidebar, dense grids, tables |

Rules:

- **CSS-first** — layout adapts via `sm:`/`md:`/`lg:` variants; no JS viewport logic for layout.
  JS `useMediaQuery` is used only where structural DOM differs (offcanvas Sheet sidebar,
  single-column ICU card).
- **Touch** — `pointer-coarse:` variants for touch targets (Phase 5, planned).
- Tokens in `frontend/src/index.css` `@theme`:
  `--breakpoint-sm: 40rem`, `--breakpoint-md: 48rem`, `--breakpoint-lg: 64rem`, `--breakpoint-xl: 80rem`.
- Shared hooks (`frontend/src/hooks/useMediaQuery.ts`): `useMediaQuery(query)` (SSR-safe via
  `useSyncExternalStore`) and `useIsMobile()` = `(max-width: 639.98px)`. jsdom has no
  `matchMedia` — `src/test/setup.ts` provides a static mock (`matches: false`).
- Consumers: `ui/Sidebar.tsx` (offcanvas behavior at ≤1023.98px) and
  `monitoring/IntensiveCareCard.tsx` (single-column card at ≤1023.98px). Both inline
  matchMedia copies were removed.

## 2. Primitives delivered (Base UI ports, no new runtime deps)

| File | Notes |
|---|---|
| `hooks/useMediaQuery.ts` | `useMediaQuery` + `useIsMobile`; `useSyncExternalStore`-based, SSR-safe |
| `components/ui/sheet.tsx` | Sheet = Drawer-style slide-over, `side: left/right/top/bottom`, Base UI Dialog under the hood; `index.css` `sheet-*` keyframes gated on `[data-slot="sheet-content"][data-side=…]` + `prefers-reduced-motion` |
| `components/ui/stepper.tsx` | custom shadcn-style stepper: `Stepper`/`Step`/`StepIndicator`/`StepSeparator`/`StepTitle`/`StepDescription`/`useStepper`; 1-based `step`, `orientation`, `size`, `nonLinear`, `onStepClick` |
| `components/ui/scroll-area.tsx` | Base UI ScrollArea with `keepMounted` scrollbars (needed by jsdom tests) |

Test coverage: `hooks/useMediaQuery.test.ts`, `components/ui/sheet.test.tsx`,
`stepper.test.tsx`, `scroll-area.test.tsx` (matchMedia mocked via `src/test/setup.ts`).
Frontend CI: 627/627 Vitest (run 32230291689).

## 3. Screen map — 28 routed pages

### Root (2)
| Page | Route | Responsive status |
|---|---|---|
| `LoginPage` | `/login` | centered card, safe as-is; Phase 2: padding on small viewports |
| `AppSelectorPage` | `/select` | module card grid — Phase 2: 1 col mobile / 2 col tablet / 3 col desktop |

### Admin (1)
| Page | Route | Notes |
|---|---|---|
| `AdminPage` | `/admin` | tabbed (users / RBAC matrix / audit / stats); RBAC matrix table overflows — Phase 2: wrap in `scroll-area.tsx` |

### ICU — doctor (4)
| Page | Route | Notes |
|---|---|---|
| `DashboardPage` | `/icu/doctor` | patient table + stats — Phase 2: horizontal table scroll (already in `ui/table.tsx`), stats stack |
| `DepartmentDashboardPage` | `/icu/doctor/department` (HOD) | HOD department view |
| `CreateCardPage` | `/icu/doctor/create-card` | patient search + form — Phase 2: form stacks on mobile |
| `PatientDayPage` | `/icu/doctor/episode/:episodeId` | `IntensiveCareCard` already single-column below 1024px |

### ICU — nurse (1)
| Page | Route | Notes |
|---|---|---|
| `NurseDashboardPage` | `/icu/nurse` | dashboard cards — Phase 2 column flow |

### Hourly grid (shared, component-level)
- `components/monitoring/HourlyGrid` — already mobile-aware: 44px cells, `OutlierRail`,
  alarm chip, `criticalRanges.ts`; `HourlyGridDialog` fullscreen modal with print CSS,
  keyboard model (Esc/Tab/Alt+Enter), `animate: none` + `prefers-reduced-motion` guards.

### Medication sheet — prescription (3)
| Page | Route | Notes |
|---|---|---|
| `PrescriptionPage` | `/prescriptions/doctor` | list + drawer «Створити листок» (gated `PRESCRIPTION_LIST_CREATE`, uncommitted WIP) — Phase 2: drawer → Sheet on mobile |
| `PrescriptionDetailPage` | `/prescriptions/doctor/:id`, `/prescriptions/nurse/:id` | 21-day grid — Phase 2: `scroll-area.tsx` rail |
| `NursePrescriptionPage` | `/prescriptions/nurse` | nurse execution view |

### Prosthetics (17)
| Page | Route | Notes |
|---|---|---|
| `ProstheticsDashboard` | `/prosthetics` | instance cards (COMPLETED rows verified rendering — BUG-001 timing artifact) |
| `prosthetics/DashboardPage` | `/prosthetics` (legacy entry) | see above |
| `setup/PatientSearchPage` | `/prosthetics/new/select-patient` | wizard step 1 |
| `setup/PatientStep` | — | step content |
| `setup/OrderSelectPage` | `/prosthetics/new/select-order` | wizard step 2 |
| `setup/OrderStep` | — | step content |
| `setup/OrderReviewPage` | `/prosthetics/new/review-order` | wizard step 3 |
| `setup/ReviewStep` | — | step content |
| `setup/TemplateSelectPage` | `/prosthetics/new/select-template` | wizard step 4; «Обрати» disabled until template selected (BUG-004 harness timing, not a product bug) |
| `setup/TemplateStep` | — | step content |
| `process/ProcessLayout` | `/prosthetics/process/:id` | layout shell |
| `process/ProcessDetail` | `/prosthetics/process/:id` | overview |
| `process/ProcessOverview` | — | status/progress |
| `process/ProcessHistoryPage` | `/prosthetics/process/:id/history` | history |
| `process/WizardScreen` | `/prosthetics/process/:id/wizard` | main work surface — Phase 2: compact `stepper.tsx` header on tablet/mobile |
| `process/MeasurementForms` | — | measurement entry |
| `process/DoneScreen` | `/prosthetics/process/:id/done` | terminal |
| `process/FailedScreen` | `/prosthetics/process/:id/failed` | terminal |

**Phase 2 plan**: wire `stepper.tsx` into `WizardScreen` + `SetupSteps`; convert
`PrescriptionPage` drawer and `AdminPage` RBAC matrix to `sheet.tsx`/`scroll-area.tsx`;
dashboard card column-flow (`sm:grid-cols-2 lg:grid-cols-3`); `pointer-coarse:` touch
targets (Phase 5).

## 4. Acceptance status (Issue #160)

- [x] Breakpoint strategy documented in AGENTS.md («Responsive UI» section) + this doc.
- [x] Sheet/Stepper/ScrollArea exist as Base UI ports; only `@base-ui/react` added.
- [x] Shared `useMediaQuery`/`useIsMobile` used by `Sidebar` + `IntensiveCareCard`; inline copies removed.
- [x] CI green: run `32230291689` (commit `2f623ba`) — all 6 jobs incl. E2E 14m41s.
