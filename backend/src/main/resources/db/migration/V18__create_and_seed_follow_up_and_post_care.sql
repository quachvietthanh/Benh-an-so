-- =====================================================
-- V18__create_and_seed_follow_up_and_post_care.sql
-- Follow-up reminders (NCL-10-CN-001) & Post-care logs (NCL-10-CN-002)
-- =====================================================

CREATE TABLE follow_up_reminders (
    id BINARY(16) NOT NULL,
    patient_id BINARY(16) NOT NULL,
    visit_id BINARY(16) NULL,
    appointment_id BINARY(16) NULL,
    follow_up_date DATE NOT NULL,
    remind_at TIMESTAMP NOT NULL,
    reminder_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    notes TEXT NULL,
    created_by BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_follow_up_reminders PRIMARY KEY (id),
    CONSTRAINT fk_follow_up_reminders_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_follow_up_reminders_visit FOREIGN KEY (visit_id) REFERENCES visits(id),
    CONSTRAINT fk_follow_up_reminders_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    CONSTRAINT fk_follow_up_reminders_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_follow_up_reminders_type CHECK (reminder_type IN ('REVISIT', 'MEDICATION_CHECK', 'GENERAL')),
    CONSTRAINT chk_follow_up_reminders_status CHECK (status IN ('PENDING', 'SENT', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX idx_follow_up_reminders_patient ON follow_up_reminders(patient_id);
CREATE INDEX idx_follow_up_reminders_status ON follow_up_reminders(status);
CREATE INDEX idx_follow_up_reminders_remind_at ON follow_up_reminders(remind_at);

CREATE TABLE post_care_logs (
    id BINARY(16) NOT NULL,
    patient_id BINARY(16) NOT NULL,
    reminder_id BINARY(16) NULL,
    visit_id BINARY(16) NULL,
    contact_channel VARCHAR(30) NOT NULL,
    contacted_at TIMESTAMP NOT NULL,
    patient_condition VARCHAR(30) NOT NULL,
    care_notes TEXT NOT NULL,
    contact_outcome VARCHAR(30) NOT NULL,
    performed_by BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_post_care_logs PRIMARY KEY (id),
    CONSTRAINT fk_post_care_logs_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_post_care_logs_reminder FOREIGN KEY (reminder_id) REFERENCES follow_up_reminders(id),
    CONSTRAINT fk_post_care_logs_visit FOREIGN KEY (visit_id) REFERENCES visits(id),
    CONSTRAINT fk_post_care_logs_performed_by FOREIGN KEY (performed_by) REFERENCES users(id),
    CONSTRAINT chk_post_care_logs_channel CHECK (contact_channel IN ('PHONE', 'SMS', 'IN_PERSON', 'ZALO')),
    CONSTRAINT chk_post_care_logs_condition CHECK (patient_condition IN ('STABLE', 'RECOVERING', 'COMPLICATIONS', 'NEEDS_REVISIT')),
    CONSTRAINT chk_post_care_logs_outcome CHECK (contact_outcome IN ('REACHED', 'UNREACHABLE', 'DECLINED'))
);

CREATE INDEX idx_post_care_logs_patient ON post_care_logs(patient_id, contacted_at);
CREATE INDEX idx_post_care_logs_reminder ON post_care_logs(reminder_id);
CREATE INDEX idx_post_care_logs_contacted_at ON post_care_logs(contacted_at);
CREATE INDEX idx_post_care_logs_channel ON post_care_logs(contact_channel);

-- Seed Follow-up Reminders (Demo US-44: Nhắc tái khám)
INSERT INTO follow_up_reminders (
    id, patient_id, visit_id, appointment_id, follow_up_date, remind_at, reminder_type, status, notes, created_by, created_at
) VALUES
(UUID_TO_BIN('fa100000-0000-0000-0000-000000000001'), UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001'), UUID_TO_BIN('d0000000-0000-0000-0000-000000000001'), NULL, DATE_ADD(CURDATE(), INTERVAL 7 DAY), DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 6 DAY), 'REVISIT', 'PENDING', 'Tái khám kiểm tra đáp ứng phác đồ viêm dạ dày.', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), CURRENT_TIMESTAMP),
(UUID_TO_BIN('fa100000-0000-0000-0000-000000000002'), UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb002'), UUID_TO_BIN('d0000000-0000-0000-0000-000000000003'), NULL, DATE_ADD(CURDATE(), INTERVAL 14 DAY), DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 13 DAY), 'MEDICATION_CHECK', 'PENDING', 'Kiểm tra huyết áp và điều chỉnh liều Amlodipine.', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), CURRENT_TIMESTAMP),
(UUID_TO_BIN('fa100000-0000-0000-0000-000000000003'), UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb004'), NULL, NULL, DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 2 DAY), 'REVISIT', 'COMPLETED', 'Đã liên hệ và bệnh nhân đã tái khám.', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 5 DAY));

-- Seed Post-care Logs (Demo US-45: Ghi nhận chăm sóc sau khám)
INSERT INTO post_care_logs (
    id, patient_id, reminder_id, visit_id, contact_channel, contacted_at, patient_condition, care_notes, contact_outcome, performed_by, created_at
) VALUES
(UUID_TO_BIN('fa200000-0000-0000-0000-000000000001'), UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001'), UUID_TO_BIN('fa100000-0000-0000-0000-000000000001'), UUID_TO_BIN('d0000000-0000-0000-0000-000000000001'), 'PHONE', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY), 'RECOVERING', 'Bệnh nhân đỡ đau thượng vị, ăn ngủ tốt, tuân thủ uống thuốc sau ăn theo dặn dò.', 'REACHED', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'), CURRENT_TIMESTAMP),
(UUID_TO_BIN('fa200000-0000-0000-0000-000000000002'), UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb002'), UUID_TO_BIN('fa100000-0000-0000-0000-000000000002'), UUID_TO_BIN('d0000000-0000-0000-0000-000000000003'), 'ZALO', CURRENT_TIMESTAMP, 'STABLE', 'Đã nhắn tin nhắc lịch đo huyết áp hàng ngày tại nhà và dặn mang nhật ký huyết áp khi tái khám.', 'REACHED', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'), CURRENT_TIMESTAMP);
