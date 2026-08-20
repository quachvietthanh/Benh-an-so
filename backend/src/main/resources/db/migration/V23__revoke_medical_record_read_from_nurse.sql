-- =====================================================
-- V23__revoke_medical_record_read_from_nurse.sql
-- NCL-10-CN-004 (Xem bệnh án trên thiết bị di động) / QTN-01:
-- viewing medical records is DOCTOR-only. Revoke MEDICAL_RECORD_READ
-- from NURSE so non-doctor roles receive HTTP 403.
-- Idempotent: deleting non-existent rows is a no-op.
-- =====================================================

DELETE rp
FROM role_permissions rp
INNER JOIN permissions p ON p.id = rp.permission_id
WHERE rp.role_id = UUID_TO_BIN('33333333-3333-3333-3333-333333333333')
  AND p.code = 'MEDICAL_RECORD_READ';
