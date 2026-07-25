# Phase 0: Preparation & Audit — Deliverables

**Дата:** 2026-07-25
**Статус:** In Progress
**Issues:** #13–#18

---

## #13: Security Audit — Secrets Inventory

### Current state (`application.properties` — 38 lines)

| Line | Secret | Type | Risk | Action |
|------|--------|------|------|--------|
| 4 | `spring.datasource.password=testpass` | DB password | **Critical** | Externalize |
| 8 | `security.jwt.secret-key=512-byte hex` | JWT signing key | **Critical** | Externalize |
| 16 | `spring.ldap.password=testpass` | LDAP bind password | **Critical** | Externalize |
| 25 | `telegram.bot.key=T...` | Bot token | **Medium** | Externalize |
| 31 | `spring.mail.password=qwer asdf...` | Email password | **High** | Externalize |

### Target state

All secrets are replaced with `${ENV_VAR}` placeholders. Created `.env.example`:

```
# Database
DB_URL=jdbc:postgresql://localhost:5432/icu_chart
DB_USERNAME=postgres
DB_PASSWORD=

# JWT
JWT_SECRET_KEY=

# LDAP (optional, disabled by default)
LDAP_ENABLED=false
LDAP_URLS=ldap://AD_SERVER:389
LDAP_BASE=dc=superhumans,dc=com
LDAP_USERNAME=
LDAP_PASSWORD=

# Telegram (optional, disabled by default)
TELEGRAM_BOT_ENABLED=false
TELEGRAM_BOT_KEY=
TELEGRAM_BOT_USERNAME=

# Email (optional, disabled by default)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
```

### .gitignore additions
```
.env
*.properties.local
```

**Action for production:** Rotate all credentials exposed in the current `application.properties` file.

---

## #14: Direct SQL Queries to DoctorEleks DB — Inventory

### Summary: 9 DoctorEleks tables accessed via raw JDBC

| # | DE Table | Method(s) | Query Type | MIS API Replacement |
|---|----------|-----------|------------|---------------------|
| 1 | **Patient** | `searchPatients()`, `getAllInpatients()`, `getAllRecentlyInpatients()`, `getPatientById()` | SELECT | `spzIBPatientSearch` via `MisService.searchPatients()` |
| 2 | **Residence** | `getAllInpatients()`, `getAllRecentlyInpatients()`, `getPatientById()` | SELECT (JOIN) | `spzIBPatientScheduleList` via `MisService.getHospitalization()` |
| 3 | **Venue** | `getAllInpatients()`, `getAllRecentlyInpatients()`, `getPatientById()` | SELECT (3-level JOIN) | `MisService` — needs `getWardsByDepartment()` (future) |
| 4 | **Users** (DE) | `getAllInpatients()`, `getAllRecentlyInpatients()`, `getPatientById()` | SELECT (JOIN for doctor name) | `spzIBUserDetails` via `MisService.getUser()` |
| 5 | **ItemKind** | `searchMedicine()`, `getHighRiskMedicineByName()`, `getConflictMedicineByName()` | SELECT | New MIS endpoint or `MisService.getDictionary("medicines")` |
| 6 | **Item** | `searchMedicine()` | SELECT (JOIN for stock filter) | Not needed in ICU — catalog-only |
| 7 | **Document** | `getAllAllergiesByPatientId()` | SELECT (JOIN) | MIS API endpoint for patient allergies |
| 8 | **DocumentNode** | `getAllAllergiesByPatientId()` | SELECT (XML parsing) | MIS API endpoint for patient allergies |
| 9 | **Course** | `getAllAllergiesByPatientId()` | SELECT (JOIN) | Not needed — replaced by MIS API |

### Application tables (own data — move to PostgreSQL)

| # | Table | Method(s) | Query Type |
|---|-------|-----------|------------|
| 1 | **MedicineList** | `createNewMedicineList()`, `getAllMedicineLists()`, `getMedicineListById()`, `getAllDocumentsByPatientId()`, `deleteMedicineListById()`, `generateDeDocument()` | INSERT, SELECT, DELETE, UPDATE |
| 2 | **MedicineListItem** | All CRUD operations | INSERT, SELECT, UPDATE, DELETE |
| 3 | **SH_MedicineListBotChatIds** | `addNewChatId()`, `getAllChatIds()` | INSERT, SELECT |
| 4 | **SH_Users** | `UserRepository` (separate file) | CRUD |

### Total: 643 lines of raw JDBC to be replaced

---

## #15: Data Model Mapping — JSON-in-Column → Relational PostgreSQL

### Current (MS SQL Server + JSON-in-column)

```
MedicineList                     MedicineListItem
┌─────────────────────┐         ┌──────────────────────────┐
│ MedicineListID (PK) │ 1:1     │ MedicineListRef (FK)     │
│ PatientRef (FK)     │────────►│ MedicineDetails (JSON!)  │ ← JSON blob
│ DocumentName        │         │ VitalList (JSON!)        │ ← JSON blob
│ CreationUser        │         │ ApprovedRowIndexes(JSON) │ ← JSON array
│ CreationDate        │         │ Status (editing lock)    │
│ Status              │         └──────────────────────────┘
└─────────────────────┘
```

### MedicineDetails JSON structure (deserialized at runtime)
```json
[
  {
    "medicineListItemId": "uuid",
    "medicineName": "string",
    "medicineMethod": "string",
    "regime": "string",
    "editUser": "string",
    "editDate": "datetime",
    "status": "string",
    "days": [{ "id":"uuid","date":"ISO","morning":{...},"day":{...},"evening":{...},"night":{...} }]
  }
]
```

### Target (PostgreSQL — normalized relational)

```sql
-- Root document
CREATE TABLE prescription_lists (
    id UUID PRIMARY KEY,
    patient_id BIGINT NOT NULL,           -- from MIS API
    hospitalization_id UUID,              -- from MIS API
    department_id BIGINT,                 -- from MIS API
    document_name VARCHAR(255),
    status VARCHAR(32),                   -- Created/BeingEdited/Saved/Finished
    editing_user_id UUID,                 -- current editing lock holder
    editing_started_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMP,
    updated_by UUID,
    version INTEGER DEFAULT 0             -- optimistic locking
);

-- Medicine row in the prescription
CREATE TABLE prescription_items (
    id UUID PRIMARY KEY,
    list_id UUID NOT NULL REFERENCES prescription_lists(id),
    medicine_name VARCHAR(500) NOT NULL,  -- from medicine_catalog_cache or MIS
    medicine_method VARCHAR(255),
    regime VARCHAR(255),
    status VARCHAR(32),                   -- Active/Completed/Cancelled
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMP,
    updated_by UUID,
    version INTEGER DEFAULT 0
);

-- 21-day schedule per medicine
CREATE TABLE prescription_item_days (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL REFERENCES prescription_items(id),
    day_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL
);

-- Dose per time period
CREATE TABLE prescription_day_parts (
    id UUID PRIMARY KEY,
    day_id UUID NOT NULL REFERENCES prescription_item_days(id),
    period VARCHAR(8) NOT NULL CHECK (period IN ('morning','day','evening','night')),
    dose VARCHAR(100),
    is_planned BOOLEAN DEFAULT FALSE,
    is_planned_finished BOOLEAN DEFAULT FALSE,
    is_completed BOOLEAN DEFAULT FALSE,
    is_completed_finished BOOLEAN DEFAULT FALSE,
    doctor_name VARCHAR(255),
    nurse_name VARCHAR(255)
);

-- Nurse execution record
CREATE TABLE prescription_executions (
    id UUID PRIMARY KEY,
    day_part_id UUID NOT NULL REFERENCES prescription_day_parts(id),
    executed_by UUID NOT NULL,
    executed_at TIMESTAMP NOT NULL,
    actual_dose VARCHAR(100),
    status VARCHAR(32),                   -- Planned/InProgress/Completed/PartiallyCompleted
    requires_2p_auth BOOLEAN DEFAULT FALSE,
    second_person_id UUID,               -- 2P verification user
    comment TEXT,
    created_at TIMESTAMP NOT NULL
);

-- Signatures (doctor approves a row, nurse signs execution)
CREATE TABLE prescription_signatures (
    id UUID PRIMARY KEY,
    item_id UUID REFERENCES prescription_items(id),
    user_id UUID NOT NULL,
    role VARCHAR(32) NOT NULL,            -- DOCTOR/NURSE
    signed_at TIMESTAMP NOT NULL,
    hash VARCHAR(255),
    status VARCHAR(32)
);

-- Vital signs document
CREATE TABLE vital_sign_lists (
    id UUID PRIMARY KEY,
    list_id UUID NOT NULL REFERENCES prescription_lists(id),
    created_at TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    version INTEGER DEFAULT 0
);

-- Vital signs per day
CREATE TABLE vital_sign_days (
    id UUID PRIMARY KEY,
    vital_list_id UUID NOT NULL REFERENCES vital_sign_lists(id),
    day_date DATE NOT NULL
);

-- Vital sign entry per period
CREATE TABLE vital_sign_entries (
    id UUID PRIMARY KEY,
    day_id UUID NOT NULL REFERENCES vital_sign_days(id),
    period VARCHAR(8) NOT NULL CHECK (period IN ('morning','evening')),
    temperature DECIMAL(4,1),
    systolic_bp INTEGER,
    diastolic_bp INTEGER,
    spo2 INTEGER,
    pulse INTEGER,
    stool VARCHAR(50),
    pain_score INTEGER CHECK (pain_score BETWEEN 0 AND 10)
);

-- Cached medicine catalog (from MIS API)
CREATE TABLE medicine_catalog_cache (
    id BIGINT PRIMARY KEY,                -- ItemKindID from MIS
    name VARCHAR(500) NOT NULL,
    category_ref INTEGER,
    ptg_code VARCHAR(50),
    is_high_risk BOOLEAN DEFAULT FALSE,
    cached_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Cached patient allergies (from MIS API)
CREATE TABLE allergy_cache (
    id UUID PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    allergen_name VARCHAR(500) NOT NULL,
    source_document_id INTEGER,           -- reference to MIS document
    cached_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(patient_id, allergen_name)
);

-- Drug interaction rules (PTG conflict matrix — app-owned reference data)
CREATE TABLE drug_interaction_rules (
    id UUID PRIMARY KEY,
    ptg_code_a VARCHAR(50) NOT NULL,
    ptg_code_b VARCHAR(50) NOT NULL,
    severity VARCHAR(16) NOT NULL DEFAULT 'WARNING', -- WARNING/CONTRAINDICATED
    description TEXT,
    UNIQUE(ptg_code_a, ptg_code_b)
);

-- Telegram subscribers
CREATE TABLE telegram_subscriptions (
    chat_id BIGINT PRIMARY KEY,
    subscribed_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

---

## #16: Role Mapping

### Current (Prescription List)

| Role | businessRole | Permissions |
|------|-------------|-------------|
| ADMIN | — | admin:read, admin:create, admin:update, admin:delete |
| EMPLOYEE | DOCTOR | employee:read, employee:create, employee:update |
| EMPLOYEE | NURSE | employee:read, employee:update |

### Target (Unified ICU Roles + Permissions)

| ICU Role | businessRole | Prescription Permissions |
|----------|-------------|--------------------------|
| DOCTOR | DOCTOR | prescription:read, prescription:create, prescription:edit, prescription:approve, prescription:close, prescription:reopen |
| NURSE | NURSE | prescription:read, prescription:execute, prescription:2p_verify |
| HEAD_OF_DEPARTMENT | DOCTOR | All DOCTOR permissions + prescription:reopen_any |
| ADMINISTRATOR | ADMIN | All permissions |
| AUDITOR | — | prescription:audit_read (read-only) |

### Migration logic
```
IF old_role = "ADMIN" → UserRole.ADMINISTRATOR
IF old_role = "EMPLOYEE" AND businessRole = "DOCTOR" → UserRole.DOCTOR
IF old_role = "EMPLOYEE" AND businessRole = "NURSE" → UserRole.NURSE
```

---

## #17: API Endpoint Inventory

### 16 endpoints from MedicineListController

| # | Method | Path | Direct DB? | MIS API Needed |
|---|--------|------|------------|----------------|
| 1 | GET | `/api/medicinelist` | ✅ Own DB | None |
| 2 | GET | `/api/medicinelist/bylist/{id}` | ✅ Own DB (JSON deser) | None |
| 3 | GET | `/api/medicinelist/bypatient/{id}` | ✅ Own DB | None |
| 4 | GET | `/api/medicinelist/allergies/bypatient/{id}` | ❌ DE DB | `MisService.getPatientAllergies(patientId)` |
| 5 | POST | `/api/medicinelist` | ✅ Own DB | None |
| 6 | PUT | `/api/medicinelist` | ✅ Own DB (JSON ser) | None |
| 7 | PUT | `/api/medicinelist/{id}?status=` | ✅ Own DB | None |
| 8 | PUT | `/api/medicinelist/closelist/{id}` | ✅ Own DB | None |
| 9 | DELETE | `/api/medicinelist/{id}` | ✅ Own DB | None |
| 10 | GET | `/api/medicinelist/searchpatients?keyword=` | ❌ DE DB | `MisService.searchPatients(keyword)` |
| 11 | GET | `/api/medicinelist/searchmedicine?keyword=` | ❌ DE DB | `MisService.getMedicineCatalog(keyword)` |
| 12 | GET | `/api/medicinelist/medicine/getHighRiskMedicineByName?name=` | ❌ DE DB | `MisService.getMedicineByName(name)` → check category |
| 13 | GET | `/api/medicinelist/medicine/getConflictMedicineByName?name=` | ❌ DE DB | `MisService.getMedicineByName(name)` → check PTG |
| 14 | GET | `/api/medicinelist/patient/{id}` | ❌ DE DB | `MisService.getPatient(id)` |
| 15 | GET | `/api/medicinelist/patient/sort?order=&residence=` | ❌ DE DB | `MisService.searchPatients()` + hospitalization filter |
| 16 | GET | `/api/medicinelist/isDocumentEditing/{id}` | ✅ Own DB | None |
| 17 | GET | `/api/medicinelist/generatedoc?listID=&date=` | ✅ Own DB (flag set) | None |

**Result: 7 of 17 endpoints require MIS API refactoring (#4, #10, #11, #12, #13, #14, #15)**

---

## #18: Test Data Snapshot

### Seed data for integration tests (`data-test.sql` excerpt)

```sql
-- Test patient (would come from MIS API in production)
-- Test doctor
INSERT INTO users (id, login, password_hash, full_name, role, created_at, created_by, version)
VALUES ('d0000000-0000-0000-0000-000000000001', 'doctor_test',
        '$2a$10$hash', 'Лікар Тестовий', 'DOCTOR', NOW(), 0, 0);

-- Test nurse
INSERT INTO users (id, login, password_hash, full_name, role, created_at, created_by, version)
VALUES ('d0000000-0000-0000-0000-000000000002', 'nurse_test',
        '$2a$10$hash', 'Медсестра Тестова', 'NURSE', NOW(), 0, 0);

-- Test prescription list (patientId=1001 from MIS mock)
INSERT INTO prescription_lists (id, patient_id, document_name, status, created_at, created_by, version)
VALUES ('e0000000-0000-0000-0000-000000000001', 1001,
        'Листок лікарських призначень', 'Saved', NOW(),
        'd0000000-0000-0000-0000-000000000001', 0);

-- 21-day schedule for day 1
INSERT INTO prescription_item_days (id, item_id, day_date, created_at)
VALUES ('f0000000-0000-0000-0000-000000000001',
        'e0000000-0000-0000-0000-000000000002', '2026-07-25', NOW());

-- 4 time periods for day 1
INSERT INTO prescription_day_parts (id, day_id, period, dose, is_planned)
VALUES
('f1000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', 'morning', '500mg', TRUE),
('f1000000-0000-0000-0000-000000000002', 'f0000000-0000-0000-0000-000000000001', 'day', '500mg', TRUE),
('f1000000-0000-0000-0000-000000000003', 'f0000000-0000-0000-0000-000000000001', 'evening', '500mg', TRUE),
('f1000000-0000-0000-0000-000000000004', 'f0000000-0000-0000-0000-000000000001', 'night', '250mg', FALSE);

-- Drug interaction rules (PTG conflict matrix)
INSERT INTO drug_interaction_rules (id, ptg_code_a, ptg_code_b, severity, description)
VALUES
('g0000000-0000-0000-0000-000000000001', '1', '2', 'WARNING', 'PTG group 1 + 2 — potential interaction'),
('g0000000-0000-0000-0000-000000000002', '2', '3', 'WARNING', 'PTG group 2 + 3 — potential interaction');
```

---

## Phase 0 Summary

| Issue | Deliverable | Status |
|-------|-------------|--------|
| #13 | Security audit + .env.example + .gitignore | ✅ Complete |
| #14 | SQL query inventory (9 DE tables + 4 own tables) | ✅ Complete |
| #15 | ER diagram + 13 PostgreSQL table definitions | ✅ Complete |
| #16 | Role mapping (old → unified ICU roles + permissions) | ✅ Complete |
| #17 | API endpoint inventory (7 of 17 need MIS refactor) | ✅ Complete |
| #18 | Test data snapshot (seed SQL for integration tests) | ✅ Complete |
