-- =====================================================
-- V51__create_clinical_reference_ranges.sql
-- Reference thresholds for clinical services (NCL-04-CN-013).
-- A child table of clinical_service_catalog, supporting
-- gender- and age-specific numeric reference ranges.
-- =====================================================

CREATE TABLE clinical_reference_ranges (
    id BINARY(16) NOT NULL,

    clinical_service_id BINARY(16) NOT NULL,

    gender VARCHAR(10) NULL,

    min_age INT NULL,
    max_age INT NULL,

    lower_bound DECIMAL(18, 4) NULL,
    upper_bound DECIMAL(18, 4) NULL,

    active BOOLEAN NOT NULL,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_clinical_reference_ranges
        PRIMARY KEY (id),

    CONSTRAINT chk_reference_range_bounds
        CHECK (
            lower_bound IS NULL
            OR upper_bound IS NULL
            OR lower_bound <= upper_bound
        ),

    CONSTRAINT fk_reference_ranges_service
        FOREIGN KEY (clinical_service_id)
        REFERENCES clinical_service_catalog(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_reference_ranges_service
    ON clinical_reference_ranges(clinical_service_id, active);

CREATE INDEX idx_reference_ranges_lookup
    ON clinical_reference_ranges(clinical_service_id, gender, min_age, max_age);
