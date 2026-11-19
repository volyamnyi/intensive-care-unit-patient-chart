--liquibase formatted sql

--changeset split-prosth:36
--comment Lower-limb side selection (issue #450/#452): «Стегно, R» and «Стегно, L»
--comment become checkboxes marking the amputated/prosthetic side. The two UUIDs
--comment stay unchanged so template-id and active-instance contracts are intact.
--comment Already-created instances keep their immutable snapshot (NUMERIC_INPUT)
--comment and their stored numeric values; no JSONB values are converted.
UPDATE prosthetics_template_elements
SET element_type = 'CHECKBOX',
    placeholder = NULL,
    unit = NULL,
    min_value = NULL,
    max_value = NULL
WHERE id IN ('f0000312-0000-0000-0000-000000000312',
             'f0000313-0000-0000-0000-000000000313')
  AND element_type <> 'CHECKBOX';

--rollback UPDATE prosthetics_template_elements
--rollback SET element_type = 'NUMERIC_INPUT',
--rollback     placeholder = 'Введіть значення',
--rollback     unit = 'см',
--rollback     min_value = 0,
--rollback     max_value = 200
--rollback WHERE id IN ('f0000312-0000-0000-0000-000000000312',
--rollback              'f0000313-0000-0000-0000-000000000313')
--rollback   AND element_type = 'CHECKBOX';
