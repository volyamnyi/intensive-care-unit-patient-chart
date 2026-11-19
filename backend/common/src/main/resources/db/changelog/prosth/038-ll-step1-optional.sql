--liquibase formatted sql

--changeset split-prosth:38
--comment TP-LL-02 step 1 (Зняття та внесення об'ємних розмірів, e0000020): the step
--comment becomes fully optional (issue #456): «Стегно, R» / «Стегно, L» side
--comment selection is no longer required to advance — validation moved out of
--comment FlowInstanceService.validateLowerLimbSideSelection and the wizard
--comment pair-rule was removed; mandatory=true -> false. Existing instances
--comment are not touched (their snapshots and stored values are immutable).
UPDATE prosthetics_template_steps
SET mandatory = false
WHERE id = 'e0000020-0000-0000-0000-000000000020'
  AND mandatory = true;

--rollback UPDATE prosthetics_template_steps
--rollback SET mandatory = true
--rollback WHERE id = 'e0000020-0000-0000-0000-000000000020'
