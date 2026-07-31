# Plan: Update AGENTS.md and README.md to Reflect Current Codebase State

## Goal
Refresh both docs to match the project as of 2026-07-30 after the clinical scales feature (Issues #1–#6 from the current session). Every number, config, endpoint listing, and structural description must reflect the actual codebase.

---

## Priority Order (most impactful first)

### 1. Configuration — `application.yml` migration to Liquibase

**Both files**: Replace all references to `ddl-auto: update` with the current config:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: none            # schema managed by Liquibase
  liquibase:
    enabled: true
    change-log: classpath:/db/changelog/db.changelog-master.yaml
```

**Files to touch:**
- `AGENTS.md` line 117: s/`ddl-auto: update`/`ddl-auto: none`/ and add sentence about Liquibase
- `README.md` lines 176-178: replace YAML excerpt with current config
- `AGENTS.md` line 494 (Conventions — DB): add Liquibase convention alongside `ddl-auto: none`

### 2. API Endpoints — Clinical Scales (README.md only)

**`README.md` lines 289-295**: The Clinical Scales table is missing 3 episode-level endpoints. Add rows:

| Method | URL | Auth | Description |
|---|---|---|---|
| GET | `/api/episodes/{episodeId}/scales` | Yes | Get episode-level scale results |
| POST | `/api/episodes/{episodeId}/scales` | Yes | Create episode-level scale result |
| POST | `/api/episodes/{episodeId}/scales/calculate` | Yes | Calculate and save scale from raw data |

(AGENTS.md already has these correctly at lines 314-323.)

### 3. Test Counts — All Sections

Audited counts from source (no test execution):

| Metric | Old (AGENTS) | Old (README) | Actual |
|---|---|---|---|
| Backend `@Test` annotations (icu-chart) | 419 (unit+integ) | 422 unit / 101 integ | 557 total (416 unit + 141 integ) |
| Backend test files (icu-chart) | 54 | — | 62 |
| Frontend Vitest `it(` calls | ~390 | 300 | ~350 |
| Frontend test files | 22 | 38 | 44 |
| E2E spec files | 40 | 38 | 46 |
| E2E `test(` calls | — | — | 190 |

**Files to touch:**
- `AGENTS.md` lines 128-132 (table), 167-168 (paragraph), 178/181 (project file counts)
- `README.md` lines 455-461 (Main Test Scenario table), 465-471 (Testing Summary)

### 4. Backend Services Table

**`AGENTS.md` lines 433-449**: The service table lists 13 services. `ScaleAuthorizationService` is already implemented and referenced in the architecture but missing from the table. Update count to **14** and add row:

| `ScaleAuthorizationService` | Per-scale role-based access control (APACHE II/SOFA → DOCTOR, others → NURSE) |

**`README.md`**: Does not have a services table — no change needed.

### 5. Frontend Components Count

**`AGENTS.md` lines 375-414**: The "Pages (7)" section is still correct. The "Common Components (24)" header should update to reflect that 24 is still the right count (14 base + 5 scale forms + SignDialog + VentilationPanel + VitalSignsForm + ThemeToggle + DepartmentPatientCard = 24). Wait — verify actual count.

Actually, let me verify the actual component count from the source tree. The AGENTS.md lists these 24 entries (lines 391-414), and the tree has more files (ThemeToggle.tsx, DepartmentPatientCard.tsx). Need to resolve.

Scan `frontend/src/components/common/` for actual component files (exclude `scales/` subdir for now):
- AuditLogTable.tsx
- ClinicalDayTimeline.tsx
- DepartmentPatientCard.tsx  ← MISSING from list
- EpisodeTable.tsx
- FluidBalancePanel.tsx
- HourlyRecordTable.tsx
- HourSelector.tsx
- LabResultsPanel.tsx
- MedicalNotesPanel.tsx
- MedicalOrdersPanel.tsx
- PatientSearch.tsx
- PatientStatePanel.tsx
- ScaleResultsPanel.tsx
- SignDialog.tsx
- ThemeToggle.tsx  ← MISSING from list
- VentilationPanel.tsx
- VitalSignsForm.tsx

That's 17 common component files (not counting `scales/`). Plus 5 scale forms = 22. Plus the dashboard components in `monitoring/`: DoctorDashboard, IntensiveCareCard, NurseDashboard, PatientSidebar (4 more). That's ~26 total.

**Fix**: Update the header to reflect the actual count (currently labeled "Common Components (24)" — verify and fix to correct number, e.g., "Common Components (26)").

### 6. README Role Permissions Table

**`README.md` lines 494-508**: Add a row for clinical scale operations:

| Create clinical scale (APACHE II/SOFA) | ✓ | ✗ | ✓ | ✗ |
| Create clinical scale (CAM-ICU/Braden/RASS) | ✓ | ✓ | ✓ | ✗ |

### 7. README Project Structure

**`README.md` lines 375-416**: Update stale numbers:
- controllers: 18 → 19
- services: "17 implementation + 2 interfaces" → verify and update
- specs/ line 414: "28 files, 79 tests" → "46 files, 190 tests"
- Add `db/` changelog dir reference under backend resources

### 8. AGENTS.md Conventions — DB

**`AGENTS.md` line 494**: Append: "Schema migrations via Liquibase changelogs in `db/changelog/changesets/`"

### 9. Minor Fixes

- **`AGENTS.md` line 179**: spec files count (40 → 46)
- **`AGENTS.md` line 521**: test file count (54 → 62)
- **`AGENTS.md` line 527**: frontend source files (59 → ~88), test files (22 → 44)
- Add a **Liquibase** entry to the compliance fixes table (new §) or add a short note
- Consider marking the 4 known issues in README as potentially outdated

---

## Validation

After both files are updated, verify:
1. No remaining references to `ddl-auto: update` in either file
2. The 3 episode-level scale endpoints appear in **both** API sections (they're already in AGENTS, need adding to README)
3. All test counts are consistent between both files
4. Service count table matches codebase (14 listed rows)
5. Role Permissions table includes clinical scales
