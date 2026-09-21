-- =====================================================
-- V77__add_priority_to_queue_items.sql
-- NCL-03-CN-013: Ưu tiên khám cho trường hợp cấp cứu
-- Support queue item priority level, priority reason, and audit fields
-- =====================================================

ALTER TABLE queue_items
    ADD COLUMN priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    ADD COLUMN priority_reason VARCHAR(500) NULL,
    ADD COLUMN prioritized_at TIMESTAMP NULL,
    ADD COLUMN prioritized_by BINARY(16) NULL;

ALTER TABLE queue_items
    ADD CONSTRAINT chk_queue_items_priority
        CHECK (priority IN ('NORMAL', 'PRIORITY', 'EMERGENCY'));

ALTER TABLE queue_items
    ADD CONSTRAINT fk_queue_items_prioritized_by
        FOREIGN KEY (prioritized_by) REFERENCES users(id);

CREATE INDEX idx_queue_items_priority_order
    ON queue_items (medical_queue_id, status, priority, prioritized_at, queue_number);
