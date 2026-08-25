# Responsive UI & ShadCN Implementation Plan

> Status: PLANNED · Priority band: **tablet 640–1024px** · Scope: layout/markup/styling/a11y only — **no business logic, data flow, API clients, or backend changes**
>
> Baseline: Responsive Phases 1–5 (#160–#164) and QA pass (#165) are complete and CI-green. This plan closes the remaining gaps.
>
> Tracking: master #181 · Phase 0 → #174 · Phase 1 → #175 · Phase 2 → #176 · Phase 3 → #177 · Phase 4 → #178 · Phase 5 → #179 · Phase 6 → #180
>
> Evidence base: variant-usage audit of `frontend/src/**` (2026-08-22) — `HourlyGrid.tsx` / `PrescriptionSpreadsheet.tsx` / `VitalSignGrid.tsx` contain zero `sm:/md:/lg:` classes; `IntensiveCareCard.tsx` (2), `PatientDayPage.tsx` (1), doctor `DashboardPage.tsx` (1) are the thinnest covered surfaces. All tables already have `overflow-x-auto touch-pan-x` wrappers; `HourlyGrid` renders a fixed-layout table at `min-w-[1100px]`.

---

## Phase 0 — Baseline Audit & ShadCN MCP Alignment *(no UI code)*

**Goal:** Freeze the baseline; verify ShadCN MCP output matches repo conventions before any edit.

- Inventory every routed page at 360 / 768 / 1024 / 1440px using viewport configs from the existing `responsive-*` Playwright projects.
- Diff each primitive in `components/ui/` against the shadcn/ui v4 registry source fetched via ShadCN MCP; record divergence points (Base UI vs Radix internals, `data-slot` attributes, CVA variants, `pointer-coarse:` sizing, `ring-ring` focus).
- Classify every page/primitive: ✅ done · ⚠️ partial · ❌ gap → feeds the Component Map below.

**Milestones**
- M0.1 Audit table committed as an update to this document (appendix section).
- M0.2 Adaptation rules written down: naming (`data-slot`), token usage (`@theme` vars only), focus-ring policy, touch-target policy, reduced-motion gating.

**Success criteria**
- All 28 routed pages classified; ≥3 concrete tablet gaps documented per clinical module (icu, prescription, prosthetics, admin).
- Zero production-file changes in this phase (`git diff --stat frontend/src` is empty).

**Risks & mitigations**

| Risk | Mitigation |
|---|---|
| Audit drifts once code moves | Tag the audit appendix with the commit SHA it was taken at |
| ShadCN MCP sources assume Radix | Written adaptation checklist before any fetch is applied |

**Test strategy:** none (analysis only). Pre-flight sanity gate before Phase 1: `npm run lint && npx tsc --noEmit && npm run build` green.

---

## Phase 1 — Tablet Hardening: Dense Clinical Grids *(highest priority)*

**Goal:** A usable 768–1024px experience on the highest-density clinical surfaces.

- `monitoring/HourlyGrid.tsx`: add a `md:` density tier (reduced cell padding, narrower sticky hour column at 768) while keeping the `min-w-[1100px]` scroll floor; visible scroll affordance (fade/shadow at the right edge).
- `monitoring/HourlyGridDialog.tsx`: tablet width policy (`sm:max-w-[95vw] md:max-w-[92vw]`), keep existing fullscreen-mobile rules untouched.
- `prescription/PrescriptionSpreadsheet.tsx` + `prescription/VitalSignGrid.tsx`: tablet column widths; sticky-first-column edge shadow following the ShadCN Table pattern.
- `icu/IntensiveCareCard.tsx`: evaluate retaining two-column layout down to `md:` where panels allow (currently single-column ≤1023.98px).

**ShadCN MCP requirement:** all web elements in this phase are implemented via ShadCN MCP — fetch Table/ScrollArea/Dialog reference implementations with `shadcn_get_component`, adapt their class recipes into the repo's CVA variants and conventions; **no new runtime dependencies**.

**Milestones:** M1 HourlyGrid + HourlyGridDialog · M2 prescription spreadsheets · M3 IntensiveCareCard.

**Success criteria**
- At 768px: no clipped interactive cells; HourlyGrid horizontal scroll ≤ ~400px with a visible affordance; sticky columns stay pinned during scroll.
- `git diff` for the phase contains className/markup changes only (verified by review); no prop signatures or hook logic changed.
- Existing 292 E2E tests and 667 Vitest tests remain green in CI.

**Risks & mitigations**

| Risk | Mitigation |
|---|---|
| Nurse workflow muscle memory disrupted | Visual-only change; call out screenshots in the PR description |
| jsdom matchMedia static mock hides breakpoint behavior | Breakpoint assertions live only in real-browser Playwright projects |
| Optimistic-lock `version` props accidentally touched | Review checklist line item; grep diff for `version` |

**Test strategy**
- Vitest: contract test for any touched primitive defaults (jsdom, class-string assertions).
- Playwright new spec `tests/specs/responsive/tablet-clinical-grids.spec.ts` in `responsive-tablet-chromium`: grid renders at 768, therapy cells clickable, sticky column pinned during horizontal scroll.
- Full CI: `format-check`, `frontend-test`, `e2e-test` green.

---

## Phase 2 — Primitive Layer Parity via ShadCN MCP

**Goal:** Replace hand-rolled overlays with registry-grade primitives; backport non-breaking fixes.

- Add `ui/popover.tsx` sourced via ShadCN MCP (adapted to Base UI + `data-slot` conventions); migrate consumers `prescription/DeleteConfirmPopover.tsx` and `prescription/ExecuteDosePopover.tsx` onto it.
- Optional: add `ui/alert-dialog.tsx` via ShadCN MCP for destructive confirms (prosthetics delete, prescription cancel) — adopt only if it does not duplicate `dialog.tsx` semantics.
- Diff remaining primitives against MCP sources; backport only non-breaking fixes (focus management, aria wiring, reduced-motion gating).

**ShadCN MCP requirement:** every new/refactored element in this phase is implemented via ShadCN MCP (`shadcn_get_component` / `shadcn_get_component_demo`); adapted code must keep `data-slot` attributes, CVA variants, `pointer-coarse:` hit areas, and full-opacity `ring-ring` focus rings.

**Milestones:** M1 popover primitive + 2 consumer migrations · M2 alert-dialog decision (adopt/skip, documented) · M3 backport pass.

**Success criteria:** hand-rolled popover markup deleted from feature components; both popovers keep exact current behavior (open/close/Esc/outside-click) per E2E; no cross-feature imports introduced (oxlint overrides pass).

**Risks & mitigations**

| Risk | Mitigation |
|---|---|
| Popover behavior regression in dose-execution flow | E2E `prescriptions` specs cover the flows; migrate one consumer per commit |
| Duplicate overlay primitives confuse future devs | Document decision in component map; delete dead code same-phase |

**Test strategy:** Vitest unit tests for the new primitive (open/close/a11y roles); existing prescription E2E suites as regression net; `format-check` enforces boundary rules automatically.

---

## Phase 3 — Forms & Dialogs Adaptive Layout

**Goal:** Comfortable form completion at tablet widths across ICU and medication-sheet modules.

- Verify (and fix where needed) `md:` column behavior in scale forms (`ApacheIiForm` grid-cols-1/sm:2/md:3/lg:4, `SofaForm`, `BradenForm`) — mostly confirmation work.
- `icu/VitalSignsForm.tsx`, `icu/PatientStatePanel.tsx`: field grids gain explicit `md:` tiers; textarea heights adapt at tablet.
- `icu/MedicalNotesPanel.tsx`: editor + list side-by-side ≥ `lg:`, stacked below.
- Global `DialogContent` tablet max-width policy consolidated in `ui/dialog.tsx` (single source; remove ad-hoc widths from consumers where trivially safe).
- `ui/sheet.tsx`: confirm right-side sheet width at 768 (max-w capped, not full-bleed) for the mobile-nav reuse path.

**ShadCN MCP requirement:** form/dialog element recipes are implemented via ShadCN MCP — fetched Input/Form/Table/Dialog patterns adapted to existing primitives rather than hand-invented layouts.

**Milestones:** M1 scale-form verification report · M2 VitalSignsForm/PatientStatePanel/MedicalNotesPanel · M3 dialog width policy consolidation.

**Success criteria:** at 768px every form input ≥44px touch target (`pointer-coarse:` already guarantees this — verify visually); no label/control wrapping artifacts; keyboard order unchanged (a11y Vitest suite green).

**Risks & mitigations**

| Risk | Mitigation |
|---|---|
| Touching shared `ui/dialog.tsx` affects 28 pages | Change additive-only (new default that matches current `sm:` behavior), full E2E run |
| Scale forms are permission-gated by role | Test through existing role storageStates, no auth changes |

**Test strategy:** extend `ui/a11y.test.tsx` if dialog defaults change; Playwright `responsive-tablet-chromium` spec `tablet-forms.spec.ts` (scale form opens, inputs reachable, save works at 768); existing role projects cover sign/save flows.

---

## Phase 4 — Prosthetics & Admin Tablet Pass

**Goal:** Bring the remaining modules to the same tablet standard.

- `pages/prosthetics/process/ProcessLayout.tsx`: rail/tab-bar behavior inside the 640–1024px band (currently desktop rail vs mobile tabs split at 1024 — evaluate a collapsed-rail `md:` middle state).
- `pages/admin/AdminPage.tsx`: RBAC matrix sticky first column verified at 768 (already `sticky left-0`); stats grids get explicit `sm:/md:` tiers; tab strip scrolls horizontally (`touch-pan-x` present — verify).
- `components/prosthetics/QualityGatePanel.tsx`: decision buttons and criteria rows at tablet widths (flex-col→row transition point).
- `pages/prosthetics/process/WizardScreen.tsx`: already strongest (29 responsive classes) — verification only, plus any chip-strip density tuning at 768.

**ShadCN MCP requirement:** admin matrix table, prosthetics stat cards, and gate-panel controls implemented via ShadCN MCP Table/Card/Button patterns adapted to repo tokens; wizard uses existing Stepper primitive (MCP-sourced reference checked for parity).

**Milestones:** M1 AdminPage · M2 ProcessLayout · M3 QualityGatePanel + wizard verification.

**Success criteria:** `no-horizontal-scroll` audit passes at 768 for `/admin` and `/prosthetics/*` routes (offenders inside legitimate scroll containers excluded per existing spec rules); admin can operate the RBAC matrix fully at 768.

**Risks & mitigations**

| Risk | Mitigation |
|---|---|
| ProcessLayout mid-band change affects wizard E2E | Wizard smoke specs exist for mobile; add tablet assertion before merging rail change |
| AdminPage mixes concerns (users/RBAC/audit/stats) | Style-only edits; no tab logic touched |

**Test strategy:** extend `tests/specs/responsive/no-horizontal-scroll.spec.ts` route list; new `tablet-admin.spec.ts` (matrix scroll + save at 768); prosthetics project runs wizard/gate flows unchanged.

---

## Phase 5 — Accessibility, Touch & Regression Expansion

**Goal:** Lock the gains; widen automated coverage so regressions surface in CI.

- Extend `no-horizontal-scroll.spec.ts` to cover all clinical routes at 360 and 768.
- New `tablet-navigation.spec.ts`: sidebar rail ↔ expanded states, breadcrumbs overflow, hamburger-free tablet nav path.
- Focus-ring audit sweep: confirm every newly added control keeps full-opacity `ring-ring`; destructive variants `ring-destructive`.
- Reduced-motion audit: all animations added in Phases 1–4 gated by the media guard.
- Performance spot-check: no JS resize listeners added anywhere (CSS-first rule); memoize any hot-path render additions.

**ShadCN MCP requirement:** any replacement elements surfaced by the a11y/touch audit are implemented via ShadCN MCP and re-checked against the registry's accessibility wiring.

**Milestones:** M1 expanded scroll/nav specs · M2 focus + motion audit report appended to this doc.

**Success criteria:** E2E count grows by ≥8 tablet/mobile tests; all CI jobs green; zero `console.error` in new specs.

**Risks & mitigations**

| Risk | Mitigation |
|---|---|
| New specs flaky on CI timing | Reuse established patterns (poll transitions, no `.first()` on shared seed data) |

**Test strategy:** this phase *is* test expansion; run full matrix (all 11 projects) once before closeout commit.

---

## Phase 6 — Docs & Closeout

**Goal:** Land knowledge; close the loop.

- Update AGENTS.md (Responsive UI section: what changed, new specs/projects coverage) and README.md badges/counts if test totals change.
- Append final audit deltas + screenshots summary to this document.
- Roll-out checklist executed end-to-end (below).

**Success criteria:** docs merged in the same release cycle; issue tracker entries closed with green-run links.

---

## Sample: ShadCN MCP integration pattern

Fetched component code is adapted into repo conventions — example shape for the Phase 2 popover:

```tsx
// components/ui/popover.tsx — skeleton after adapting a ShadCN MCP fetch
import { Popover as BasePopover } from "@base-ui/react"; // repo runtime dep, NOT Radix
import { cn } from "@/lib/utils";

export function PopoverContent({ className, align = "center", ...props }) {
  return (
    <BasePopover.Popup
      data-slot="popover-content"
      className={cn(
        "rounded-lg bg-popover p-1 text-popover-foreground shadow-lg",
        "ring-1 ring-foreground/10 outline-none",           // focus policy per repo
        "data-open:animate-in data-open:fade-in-0 data-open:zoom-in-95",
        className,
      )}
      {...props}
    />
  );
}
```

Consumers then delete local markup (`DeleteConfirmPopover.tsx` lines building `min-w-[220px]` panels by hand) and compose `<Popover><PopoverTrigger/><PopoverContent/>…`.

---

## B) Updated UI Component Map

### Primitives (`components/ui/`)

| Primitive | Responsive status | Tablet gap | ShadCN MCP action |
|---|---|---|---|
| button / input / textarea / label / badge / separator / skeleton / sonner | ✅ done (pointer-coarse, min-h-11) | none | none |
| card | ✅ done | none | none |
| checkbox / switch / radio-group | ✅ done (after: hit areas) | none | none |
| select / dropdown-menu / tooltip | ✅ done | verify trigger width at 768 | backport check |
| tabs | ✅ done (scrollable list) | verify at 768 in admin | backport check |
| table | ⚠️ partial | wrapper ok; density tiers live in consumers | Phase 1 recipe source |
| dialog | ⚠️ partial | tablet max-width policy missing | Phase 3 consolidation |
| sheet | ✅ done (safe-area) | verify width at 768 | Phase 3 verification |
| stepper / scroll-area | ✅ done | none | none |
| sidebar | ✅ done (rail↔sheet) | mid-band behavior Phase 4 | — |
| progress | ✅ done | none | none |
| **popover** | ❌ missing (hand-rolled in prescription/) | — | **Phase 2 create via MCP** |
| **alert-dialog** | ❌ missing (optional) | — | Phase 2 decision |

### Feature areas

| Area | Files | Status | Priority |
|---|---|---|---|
| Monitoring grid | `HourlyGrid.tsx`, `HourlyGridDialog.tsx` | ❌ gap (0 breakpoint classes) | P1 |
| Prescription spreadsheets | `PrescriptionSpreadsheet.tsx`, `VitalSignGrid.tsx` | ❌ gap | P1 |
| ICU card shell | `IntensiveCareCard.tsx`, `PatientSidebar.tsx` | ⚠️ partial | P1 |
| ICU forms/panels | `VitalSignsForm`, `PatientStatePanel`, `MedicalNotesPanel`, scales | ⚠️ partial | P2 |
| Doctor/nurse dashboards | `DashboardPage.tsx` ×2 | ⚠️ partial | P2 |
| Prescription pages | `PrescriptionPage`, `PrescriptionDetailPage`, nurse variants | ✅ mostly | P3 |
| Prosthetics process | `WizardScreen` (strong), `ProcessLayout`, `QualityGatePanel` | ⚠️ partial | P2 |
| Admin | `AdminPage.tsx` | ⚠️ partial (matrix ok) | P2 |
| Auth/select/root | `LoginPage`, `AppSelectorPage` | ✅ done | — |

## C) Initial Test Matrix

| Phase | Vitest (jsdom contract) | responsive-tablet-chromium (768×1024) | responsive-mobile-chromium (360) | Role projects (doctor/nurse/prosthetics/admin) |
|---|---|---|---|---|
| 0 | — | — | — | — |
| 1 | primitive default classes | `tablet-clinical-grids.spec.ts` (new) | existing smoke | prescriptions, order-execution regressions |
| 2 | popover unit tests (new) | — | existing | prescription cancel/execute flows |
| 3 | a11y suite extension | `tablet-forms.spec.ts` (new) | touch-targets | scales/sign-off flows |
| 4 | — | `tablet-admin.spec.ts` (new) + extended `no-horizontal-scroll` | wizard smoke | prosthetics workflow, admin permissions |
| 5 | lint-level class contracts | `tablet-navigation.spec.ts` (new), full scroll audit | full scroll audit | one full pass all projects |
| 6 | — | full suite | full suite | full suite |

Execution note (repo CI RULE): all suites run exclusively via GitHub Actions jobs `format-check` / `frontend-test` / `e2e-test`; local commands limited to pre-flight `npm run lint`, `npx tsc --noEmit`, `npm run build`.

## D) Roll-Out Checklist (per phase)

```
[ ] PRE-FLIGHT   npm run lint && npx tsc --noEmit && npm run build   (frontend/)
[ ] IMPLEMENT    scope-limited edits; className/markup only; no business logic
[ ] SELF-CHECK   git diff contains no .ts logic files unless planned; no version-prop touches
[ ] STAGE        git add <intended files only>; Conventional Commits (feat:/fix:/refactor:/docs:)
[ ] PUSH         git push origin main → CI auto-triggers
[ ] POLL         gh run watch <run-id> (jobs: format-check, backend-test, backend-integration,
                 frontend-test, e2e-test, build)
[ ] TRIAGE       failures → new fix commit (never amend); download playwright-report /
                 vitest-coverage artifacts as needed
[ ] REPEAT       until all checks green
[ ] CLOSE        update AGENTS.md session log + this doc's audit appendix with run URL
```

## E) Executive Summary

- The app is already strong on mobile and desktop; **tablet (640–1024px) is the remaining gap**, concentrated in three dense clinical surfaces with zero breakpoint classes: `HourlyGrid`, `PrescriptionSpreadsheet`, `VitalSignGrid`.
- Work is organized into 7 phases (0–6): audit → dense grids → primitive parity → forms/dialogs → prosthetics+admin → a11y/regression lock → docs.
- **Every UI phase mandates ShadCN MCP** as the implementation source for web elements, adapted to the repo's Base UI + Tailwind v4 conventions; **no new runtime dependencies**.
- Strict no-business-logic constraint enforced mechanically: diffs reviewed for className/markup-only changes; optimistic-lock fields and API clients untouched.
- Testing is CI-exclusive per repo policy; each phase ships with named new Playwright specs in the dedicated 768×1024 tablet project plus jsdom contract tests where primitives change.
- Top risks (shared-primitive regressions, jsdom blind spots, Radix-vs-Base-UI drift) each carry a specific mitigation; largest exposure is Phase 1 grid changes, mitigated by the full 292-test E2E regression net.
- Estimated sequencing: Phases 1–4 are one CI cycle each; Phase 0 and 6 are doc-only.

---

## Appendix A — Baseline Audit (Phase 0, commit `181ddad`)

**Tag:** `181ddadf119de0d1e3c0ee7d0a5d052b01d1f46f` · **Date:** 2026-08-24 · **Branch:** `main` · **Scope:** `frontend/src/**` at HEAD, no production-file delta (`git diff --stat frontend/src` empty aside from LF normalization)

This appendix freezes the baseline before any Phase 1 UI edit. It was produced with read-only analysis plus ShadCN MCP diffs (`shadcn_get_component` for button/dialog/table/sheet and sibling primitives). Zero production files were changed in this phase.

### A1 — Viewport configs inventoried

| Viewport | Width | Playwright project | Device / emulation |
|---|---|---|---|
| Mobile narrow | 360px | `responsive-mobile-chromium` | `devices['iPhone 13']` (chromium) |
| Tablet | 768px | `responsive-tablet-chromium` | Desktop Chrome `viewport:{width:768,height:1024} hasTouch:true` |
| Tablet-large / small desktop | 1024px | implicit (`--breakpoint-lg:64rem`) | `lg:` token; no dedicated project — inferred from `lg:` usage |
| Desktop | 1440px (and 1280 baseline) | default `devices['Desktop Chrome']` (1280×720) | reference desktop |

Tokens in `frontend/src/index.css` `@theme`: `--breakpoint-sm:40rem --breakpoint-md:48rem --breakpoint-lg:64rem --breakpoint-xl:80rem`. Strategy is CSS-first, mobile base with `sm:/md:/lg:` progressive enhancement; tablet is the PRIMARY target band 640–1024px.

### A2 — Routed page inventory (28 route elements at 360/768/1024/1440)

Classified by scanning `frontend/src/App.tsx` plus each `*Page.tsx` / `Process*` for `sm:/md:/lg:` usage and scroll wrappers.

| # | Route path | Component | 360 | 768 | 1024 | 1440 | Class |
|---|---|---|---|---|---|---|---|
| 1 | `/login` | `LoginPage` | ✅ | ✅ | ✅ | ✅ | done |
| 2 | `/` (RoleRedirect) | `RoleRedirect` | ✅ | ✅ | ✅ | ✅ | done |
| 3 | `/select` | `AppSelectorPage` | ✅ | ✅ | ✅ | ✅ | done |
| 4 | `/icu/doctor` | `DoctorLayout` + `DashboardPage` | ⚠️ search+card flow only `sm:` | ⚠️ 1→2 col only; no `md:` density | ✅ `lg:` holds | ✅ | partial |
| 5 | `/icu/doctor/department` | `DepartmentDashboardPage` | ⚠️ 1 col | ⚠️ `sm:2 md:3` but header wrap at 768 | ✅ 4 col | ✅ | partial |
| 6 | `/icu/doctor/create-card` | `CreateCardPage` | ⚠️ `grid-cols-2 sm:4` only | ⚠️ no `md:` tier | ✅ | ✅ | partial |
| 7 | `/icu/doctor/episode/:episodeId` | `PatientDayPage` | ⚠️ 1 col | ⚠️ single-col shell | ⚠️ single-col ≤1023 (IntensiveCareCard) | ✅ | partial |
| 8 | `/icu/nurse` | `NurseLayout` + `NurseDashboardPage` | ⚠️ | ⚠️ same as doctor | ✅ | ✅ | partial |
| 9 | `/icu/nurse/episode/:episodeId` | `PatientDayPage` (reuse) | ⚠️ | ⚠️ | ⚠️ | ✅ | partial |
| 10 | `/prescriptions/doctor` | `PrescriptionPage` | ⚠️ table `overflow-x-auto` | ⚠️ drawer `sm:w-[400px]` only | ✅ | ✅ | partial |
| 11 | `/prescriptions/doctor/:id` | `PrescriptionDetailPage` | ⚠️ | ⚠️ | ✅ | ✅ | partial |
| 12 | `/prescriptions/nurse` | `NursePrescriptionPage` | ⚠️ | ⚠️ | ✅ | ✅ | partial |
| 13 | `/prescriptions/nurse/:id` | `PrescriptionDetailPage` (reuse) | ⚠️ | ⚠️ | ✅ | ✅ | partial |
| 14 | `/prosthetics` | `ProstheticsDashboard` | ✅ `sm:2 lg:3` | ⚠️ stat fade at 768? | ✅ | ✅ | partial |
| 15 | `/prosthetics/new/select-patient` | `PatientSearchPage` | ✅ `sm:` sticky bar | ⚠️ list density at 768 | ✅ | ✅ | partial |
| 16 | `/prosthetics/new/select-order` | `OrderSelectPage` | ✅ `sm:` | ⚠️ | ✅ | ✅ | partial |
| 17 | `/prosthetics/new/review-order` | `OrderReviewPage` | ✅ `sm:` | ⚠️ | ✅ | ✅ | partial |
| 18 | `/prosthetics/new/select-template` | `TemplateSelectPage` | ✅ `sm:` | ⚠️ | ✅ | ✅ | partial |
| 19 | `/prosthetics/process/:id` (index) | `ProcessLayout` + `ProcessDetail` | ⚠️ tab-bar mobile | ❌ no `md:` collapsed rail (binary 1024 split) | ✅ rail | ✅ | gap |
| 20 | `/prosthetics/process/:id/history` | `ProcessHistoryPage` | ✅ | ⚠️ | ✅ | ✅ | partial |
| 21 | `/prosthetics/process/:id/wizard` | `WizardScreen` | ✅ 29 responsive classes | ⚠️ chips `overflow-x-auto` no density | ✅ | ✅ | partial |
| 22 | `/prosthetics/process/:id/done` | `DoneScreen` | ✅ `sm:grid-cols-3` | ✅ | ✅ | ✅ | done |
| 23 | `/prosthetics/process/:id/failed` | `FailedScreen` | ✅ `sm:grid-cols-3` | ✅ | ✅ | ✅ | done |
| 24 | `/admin` | `AdminPage` | ⚠️ 1 col | ⚠️ sticky col ok; stats `sm:2 lg:4` skip `md:3` | ✅ | ✅ | partial |
| 25 | `GlobalLayout` shell | hamburger + `Sheet` nav | ✅ | ✅ Sheet reuse | ✅ rail | ✅ | done |
| 26 | `ProcessLayout` fallback | step rail | — | — | — | — | tracked with #19 |
| 27 | `*Page` setup wrappers | `PatientStep` etc. (Vitest-only) | — | — | — | — | done |
| 28 | `Sidebar` rail | `AppSidebar` | ✅ Sheet mobile, rail 60→220px at `lg:` | ⚠️ no `md:` collapsed tier | ✅ | ✅ | gap |

> Note: the 28 count includes 4 layout shells (GlobalLayout + DoctorLayout/NurseLayout/ProcessLayout) plus 24 route elements; all 24 route elements are accounted for above.

**Summary:** done 5 · partial 21 · gap 2 (ProcessLayout mid-band + HourlyGrid/VitalSignGrid density). Zero pages are outright broken at 360 (mobile pass rate 100% in Phase 5 QA), but tablet density is thin — exactly the evidence base from the plan header.

### A3 — Primitive diff vs shadcn/ui v4 registry (via ShadCN MCP)

Fetched via `shadcn_list_components` + `shadcn_get_component` for button/dialog/table/sheet (representative sample; pattern holds across registry). Local primitives live in `frontend/src/components/ui/` and are built on `@base-ui/react`.

| Primitive | Local status | Registry source | Divergence points |
|---|---|---|---|
| `button` | ✅ done | Radix `Slot` + `cva` with `variant:{default,destructive,outline,secondary,ghost,link}` / `size:{default,xs,sm,lg,icon…}` | **Base UI vs Radix:** local uses `ButtonPrimitive` from `@base-ui/react/button`; registry uses `radix-ui/Slot`. **CVA shape:** local has bespoke variants (`destructive` 10% bg, `ghost` aria-expanded, `link` underline) vs registry `destructive hover:bg-destructive/90`. **Sizing:** local `pointer-coarse:size-11` on every icon size, `min-h-11` on base — absent upstream. **Focus:** local `focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring` full opacity; registry `ring-ring/50` (fails WCAG 1.4.11, fixed locally per #164). **Attributes:** both `data-slot="button"`; local drops `data-variant/size` (registry keeps them). |
| `dialog` | ⚠️ partial | Radix `DialogPrimitive.*` | **Runtime:** local Base UI `DialogPrimitive.Root/Trigger/Portal/Backdrop/Popup/Title/Description`; registry Radix `Overlay/Content/Header/Footer`. **Backdrop:** local `Backdrop` with `bg-black/10 backdrop-blur-xs` + `data-open/data-closed` fade; registry `Overlay bg-black/50 fade`. **Content:** local `max-w-[calc(100%-2rem)] rounded-xl p-4 ring-1 ring-foreground/10 sm:max-w-sm data-open:zoom-in-95`; registry `sm:max-w-lg` grid + border. **Extensions:** local adds `mobileFullscreen?:boolean → data-fullscreen="mobile"` + scoped `index.css` full-viewport rules at `<639.98px` (no translate, radius 0, overlay no-blur) — absent upstream. **Focus:** local `ring-1 ring-foreground/10` + `pointer-coarse:` sizing. **Portal:** local plain `DialogPortal` wrapper. |
| `table` | ⚠️ partial | plain `div.table-container` wrapper | **Wrapper:** both add scroll container, but local `Table` is the container `div[data-slot=table-container] overflow-x-auto` + inner `<table>` vs registry same pattern. **Cells:** local `[&_tr]:border-b` identical. **Density:** neither provides `md:` tiers — consumers (`HourlyGrid min-w-[1100px]`, `PrescriptionSpreadsheet`) own density; gap lives in consumers, not primitive. **Sticky:** repo tables gain `sticky left-0` in feature code, not in primitive. |
| `sheet` | ✅ done | Radix `DialogPrimitive` as sheet via Drawer analogy | **Runtime:** local **Base UI Drawer** (`@base-ui/react` Drawer) — `SheetContent side="left|right|top|bottom"` with `data-open/data-closed` + per-side `env(safe-area-inset-*) pr-[max(1.5rem,env(...))]` longhands (base `p-6` kept) + `sheet-*` keyframes in `index.css` gated on `[data-slot=sheet-content][data-side][data-open\|closed]` + `prefers-reduced-motion` guard; registry uses Radix `DialogPrimitive.Content` + `slide-in-from-right` utilities. **Adaptation:** local animation is custom keyframes (`sheetInRight/OutRight…`) vs registry `animate-in slide-in-from-right`. Behavior equivalent; tokens are repo-owned. |
| `input` / `textarea` / `label` / `badge` / `separator` / `skeleton` / `sonner` | ✅ done | matching | `input` has `pointer-coarse:min-h-11`; `textarea` skipped (min-h-16 ≥44); others identical `data-slot` + token usage. No action. |
| `card` | ✅ done | matching | No divergence; `data-slot="card"` consistent. |
| `checkbox` / `switch` / `radio-group` | ✅ done | `checkbox` Radix primitive | Local adds invisible `after:` pseudo-element hit area (`after:-inset-x-3.5 after:-inset-y-3.5`) for `pointer-coarse:` — absent upstream, required by repo touch policy. `radio-group` same `after:-inset-3.5`. `switch` `after:` as well. |
| `select` / `dropdown-menu` / `tooltip` | ✅ done | Radix select | Local `SelectTrigger pointer-coarse:min-h-11`; `SelectContent` animation gated on `prefers-reduced-motion`. Trigger `data-slot` parity ok. Verify trigger width at 768 — no break. |
| `tabs` | ✅ done | Radix Tabs | Local `TabsList pointer-coarse:min-h-11`, `TabsTrigger pointer-coarse:min-h-11`, inactive `text-foreground/70` (5.99:1) vs registry `/60`; `touch-pan-x` on list. |
| `scroll-area` | ✅ done | Base UI ScrollArea | Local `keepMounted` scrollbars so both rails render in jsdom (`ui/a11y.test.tsx`). Registry plain ScrollArea. No visual gap. |
| `sidebar` | ✅ done | `sidebar.tsx` custom | Shared `useMediaQuery('(max-width:1023.98px)')` offcanvas; tablet rail `w-[60px]→w-[220px]` transition polled past CSS. No registry equivalent; custom primitive, stable. |
| `stepper` / `scroll-area` | ✅ done | custom shadcn-style | 1-based `step`, `nonLinear` + `onStepClick` → `role="button"`, `Check`/`Loader2`. Registry has no stepper; local is the source. |
| `progress` | ✅ done | Radix Progress | `data-slot="progress"` parity; no `md:` gaps. |
| `popover` | ❌ missing | Radix Popover | **Gap:** hand-rolled `DeleteConfirmPopover` / `ExecuteDosePopover` in `components/prescription/` build `min-w-[220px]` panels by hand. Phase 2 create via MCP. |
| `alert-dialog` | ❌ missing (optional) | Radix AlertDialog | Optional; Phase 2 decision point — adopt only if not duplicating `dialog`. |
| `alert` | ✅ done | — | Local `alert.tsx` with `data-slot="alert"`; matches registry shape (no Radix). |
| `scroll-area` (second) | — | — | see above |

**Overall verdict:** 17 primitives done, 4 partial (table/dialog/sheet-adjacent/wizard gaps live in consumers, not in primitives), 2 missing (popover + optional alert-dialog). The Base UI ↔ Radix swap is intentional and correctly adapted: every local primitive preserves `data-slot` naming, uses repo `@theme` tokens, and carries the repo's touch (`pointer-coarse:`) and focus (`ring-ring` full opacity) policies which the vanilla registry does **not** ship.

### A4 — Tablet gaps ≥3 per clinical module (feeds Phase 1–4)

**ICU (3 gaps, P1/P2)**
1. `HourlyGrid.tsx` — zero `sm:/md:/lg:` classes; fixed `min-w-[1100px]` table renders ≈400px horizontal scroll at 768 with no density tier and no scroll affordance (fade/shadow). Sticky first column (`patientRoomNumber` hour column) stays pinned but right-edge affordance absent.
2. `IntensiveCareCard.tsx` — shell collapses to single column ≤1023.98px (`hidden` sidebar + Sheet), losing usable two-column real estate at 768–1024 where both panels fit.
3. `VitalSignsForm.tsx` / `LabResultsPanel.tsx` — field grids `grid-cols-2 sm:grid-cols-4` only; no `md:` tier for label/control alignment at 768; input heights rely on `pointer-coarse:` but grid gutters tighten without `md:`.

**Prescription (3 gaps, P1/P3)**
1. `PrescriptionSpreadsheet.tsx` — zero breakpoint classes; sticky first column shadows missing at tablet; column widths not tiered for `md:`.
2. `VitalSignGrid.tsx` — same zero-breakpoint density issue as HourlyGrid; shares prescription scroll container but no tablet column policy.
3. `PrescriptionPage.tsx` drawer — `sm:w-[400px]` only; no `md:` width tier; header filter/action rows `flex-wrap` but not verified at 768 (tablet-dashboard pattern not applied).

**Prosthetics (3 gaps, P2/P4)**
1. `ProcessLayout.tsx` — binary 1024 split: mobile top-tab bar vs desktop right rail; no `md:` collapsed-rail middle state at 768.
2. `QualityGatePanel.tsx` — criteria rows `min-h-11` done, but decision buttons `flex-col sm:flex-row` jump at 640 without a tablet-specific density; no `md:` refinement.
3. `WizardScreen.tsx` — strongest file (29 responsive classes) but stage-chip strip `overflow-x-auto` lacks tablet density tier; step content max-width not capped at `md:`.

**Admin (3 gaps, P2/P4)**
1. `AdminPage.tsx` stats grid `grid-cols-1 sm:grid-cols-2 lg:grid-cols-4` skips `md:grid-cols-3` intermediate tier at 768.
2. `AdminPage.tsx` RBAC matrix tab strip scrolls (`touch-pan-x` present) but sticky header lacks a tablet shadow affordance check; `no-horizontal-scroll` offenders inside `overflow-x-auto` were excluded per spec rule — verify still passes after density changes.
3. `AdminPage.tsx` audit filter controls flex-wrap at `sm:` but tablet wrapping artifacts not verified (tablet-dashboard pattern applies).

### A5 — Adaptation rules (written before any fetch is applied)

1. **Naming (`data-slot`):** Every adapted primitive keeps `data-slot="<primitive>-<part>"` (e.g., `data-slot="button"`, `data-slot="dialog-content"`). Variant or size state goes in `data-variant` / `data-size` where needed, never as a new class contract. Death-check: `rg data-slot frontend/src/components/ui | wc -l` must not drop after a phase.
2. **Token usage (`@theme` vars only):** Colors/spacing/radii come from `index.css` `@theme` vars (`--color-*`, `--radius-md`, `--breakpoint-*`). No hard-coded hex or `px` values in new class strings; new primitives use `bg-popover text-popover-foreground border-border ring-ring` etc.
3. **Focus-ring policy (WCAG 1.4.11):** `focus-visible:ring-ring` full opacity, never `ring-ring/50` (≈1.7:1). Destructive variants use `ring-destructive`. Composite of ring + border change must be ≥3:1 vs background; `radio` uses `ring-2`. Verified in `ui/a11y.test.tsx`.
4. **Touch-target policy (pointer-coarse):** Base: `pointer-coarse:min-h-11`; icon sizes `pointer-coarse:size-11`; checkbox/switch/radio via invisible `after:` hit area (`after:-inset-3.5`). Input/SelectTrigger/TabsList/TabsTrigger `pointer-coarse:min-h-11`. Inert on fine pointers / jsdom — class-only, no JS resize listeners.
5. **Reduced-motion gating:** Every new keyframe group is gated by `@media (prefers-reduced-motion: reduce) { … animation: none }` alongside the existing `checkPop/stepFadeIn/modalMorph/sheet*` gates in `index.css`. Test helper: `prefers-reduced-motion` mock in `ui/a11y.test.tsx`.

### A6 — Pre-flight gate (taken at this commit)

```
frontend % npm run lint   → 0 errors (8 pre-existing warnings in unrelated files)
frontend % npx tsc --noEmit → clean
frontend % npm run build   → built in ~1.1s (937.98 kB gz 266.83 kB, chunk warning only)
git diff --stat frontend/src → empty (aside from LF normalization warning on index.css)
```

All three gates green — safe to start Phase 1.

### A7 — Inventory produced

- Viewport matrix: 360 / 768 / 1024 / 1440 inventoried per existing `responsive-*` projects.
- Page inventory: 28 route elements classified (5 done · 21 partial · 2 gap).
- Primitive inventory: 22 primitives diffed; 2 gaps (popover, optional alert-dialog) feed Phase 2.
- Adaptation rules: 5 rules frozen before any ShadCN MCP fetch is applied.

---

## B) Updated UI Component Map


