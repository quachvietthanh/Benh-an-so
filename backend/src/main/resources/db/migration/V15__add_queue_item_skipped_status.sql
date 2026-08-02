ALTER TABLE queue_items
    ADD COLUMN skipped_at TIMESTAMP NULL AFTER cancelled_at,
    ADD COLUMN skip_reason VARCHAR(500) NULL AFTER cancel_reason;

ALTER TABLE queue_items
    DROP CHECK ck_queue_items_status;

ALTER TABLE queue_items
    ADD CONSTRAINT ck_queue_items_status
    CHECK (status IN ('WAITING', 'IN_PROGRESS', 'WAITING_FOR_RESULT', 'COMPLETED', 'CANCELLED', 'SKIPPED'));
