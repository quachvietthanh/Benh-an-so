-- =====================================================
-- V48__seed_user_stories_demo_data.sql
-- Seed comprehensive demo data for 78 User Stories (Non-prod only):
-- - Additional doctor account (Dr. Le Hoang Nam)
-- - Sample patient medication allergies (US-26, US-27)
-- - Sample follow-up reminders (US-55, US-56)
-- - Sample post-care interaction logs (US-57, US-58)
-- - Sample security alerts (US-75, US-76)
-- Fail-Secure Guard: Only executes when '${seed_demo}' = 'true'.
-- In production or default environments, this migration executes 0 inserts.
-- =====================================================

-- 1. Additional Doctor Account (Explicitly gated)
INSERT INTO users (id, username, password_hash, full_name, email, phone, role_id, active, must_change_password, last_login_at, created_at)
SELECT UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa14'),
       'doctor_new',
       '$2a$10$OY5a1YZ/5Iaz2PcEKjfOveEyy3FVXm7ei9OxTW6jPMyap/Hlk.5sK',
       'Dr. Le Hoang Nam',
       'nam.lh@benhsoan.com',
       '0901000008',
       UUID_TO_BIN('22222222-2222-2222-2222-222222222222'),
       TRUE,
       FALSE,
       NULL,
       CURRENT_TIMESTAMP
WHERE LOWER(TRIM('${seed_demo}')) = 'true'
  AND NOT EXISTS (SELECT 1 FROM users WHERE username = 'doctor_new');

-- 2. Sample Patient Medication Allergies (Patient BN000001, BN000002)
INSERT INTO patient_allergies (id, patient_id, allergen_type, allergen_name, normalized_allergen_name, severity, reaction, notes, active, created_by, created_at, updated_by, updated_at)
SELECT UUID_TO_BIN('ea000000-0000-0000-0000-000000000001'),
       UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001'),
       'MEDICATION',
       'Aspirin',
       'aspirin',
       'SEVERE',
       'Noi me day, kho tho nhe',
       'Tien su di ung NSAIDs tu 2023',
       TRUE,
       UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'),
       CURRENT_TIMESTAMP,
       NULL,
       CURRENT_TIMESTAMP
WHERE LOWER(TRIM('${seed_demo}')) = 'true'
  AND NOT EXISTS (SELECT 1 FROM patient_allergies WHERE id = UUID_TO_BIN('ea000000-0000-0000-0000-000000000001'));

INSERT INTO patient_allergies (id, patient_id, allergen_type, allergen_name, normalized_allergen_name, severity, reaction, notes, active, created_by, created_at, updated_by, updated_at)
SELECT UUID_TO_BIN('ea000000-0000-0000-0000-000000000002'),
       UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb002'),
       'MEDICATION',
       'Amoxicillin',
       'amoxicillin',
       'MODERATE',
       'Phan ban do toan than',
       'Phat hien khi dung dieu tri viem hong',
       TRUE,
       UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'),
       CURRENT_TIMESTAMP,
       NULL,
       CURRENT_TIMESTAMP
WHERE LOWER(TRIM('${seed_demo}')) = 'true'
  AND NOT EXISTS (SELECT 1 FROM patient_allergies WHERE id = UUID_TO_BIN('ea000000-0000-0000-0000-000000000002'));

-- 3. Sample Follow-up Reminders (US-55, US-56)
INSERT INTO follow_up_reminders (id, patient_id, visit_id, appointment_id, follow_up_date, remind_at, reminder_type, status, notes, created_by, created_at)
SELECT UUID_TO_BIN('fa000000-0000-0000-0000-000000000001'),
       UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001'),
       NULL,
       NULL,
       CURRENT_DATE,
       CURRENT_TIMESTAMP,
       'REVISIT',
       'PENDING',
       'Kinh moi quy khach Nguyen Van An den tai kham noi khoa dinh ky.',
       UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'),
       CURRENT_TIMESTAMP
WHERE LOWER(TRIM('${seed_demo}')) = 'true'
  AND NOT EXISTS (SELECT 1 FROM follow_up_reminders WHERE id = UUID_TO_BIN('fa000000-0000-0000-0000-000000000001'));

-- 4. Sample Post Care Logs (US-57, US-58)
INSERT INTO post_care_logs (id, patient_id, reminder_id, visit_id, contact_channel, contacted_at, patient_condition, care_notes, contact_outcome, performed_by, created_at)
SELECT UUID_TO_BIN('fb000000-0000-0000-0000-000000000001'),
       UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001'),
       UUID_TO_BIN('fa000000-0000-0000-0000-000000000001'),
       NULL,
       'PHONE',
       CURRENT_TIMESTAMP,
       'STABLE',
       'Benh nhan da giam sot, tuan thu dung lieu thuoc da ke.',
       'REACHED',
       UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'),
       CURRENT_TIMESTAMP
WHERE LOWER(TRIM('${seed_demo}')) = 'true'
  AND NOT EXISTS (SELECT 1 FROM post_care_logs WHERE id = UUID_TO_BIN('fb000000-0000-0000-0000-000000000001'));

-- 5. Sample Security Alerts (US-75, US-76)
INSERT INTO security_alerts (id, user_id, alert_type, severity, description, access_count, window_start, window_end, status, created_at)
SELECT UUID_TO_BIN('fc000000-0000-0000-0000-000000000001'),
       UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'),
        'THRESHOLD_EXCEEDED',
        'HIGH',
        'Phat hien vuot nguong 5 lan truy cap bat thuong tu IP 192.168.1.100',
        5,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        'UNREAD',
        CURRENT_TIMESTAMP
WHERE LOWER(TRIM('${seed_demo}')) = 'true'
  AND NOT EXISTS (SELECT 1 FROM security_alerts WHERE id = UUID_TO_BIN('fc000000-0000-0000-0000-000000000001'));
