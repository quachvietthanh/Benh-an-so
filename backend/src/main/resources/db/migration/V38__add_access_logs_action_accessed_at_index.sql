-- =====================================================
-- V38__add_access_logs_action_accessed_at_index.sql
-- Composite index to back the anomaly scanner's per-action,
-- time-window lookup without a full table scan (NCL-15 / QTN-25).
-- =====================================================

CREATE INDEX idx_access_logs_action_accessed_at
    ON medical_record_access_logs (action, accessed_at);
