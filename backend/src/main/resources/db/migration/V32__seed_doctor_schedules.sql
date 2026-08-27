-- =====================================================
-- V32__seed_doctor_schedules.sql
-- Seed sample working schedules so online appointment booking (NCL-14-CN-003 / QTN-04)
-- has valid slots to compute and book against.
-- =====================================================

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
