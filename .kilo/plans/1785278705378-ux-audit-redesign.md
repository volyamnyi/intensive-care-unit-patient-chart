# UX Audit & Redesign Strategy — ICU Patient Chart

## Executive Summary

The ICU Patient Chart is a functional clinical documentation tool with solid role-based access and domain logic. However, it has **critical design system gaps** (missing tokens making dropdowns/popovers invisible, no focus rings), **two oversized components** (IntensiveCareCard 813 lines, PrescriptionGrid 587 lines) that concentrate complexity, **inconsistent dark mode** (40+ hardcoded hex values, 3 different border colors), and **accessibility failures** (no skip-nav, no ARIA grid, non-keyboard-accessible clickable cells). The redesign prioritizes fixing the broken design tokens first, then restructuring navigation and component architecture.

---

## Part 1: Critical Design System Repairs (P0)

### 1.1 Missing Color Tokens

Five token families are referenced by shadcn/ui components but **never defined**, causing invisible backgrounds, transparent hover states, and missing focus rings.

**Action:** Add to `src/index.css` `@theme` block:

```css
/* Light mode defaults (inside existing @theme) */
--color-accent: #F0EDE8;
--color-accent-foreground: #1F1F1F;
--color-destructive: #FF5252;
--color-destructive-foreground: #FFFFFF;
--color-popover: #FFFFFF;
--color-popover-foreground: #1F1F1F;
--color-ring: #FF5F33;

/* Dark mode overrides (inside existing .dark) */
.dark {
  --color-accent: #2A2A2A;
  --color-accent-foreground: #FFFFFF;
  --color-destructive: #FF6B6B;
  --color-destructive-foreground: #FFFFFF;
  --color-popover: #1A1A1A;
  --color-popover-foreground: #FFFFFF;
  --color-ring: #FF8C66;
}
```

### 1.2 Missing Border Radius Token

Button size variants reference `var(--radius-md)` which is undefined.

**Action:** Add `--radius-md: 0.5rem;` to `@theme` block.

### 1.3 Font System Fix

Three font families are loaded (Rubik, Mulish, Inter) but Inter has zero usage, and UI primitives (Button, Input, Card, Badge, Table) render in system font while surrounding text uses Rubik/Mulish — creating a jarring visual mismatch.

**Action:**
- Remove Inter font import from `index.html` (saves a CDN request)
- Set Rubik as the base font via `--font-sans: "Rubik", sans-serif` in `@theme`
- Keep Mulish for body/description text via explicit `font-mulish` classes

### 1.4 Hardcoded Color Elimination

40+ unique hex values scattered across components bypass the token system. Key offenders:

| Component | Hardcoded Values | Fix |
|---|---|---|
| GlobalLayout nav | `#FF8C66`, `rgba(255,95,51,0.08)` | `text-secondary`, `hover:bg-primary/8` |
| DoctorDashboard/NurseDashboard | `#2A2A2A`, `#D0CEC9`, `#E0DED9` borders | `border-border` |
| IntensiveCareCard | `#F4F2ED`, `#EDEBE6`, `#1A1A1A`, `#202020` row alts | `bg-muted/30`, `bg-card` |
| HourlyRecordTable | `#E8F5E9`, `#FFEBEE`, `#1A3A2A`, `#3A1A1A` | `bg-success/10`, `bg-error/10` (dark: `bg-success/20`, `bg-error/20`) |
| ClinicalDayTimeline | `#F0F7F0`, `#F0F4FF`, `#42A5F5`, `#FF9800` | Token-based status colors |
| LoginPage | `#0D0D0D`, `#1976d2`, `#2e7d32` | `bg-background`, `text-info`, `text-success` |

### 1.5 Global Transition Bloat

The `* { transition: ... }` rule applies transitions to ALL elements including `::before`/`::after` pseudo-elements, causing performance overhead and unexpected animations.

**Action:** Scope to interactive elements only:
```css
button, a, input, select, textarea, [role="button"], [tabindex] {
  transition: background-color 0.2s ease, color 0.15s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}
```

---

## Part 2: Navigation & Information Architecture (P1)

### 2.1 Current Navigation Problems

- **No breadcrumb trail** — Users deep in `/prescriptions/doctor/:id` have no path back except browser back or re-selecting from "Додатки"
- **AppSelectorPage as hub** — Every module switch requires returning to `/select`, adding an extra click
- **No active state on nav** — The header has "Відділення" and "Додатки" buttons but no visual indicator of which module the user is in
- **DoctorLayout/NurseLayout are empty wrappers** — 9 lines each, just `<div className="fade-in-up"><Outlet /></div>`, add no value

### 2.2 Proposed Navigation Redesign

**Replace header-only nav with a contextual sidebar + breadcrumb system:**

```
┌─────────────────────────────────────────────────┐
│ [Logo] ВАІТ — Карта інтенсивної терапії    [🌙] [👤] │
├─────────────────────────────────────────────────┤
│ breadcrumbs: Призначення > Петренко Олександр > День 3 │
├────────────┬────────────────────────────────────┤
│ Sidebar    │  Main Content Area                  │
│            │                                     │
│ • Пацієнти │  (page content)                     │
│ • Призначення│                                   │
│ • Відділення│                                   │
│ • Додатки  │                                     │
│            │                                     │
│ ───────── │                                     │
│ Quick nav: │                                     │
│ Today ▸    │                                     │
│ Patient 1  │                                     │
│ Patient 2  │                                     │
└────────────┴────────────────────────────────────┘
```

**Key changes:**
1. **Persistent left sidebar** (already exists as `Sidebar.tsx` but unused) — provide always-visible navigation instead of header-only buttons
2. **Breadcrumb trail** — show hierarchical path (Module > Patient > Day) for back-navigation
3. **Remove DoctorLayout/NurseLayout** — consolidate into GlobalLayout with role-aware content
4. **Active nav highlighting** — current module/section highlighted in sidebar
5. **Quick patient list** in sidebar — recently viewed patients for fast switching

### 2.3 Route Simplification

Current: `/doctor`, `/nurse`, `/prescriptions/doctor`, `/prescriptions/nurse` — 4 separate route trees with duplicated logic.

**Proposed:** Merge into unified routes with role-aware rendering:
```
/patients                    → patient list (role-aware dashboard)
/patients/:episodeId         → patient day view (role-aware)
/prescriptions               → prescription list (role-aware)
/prescriptions/:id           → prescription detail (role-aware)
/departments                 → HOD department view
/admin                       → admin panel
```

This eliminates the `/doctor/*` vs `/nurse/*` split and the `/select` intermediary step.

---

## Part 3: Component Architecture Redesign (P2)

### 3.1 IntensiveCareCard Decomposition (813 lines → ~5 components)

The monolithic IntensiveCareCard handles: 24-column hourly grid, vital signs editing, therapy rows, fluid balance sidebar, medical notes with auto-save, ventilation settings, lab results, patient state assessment, scale results, and clinical scale display.

**Proposed decomposition:**

```
IntensiveCareCard (orchestrator, ~150 lines)
├── HourlyGrid (grid + row headers, ~200 lines)
│   ├── HourlyCell (individual cell with edit/view modes)
│   └── RowHeader (vital sign label + units)
├── TherapySection (medication rows, ~100 lines)
├── Sidebar (tabbed panel, ~100 lines)
│   ├── PatientInfoTab
│   ├── FluidBalanceTab (wraps FluidBalancePanel)
│   ├── NotesTab (wraps MedicalNotesPanel)
│   ├── VentilationTab (wraps VentilationPanel)
│   ├── LabsTab (wraps LabResultsPanel)
│   └── ScalesTab (wraps ScaleResultsPanel)
└── AutoSaveIndicator (save status, ~30 lines)
```

### 3.2 PrescriptionGrid Decomposition (587 lines → ~4 components)

**Proposed decomposition:**

```
PrescriptionGrid (orchestrator, ~150 lines)
├── PrescriptionSpreadsheet (21-day grid, ~200 lines)
│   ├── DoseCell (individual dose with plan/cancel/execute)
│   └── DayHeader (date + day-of-week)
├── MedicineSearchDialog (search + allergy check, ~80 lines)
├── ExecuteDoseDialog (2FA confirmation, ~60 lines)
└── DeleteConfirmDialog (delete with warning, ~40 lines)
```

### 3.3 Loading State Improvements

**Current:** Raw spinner (`Loader2` rotating) during all data fetches.

**Proposed:** Skeleton loading patterns for each page type:
- Patient list: skeleton card grid
- Patient day: skeleton grid + sidebar panels
- Prescription list: skeleton table rows
- Prescription detail: skeleton spreadsheet

### 3.4 Error Handling Improvements

**Current issues:**
- AdminPage silently swallows all errors (`catch { /* */ }`)
- PatientDayPage shows raw error messages
- No retry mechanisms except manual page refresh

**Proposed:**
- Global error boundary with retry button
- Toast notifications for non-critical errors (admin operations)
- Inline error states with retry for critical data loads
- Optimistic locking conflict resolution with auto-retry

---

## Part 4: Accessibility Improvements (P3)

### 4.1 Critical Fixes

| Issue | Fix |
|---|---|
| No skip-to-content link | Add `<a href="#main-content" className="sr-only focus:not-sr-only">Skip to content</a>` in GlobalLayout |
| No `<nav>` landmark | Wrap header navigation buttons in `<nav aria-label="Primary">` |
| Missing table captions | Add `<caption>` or `aria-label` to EpisodeTable, HourlyRecordTable, PrescriptionGrid |
| No aria-live for feedback | Add `aria-live="polite"` to PatientDayPage feedback banner |
| Clickable grid cells not keyboard-accessible | Add `role="button"`, `tabIndex={0}`, `onKeyDown` for Enter/Space to PrescriptionGrid cells |

### 4.2 Focus Management

- **Dialog focus trapping** — already handled by Base UI Dialog ✅
- **Dropdown keyboard navigation** — already handled by Base UI Menu ✅
- **Timeline keyboard navigation** — already has `tabIndex` + `role="button"` ✅
- **Grid cell keyboard navigation** — needs implementation (arrow key navigation in PrescriptionGrid and IntensiveCareCard)

### 4.3 Color Contrast

- **WCAG AA requires 4.5:1 for normal text, 3:1 for large text**
- Current `--color-muted-foreground: #5A5A5A` on `--color-background: #FAFAF8` = ratio ~4.8:1 ✅
- `--color-muted-foreground: #A0A0A0` (dark) on `--color-background: #0D0D0D` (dark) = ratio ~6.2:1 ✅
- Status colors (`#4CAF50`, `#FF5252`) used as text on white need verification

### 4.4 Screen Reader Support

- Add `aria-label` to all icon-only buttons (ThemeToggle ✅, user menu ✅)
- Add `aria-describedby` for validation messages on form fields
- Add `aria-busy` to loading states
- Prescription grid needs `role="grid"`, `role="row"`, `role="gridcell"` ARIA pattern

---

## Part 5: UX Polish & Modernization (P4)

### 5.1 Feedback & Micro-interactions

| Current | Proposed |
|---|---|
| No save confirmation | Toast "Збережено" on auto-save completion |
| Sign-off shows raw success banner | Animated checkmark + "День підписано" toast |
| No undo for delete/cancel | 5-second undo snackbar for destructive actions |
| Middle-click to cancel dose | Explicit cancel button with confirmation |
| PrescriptionGrid cell click opens popover | Hover preview + click to edit (more intuitive) |

### 5.2 Data Density Optimization

- **IntensiveCareCard** — 24-column grid is dense; add collapsible hour groups (morning/afternoon/evening/night) to reduce visual overload
- **DepartmentDashboardPage** — 8 stat cards could be condensed into a 2×4 grid with sparklines
- **Patient lists** — Add card view toggle (table vs. card grid) for different screen sizes

### 5.3 Responsive Design

- **Current breakpoint:** `isMobile` at 1200px (sidebar stacks)
- **Proposed:** Three-tier responsive strategy:
  - `≥1200px`: Full sidebar + content
  - `768-1199px`: Collapsible sidebar + content
  - `<768px`: Bottom navigation bar + stacked content

### 5.4 Dark Mode Consistency

- **Current:** Mix of Tailwind `dark:` variants and JavaScript `isDark ?` ternaries
- **Proposed:** Migrate all components to Tailwind `dark:` variants exclusively; remove all `isDark` ternaries and hardcoded hex values. Use `bg-card`, `bg-background`, `border-border`, `text-foreground`, `text-muted-foreground` tokens consistently.

---

## Implementation Sequence

| Phase | Scope | Dependencies |
|---|---|---|
| **Phase 1** | Add missing design tokens (P0) | None — standalone fix |
| **Phase 2** | Eliminate hardcoded colors, fix font system, scope transitions (P0) | Phase 1 tokens |
| **Phase 3** | Decompose IntensiveCareCard + PrescriptionGrid (P2) | Phase 2 tokens for consistent styling |
| **Phase 4** | Navigation redesign — breadcrumbs + sidebar (P1) | Phase 3 component structure |
| **Phase 5** | Accessibility fixes — skip-nav, ARIA, keyboard (P3) | Phase 4 navigation landmarks |
| **Phase 6** | Dark mode unification (P4) | Phase 2 tokens, Phase 3 components |
| **Phase 7** | UX polish — toasts, undo, skeletons, responsive (P4) | All previous phases |

Each phase is independently shippable and testable. No phase blocks another.

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Adding missing tokens changes existing component appearance | Medium — dropdowns/popovers gain backgrounds they never had | Visual regression testing per phase |
| Decomposing IntensiveCareCard breaks auto-save logic | High — core clinical workflow | Extract components first, wire auto-save last; test with existing 307 tests |
| Navigation redesign changes all route paths | Medium — breaks bookmarks, E2E tests | Keep old routes as redirects during transition |
| Dark mode migration misses edge cases | Low — already partially working | Systematic grep for `isDark` after each phase |

---

## Open Questions for User

1. **Navigation model**: Do you want a persistent sidebar, or is the current header-only nav acceptable with just breadcrumbs added?
2. **Route restructuring**: Should old `/doctor/*` and `/nurse/*` routes be kept as redirects, or is a clean break acceptable?
3. **IntensiveCareCard decomposition**: Should this be done incrementally (extract one sub-component at a time) or as a full rewrite?
4. **Skeleton loading**: Is this a priority now, or can it wait until the design system is stable?
5. **Accessibility compliance target**: WCAG 2.1 AA, or are you targeting AAA for specific areas?
