-- =====================================================
-- V46__fix_appointment_status_enum.sql
-- Fix legacy/inconsistent appointment status to ensure
-- compatibility with AppointmentStatus enum (SCHEDULED)
-- =====================================================

UPDATE appointments 
SET status = 'SCHEDULED' 
WHERE status = 'PENDING';
