CREATE INDEX idx_access_user
    ON medical_record_access_logs(accessed_by);

CREATE INDEX idx_access_time
    ON medical_record_access_logs(accessed_at);

CREATE INDEX idx_access_user_time
    ON medical_record_access_logs(accessed_by, accessed_at);
