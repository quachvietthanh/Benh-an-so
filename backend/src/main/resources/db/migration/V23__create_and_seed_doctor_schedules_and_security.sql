-- =====================================================
-- V23__create_and_seed_doctor_schedules_and_security.sql
-- Doctor Weekly Schedules & Time Offs (NCL-03-CN-006)
-- Security Alerts (NCL-15-CN-002) & Audit Logs (NCL-08-CN-003, NCL-15-CN-004)
-- =====================================================

CREATE TABLE doctor_weekly_schedules (
    id BINARY(16) NOT NULL,
    doctor_id BINARY(16) NOT NULL,
    day_of_week ENUM('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY') NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_doctor_weekly_schedules PRIMARY KEY (id),
    CONSTRAINT uk_doctor_weekly_schedules UNIQUE (doctor_id, day_of_week),
    CONSTRAINT fk_dws_doctor FOREIGN KEY (doctor_id) REFERENCES users(id),
    CONSTRAINT chk_dws_time_range CHECK (end_time > start_time)
);

CREATE INDEX idx_doctor_weekly_schedules_doctor ON doctor_weekly_schedules(doctor_id, active);

CREATE TABLE doctor_time_offs (
    id BINARY(16) NOT NULL,
    doctor_id BINARY(16) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status ENUM('ACTIVE', 'CANCELLED') NOT NULL DEFAULT 'ACTIVE',
    created_by BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_doctor_time_offs PRIMARY KEY (id),
    CONSTRAINT fk_dto_doctor FOREIGN KEY (doctor_id) REFERENCES users(id),
    CONSTRAINT fk_dto_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_dto_time_range CHECK (end_time > start_time)
);

CREATE INDEX idx_doctor_time_offs_doctor_time ON doctor_time_offs(doctor_id, status, start_time, end_time);
CREATE INDEX idx_doctor_time_offs_created_by ON doctor_time_offs(created_by);

CREATE TABLE security_alerts (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    description TEXT NOT NULL,
    access_count INT NOT NULL,
    window_start DATETIME(6) NOT NULL,
    window_end DATETIME(6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_security_alerts PRIMARY KEY (id),
    CONSTRAINT uq_security_alerts_user_type_window UNIQUE (user_id, alert_type, window_start),
    CONSTRAINT fk_security_alerts_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
);

CREATE INDEX idx_security_alerts_user_created
    ON security_alerts (user_id, created_at);

-- 1. Seed Doctor Weekly Recurring Schedules (Demo US-74: Lịch làm việc tuần lặp lại của bác sĩ)
INSERT INTO doctor_weekly_schedules (id, doctor_id, day_of_week, start_time, end_time, active, created_at, updated_at) VALUES
-- Dr. Nguyen Minh Anh (doctor1): Thứ 2 đến Thứ 6 (Day 2..6: 08:00 - 17:00)
(UUID_TO_BIN('d2000000-0000-0000-0000-000000000002'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), 'MONDAY', '08:00:00', '17:00:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(UUID_TO_BIN('d2000000-0000-0000-0000-000000000003'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), 'TUESDAY', '08:00:00', '17:00:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(UUID_TO_BIN('d2000000-0000-0000-0000-000000000004'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), 'WEDNESDAY', '08:00:00', '17:00:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(UUID_TO_BIN('d2000000-0000-0000-0000-000000000005'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), 'THURSDAY', '08:00:00', '17:00:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(UUID_TO_BIN('d2000000-0000-0000-0000-000000000006'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), 'FRIDAY', '08:00:00', '17:00:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Dr. Tran Quang Huy (doctor2): Thứ 3 đến Thứ 7 (Day 3..7: 08:00 - 17:00)
(UUID_TO_BIN('d2000000-0000-0000-0000-000000000023'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3'), 'TUESDAY', '08:00:00', '17:00:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(UUID_TO_BIN('d2000000-0000-0000-0000-000000000024'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3'), 'WEDNESDAY', '08:00:00', '17:00:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(UUID_TO_BIN('d2000000-0000-0000-0000-000000000025'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3'), 'THURSDAY', '08:00:00', '17:00:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(UUID_TO_BIN('d2000000-0000-0000-0000-000000000026'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3'), 'FRIDAY', '08:00:00', '17:00:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(UUID_TO_BIN('d2000000-0000-0000-0000-000000000027'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3'), 'SATURDAY', '08:00:00', '17:00:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 2. Seed Doctor Time Offs (Demo US-74: Nghỉ phép của bác sĩ)
INSERT INTO doctor_time_offs (id, doctor_id, start_time, end_time, reason, status, created_by, created_at, updated_at) VALUES
(UUID_TO_BIN('d3000000-0000-0000-0000-000000000001'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 2 DAY), DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 50 HOUR), 'Tham gia hội nghị Y khoa Tim mạch Quốc gia', 'ACTIVE', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa7'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(UUID_TO_BIN('d3000000-0000-0000-0000-000000000002'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3'), DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 5 DAY), DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 122 HOUR), 'Nghỉ phép việc gia đình', 'ACTIVE', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 3. Seed Security Alerts (Demo US-69: Cảnh báo truy cập bệnh án và an toàn bất thường)
INSERT INTO security_alerts (id, user_id, alert_type, severity, description, access_count, window_start, window_end, status, created_at) VALUES
(UUID_TO_BIN('ee100000-0000-0000-0000-000000000001'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3'), 'THRESHOLD_EXCEEDED', 'HIGH', 'Phat hien 25 luot truy cap ho so benh an trong 5 phut.', 25, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 10 MINUTE), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 5 MINUTE), 'UNREAD', CURRENT_TIMESTAMP),
(UUID_TO_BIN('ee100000-0000-0000-0000-000000000002'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'), 'OFF_HOURS_ACCESS', 'LOW', 'Truy cap ho so ngoai gio lam viec: le tan tra cuu luc 23:45.', 1, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 23 HOUR), 'READ', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY));

-- 4. Seed Audit Logs (Demo US-35, US-71: Rà soát nhật ký truy cập và xuất báo cáo kiểm toán)
INSERT INTO audit_logs (id, user_id, action_type, resource_type, resource_id, detail, ip_address, created_at) VALUES
(UUID_TO_BIN('e3000000-0000-0000-0000-000000000001'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), 'VIEW', 'MEDICAL_RECORD', UUID_TO_BIN('50000000-0000-0000-0000-000000000001'), '{"action": "VIEW_RECORD", "patientCode": "BN000001", "recordStatus": "SIGNED"}', '192.168.1.10', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 2 HOUR)),
(UUID_TO_BIN('e3000000-0000-0000-0000-000000000002'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), 'CREATE', 'PRESCRIPTION', UUID_TO_BIN('16200000-0000-0000-0000-000000000005'), '{"action": "ISSUE_PRESCRIPTION", "prescriptionCode": "RX000005", "medicines": ["Paracetamol"]}', '192.168.1.10', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 HOUR)),
(UUID_TO_BIN('e3000000-0000-0000-0000-000000000003'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'), 'CREATE', 'APPOINTMENT', UUID_TO_BIN('cccccccc-cccc-cccc-cccc-ccccccccc001'), '{"action": "BOOK_APPOINTMENT", "patientCode": "BN000001", "channel": "RECEPTIONIST"}', '192.168.1.20', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 3 HOUR)),
(UUID_TO_BIN('e3000000-0000-0000-0000-000000000004'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'), 'DISPENSE', 'PRESCRIPTION', UUID_TO_BIN('16200000-0000-0000-0000-000000000001'), '{"action": "DISPENSE_MEDICINE", "prescriptionCode": "RX000001", "pharmacist": "Vo Thanh Nam"}', '192.168.1.30', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 4 HOUR)),
(UUID_TO_BIN('e3000000-0000-0000-0000-000000000005'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa7'), 'EXPORT', 'REPORT', NULL, '{"action": "EXPORT_REVENUE_REPORT", "format": "EXCEL", "period": "2026-09"}', '192.168.1.40', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 5 HOUR)),
(UUID_TO_BIN('e3000000-0000-0000-0000-000000000006'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1'), 'UPDATE', 'USER_ROLE', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'), '{"action": "SYNC_PERMISSIONS", "targetUser": "receptionist1"}', '192.168.1.1', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY));
