-- =====================================================
-- V101__add_guardian_link_review_notifications.sql
-- NCL-14-CN-010 TC-03: Ra soat lien ket giam ho khi nguoi phu thuoc du 18 tuoi.
--
-- Extends the existing patient-portal notification store (NCL-14-CN-008, V87) with
-- exactly one new category so the affected accounts can be told that a guardian
-- link should be reviewed. No new notification subsystem is introduced.
--
-- The guardian relationship itself is NOT removed by this migration or by the
-- sweep that writes these notifications: TC-03 says the link is "proposed for
-- removal", so removal stays with the existing authorized staff flow
-- (PUT /patients/{patientId} with PATIENT_UPDATE, or transitionToAdult).
-- =====================================================

-- 1. Allow the new notification category. The original CHECK constraint (V87)
--    listed only the three NCL-14-CN-008 categories.
ALTER TABLE patient_portal_notifications
    DROP CHECK chk_ppn_type;

ALTER TABLE patient_portal_notifications
    ADD CONSTRAINT chk_ppn_type CHECK (
        type IN (
            'APPOINTMENT_REMINDER',
            'APPOINTMENT_CHANGED',
            'LAB_RESULT_AVAILABLE',
            'GUARDIAN_LINK_REVIEW'
        )
    );

-- 2. Idempotency key: the dependent whose adulthood triggered the review. Each
--    recipient receives exactly one review notification per dependent, so repeated
--    scheduler runs never create duplicates. NULL for every other category, which
--    MySQL and H2 both treat as distinct so the other unique indexes are unaffected.
ALTER TABLE patient_portal_notifications
    ADD COLUMN guardian_review_dependent_patient_id BINARY(16) NULL;

ALTER TABLE patient_portal_notifications
    ADD CONSTRAINT fk_ppn_guardian_review_dependent
    FOREIGN KEY (guardian_review_dependent_patient_id) REFERENCES patients(id)
    ON DELETE CASCADE;

CREATE UNIQUE INDEX uk_ppn_guardian_review
    ON patient_portal_notifications(patient_id, type, guardian_review_dependent_patient_id);

-- 3. Sweep support: find adult dependents that still carry a guardian link without
--    scanning the whole table. status/active keep merged and deactivated dependents
--    out of the review sweep.
CREATE INDEX idx_patients_guardian_user_status
    ON patients(guardian_user_id, status, active);
