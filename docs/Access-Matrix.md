# Access matrix

<!-- GENERATED FILE - do not edit by hand. Source of truth: `SecurityConfig`, `ClinicalSecurityRules` and the controller mappings. `AccessMatrixArtifactTest` fails when it goes stale. -->

Endpoint count (code): **151**

## URL rules (first match wins)

| # | Matchers | Rule |
|---|---|---|
| 1 | `"/api/auth/**"` | `permitAll()` |
| 2 | `"/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**"` | `permitAll()` |
| 3 | `"/actuator/**"` | `hasAnyRole("ADMINISTRATOR", "AUDITOR")` |
| 4 | `HttpMethod.GET, "/api/episodes/**"` | `spEl(ICU_READ_SPEL)` |
| 5 | `HttpMethod.GET, "/api/clinical-days/**"` | `spEl(ICU_READ_SPEL)` |
| 6 | `HttpMethod.GET, "/api/notes/**"` | `spEl(ICU_READ_SPEL)` |
| 7 | `HttpMethod.GET, "/api/scales/**"` | `spEl(ICU_READ_SPEL)` |
| 8 | `HttpMethod.GET, "/api/hourly-records/**"` | `spEl(ICU_READ_SPEL)` |
| 9 | `HttpMethod.GET, "/api/orders/**"` | `spEl(ICU_READ_SPEL)` |
| 10 | `HttpMethod.GET, "/api/patients/**"` | `spEl(ICU_OR_MEDICATION_READ_SPEL)` |
| 11 | `HttpMethod.GET, "/api/users/me/**"` | `authenticated()` |
| 12 | `HttpMethod.GET, "/api/users/**"` | `spEl(USERS_READ_SPEL)` |
| 13 | `HttpMethod.GET, "/api/prescriptions/**"` | `spEl(MEDICATION_READ_SPEL)` |
| 14 | `HttpMethod.GET, "/api/vital-signs/**"` | `spEl(MEDICATION_READ_SPEL)` |
| 15 | `HttpMethod.GET, "/api/department/**"` | `spEl(ICU_READ_SPEL)` |
| 16 | `HttpMethod.GET, "/api/settings/**"` | `spEl(USERS_READ_SPEL)` |
| 17 | `HttpMethod.POST, "/api/episodes"` | `hasAnyRole(CLINICAL_ROLES)` |
| 18 | `"/api/episodes/**"` | `hasAnyRole(CLINICAL_ROLES)` |
| 19 | `HttpMethod.POST, "/api/clinical-days"` | `hasAnyRole(CLINICAL_ROLES)` |
| 20 | `HttpMethod.POST, "/api/clinical-days/*/sign/**"` | `hasAnyRole(CLINICAL_ROLES)` |
| 21 | `HttpMethod.POST, "/api/clinical-days/*/orders"` | `hasAnyRole(CLINICAL_ROLES)` |
| 22 | `"/api/clinical-days/**"` | `hasAnyRole(CLINICAL_ROLES)` |
| 23 | `"/api/notes/**"` | `hasAnyRole(CLINICAL_ROLES)` |
| 24 | `"/api/scales/**"` | `hasAnyRole(CLINICAL_ROLES)` |
| 25 | `"/api/hourly-records/**"` | `hasAnyRole(CLINICAL_ROLES)` |
| 26 | `HttpMethod.POST, "/api/orders/*/execute/finish"` | `hasAnyRole(CLINICAL_ROLES)` |
| 27 | `HttpMethod.POST, "/api/orders/*/execute"` | `hasAnyRole(CLINICAL_ROLES)` |
| 28 | `HttpMethod.PUT, "/api/orders/*/plan/finish"` | `hasAnyRole(CLINICAL_ROLES)` |
| 29 | `HttpMethod.PUT, "/api/orders/*/plan"` | `hasAnyRole(CLINICAL_ROLES)` |
| 30 | `HttpMethod.PUT, "/api/orders/*/cancel"` | `hasAnyRole(CLINICAL_ROLES)` |
| 31 | `HttpMethod.POST, "/api/orders/**"` | `hasAnyRole(CLINICAL_ROLES)` |
| 32 | `"/api/orders/**"` | `hasAnyRole(CLINICAL_ROLES)` |
| 33 | `HttpMethod.PATCH, "/api/executions/**"` | `hasAnyRole(CLINICAL_ROLES)` |
| 34 | `"/api/audit/**"` | `spEl( "hasAnyRole('ADMINISTRATOR', 'AUDITOR') or @permissionService.has('AUDIT_ACCESS')")` |
| 35 | `"/api/patients/**"` | `hasAnyRole(CLINICAL_ROLES)` |
| 36 | `"/api/users/**"` | `hasAnyRole(CLINICAL_ROLES)` |
| 37 | `HttpMethod.POST, "/api/prescriptions"` | `hasAnyRole(CLINICAL_ROLES)` |
| 38 | `HttpMethod.POST, "/api/prescriptions/*/items"` | `hasAnyRole(CLINICAL_ROLES)` |
| 39 | `HttpMethod.PUT, "/api/prescriptions/day-parts/*/plan"` | `hasAnyRole(CLINICAL_ROLES)` |
| 40 | `HttpMethod.PUT, "/api/prescriptions/day-parts/*/complete"` | `hasAnyRole(CLINICAL_ROLES)` |
| 41 | `HttpMethod.POST, "/api/prescriptions/day-parts/*/execute"` | `hasAnyRole(CLINICAL_ROLES)` |
| 42 | `HttpMethod.POST, "/api/prescriptions/*/close"` | `hasAnyRole(CLINICAL_ROLES)` |
| 43 | `HttpMethod.DELETE, "/api/prescriptions/items/*"` | `hasAnyRole(CLINICAL_ROLES)` |
| 44 | `HttpMethod.DELETE, "/api/prescriptions/*"` | `hasAnyRole(CLINICAL_ROLES)` |
| 45 | `"/api/prescriptions/**"` | `hasAnyRole(CLINICAL_ROLES)` |
| 46 | `"/api/vital-signs/**"` | `hasAnyRole(CLINICAL_ROLES)` |
| 47 | `"/api/admin/**"` | `hasAnyRole("ADMINISTRATOR")` |
| 48 | `"/api/**"` | `hasAnyRole(CLINICAL_ROLES)` |

## Endpoints by module

| Module | Java sources | Endpoints |
|---|---|---|
| `common` | 177 | 24 |
| `icu-chart` | 84 | 54 |
| `medication-sheet` | 68 | 30 |
| `prosthesis-manufacturing` | 101 | 43 |

## Endpoint matrix

`IOC` = object-level ownership check (assigned user or allow-all; strangers get 404 instead of 403 so ids cannot be enumerated). `-` = permission/ceiling only, no caller-scope check. `(class-level)` = inherited from the controller.

| Module | Controller | HTTP | Path | URL ceiling | @PreAuthorize | Object-level |
|---|---|---|---|---|---|---|
| `common` | `AdminController` | POST | `/api/admin/audit/backfill` | `hasAnyRole("ADMINISTRATOR")` | `hasRole('ADMINISTRATOR')` | - |
| `common` | `AdminController` | POST | `/api/admin/audit/retention/run` | `hasAnyRole("ADMINISTRATOR")` | `hasRole('ADMINISTRATOR')` | - |
| `medication-sheet` | `DrugInteractionController` | GET | `/api/admin/drug-interactions` | `hasAnyRole("ADMINISTRATOR")` | `hasRole('ADMINISTRATOR')` | - |
| `medication-sheet` | `DrugInteractionController` | POST | `/api/admin/drug-interactions/import` | `hasAnyRole("ADMINISTRATOR")` | `hasRole('ADMINISTRATOR')` | - |
| `common` | `AdminController` | GET | `/api/admin/permissions` | `hasAnyRole("ADMINISTRATOR")` | `(class-level)` | - |
| `common` | `AdminController` | PUT | `/api/admin/permissions` | `hasAnyRole("ADMINISTRATOR")` | `hasRole('ADMINISTRATOR')` | - |
| `common` | `AdminController` | GET | `/api/admin/stats` | `hasAnyRole("ADMINISTRATOR")` | `(class-level)` | - |
| `common` | `AdminController` | GET | `/api/admin/users` | `hasAnyRole("ADMINISTRATOR")` | `-` | - |
| `common` | `AdminController` | DELETE | `/api/admin/users/{id}` | `hasAnyRole("ADMINISTRATOR")` | `hasRole('ADMINISTRATOR')` | - |
| `common` | `AdminController` | GET | `/api/admin/users/{id}` | `hasAnyRole("ADMINISTRATOR")` | `-` | - |
| `common` | `AdminController` | PUT | `/api/admin/users/{id}/role` | `hasAnyRole("ADMINISTRATOR")` | `hasRole('ADMINISTRATOR')` | - |
| `common` | `AuditController` | GET | `/api/audit` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('AUDIT_ACCESS') or hasRole('AUDITOR')` | - |
| `common` | `AuditObjectHistoryController` | GET | `/api/audit/entities/{entityType}/{entityId}` | `spEl( "hasAnyRole('ADMINISTRATOR', 'AUDITOR') or @permissionService.has('AUDIT_ACCESS')")` | `@permissionService.has('AUDIT_ACCESS') or hasRole('AUDITOR')` | - |
| `common` | `AuditEventQueryController` | GET | `/api/audit/events` | `spEl( "hasAnyRole('ADMINISTRATOR', 'AUDITOR') or @permissionService.has('AUDIT_ACCESS')")` | `@permissionService.has('AUDIT_ACCESS') or hasRole('AUDITOR')` | - |
| `common` | `AuditEventQueryController` | GET | `/api/audit/events/{auditId}` | `spEl( "hasAnyRole('ADMINISTRATOR', 'AUDITOR') or @permissionService.has('AUDIT_ACCESS')")` | `@permissionService.has('AUDIT_ACCESS') or hasRole('AUDITOR')` | - |
| `common` | `AuditController` | GET | `/api/audit/{id}` | `spEl( "hasAnyRole('ADMINISTRATOR', 'AUDITOR') or @permissionService.has('AUDIT_ACCESS')")` | `@permissionService.has('AUDIT_ACCESS') or hasRole('AUDITOR')` | - |
| `common` | `AuthController` | POST | `/api/auth/login` | `permitAll()` | `-` | - |
| `common` | `AuthController` | POST | `/api/auth/logout` | `permitAll()` | `-` | - |
| `icu-chart` | `ClinicalDayController` | POST | `/api/clinical-days` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('CLINICAL_DAY_CREATE')` | - |
| `icu-chart` | `FluidBalanceController` | GET | `/api/clinical-days/{clinicalDayId}/fluid-balance` | `spEl(ICU_READ_SPEL)` | `-` | - |
| `icu-chart` | `FluidBalanceController` | POST | `/api/clinical-days/{clinicalDayId}/fluid-balance/recalculate` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('SCALE_APACHE_SOFA','SCALE_CAMICU_BRADEN_RASS','VITALS_ENTER')` | - |
| `icu-chart` | `HourlyRecordController` | GET | `/api/clinical-days/{clinicalDayId}/hourly-records` | `spEl(ICU_READ_SPEL)` | `-` | - |
| `icu-chart` | `HourlyRecordController` | POST | `/api/clinical-days/{clinicalDayId}/hourly-records` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('VITALS_ENTER')` | - |
| `icu-chart` | `LabResultController` | GET | `/api/clinical-days/{clinicalDayId}/lab-results` | `spEl(ICU_READ_SPEL)` | `-` | - |
| `icu-chart` | `LabResultController` | POST | `/api/clinical-days/{clinicalDayId}/lab-results` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('SCALE_APACHE_SOFA','SCALE_CAMICU_BRADEN_RASS','VITALS_ENTER')` | - |
| `icu-chart` | `MedicalNoteController` | GET | `/api/clinical-days/{clinicalDayId}/notes` | `spEl(ICU_READ_SPEL)` | `-` | - |
| `icu-chart` | `MedicalNoteController` | POST | `/api/clinical-days/{clinicalDayId}/notes` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('SCALE_APACHE_SOFA','SCALE_CAMICU_BRADEN_RASS','VITALS_ENTER')` | - |
| `icu-chart` | `MedicalOrderController` | GET | `/api/clinical-days/{clinicalDayId}/orders` | `spEl(ICU_READ_SPEL)` | `-` | - |
| `icu-chart` | `MedicalOrderController` | POST | `/api/clinical-days/{clinicalDayId}/orders` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PRESCRIPTION_CREATE')` | - |
| `icu-chart` | `PatientStateAssessmentController` | GET | `/api/clinical-days/{clinicalDayId}/patient-state` | `spEl(ICU_READ_SPEL)` | `-` | - |
| `icu-chart` | `PatientStateAssessmentController` | POST | `/api/clinical-days/{clinicalDayId}/patient-state` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('SCALE_APACHE_SOFA','SCALE_CAMICU_BRADEN_RASS','VITALS_ENTER')` | - |
| `icu-chart` | `PdfController` | GET | `/api/clinical-days/{clinicalDayId}/pdf` | `spEl(ICU_READ_SPEL)` | `@permissionService.has('PATIENT_VIEW')` | - |
| `icu-chart` | `PdfController` | POST | `/api/clinical-days/{clinicalDayId}/pdf` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('SCALE_APACHE_SOFA','SCALE_CAMICU_BRADEN_RASS','VITALS_ENTER')` | - |
| `icu-chart` | `PdfController` | GET | `/api/clinical-days/{clinicalDayId}/pdf/file` | `spEl(ICU_READ_SPEL)` | `@permissionService.has('PATIENT_VIEW')` | - |
| `icu-chart` | `ClinicalScaleController` | GET | `/api/clinical-days/{clinicalDayId}/scales` | `spEl(ICU_READ_SPEL)` | `-` | - |
| `icu-chart` | `ClinicalScaleController` | POST | `/api/clinical-days/{clinicalDayId}/scales` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('SCALE_APACHE_SOFA','SCALE_CAMICU_BRADEN_RASS')` | - |
| `icu-chart` | `VentilationSettingsController` | GET | `/api/clinical-days/{clinicalDayId}/ventilation` | `spEl(ICU_READ_SPEL)` | `-` | - |
| `icu-chart` | `VentilationSettingsController` | POST | `/api/clinical-days/{clinicalDayId}/ventilation` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('SCALE_APACHE_SOFA','SCALE_CAMICU_BRADEN_RASS','VITALS_ENTER')` | - |
| `icu-chart` | `ClinicalDayController` | GET | `/api/clinical-days/{id}` | `spEl(ICU_READ_SPEL)` | `-` | - |
| `icu-chart` | `ClinicalDayController` | PATCH | `/api/clinical-days/{id}` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('CLINICAL_DAY_CREATE')` | - |
| `icu-chart` | `ClinicalDayController` | POST | `/api/clinical-days/{id}/close-early` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('CLINICAL_DAY_CREATE')` | - |
| `icu-chart` | `ClinicalDayController` | POST | `/api/clinical-days/{id}/reopen` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('REOPEN_DAY')` | - |
| `icu-chart` | `ClinicalDayController` | POST | `/api/clinical-days/{id}/sign/doctor` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('SIGN_DOCTOR')` | - |
| `icu-chart` | `ClinicalDayController` | POST | `/api/clinical-days/{id}/sign/nurse` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('SIGN_NURSE')` | - |
| `icu-chart` | `DepartmentController` | GET | `/api/department/patients` | `spEl(ICU_READ_SPEL)` | `-` | - |
| `icu-chart` | `DepartmentController` | GET | `/api/department/stats` | `spEl(ICU_READ_SPEL)` | `-` | - |
| `icu-chart` | `EpisodeController` | GET | `/api/episodes` | `hasAnyRole(CLINICAL_ROLES)` | `-` | - |
| `icu-chart` | `EpisodeController` | POST | `/api/episodes` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('EPISODE_CREATE')` | - |
| `icu-chart` | `ClinicalScaleController` | GET | `/api/episodes/{episodeId}/scales` | `spEl(ICU_READ_SPEL)` | `(class-level)` | - |
| `icu-chart` | `ClinicalScaleController` | POST | `/api/episodes/{episodeId}/scales` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('SCALE_APACHE_SOFA','SCALE_CAMICU_BRADEN_RASS')` | - |
| `icu-chart` | `ClinicalScaleController` | POST | `/api/episodes/{episodeId}/scales/calculate` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('SCALE_APACHE_SOFA','SCALE_CAMICU_BRADEN_RASS')` | - |
| `icu-chart` | `EpisodeController` | GET | `/api/episodes/{id}` | `spEl(ICU_READ_SPEL)` | `-` | - |
| `icu-chart` | `EpisodeController` | PATCH | `/api/episodes/{id}` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('EPISODE_CREATE')` | - |
| `icu-chart` | `EpisodeController` | PUT | `/api/episodes/{id}/archive` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('EPISODE_CREATE')` | - |
| `icu-chart` | `EpisodeController` | GET | `/api/episodes/{id}/clinical-days` | `spEl(ICU_READ_SPEL)` | `-` | - |
| `icu-chart` | `EpisodeController` | POST | `/api/episodes/{id}/close` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('EPISODE_CREATE')` | - |
| `icu-chart` | `OrderExecutionController` | PATCH | `/api/executions/{id}` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PRESCRIPTION_EXECUTE')` | - |
| `icu-chart` | `HourlyRecordController` | PATCH | `/api/hourly-records/{id}` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('VITALS_ENTER')` | - |
| `icu-chart` | `LabResultController` | PATCH | `/api/lab-results/{id}` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('SCALE_APACHE_SOFA','SCALE_CAMICU_BRADEN_RASS','VITALS_ENTER')` | - |
| `icu-chart` | `MedicalNoteController` | PATCH | `/api/notes/{id}` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('SCALE_APACHE_SOFA','SCALE_CAMICU_BRADEN_RASS','VITALS_ENTER')` | - |
| `icu-chart` | `MedicalOrderController` | PATCH | `/api/orders/{id}` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PRESCRIPTION_CREATE')` | - |
| `icu-chart` | `MedicalOrderController` | POST | `/api/orders/{id}/cancel` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PRESCRIPTION_CREATE')` | - |
| `icu-chart` | `OrderExecutionController` | PUT | `/api/orders/{orderId}/cancel` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PRESCRIPTION_CREATE')` | - |
| `icu-chart` | `OrderExecutionController` | POST | `/api/orders/{orderId}/execute` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PRESCRIPTION_EXECUTE')` | - |
| `icu-chart` | `OrderExecutionController` | POST | `/api/orders/{orderId}/execute/finish` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PRESCRIPTION_EXECUTE')` | - |
| `icu-chart` | `OrderExecutionController` | GET | `/api/orders/{orderId}/executions` | `spEl(ICU_READ_SPEL)` | `-` | - |
| `icu-chart` | `OrderExecutionController` | PUT | `/api/orders/{orderId}/plan` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PRESCRIPTION_CREATE')` | - |
| `icu-chart` | `OrderExecutionController` | PUT | `/api/orders/{orderId}/plan/finish` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PRESCRIPTION_CREATE')` | - |
| `icu-chart` | `PatientStateAssessmentController` | PATCH | `/api/patient-state/{id}` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('SCALE_APACHE_SOFA','SCALE_CAMICU_BRADEN_RASS','VITALS_ENTER')` | - |
| `common` | `PatientController` | GET | `/api/patients` | `hasAnyRole(CLINICAL_ROLES)` | `-` | - |
| `common` | `PatientController` | GET | `/api/patients/pool` | `spEl(ICU_OR_MEDICATION_READ_SPEL)` | `-` | - |
| `common` | `PatientController` | GET | `/api/patients/{id}` | `spEl(ICU_OR_MEDICATION_READ_SPEL)` | `-` | - |
| `medication-sheet` | `PrescriptionController` | GET | `/api/prescriptions` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PATIENT_VIEW')` | - |
| `medication-sheet` | `PrescriptionController` | POST | `/api/prescriptions` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PRESCRIPTION_CREATE')` | - |
| `medication-sheet` | `PrescriptionController` | PUT | `/api/prescriptions/day-parts/{dayPartId}/cancel` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PRESCRIPTION_CREATE')` | - |
| `medication-sheet` | `PrescriptionController` | PUT | `/api/prescriptions/day-parts/{dayPartId}/cancel-assignment` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PRESCRIPTION_EXECUTE')` | - |
| `medication-sheet` | `PrescriptionController` | PUT | `/api/prescriptions/day-parts/{dayPartId}/complete` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PRESCRIPTION_CREATE')` | - |
| `medication-sheet` | `PrescriptionController` | POST | `/api/prescriptions/day-parts/{dayPartId}/execute` | `hasAnyRole(CLINICAL_ROLES)` | `(class-level)` | - |
| `medication-sheet` | `PrescriptionController` | PUT | `/api/prescriptions/day-parts/{dayPartId}/plan` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PRESCRIPTION_EXECUTE')` | - |
| `medication-sheet` | `PrescriptionController` | PUT | `/api/prescriptions/day-parts/{dayPartId}/replan` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PRESCRIPTION_CREATE')` | - |
| `medication-sheet` | `PrescriptionController` | DELETE | `/api/prescriptions/items/{itemId}` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PRESCRIPTION_CREATE')` | - |
| `medication-sheet` | `PrescriptionController` | POST | `/api/prescriptions/items/{itemId}/days` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PRESCRIPTION_CREATE')` | - |
| `medication-sheet` | `PrescriptionController` | DELETE | `/api/prescriptions/items/{itemId}/days/{dayId}` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PRESCRIPTION_CREATE')` | - |
| `medication-sheet` | `PrescriptionController` | GET | `/api/prescriptions/medicine-catalog` | `spEl(MEDICATION_READ_SPEL)` | `@permissionService.has('PATIENT_VIEW')` | - |
| `medication-sheet` | `PrescriptionController` | DELETE | `/api/prescriptions/{id}` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PRESCRIPTION_CREATE')` | - |
| `medication-sheet` | `PrescriptionController` | GET | `/api/prescriptions/{id}` | `spEl(MEDICATION_READ_SPEL)` | `@permissionService.has('PATIENT_VIEW')` | - |
| `medication-sheet` | `PrescriptionController` | POST | `/api/prescriptions/{id}/close` | `hasAnyRole(CLINICAL_ROLES)` | `(class-level)` | - |
| `medication-sheet` | `PrescriptionController` | POST | `/api/prescriptions/{id}/pdf/file` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PATIENT_VIEW')` | - |
| `medication-sheet` | `PrescriptionController` | GET | `/api/prescriptions/{id}/pdf/info` | `spEl(MEDICATION_READ_SPEL)` | `@permissionService.has('PATIENT_VIEW')` | - |
| `medication-sheet` | `PrescriptionController` | POST | `/api/prescriptions/{id}/pdf/pages/{pageIndex}` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PATIENT_VIEW')` | - |
| `medication-sheet` | `DrugInteractionController` | GET | `/api/prescriptions/{listId}/interactions` | `spEl(MEDICATION_READ_SPEL)` | `@permissionService.has('PATIENT_VIEW')` | - |
| `medication-sheet` | `PrescriptionController` | GET | `/api/prescriptions/{listId}/items` | `spEl(MEDICATION_READ_SPEL)` | `@permissionService.has('PATIENT_VIEW')` | - |
| `medication-sheet` | `PrescriptionController` | POST | `/api/prescriptions/{listId}/items` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PRESCRIPTION_CREATE')` | - |
| `prosthesis-manufacturing` | `FlowInstanceController` | GET | `/api/prosthesis-manufacturing/instances` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')` | - |
| `prosthesis-manufacturing` | `FlowInstanceController` | POST | `/api/prosthesis-manufacturing/instances` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PROSTHETICS_INSTANCE_CREATE')` | - |
| `prosthesis-manufacturing` | `FlowInstanceController` | GET | `/api/prosthesis-manufacturing/instances/page` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')` | - |
| `prosthesis-manufacturing` | `FlowInstanceController` | GET | `/api/prosthesis-manufacturing/instances/{id}` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')` | IOC |
| `prosthesis-manufacturing` | `FlowInstanceController` | POST | `/api/prosthesis-manufacturing/instances/{id}/backward` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PROSTHETICS_STEP_COMPLETE')` | - |
| `prosthesis-manufacturing` | `FlowInstanceController` | POST | `/api/prosthesis-manufacturing/instances/{id}/brak` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PROSTHETICS_STEP_COMPLETE')` | - |
| `prosthesis-manufacturing` | `FlowInstanceController` | GET | `/api/prosthesis-manufacturing/instances/{id}/brak-events` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')` | IOC |
| `prosthesis-manufacturing` | `FlowInstanceController` | GET | `/api/prosthesis-manufacturing/instances/{id}/branches` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')` | IOC |
| `prosthesis-manufacturing` | `FlowInstanceController` | GET | `/api/prosthesis-manufacturing/instances/{id}/evidence` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')` | IOC |
| `prosthesis-manufacturing` | `FlowInstanceController` | POST | `/api/prosthesis-manufacturing/instances/{id}/evidence` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PROSTHETICS_STEP_COMPLETE')` | IOC |
| `prosthesis-manufacturing` | `FlowInstanceController` | DELETE | `/api/prosthesis-manufacturing/instances/{id}/evidence/{fileId}` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PROSTHETICS_STEP_COMPLETE')` | IOC |
| `prosthesis-manufacturing` | `FlowInstanceController` | GET | `/api/prosthesis-manufacturing/instances/{id}/evidence/{fileId}` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')` | - |
| `prosthesis-manufacturing` | `FlowInstanceController` | POST | `/api/prosthesis-manufacturing/instances/{id}/fail` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PROSTHETICS_STEP_COMPLETE')` | IOC |
| `prosthesis-manufacturing` | `FlowInstanceController` | GET | `/api/prosthesis-manufacturing/instances/{id}/failure-snapshot` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')` | IOC |
| `prosthesis-manufacturing` | `FlowInstanceController` | POST | `/api/prosthesis-manufacturing/instances/{id}/pause` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PROSTHETICS_PAUSE_RESUME')` | IOC |
| `prosthesis-manufacturing` | `FlowInstanceController` | POST | `/api/prosthesis-manufacturing/instances/{id}/pdf` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')` | IOC |
| `prosthesis-manufacturing` | `FlowInstanceController` | GET | `/api/prosthesis-manufacturing/instances/{id}/resources` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')` | IOC |
| `prosthesis-manufacturing` | `FlowInstanceController` | POST | `/api/prosthesis-manufacturing/instances/{id}/resume` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PROSTHETICS_PAUSE_RESUME')` | IOC |
| `prosthesis-manufacturing` | `FlowInstanceController` | GET | `/api/prosthesis-manufacturing/instances/{id}/snapshot` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')` | IOC |
| `prosthesis-manufacturing` | `FlowInstanceController` | POST | `/api/prosthesis-manufacturing/instances/{id}/start` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PROSTHETICS_STEP_COMPLETE')` | IOC |
| `prosthesis-manufacturing` | `FlowInstanceController` | GET | `/api/prosthesis-manufacturing/instances/{id}/step-executions` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')` | IOC |
| `prosthesis-manufacturing` | `FlowInstanceController` | PATCH | `/api/prosthesis-manufacturing/instances/{id}/step-executions/{executionId}` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PROSTHETICS_STEP_COMPLETE')` | IOC |
| `prosthesis-manufacturing` | `FlowInstanceController` | POST | `/api/prosthesis-manufacturing/instances/{id}/steps/{executionId}/complete` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PROSTHETICS_STEP_COMPLETE')` | IOC |
| `prosthesis-manufacturing` | `ProstheticsOrderController` | GET | `/api/prosthesis-manufacturing/orders` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')` | - |
| `prosthesis-manufacturing` | `ProstheticsOrderController` | GET | `/api/prosthesis-manufacturing/orders/documents` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')` | - |
| `prosthesis-manufacturing` | `ProstheticsOrderController` | POST | `/api/prosthesis-manufacturing/orders/provision` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PROSTHETICS_ORDER_MANAGE')` | - |
| `prosthesis-manufacturing` | `ProstheticsOrderController` | GET | `/api/prosthesis-manufacturing/orders/{id}` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')` | - |
| `prosthesis-manufacturing` | `ProstheticsOrderController` | GET | `/api/prosthesis-manufacturing/orders/{id}/document-url` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')` | - |
| `prosthesis-manufacturing` | `ProstheticsPatientController` | GET | `/api/prosthesis-manufacturing/patients` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')` | - |
| `prosthesis-manufacturing` | `ProstheticsPatientController` | GET | `/api/prosthesis-manufacturing/patients/candidates` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')` | - |
| `prosthesis-manufacturing` | `ProstheticsPatientController` | GET | `/api/prosthesis-manufacturing/patients/{id}` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')` | - |
| `prosthesis-manufacturing` | `ProductionController` | GET | `/api/prosthesis-manufacturing/production` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PROSTHETICS_PRODUCTION_VIEW')` | - |
| `prosthesis-manufacturing` | `ProductionController` | GET | `/api/prosthesis-manufacturing/production/attention` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PROSTHETICS_PRODUCTION_VIEW')` | - |
| `prosthesis-manufacturing` | `ProductionController` | GET | `/api/prosthesis-manufacturing/production/settings/normative` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PROSTHETICS_PRODUCTION_VIEW_ALL')` | - |
| `prosthesis-manufacturing` | `ProductionController` | PUT | `/api/prosthesis-manufacturing/production/settings/normative` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PROSTHETICS_PRODUCTION_VIEW_ALL')` | - |
| `prosthesis-manufacturing` | `ProductionController` | GET | `/api/prosthesis-manufacturing/production/summary` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PROSTHETICS_PRODUCTION_VIEW')` | - |
| `prosthesis-manufacturing` | `ProductionController` | GET | `/api/prosthesis-manufacturing/production/team` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PROSTHETICS_PRODUCTION_VIEW_ALL')` | - |
| `prosthesis-manufacturing` | `ProductionController` | GET | `/api/prosthesis-manufacturing/production/{id}` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PROSTHETICS_PRODUCTION_VIEW')` | - |
| `prosthesis-manufacturing` | `FlowTemplateController` | GET | `/api/prosthesis-manufacturing/templates` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')` | - |
| `prosthesis-manufacturing` | `FlowTemplateController` | POST | `/api/prosthesis-manufacturing/templates` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PROSTHETICS_TEMPLATE_MANAGE')` | - |
| `prosthesis-manufacturing` | `FlowTemplateController` | DELETE | `/api/prosthesis-manufacturing/templates/{id}` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PROSTHETICS_TEMPLATE_MANAGE')` | - |
| `prosthesis-manufacturing` | `FlowTemplateController` | GET | `/api/prosthesis-manufacturing/templates/{id}` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')` | - |
| `prosthesis-manufacturing` | `FlowTemplateController` | PATCH | `/api/prosthesis-manufacturing/templates/{id}` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('PROSTHETICS_TEMPLATE_MANAGE')` | - |
| `icu-chart` | `ClinicalScaleController` | GET | `/api/scales` | `hasAnyRole(CLINICAL_ROLES)` | `-` | - |
| `icu-chart` | `ClinicalScaleController` | PATCH | `/api/scales/{id}` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('SCALE_APACHE_SOFA','SCALE_CAMICU_BRADEN_RASS')` | - |
| `common` | `SystemSettingsController` | GET | `/api/settings/{key}` | `spEl(USERS_READ_SPEL)` | `-` | - |
| `common` | `UserController` | GET | `/api/users/doctors` | `spEl(USERS_READ_SPEL)` | `-` | - |
| `common` | `UserController` | GET | `/api/users/me` | `spEl(USERS_READ_SPEL)` | `-` | - |
| `common` | `UserController` | GET | `/api/users/me/permissions` | `authenticated()` | `-` | - |
| `common` | `UserController` | GET | `/api/users/nurses` | `spEl(USERS_READ_SPEL)` | `-` | - |
| `icu-chart` | `VentilationSettingsController` | PATCH | `/api/ventilation/{id}` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.hasAny('SCALE_APACHE_SOFA','SCALE_CAMICU_BRADEN_RASS','VITALS_ENTER')` | - |
| `medication-sheet` | `VitalSignController` | GET | `/api/vital-signs` | `hasAnyRole(CLINICAL_ROLES)` | `-` | - |
| `medication-sheet` | `VitalSignController` | POST | `/api/vital-signs` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('VITALS_ENTER')` | - |
| `medication-sheet` | `VitalSignController` | PUT | `/api/vital-signs/cells` | `hasAnyRole(CLINICAL_ROLES)` | `(class-level)` | - |
| `medication-sheet` | `VitalSignController` | GET | `/api/vital-signs/days/{dayId}/entries` | `spEl(MEDICATION_READ_SPEL)` | `-` | - |
| `medication-sheet` | `VitalSignController` | PUT | `/api/vital-signs/entries/{entryId}` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('VITALS_ENTER')` | - |
| `medication-sheet` | `VitalSignController` | GET | `/api/vital-signs/grid` | `spEl(MEDICATION_READ_SPEL)` | `@permissionService.has('VITALS_ENTER')` | - |
| `medication-sheet` | `VitalSignController` | POST | `/api/vital-signs/grid` | `hasAnyRole(CLINICAL_ROLES)` | `@permissionService.has('VITALS_ENTER')` | - |

Endpoints: **151** (with an ownership check: **17**).
