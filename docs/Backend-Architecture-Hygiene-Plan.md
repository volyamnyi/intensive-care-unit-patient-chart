# Backend Architecture Hygiene Follow-up (#427)

This note records deliberately deferred extraction plans. The code review classified these as maintainability opportunities, not behavior defects; do not combine them into unrelated bug-fix changes. Each extraction requires its own focused tests and must preserve transaction, audit, and wire contracts.

## FlowInstanceService

Current service has several cohesive responsibilities. Extract in this order:

1. **`StepValueValidator` (first, pure logic).** Move the element/type validation and TP-LL-02 special-case rules into a stateless validator. Keep the service call at the existing step-completion boundary. Preserve error codes/messages and validate the serialized snapshot, not live template data. Add a table-driven test for every element kind, required/optional values, malformed regex, and the TP-LL-02 exceptions before moving code.
2. **`InstanceResponseAssembler` (after query path is isolated).** Move mapping/enrichment from `FlowInstance` to response DTOs behind an explicit input model. Do not resolve MIS data per row; accept already-batched patient/order data. Preserve the existing owner/masking behavior and response shape.
3. **`FlowInstanceAuditEvents` (last).** Extract event construction only, not transaction ownership or emission timing. Preserve action IDs, targets, related entities, outcome/error codes, and after-commit behavior. Pin emitted events in service tests before extraction.

Avoid a broad service split: keep lifecycle orchestration in `FlowInstanceService`, inject all collaborators, and do not use `SpringContext` for domain dependencies.

## ClinicalDayService

Extract scheduled work into `ClinicalDayJobScheduler` only after characterizing cron hours, timezone, signing-window behavior, and failure handling. The scheduler should call narrow service operations; it must not duplicate transaction boundaries or swallow per-day failures. Keep the scheduling properties as the single source of time configuration.

## Medication sheet

- **`PrescriptionDayFactory`**: centralize creation of the four day-parts shared by item creation and add-day paths. Preserve order, planned/completed defaults, audit behavior, and the uniqueness/idempotency contract. Benchmark the proposed `saveAll` before changing write strategy.
- **`DoseTransitionGuard`**: extract shared preconditions for plan/complete/cancel/replan transitions. Keep endpoint-specific authorization, two-person verification, audit events, and distinct error codes at their current boundaries. Build a transition matrix first; do not collapse behavior into a generic state setter.

## Integration-test host

`icu-chart` remains the intentional host for cross-module integration suites for this reactor. These tests need the full application configuration and all four datasources; moving them to a new module is deferred until the next reactor restructuring. Preserve these invariants:

- integration profile is sequential; no parallel class execution against shared truncate/reseed data;
- respect the documented per-context connection budget and `APP_DATASOURCE_POOL_MAX`;
- use the existing rollback/seed patterns per module, and do not boot a feature module in isolation for cross-module API checks;
- keep the MIS stub local to test infrastructure and LDAP disabled unless the test is explicitly gated for local LDAP.

Any future move must retain the same datasource routing, test data isolation, architecture boundaries, and CI opt-in policy, with before/after suite counts recorded.
