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
