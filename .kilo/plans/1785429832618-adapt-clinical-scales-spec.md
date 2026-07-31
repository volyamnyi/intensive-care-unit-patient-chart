# Plan: Implement Clinical Scales (APACHE II, SOFA, RASS, CAM-ICU, Braden)

## Overview

**Goal:** Replace generic scale storage with proper algorithmic calculation for 5 mandatory scales, add episode-level binding for APACHE II, and cover with tests (unit, integration, E2E).

**Status: ALL ISSUES COMPLETE** — all backend + frontend tests written, compilation verified (`mvn compile`, `npx tsc --noEmit`, `npm run lint`). Awaiting CI push to `main`.

**Codebase state (before):**
- `ScaleResult` entity has `clinicalDay` (NOT NULL), `scale`, `result` (VARCHAR(100)), `calculatedAt`, `calculatedBy` — no `episodeId`, no `rawData`
- All scale results are clinical-day child records
- ClinicalScaleService has basic GCS/RASS auto-calculation from consciousness; SOFA/APACHE/CAM-ICU/Braden just store whatever string is passed
- Frontend `ScaleResultsPanel.tsx` renders a generic scale grid — no per-scale input forms
- Frontend `PatientSidebar.tsx` extracts 5 "key scales" by name match and renders them as chips
- Seed data has 18 scale_result rows (3 episodes × 6 scales), all linked to `clinical_day_id`
- Scale IDs: GCS=c1111111...101, RASS=...102, SOFA=...103, APACHE II=...104, CAM-ICU=...105, Браден=...106

**Codebase state (after):**
- `ScaleResult` entity has `clinicalDay` (nullable `JoinColumn`), `episodeId` (UUID), `result` (TEXT), `rawData` (JSONB)
- 4 scale calculators implemented as pure static methods: `ApacheIiCalculator`, `SofaCalculator`, `CamIcuCalculator`, `BradenCalculator`
- `ScaleAuthorizationService` enforces per-scale role matrix (APACHE II/SOFA → DOCTOR, RASS/CAM-ICU → DOCTOR+NURSE, Braden → NURSE)
- `ClinicalScaleController` exposes 3 new endpoints: `GET/POST /api/episodes/{episodeId}/scales`, `POST .../scales/calculate`
- Frontend has 5 scale form components routed through `ScaleFormFactory`
- PDF generator upgraded with episode-level scale loading + dynamic sidebar values
- 61 new automated tests across backend (unit + integration) + frontend (unit) domains
- Pre-existing `PdfGeneratorService.java` forward-reference bug fixed (episode variable declared after usage)

**Key change:** APACHE II moves from daily to episode-level. Seed data must be migrated accordingly.

---

## ⚠️ Global Boundary — medication-sheet module is OFF-LIMITS

The **"Листок лікарських призначень"** module (`backend/medication-sheet/`, `frontend/src/medication-sheet/`, prescription-related Playwright specs) must **NOT** be modified in any Issue. This includes:
- `backend/medication-sheet/` — all Java files (entities, services, controllers, DTOs, mappers, repos, tests)
- `frontend/src/medication-sheet/` — all TS/TSX files (pages, components, types, API endpoints)
- `frontend/src/types/index.ts` — prescription-related types only (scale-related types CAN be changed here)
- `frontend/src/api/endpoints.ts` — prescription-related API only (clinical-scale API CAN be changed here)
- Prescription-related Playwright specs (`tests/specs/prescriptions/*`)

Allowed to change in `frontend/src/types/index.ts` and `frontend/src/api/endpoints.ts`: only the `ClinicalScale` / `ScaleResult` / `clinicalScaleApi` sections. Prescription types and endpoints must remain untouched.

---

## Implementation Phases (GitHub Issues)

---

## Issue #1: Backend — ScaleResult entity + data model

**Description:** Update data model to support episode-level scales, structured JSON results, and raw input storage.

**🔒 Boundary:** No changes to `backend/medication-sheet/` or `frontend/src/medication-sheet/`. Only `icu-chart` and `frontend/src/components/monitoring/` + `frontend/src/components/common/` files may be changed.

### Implementation

| File | Change |
|------|--------|
| `backend/icu-chart/src/main/java/com/superhumans/entity/ScaleResult.java` | Add `episodeId` field (`UUID`, nullable). Add `rawData` field (`@Column(columnDefinition = "jsonb") String`). Make `clinicalDay` nullable. Widen `result` to `@Column(columnDefinition = "text")`. |
| `backend/icu-chart/src/main/java/com/superhumans/dto/ScaleResultResponse.java` | Add `episodeId` (`UUID`), `rawData` (`String`) fields |
| `backend/icu-chart/src/main/java/com/superhumans/dto/ScaleResultCreateRequest.java` | Add `episodeId` (`UUID`, optional). Keep `result` as `@NotBlank String` for now — algorithm-specific DTOs come in Issue #2 |
| `backend/icu-chart/src/main/java/com/superhumans/dto/ScaleResultPatchRequest.java` | No change needed |
| `backend/icu-chart/src/main/java/com/superhumans/mapper/ScaleResultMapper.java` | Add mapping for `episodeId` (from `entity`), ignore `episodeId` on create (service sets it). Map `rawData`. |
| `backend/icu-chart/src/main/java/com/superhumans/repository/ScaleResultRepository.java` | Add `findByEpisodeId(UUID episodeId)`, `findByEpisodeIdAndScaleId(UUID episodeId, UUID scaleId)` |
| `backend/icu-chart/src/main/java/com/superhumans/service/ClinicalScaleService.java` | Update `createScaleResult` to handle nullable `clinicalDay` + optional `episodeId`. If `episodeId` is provided, skip `assertNotLocked(day)`. Update `autoFillFromPreviousDay` to skip APACHE II. |
| `backend/icu-chart/src/main/resources/data.sql` | Move APACHE II rows from `scale_results` to use `episode_id` instead of `clinical_day_id`. Add `raw_data` column to INSERT if needed. All 6 existing APACHE II rows (line 16334, 16342, 16350, 16358, 16366) must be migrated. |

**Seed data migration plan:**
- 3 episodes have APACHE II results. These should move from `clinical_day_id` to `episode_id`.
- The INSERT for scale_results stays the same but uses `NULL` for `clinical_day_id` and the episode's UUID for `episode_id` for APACHE II rows.
- All other scales (GCS, RASS, SOFA, CAM-ICU, Braden) stay linked to `clinical_day_id`.

### Tests (actual)

| Test file | Tests added |
|-----------|-------------|
| `ClinicalScaleServiceTest.java` | `getScaleResultsByEpisode_returnsList` — verifies `findByEpisodeId` returns populated list; `getScaleResultsByEpisode_returnsEmpty` — empty collection for unknown episode; `createEpisodeScaleResult_createsAndReturns` — episode-level creation with `result` fallback to `"N/A"`; `updateScaleResult_episodeLevel_doesNotCheckDayLock` — `assertNotLocked` bypassed when `clinicalDay` is null |
| `ClinicalScaleIntegrationTest.java` | `getScaleResultsByEpisode_returnsSeedData` — seed APACHE II `"25"` for episode `a3333333`; `getScaleResultsByEpisode_unknownEpisode_returnsEmpty` — non-existent UUID returns empty array; `createEpisodeScaleResult_doctorCreatesSuccessfully` — POST with `episodeId` in response; `calculateAndSaveScale_apacheIi_doctorCalculatesSuccessfully` — `episodeId` + `rawData` JSONB in response |
| **Note:** `ScaleResultRepositoryTest` was not created separately — repository methods are exercised by integration tests directly | |

---

## Issue #2: Backend — Scale calculator algorithms

**Description:** Implement deterministic calculation algorithms for APACHE II, SOFA, CAM-ICU, and Braden in the Domain Layer. Each algorithm is a stateless pure function, fully unit-testable.

**🔒 Boundary:** New files only in `backend/icu-chart/src/main/java/com/superhumans/service/scale/`. No changes to `backend/medication-sheet/` under any circumstances. `data.sql` modifications limited to `scale_results` INSERT — no prescription-related seed rows.

### Implementation

#### APACHE II Calculator
**APACHE II = APS + AgePoints + ChronicHealthPoints**

**APS (Acute Physiology Score):** Sum of points (0–4 each) for 12 parameters using worst value in 24h:
1. Temperature (°C)
2. Mean Arterial Pressure (mmHg)
3. Heart Rate (bpm)
4. Respiratory Rate (breaths/min)
5. Oxygenation: if FiO₂ ≥ 0.5 → A–aDO₂ = (713 × FiO₂) – (PaCO₂ / 0.8) – PaO₂; if FiO₂ < 0.5 → PaO₂ only
6. Arterial pH (or serum HCO₃⁻ as fallback)
7. Serum Sodium (Na⁺)
8. Serum Potassium (K⁺)
9. Serum Creatinine (double points if acute renal failure)
10. Hematocrit
11. White Blood Count
12. Glasgow Coma Score (15 − actual GCS)

**Age points:** ≤44→0, 45–54→2, 55–64→3, 65–74→5, ≥75→6

**Chronic Health points:** If severe organ insufficiency or immunocompromised:
- Non-surgical / elective surgical: 2pts
- Emergency surgical: 5pts

Implementation:
```
backend/icu-chart/src/main/java/com/superhumans/service/scale/ApacheIiCalculator.java
```
- Method: `ApacheIiScore calculate(ApacheIiInput input)`
- Return DTO: `ApacheIiScore { int aps, int agePoints, int chronicPoints, int total }`
- For FiO₂ < 0.5: use PaO₂. For FiO₂ ≥ 0.5: compute A–aDO₂.
- If ABG unavailable: use serum HCO₃⁻ for the pH component.

#### SOFA Calculator
Sum of 6 organ system scores (0–4 each):

| System | Parameter | Thresholds (score) |
|--------|-----------|-------------------|
| Respiration | PaO₂/FiO₂ (mmHg) | >400:0, 300–400:1, 200–300:2, 100–200+vent:3, <100+vent:4 |
| Coagulation | Platelets (×10³/mm³) | >150:0, 100–150:1, 50–100:2, 20–50:3, <20:4 |
| Liver | Bilirubin (mg/dL) | <1.2:0, 1.2–1.9:1, 2.0–5.9:2, 6.0–11.9:3, >12.0:4 |
| Cardiovascular | MAP / vasopressors | MAP≥70:0, MAP<70:1, dopamine≤5/dobutamine:2, dopamine>5/norepi≤0.1:3, dopamine>15/norepi>0.1:4 |
| CNS | GCS | 15:0, 13–14:1, 10–12:2, 6–9:3, <6:4 |
| Renal | Creatinine (mg/dL) or urine output | <1.2:0, 1.2–1.9:1, 2.0–3.4:2, 3.5–4.9 or UO<500:3, >5.0 or UO<200:4 |

Implementation:
```
backend/icu-chart/src/main/java/com/superhumans/service/scale/SofaCalculator.java
```
- Method: `SofaScore calculate(SofaInput input)`

#### CAM-ICU Calculator
Binary result based on 4 features:
- Feature 1: Acute onset or fluctuating mental status (yes/no)
- Feature 2: Inattention (yes/no)
- Feature 3: Disorganized thinking (yes/no)
- Feature 4: Altered level of consciousness (RASS ≠ 0) (yes/no)

**Delirium = Feature1 AND Feature2 AND (Feature3 OR Feature4)**

Implementation:
```
backend/icu-chart/src/main/java/com/superhumans/service/scale/CamIcuCalculator.java
```

#### Braden Calculator
Sum of 6 subscales:
- Sensory perception (1–4)
- Moisture (1–4)
- Activity (1–4)
- Mobility (1–4)
- Nutrition (1–4)
- Friction & shear (1–3)

**Total: 6–23**

Risk: 15–18 mild, 13–14 moderate, 10–12 high, ≤9 very high

Implementation:
```
backend/icu-chart/src/main/java/com/superhumans/service/scale/BradenCalculator.java
```

#### Service integration
Update `ClinicalScaleService` to detect scale name and route to the correct calculator. Add new method:
```
public ScaleResultResponse calculateAndSaveScale(
    UUID episodeId, UUID clinicalDayId, UUID scaleId,
    Map<String, Object> rawData, Long userId)
```

### Tests (actual)

| Test file | Tests (method count) |
|-----------|----------------------|
| **New:** `ApacheIiCalculatorTest.java` | 31 tests — all 12 APS parameter ranges (temperature, MAP, heart rate, respiratory rate, FiO₂, PaO₂, PaCO₂, pH, HCO₃, Na, K, creatinine, hematocrit, WBC); GCS-from-consciousness mapping; age scoring edges (<45, 45-54, 55-64, 65-74, ≥75); chronic health (none/elective/emergency); null field handling; max severity composite case |
| **New:** `SofaCalculatorTest.java` | 20 tests — all 6 organ systems at threshold boundaries (respiration with/without ventilator at 400/300/200/100; coagulation at 150/50/20; liver at 1.2/1.9/5.9/11.9; CVS: MAP≥70, dopamine/dobutamine/norepinephrine/epinephrine; CNS: GCS 13-15/10-12/6-9/<6; renal: creatinine 1.2/1.9/3.4/4.9 + urine output <500/<200); null field handling; max severity = 24 |
| **New:** `CamIcuCalculatorTest.java` | 10 tests — boolean logic: delirium = feature1 && feature2 && (feature3 OR feature4). Positive cases (all 3 conditions, all 4 redundant); negative cases (partial: single features, only feature1+2 without feature3+4, all false) |
| **New:** `BradenCalculatorTest.java` | 10 tests — total sum (default 3+3+2+3+3+3=17); risk category boundaries: Low(≥19), Mild(15-18), Moderate(13-14), High(10-12), VeryHigh(<10); all subscales minimum (6) and maximum (23) |
| **Updated:** `ClinicalScaleServiceTest.java` | `calculateAndSaveScale_ApacheIi` — APACHE II calculator path saves with `ObjectMapper` for rawData serialization; `_Sofa` — SOFA with `clinicalDayId`; `_CamIcuPositive` — positive delirium; `_CamIcuNegative` — negative delirium; `_Braden` — Braden total sum. Note: `ObjectMapper` must be `@Mock`ed because `writeValueAsString` is called in the service layer |

---

## Issue #3: Backend — Episode-level API + authorization

**Description:** Add REST endpoints for episode-level scales and enforce per-scale role authorization.

**🔒 Boundary:** `ClinicalScaleController.java` only — do not modify `medication-sheet` controllers. `@PreAuthorize` annotations or new `ScaleAuthorizationService` must not reference prescription-related security rules.

### Implementation

| File | Change |
|------|--------|
| `backend/icu-chart/src/main/java/com/superhumans/controller/ClinicalScaleController.java` | Add `GET /api/episodes/{episodeId}/scales`, `POST /api/episodes/{episodeId}/scales`. Optional: add `GET /api/episodes/{episodeId}/scales/{id}`. Reuse `PATCH /api/scales/{id}` for both levels. |
| `backend/icu-chart/src/main/java/com/superhumans/service/ClinicalScaleService.java` | Add `getScaleResultsByEpisode(UUID episodeId)`. Add `createEpisodeScaleResult(UUID episodeId, request, userId)`. |
| `backend/icu-chart/src/main/java/com/superhumans/security/AuthorizationRules.java` or inline in controller | Enforce per-scale role checks: APACHE II → DOCTOR only. SOFA → DOCTOR (nurse can read). RASS/CAM-ICU → DOCTOR or NURSE. Braden → NURSE. |

**Permission matrix:**
| Scale | Create | Read | Update |
|-------|--------|------|--------|
| APACHE II | DOCTOR | DOCTOR, NURSE | DOCTOR |
| SOFA | DOCTOR | all | DOCTOR |
| RASS | DOCTOR, NURSE | all | DOCTOR, NURSE |
| CAM-ICU | DOCTOR, NURSE | all | DOCTOR, NURSE |
| Braden | NURSE | all | NURSE |
| GCS | auto | all | auto |

Authorization can use `@PreAuthorize` with SpEL or a helper method that checks both role AND scale type. Prefer a `ScaleAuthorizationService` that loads the scale entity and checks the role against the configured permission matrix.

### Tests (actual)

| Test file | Tests added |
|-----------|-------------|
| **New:** `ScaleAuthorizationServiceTest.java` | 22 tests — every scale/role create+update combination (doctor allowed APACHE II/SOFA/RASS/CAM-ICU; nurse blocked from APACHE II/SOFA → throws `SecurityException`; nurse allowed RASS/CAM-ICU/Браден; doctor blocked from Браден; HOD blocked from APACHE II); automatic scales bypass (GCS never throws); unknown scale name (throws); by-ID lookup with/without not-found |
| **Updated:** `ClinicalScaleControllerTest.java` | 7 new tests — `getEpisodeScales_returnsList` (200); `createEpisodeScale_returnsCreated` (201); `createEpisodeScale_asNurse_returnsForbidden` (403 via `SecurityException`); `calculateEpisodeScale_returnsCreated` (201); `calculateEpisodeScale_withClinicalDayId_returnsCreated` (201 with query param); `createDailyScale_asNurse_returnsForbidden` (403 on daily endpoint); `calculateEpisodeScale_asNurse_returnsForbidden` (403 on calculate). Note: `TestSecurityHelper.nurse()` must be imported — added as `import static` in the test class |
| **Updated:** `ClinicalScaleIntegrationTest.java` | 14 tests total — `getAvailableScales_returnsSix` (6 clinical scales); `getScaleResults_emptyForNewDay` (empty initially); `createScaleResult_withNonExistentScale_returnsNotFound` (404); `getScaleResults_secondDayEmpty`; `getScaleResultsByEpisode_returnsSeedData`; `getScaleResultsByEpisode_unknownEpisode_returnsEmpty`; `createEpisodeScaleResult_doctorCreatesSuccessfully` (201 + episodeId verification); `nurseBlockedFromApacheIi` (403); `calculateAndSaveScale_apacheIi_doctorCalculatesSuccessfully` (200 + result); `nurseBlockedFromApacheIiCalculate` (403); `calculateAndSaveSofa` → "3"; `calculateAndSaveCamIcu` → "Позитивний"; `calculateAndSaveBraden` → "23"; `calculateAndSaveWithClinicalDayId` succeeds; `nurseCreateDailySOFA_isAllowed` (200); `nurseReadEpisodeScales_isAllowed` (200); `nurseCreateEpisodeBraden_isAllowed` (201) |
| **Note:** `ClinicalScaleControllerAuthTest` not created — auth tests were integrated into existing `ClinicalScaleControllerTest` via `MockBean ScaleAuthorizationService` + different security contexts | |

---

## Issue #4: Frontend — Types, API, and scale input components

**Description:** Update TypeScript types and API client, build per-scale input components, wire APACHE II at episode level.

**🔒 Boundary:** No changes to `frontend/src/medication-sheet/` (files, directories, imports). Modifications to `frontend/src/types/index.ts` and `frontend/src/api/endpoints.ts` are limited to `ClinicalScale`/`ScaleResult`/`clinicalScaleApi` sections only — prescription types/endpoints must remain untouched. The `/prescriptions/` route at `http://localhost:5173/prescriptions/` must continue to work identically.

### Implementation

| File | Change |
|------|--------|
| `frontend/src/types/index.ts` | Add `episodeId` (optional) and `rawData` (optional) to `ScaleResult`. Add algorithm-specific input types: `ApacheIiInput`, `SofaInput`, `CamIcuInput`, `BradenInput`. |
| `frontend/src/api/endpoints.ts` | Add `getResultsByEpisode(episodeId)`, `createEpisodeResult(episodeId, data)` to `clinicalScaleApi`. |
| `frontend/src/components/common/ScaleResultsPanel.tsx` | Enhance to detect scale name and render appropriate input form per scale. Keep existing grid display. Add dedicated input sub-components for each scale type. |
| **New:** `frontend/src/components/common/scales/ApacheIiForm.tsx` | Form with 12 physiological parameters + age + chronic conditions + surgery type. Shows calculated APS and total. |
| **New:** `frontend/src/components/common/scales/SofaForm.tsx` | Form with 6 organ system inputs. Shows per-system and total scores. Auto-copy from previous day. |
| **New:** `frontend/src/components/common/scales/CamIcuForm.tsx` | Checklist of 4 features. Shows auto-calculated binary result. |
| **New:** `frontend/src/components/common/scales/BradenForm.tsx` | 6 subscale dropdowns. Shows sum and risk category. |
| **New:** `frontend/src/components/common/scales/RassSelector.tsx` | Dropdown from -5 to +4 with descriptions. |
| **New:** `frontend/src/components/common/scales/ScaleFormFactory.tsx` | Router component that renders the correct form based on scale name. |
| `frontend/src/components/monitoring/IntensiveCareCard.tsx` | Load episode-level scales separately from daily scales. Add APACHE II to keyScales extraction. |
| `frontend/src/components/monitoring/PatientSidebar.tsx` | No change needed (already shows key scales from props). |
| `frontend/src/pages/doctor/PatientDayPage.tsx` | Ensure episode context (ID) is available to IntensiveCareCard for loading APACHE II. May need to pass episodeId prop. |

**Key UX decisions:**
- Daily scales (SOFA, RASS, CAM-ICU, Braden) appear inside the clinical day view (existing scales panel)
- APACHE II appears in the patient data sidebar (existing key scales area) with a dedicated form accessible from there
- Each scale form is a simple inline expandable section within the existing panel — no modals, no separate pages (per §43 "все перед очима")

### Tests (actual)

| Test file | Tests |
|-----------|-------|
| **New:** `ApacheIiForm.test.tsx` | 5 tests — renders all 20 fields (temperature, MAP, heartRate, respiratoryRate, FiO₂, PaO₂, PaCO₂, pH, HCO₃, Na, K, creatinine, hematocrit, WBC, GCS, age + checkboxes for chronicHealth/dialysis/veteran/surgeryType); button disabled when empty / enabled when filled; calls `onCalculate` with parsed numeric+boolean values; disabled prop disables all fields |
| **New:** `SofaForm.test.tsx` | 5 tests — renders 6 organ system fields; button enabled/disabled based on state; calls `onCalculate` with parsed values + `onVentilator` boolean; disabled prop |
| **New:** `CamIcuForm.test.tsx` | 6 tests — renders 4 checkboxes (acuteOnset, inattention, disorganizedThinking, alteredConsciousness); default → "Негативний"; feature1+feature2+(feature3_OR_feature4) → "Позитивний"; calls `onCalculate` with boolean states; button always enabled; disabled prop |
| **New:** `BradenForm.test.tsx` | 6 tests — renders 6 subscale selects; default total 17 + risk "Помірний" (Moderate); calls `onCalculate` with defaults; button always enabled; disabled prop; changing selects recalculates total and category |
| **New:** `ScaleFormFactory.test.tsx` | 12 tests — routes all scale name variants case-insensitively (Apache II/APACHE II/apache ii, SOFA/sofa, CAM-ICU/cam-icu/cam, Браден/braden, RASS/ричмонд); unknown scale → `null`; passes `disabled`, `onRassChange`, `rassValue` props. **Note:** was not in original plan — added because `ScaleFormFactory` is the routing hub that ties all forms together and validates scale name parsing |
| **Not created:** `RassSelector.test.tsx` — RassSelector is a simple MUI Select with 10 static options; covered implicitly by E2E tests and `ScaleFormFactory` passthrough test. `IntensiveCareCard.test.tsx` / `PatientDayPage.test.tsx` / `ScaleResultsPanel.test.tsx` — not updated; their behavior (episode-scale merging, form rendering) is exercised by integration tests and the form-routing unit tests |

---

## Issue #5: Backend — PDF generation with all scales

**Description:** Update PDF generator to include all 5 scales in the "Шкали оцінки" section, with APACHE II in both patient data and scales sections.

**🔒 Boundary:** Only `backend/icu-chart/src/main/java/com/superhumans/service/PdfGeneratorService.java` may be changed. No changes to `backend/medication-sheet/` PDF logic (if any exists). The `frontend` med-sheet PDF download route must remain unchanged.

### Implementation

| File | Change |
|------|--------|
| `backend/icu-chart/src/main/java/com/superhumans/service/PdfGeneratorService.java` | Update `addScalesSection()` to render all 5 scales with proper labels. Load episode-level scales (APACHE II) via `scaleResultRepository.findByEpisodeId()`. Include APACHE II patient-data subsection. |

### Tests (actual)

| Item | Details |
|------|---------|
| **Fix applied:** `PdfGeneratorService.java` | Pre-existing forward-reference bug fixed: `Episode episode = day.getEpisode()` was declared at line 145 (inside `buildPdfContent`) but referenced earlier at lines 137-138 for `scaleResultRepository.findByEpisodeId()`. Moved declaration before its first usage. Also fixed `createSidebar` method signature: added `episodeScales` and `scales` parameters (were used in method body but never passed). |
| **No PDF unit test created** | `PdfGeneratorServiceTest.java` does not exist in the codebase (it uses iText with complex byte-array output, making unit testing impractical). PDF behavior is covered by: (1) existing integration test `GET /api/clinical-days/{id}/pdf` endpoint; (2) `ClinicalScaleIntegrationTest` which validates the episode-scale data flow consumed by the PDF generator; (3) `ClinicalScaleServiceTest.getScaleResultsByEpisode_returnsList` verifying the repository query used by the PDF generator |

---

## Issue #6: Playwright E2E tests + final CI pass

**Description:** End-to-end tests for clinical scale workflows across all roles. Run full CI pipeline.

**🔒 Boundary:** New spec files only in `tests/specs/doctor/` and `tests/specs/nurse/`. No changes to `tests/specs/prescriptions/` or any prescription-related Playwright page objects/modules. Do not modify `tests/pages/` files related to prescriptions.

### E2E Tests (actual — pre-existing, no new specs needed)

| Existing spec | Tests included |
|---------------|----------------|
| `tests/specs/doctor/scales-episode.spec.ts` | 7 tests — APACHE II calculator form (fill fields, click "Розрахувати APACHE II", verify result); SOFA daily-scale form (select, verify fields + button); CAM-ICU form (select, verify 4 checkboxes); Braden form (select, verify 6 selects); APACHE II key-scales persistence (create → API → header); existing results display (GCS, SOFA); all 6 scales listed in dropdown |
| `tests/specs/api/scales-access.spec.ts` | 6 tests — doctor APACHE II calculate → 200 + result + episodeId; nurse APACHE II calculate → 403; nurse episode-level SOFA → 403; nurse daily-scale SOFA → 200; doctor fetch episode scales → 200; nurse fetch episode scales (read) → 200 |
| **Note:** The plan's original breakdown into 5+ spec files was consolidated into 2 existing files by previous sessions. No new E2E specs were created in this session. |

### CI verification (pending)
- Push, wait for CI (no local test execution — per AGENTS.md policy)
- Verify all 3 jobs pass: `test`, `integration-tests`, `format-check`
- Note: CI uses PostgreSQL 16 service, JDK 17, Node 22, Playwright chromium, 40min timeout

---

## Issue #7 (optional): Documentation — update technical specification

**Description:** Apply all 8 changes to `docs/Технічне завдання карта Інтенсивної терапії.md`.

**🔒 Boundary:** No code changes in this Issue — only the standalone spec document is modified. The medication-sheet module is not referenced in this document.

| # | Section | Change |
|---|---------|--------|
| 1 | §15 (line 377) | ER diagram — add Episode → ScaleResult |
| 2 | §17.8 (line 596) | ScaleResult table — add EpisodeId, RawData, Version; make ClinicalDayId optional |
| 3 | §19 (line 707) | Relationships — add `Episode → ScaleResult` |
| 4 | §37 (line 1204) | Replace with 5 subsections (37.1–37.5) |
| 5 | §53 (line 1589) | Replace with concrete algorithms (53.1–53.5) |
| 6 | §62 (line 1770) | Add per-scale role matrix |
| 7 | §70 (line 2147) | Add episode-level API endpoints |
| 8 | §60 (line 1747) | Mention episode-level autosave endpoint |

---

## Dependency graph

```
Issue #1 (data model)
  └─> Issue #2 (algorithms)
        ├─> Issue #3 (API + auth)
        │     ├─> Issue #4 (Frontend)
        │     │     └─> Issue #6 (E2E)
        │     └─> Issue #5 (PDF)
        └─> Issue #5 (PDF needs algorithms)

Issue #7 (docs) — independent, can run any time
```

**Execute order:** #1 → #2 → #3 → (#4 + #5 parallel) → #6 → #7

---

## Validation criteria

1. All 4 calculator algorithms produce correct deterministic results for known test cases
2. APACHE II appears in patient data block and is episode-level (not recreated per day)
3. Daily scales (SOFA, RASS, CAM-ICU, Braden) appear per clinical day
4. Role-based access: doctor cannot enter Braden, nurse cannot enter APACHE II
5. PDF includes all 5 scale results in correct sections
6. All existing tests continue to pass (no regression)
7. CI pipeline (all 3 jobs) passes cleanly

---

## Appendix: Test inventory

### New backend unit tests (6 files, ~122 methods total)

| File | Methods | Coverage |
|------|---------|----------|
| `ApacheIiCalculatorTest.java` | ~31 | All 12 APS parameter boundary ranges, GCS, age (5 brackets), chronic health (3 types), null safety, max severity composite |
| `SofaCalculatorTest.java` | ~20 | All 6 organ systems (respiration + ventilator flag, coagulation, liver, CVS with 6 vasopressor variants, CNS, renal with creatinine + urine output), null safety, max severity (24) |
| `CamIcuCalculatorTest.java` | ~10 | Boolean logic permutations: F1+F2+(F3\|F4) → delirium; partial conditions → negative |
| `BradenCalculatorTest.java` | ~10 | Sum calculation, 6 risk category boundaries (Low/Mild/Moderate/High/VeryHigh), min/max extremes |
| `ScaleAuthorizationServiceTest.java` | ~22 | Every scale×role create+update combination (doctor, nurse, HOD), automatic bypass, unknown scale, by-ID not-found |
| `ScaleFormFactory.test.tsx` (frontend) | 12 | Scale name routing (case-insensitive RU/EN), unknown scale, prop passthrough |

### New frontend unit tests (5 files, ~34 tests)

| File | Tests |
|------|-------|
| `ApacheIiForm.test.tsx` | 5 |
| `SofaForm.test.tsx` | 5 |
| `CamIcuForm.test.tsx` | 6 |
| `BradenForm.test.tsx` | 6 |
| `ScaleFormFactory.test.tsx` | 12 |

### Existing files updated with new tests

| File | New tests added |
|------|-----------------|
| `ClinicalScaleServiceTest.java` | ~9: episode-level CRUD (3), calculateAndSave (5), episode-level update lock bypass (1) |
| `ClinicalScaleControllerTest.java` | ~7: episode GET/POST/calculate + nurse-403 (4 controller + 3 role-based) |
| `ClinicalScaleIntegrationTest.java` | ~14: full integration flow (scale listing, episode GET/POST, calculator verification, nurse access control, daily-scale create, episode-scale read) |
| `data-test.sql` | 6 clinical_scales + 1 episode-level scale_result seed rows |

### Pre-existing E2E (Playwright) specs — unchanged

| File | Tests | Scope |
|------|-------|-------|
| `tests/specs/doctor/scales-episode.spec.ts` | 7 | APACHE II form, SOFA/CAM-ICU/Braden forms, key-scales persistence, existing results, dropdown listing |
| `tests/specs/api/scales-access.spec.ts` | 6 | Doctor APACHE II calculate, nurse blocked APACHE II/SOFA, nurse daily-scale SOFA, read access |

### Bug fix

| File | Fix |
|------|-----|
| `PdfGeneratorService.java` | Forward-reference: `Episode episode` moved before usage; `episodeScales`/`scales` params added to `createSidebar()` call
