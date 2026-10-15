--liquibase formatted sql

--changeset split-prosth:34 runInTransaction:false
--comment flow_instances.order_id — the partial unique covers only non-terminal statuses; the brak-chain counts (countByOrderId / countByOrderIdUpTo, fired on every confirmed stage-6 brak and every threshold mail) read ALL statuses and previously seq-scanned
DROP INDEX CONCURRENTLY IF EXISTS idx_flow_instances_order;
CREATE INDEX CONCURRENTLY idx_flow_instances_order
    ON prosthetics_flow_instances (order_id);
--rollback DROP INDEX IF EXISTS idx_flow_instances_order;
