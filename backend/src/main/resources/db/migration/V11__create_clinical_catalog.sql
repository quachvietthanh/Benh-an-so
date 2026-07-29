-- =====================================================
-- V11 - Clinical Orders, Results and Attachments
-- =====================================================

-- ===========================
-- Clinical Service Catalog
-- ===========================

CREATE TABLE clinical_service_catalog (
    id BINARY(16) NOT NULL,

    service_code VARCHAR(30) NOT NULL,
    service_name VARCHAR(150) NOT NULL,
    service_type VARCHAR(30) NOT NULL,

    result_data_type VARCHAR(30) NOT NULL,
    unit VARCHAR(50) NULL,
    reference_range VARCHAR(255) NULL,

    description TEXT NULL,
    active BOOLEAN NOT NULL,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_clinical_service_catalog
        PRIMARY KEY (id),

    CONSTRAINT uk_clinical_service_code
        UNIQUE (service_code)
);

CREATE INDEX idx_clinical_service_name
    ON clinical_service_catalog(service_name);

CREATE INDEX idx_clinical_service_type
    ON clinical_service_catalog(service_type);

CREATE INDEX idx_clinical_service_active
    ON clinical_service_catalog(active);


-- ===========================
-- Clinical Orders
-- ===========================

CREATE TABLE clinical_orders (
    id BINARY(16) NOT NULL,
    order_code VARCHAR(30) NOT NULL,

    visit_id BINARY(16) NOT NULL,
    medical_record_id BINARY(16) NOT NULL,
    patient_id BINARY(16) NOT NULL,

    ordered_by BINARY(16) NOT NULL,
    clinical_reason TEXT NULL,

    status VARCHAR(30) NOT NULL,

    ordered_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NULL,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_clinical_orders
        PRIMARY KEY (id),

    CONSTRAINT uk_clinical_orders_code
        UNIQUE (order_code),

    CONSTRAINT fk_orders_visit
        FOREIGN KEY (visit_id)
        REFERENCES visits(id),

    CONSTRAINT fk_orders_record
        FOREIGN KEY (medical_record_id)
        REFERENCES medical_records(id),

    CONSTRAINT fk_orders_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(id),

    CONSTRAINT fk_orders_user
        FOREIGN KEY (ordered_by)
        REFERENCES users(id)
);

CREATE INDEX idx_orders_visit
    ON clinical_orders(visit_id);

CREATE INDEX idx_orders_record
    ON clinical_orders(medical_record_id);

CREATE INDEX idx_orders_patient
    ON clinical_orders(patient_id);

CREATE INDEX idx_orders_status
    ON clinical_orders(status);

CREATE INDEX idx_orders_ordered_at
    ON clinical_orders(ordered_at);


-- ===========================
-- Clinical Order Items
-- ===========================

CREATE TABLE clinical_order_items (
    id BINARY(16) NOT NULL,

    clinical_order_id BINARY(16) NOT NULL,
    clinical_service_id BINARY(16) NOT NULL,

    service_code VARCHAR(30) NOT NULL,
    service_name VARCHAR(150) NOT NULL,

    instruction TEXT NULL,
    status VARCHAR(30) NOT NULL,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_clinical_order_items
        PRIMARY KEY (id),

    CONSTRAINT uk_order_items_service
        UNIQUE (
            clinical_order_id,
            clinical_service_id
        ),

    CONSTRAINT fk_items_order
        FOREIGN KEY (clinical_order_id)
        REFERENCES clinical_orders(id),

    CONSTRAINT fk_items_service
        FOREIGN KEY (clinical_service_id)
        REFERENCES clinical_service_catalog(id)
);

CREATE INDEX idx_items_order
    ON clinical_order_items(clinical_order_id);

CREATE INDEX idx_items_service
    ON clinical_order_items(clinical_service_id);

CREATE INDEX idx_items_status
    ON clinical_order_items(status);


-- ===========================
-- Clinical Results
-- ===========================

CREATE TABLE clinical_results (
    id BINARY(16) NOT NULL,

    clinical_order_item_id BINARY(16) NOT NULL,
    visit_id BINARY(16) NOT NULL,

    result_type VARCHAR(30) NOT NULL,

    numeric_value DECIMAL(18, 4) NULL,
    text_value TEXT NULL,

    unit VARCHAR(50) NULL,
    reference_range VARCHAR(255) NULL,
    abnormal_flag VARCHAR(30) NOT NULL,

    conclusion TEXT NULL,
    status VARCHAR(30) NOT NULL,

    entered_by BINARY(16) NOT NULL,
    entered_at TIMESTAMP NOT NULL,

    updated_by BINARY(16) NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_clinical_results
        PRIMARY KEY (id),

    CONSTRAINT uk_results_order_item
        UNIQUE (clinical_order_item_id),

    CONSTRAINT fk_results_item
        FOREIGN KEY (clinical_order_item_id)
        REFERENCES clinical_order_items(id),

    CONSTRAINT fk_results_visit
        FOREIGN KEY (visit_id)
        REFERENCES visits(id),

    CONSTRAINT fk_results_entered_by
        FOREIGN KEY (entered_by)
        REFERENCES users(id),

    CONSTRAINT fk_results_updated_by
        FOREIGN KEY (updated_by)
        REFERENCES users(id)
);

CREATE INDEX idx_results_visit
    ON clinical_results(visit_id);

CREATE INDEX idx_results_status
    ON clinical_results(status);

CREATE INDEX idx_results_entered_at
    ON clinical_results(entered_at);


-- ===========================
-- Clinical Result Histories
-- ===========================

CREATE TABLE clinical_result_histories (
    id BINARY(16) NOT NULL,
    clinical_result_id BINARY(16) NOT NULL,

    old_result_type VARCHAR(30) NULL,
    new_result_type VARCHAR(30) NULL,

    old_numeric_value DECIMAL(18, 4) NULL,
    new_numeric_value DECIMAL(18, 4) NULL,

    old_text_value TEXT NULL,
    new_text_value TEXT NULL,

    old_unit VARCHAR(50) NULL,
    new_unit VARCHAR(50) NULL,

    old_reference_range VARCHAR(255) NULL,
    new_reference_range VARCHAR(255) NULL,

    old_abnormal_flag VARCHAR(30) NULL,
    new_abnormal_flag VARCHAR(30) NULL,

    old_conclusion TEXT NULL,
    new_conclusion TEXT NULL,

    old_status VARCHAR(30) NULL,
    new_status VARCHAR(30) NULL,

    change_reason TEXT NOT NULL,

    changed_by BINARY(16) NOT NULL,
    changed_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_clinical_result_histories
        PRIMARY KEY (id),

    CONSTRAINT fk_histories_result
        FOREIGN KEY (clinical_result_id)
        REFERENCES clinical_results(id),

    CONSTRAINT fk_histories_user
        FOREIGN KEY (changed_by)
        REFERENCES users(id)
);

CREATE INDEX idx_histories_result_time
    ON clinical_result_histories(
        clinical_result_id,
        changed_at
    );


-- ===========================
-- Medical Attachments
-- ===========================

CREATE TABLE medical_attachments (
    id BINARY(16) NOT NULL,

    visit_id BINARY(16) NOT NULL,
    medical_record_id BINARY(16) NULL,
    clinical_result_id BINARY(16) NULL,

    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,

    storage_key VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    checksum VARCHAR(128) NULL,

    attachment_type VARCHAR(30) NOT NULL,

    uploaded_by BINARY(16) NOT NULL,
    uploaded_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_medical_attachments
        PRIMARY KEY (id),

    CONSTRAINT uk_attachments_storage_key
        UNIQUE (storage_key),

    CONSTRAINT fk_attachments_visit
        FOREIGN KEY (visit_id)
        REFERENCES visits(id),

    CONSTRAINT fk_attachments_record
        FOREIGN KEY (medical_record_id)
        REFERENCES medical_records(id),

    CONSTRAINT fk_attachments_result
        FOREIGN KEY (clinical_result_id)
        REFERENCES clinical_results(id),

    CONSTRAINT fk_attachments_user
        FOREIGN KEY (uploaded_by)
        REFERENCES users(id)
);

CREATE INDEX idx_attachments_visit
    ON medical_attachments(visit_id);

CREATE INDEX idx_attachments_record
    ON medical_attachments(medical_record_id);

CREATE INDEX idx_attachments_result
    ON medical_attachments(clinical_result_id);

CREATE INDEX idx_attachments_type
    ON medical_attachments(attachment_type);

CREATE INDEX idx_attachments_uploaded_at
    ON medical_attachments(uploaded_at);