CREATE INDEX idx_prescriptions_status_prescribed_at
    ON prescriptions(status, prescribed_at);
