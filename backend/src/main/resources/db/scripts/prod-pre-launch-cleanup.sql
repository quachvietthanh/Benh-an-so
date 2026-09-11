-- =============================================================================
-- prod-pre-launch-cleanup.sql
-- Enterprise Database Sanitization Runbook Script (Pre-Launch / Go-Live)
-- Purpose: Completely purges all seeded demo transactions and demo entities
--          before the hospital officially commences live clinical operations.
-- Usage: Run by DBA once during the Go-Live transition window.
-- =============================================================================

SET @ORIGINAL_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

START TRANSACTION;

-- 1. Purge Demo Billing & Invoices
DELETE FROM invoice_lines WHERE invoice_id = UUID_TO_BIN('23100000-0000-0000-0000-000000000001');
DELETE FROM invoices WHERE id = UUID_TO_BIN('23100000-0000-0000-0000-000000000001');
DELETE FROM payments WHERE id = UUID_TO_BIN('23000000-0000-0000-0000-000000000001');

-- 2. Purge Demo Prescriptions & Warnings
DELETE FROM prescription_interconnection_logs WHERE HEX(prescription_id) LIKE '1620000000000000000000000000000%';
DELETE FROM prescription_warning_logs WHERE HEX(prescription_id) LIKE '1620000000000000000000000000000%';
DELETE FROM prescription_amendments WHERE HEX(prescription_id) LIKE '1620000000000000000000000000000%';
DELETE FROM prescription_items WHERE HEX(prescription_id) LIKE '1620000000000000000000000000000%';
DELETE FROM prescriptions WHERE HEX(id) LIKE '1620000000000000000000000000000%';

-- 3. Purge Demo Clinical Visits, Orders & Medical Records
DELETE FROM prescription_allergy_warning_logs WHERE HEX(patient_id) LIKE 'BBBBBBBBBBBBBBBBBBBBBBBBB0%';
DELETE FROM patient_allergy_change_logs WHERE HEX(patient_id) LIKE 'BBBBBBBBBBBBBBBBBBBBBBBBB0%';
DELETE FROM patient_allergies WHERE HEX(patient_id) LIKE 'BBBBBBBBBBBBBBBBBBBBBBBBB0%';
DELETE FROM post_care_logs WHERE HEX(patient_id) LIKE 'BBBBBBBBBBBBBBBBBBBBBBBBB0%';
DELETE FROM follow_up_reminders WHERE HEX(patient_id) LIKE 'BBBBBBBBBBBBBBBBBBBBBBBBB0%';
DELETE FROM clinical_results WHERE HEX(visit_id) LIKE 'D000000000000000000000000000000%';
DELETE FROM clinical_order_items WHERE clinical_order_id IN (SELECT id FROM clinical_orders WHERE HEX(visit_id) LIKE 'D000000000000000000000000000000%');
DELETE FROM clinical_orders WHERE HEX(visit_id) LIKE 'D000000000000000000000000000000%';
DELETE FROM medical_attachments WHERE HEX(visit_id) LIKE 'D000000000000000000000000000000%';
DELETE FROM medical_record_access_logs WHERE HEX(visit_id) LIKE 'D000000000000000000000000000000%';
DELETE FROM medical_record_diagnoses WHERE HEX(medical_record_id) LIKE 'E000000000000000000000000000000%';
DELETE FROM medical_records WHERE HEX(id) LIKE 'E000000000000000000000000000000%';
DELETE FROM queue_items WHERE HEX(patient_id) LIKE 'BBBBBBBBBBBBBBBBBBBBBBBBB0%';
DELETE FROM visits WHERE HEX(id) LIKE 'D000000000000000000000000000000%';

-- 4. Purge Demo Appointments & Schedules
DELETE FROM appointment_notification_logs WHERE HEX(appointment_id) LIKE 'CCCCCCCCCCCCCCCCCCCCCCCCC0%';
DELETE FROM appointments WHERE HEX(patient_id) LIKE 'BBBBBBBBBBBBBBBBBBBBBBBBB0%' OR HEX(id) LIKE 'CCCCCCCCCCCCCCCCCCCCCCCCC0%';
DELETE FROM doctor_schedules WHERE HEX(id) LIKE '8B00000000000000000000000000000%';
DELETE FROM doctor_room_assignments WHERE HEX(id) LIKE '9100000000000000000000000000000%';

-- 5. Purge Demo Patients
DELETE FROM patient_change_logs WHERE HEX(patient_id) LIKE 'BBBBBBBBBBBBBBBBBBBBBBBBB0%';
DELETE FROM patients WHERE HEX(id) LIKE 'BBBBBBBBBBBBBBBBBBBBBBBBB0%';

COMMIT;

SET FOREIGN_KEY_CHECKS = @ORIGINAL_FOREIGN_KEY_CHECKS;

-- Verification query
SELECT 'Patients remaining' AS entity, COUNT(*) AS `count` FROM patients
UNION ALL
SELECT 'Appointments remaining', COUNT(*) FROM appointments
UNION ALL
SELECT 'Invoices remaining', COUNT(*) FROM invoices
UNION ALL
SELECT 'Payments remaining', COUNT(*) FROM payments;
