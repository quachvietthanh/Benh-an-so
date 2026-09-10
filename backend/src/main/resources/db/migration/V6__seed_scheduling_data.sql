-- =====================================================
-- V6__seed_scheduling_data.sql
-- Seed Scheduling, Rooms, Doctor Assignments, Doctor Schedules,
-- Diverse Appointments (Portal/Reception, No-Show) & Notification Logs
-- =====================================================

-- 1. Appointments
INSERT INTO appointments (id, appointment_code, patient_id, doctor_id, start_time, end_time, status, reason, cancel_reason, checked_in_at, completed_at, created_by, booking_channel, created_at) VALUES
(UUID_TO_BIN('cccccccc-cccc-cccc-cccc-ccccccccc001'), 'LH000001', UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 30 MINUTE), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 MINUTE), 'SCHEDULED', 'Kham tong quat qua han', NULL, NULL, NULL, UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'), 'RECEPTIONIST', CURRENT_TIMESTAMP),
(UUID_TO_BIN('cccccccc-cccc-cccc-cccc-ccccccccc002'), 'LH000002', UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb002'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 1 DAY), DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 1470 MINUTE), 'SCHEDULED', 'Tai kham noi khoa', NULL, NULL, NULL, UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'), 'RECEPTIONIST', CURRENT_TIMESTAMP),
(UUID_TO_BIN('cccccccc-cccc-cccc-cccc-ccccccccc003'), 'LH000003', UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb003'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3'), DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 2 DAY), DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 2910 MINUTE), 'SCHEDULED', 'Kham chuyen khoa', NULL, NULL, NULL, UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'), 'RECEPTIONIST', CURRENT_TIMESTAMP),
(UUID_TO_BIN('cccccccc-cccc-cccc-cccc-ccccccccc004'), 'LH000004', UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb004'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 3 DAY), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 4290 MINUTE), 'COMPLETED', 'Kham da hoan tat', NULL, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 3 DAY), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 2 DAY), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'), 'RECEPTIONIST', CURRENT_TIMESTAMP),
-- Demo US-11: No-Show appointment
(UUID_TO_BIN('cccccccc-cccc-cccc-cccc-ccccccccc005'), 'LH000005', UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb005'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3'), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 2 DAY), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 2850 MINUTE), 'NO_SHOW', 'Benh nhan vang mat', NULL, NULL, NULL, UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'), 'RECEPTIONIST', CURRENT_TIMESTAMP),
(UUID_TO_BIN('cccccccc-cccc-cccc-cccc-ccccccccc006'), 'LH000006', UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb006'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 3 DAY), DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 4350 MINUTE), 'CANCELLED', 'Lich da huy', 'Benh nhan ban viec dot xuat', NULL, NULL, UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'), 'RECEPTIONIST', CURRENT_TIMESTAMP),
-- Demo US-65/US-66 (NCL-14): Online Portal Appointments
(UUID_TO_BIN('cccccccc-cccc-cccc-cccc-ccccccccc009'), 'LH000009', UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 4 DAY), DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 5790 MINUTE), 'SCHEDULED', 'Dat lich truc tuyen qua cong benh nhan', NULL, NULL, NULL, UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa11'), 'PATIENT_PORTAL', CURRENT_TIMESTAMP),
(UUID_TO_BIN('cccccccc-cccc-cccc-cccc-ccccccccc010'), 'LH000010', UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb002'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3'), DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 5 DAY), DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 7230 MINUTE), 'CONFIRMED', 'Dat lich kham tim mach truc tuyen', NULL, NULL, NULL, UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa12'), 'PATIENT_PORTAL', CURRENT_TIMESTAMP);

-- 2. Rooms
INSERT INTO rooms (id, room_code, room_name, active, created_at, updated_at) VALUES
(UUID_TO_BIN('90000000-0000-0000-0000-000000000001'), 'P101', 'Phong kham 101', TRUE, CURRENT_TIMESTAMP, NULL),
(UUID_TO_BIN('90000000-0000-0000-0000-000000000002'), 'P102', 'Phong kham 102', TRUE, CURRENT_TIMESTAMP, NULL);

-- 3. Doctor Room Assignments
INSERT INTO doctor_room_assignments (id, doctor_id, room_id, assigned_by, assigned_at) VALUES
(UUID_TO_BIN('91000000-0000-0000-0000-000000000001'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), UUID_TO_BIN('90000000-0000-0000-0000-000000000001'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1'), CURRENT_TIMESTAMP),
(UUID_TO_BIN('91000000-0000-0000-0000-000000000002'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3'), UUID_TO_BIN('90000000-0000-0000-0000-000000000002'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1'), CURRENT_TIMESTAMP);

-- 4. Doctor Working Schedules (Consolidated from V32)
INSERT INTO doctor_schedules (id, doctor_id, schedule_date, start_time, end_time, active, created_at, updated_at) VALUES
(UUID_TO_BIN('8b000000-0000-0000-0000-000000000001'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), DATE_ADD(CURDATE(), INTERVAL 1 DAY),  '08:00:00', '17:00:00', TRUE, CURRENT_TIMESTAMP, NULL),
(UUID_TO_BIN('8b000000-0000-0000-0000-000000000002'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), DATE_ADD(CURDATE(), INTERVAL 2 DAY),  '08:00:00', '17:00:00', TRUE, CURRENT_TIMESTAMP, NULL),
(UUID_TO_BIN('8b000000-0000-0000-0000-000000000003'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), DATE_ADD(CURDATE(), INTERVAL 3 DAY),  '08:00:00', '17:00:00', TRUE, CURRENT_TIMESTAMP, NULL),
(UUID_TO_BIN('8b000000-0000-0000-0000-000000000004'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), DATE_ADD(CURDATE(), INTERVAL 4 DAY),  '08:00:00', '17:00:00', TRUE, CURRENT_TIMESTAMP, NULL),
(UUID_TO_BIN('8b000000-0000-0000-0000-000000000005'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), DATE_ADD(CURDATE(), INTERVAL 5 DAY),  '08:00:00', '17:00:00', TRUE, CURRENT_TIMESTAMP, NULL),
(UUID_TO_BIN('8b000000-0000-0000-0000-000000000006'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), DATE_ADD(CURDATE(), INTERVAL 6 DAY),  '08:00:00', '17:00:00', TRUE, CURRENT_TIMESTAMP, NULL),
(UUID_TO_BIN('8b000000-0000-0000-0000-000000000007'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), DATE_ADD(CURDATE(), INTERVAL 7 DAY),  '08:00:00', '17:00:00', TRUE, CURRENT_TIMESTAMP, NULL),
(UUID_TO_BIN('8b000000-0000-0000-0000-000000000008'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3'), DATE_ADD(CURDATE(), INTERVAL 1 DAY),  '08:00:00', '17:00:00', TRUE, CURRENT_TIMESTAMP, NULL),
(UUID_TO_BIN('8b000000-0000-0000-0000-000000000009'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3'), DATE_ADD(CURDATE(), INTERVAL 2 DAY),  '08:00:00', '17:00:00', TRUE, CURRENT_TIMESTAMP, NULL),
(UUID_TO_BIN('8b000000-0000-0000-0000-00000000000a'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3'), DATE_ADD(CURDATE(), INTERVAL 3 DAY),  '08:00:00', '17:00:00', TRUE, CURRENT_TIMESTAMP, NULL),
(UUID_TO_BIN('8b000000-0000-0000-0000-00000000000b'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3'), DATE_ADD(CURDATE(), INTERVAL 4 DAY),  '08:00:00', '17:00:00', TRUE, CURRENT_TIMESTAMP, NULL),
(UUID_TO_BIN('8b000000-0000-0000-0000-00000000000c'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3'), DATE_ADD(CURDATE(), INTERVAL 5 DAY),  '08:00:00', '17:00:00', TRUE, CURRENT_TIMESTAMP, NULL),
(UUID_TO_BIN('8b000000-0000-0000-0000-00000000000d'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3'), DATE_ADD(CURDATE(), INTERVAL 6 DAY),  '08:00:00', '17:00:00', TRUE, CURRENT_TIMESTAMP, NULL),
(UUID_TO_BIN('8b000000-0000-0000-0000-00000000000e'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3'), DATE_ADD(CURDATE(), INTERVAL 7 DAY),  '08:00:00', '17:00:00', TRUE, CURRENT_TIMESTAMP, NULL);

-- 5. Seed Appointment Notification Logs (Demo US-13: Nhắc lịch hẹn)
INSERT INTO appointment_notification_logs (id, appointment_id, patient_id, notification_type, channel, content, status, attempted_at, sent_at, failure_reason, created_at) VALUES
(UUID_TO_BIN('7a000000-0000-0000-0000-000000000001'), UUID_TO_BIN('cccccccc-cccc-cccc-cccc-ccccccccc002'), UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb002'), 'APPOINTMENT_REMINDER', 'SMS', 'Nhắc hẹn: Quý khách Tran Thi Binh có lịch hẹn khám ngày mai lúc 08:30 tại Phòng 101, PK Bệnh Án Số.', 'SENT', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 2 HOUR), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 2 HOUR), NULL, CURRENT_TIMESTAMP),
(UUID_TO_BIN('7a000000-0000-0000-0000-000000000002'), UUID_TO_BIN('cccccccc-cccc-cccc-cccc-ccccccccc003'), UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb003'), 'APPOINTMENT_REMINDER', 'EMAIL', 'Kính gửi Le Minh Chau, hệ thống xin thông báo lịch hẹn khám chuyên khoa của quý khách vào 2 ngày tới.', 'SENT', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 HOUR), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 HOUR), NULL, CURRENT_TIMESTAMP);
