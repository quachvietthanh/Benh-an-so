CREATE TABLE appointment_notification_logs (
    id BINARY(16) NOT NULL,
    appointment_id BINARY(16) NOT NULL,
    patient_id BINARY(16) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempted_at TIMESTAMP NOT NULL,
    sent_at TIMESTAMP NULL,
    failure_reason VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_appointment_notification_logs PRIMARY KEY (id),
    CONSTRAINT fk_appointment_notification_logs_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    CONSTRAINT fk_appointment_notification_logs_patient
        FOREIGN KEY (patient_id) REFERENCES patients(id)
);

CREATE INDEX idx_appointment_notification_logs_lookup
    ON appointment_notification_logs(appointment_id, notification_type, status);
CREATE INDEX idx_appointment_notification_logs_attempted_at
    ON appointment_notification_logs(attempted_at);
