-- =====================================================
-- V52__add_clinical_result_reference_snapshot.sql
-- Snapshot the resolved numeric reference bounds on results and
-- history so a later threshold change never rewrites the meaning
-- of an existing clinical result (NCL-04-CN-013 / TC-03).
-- =====================================================

ALTER TABLE clinical_results
    ADD COLUMN lower_bound DECIMAL(18, 4) NULL,
    ADD COLUMN upper_bound DECIMAL(18, 4) NULL;

ALTER TABLE clinical_result_histories
    ADD COLUMN old_lower_bound DECIMAL(18, 4) NULL,
    ADD COLUMN new_lower_bound DECIMAL(18, 4) NULL,
    ADD COLUMN old_upper_bound DECIMAL(18, 4) NULL,
    ADD COLUMN new_upper_bound DECIMAL(18, 4) NULL;
