-- =====================================================
-- V20__create_inventory_receipt_tables.sql
-- Stock receipt / nhập kho thuốc: medicine batches,
-- inventory receipts and receipt items
-- MySQL 8.x
-- =====================================================

-- ===========================
-- Medicine Batches
-- ===========================

CREATE TABLE medicine_batches (
    id BINARY(16) NOT NULL,
    medicine_id BINARY(16) NOT NULL,
    batch_number VARCHAR(50) NOT NULL,
    expiry_date DATE NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_medicine_batches PRIMARY KEY (id),
    CONSTRAINT fk_medicine_batches_medicine
        FOREIGN KEY (medicine_id)
        REFERENCES medicines(id),
    CONSTRAINT chk_medicine_batches_quantity CHECK (quantity >= 0),
    CONSTRAINT chk_medicine_batches_status CHECK (
        status IN ('ACTIVE', 'DEPLETED', 'EXPIRED')
    )
);

CREATE INDEX idx_medicine_batches_medicine
    ON medicine_batches(medicine_id);

CREATE INDEX idx_medicine_batches_batch_number
    ON medicine_batches(batch_number);

CREATE UNIQUE INDEX uk_medicine_batches_medicine_batch
    ON medicine_batches(medicine_id, batch_number);

-- ===========================
-- Inventory Receipts
-- ===========================

CREATE TABLE inventory_receipts (
    id BINARY(16) NOT NULL,
    received_by BINARY(16) NOT NULL,
    received_at TIMESTAMP NOT NULL,
    note TEXT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_inventory_receipts PRIMARY KEY (id),
    CONSTRAINT fk_inventory_receipts_received_by
        FOREIGN KEY (received_by)
        REFERENCES users(id)
);

CREATE INDEX idx_inventory_receipts_received_by
    ON inventory_receipts(received_by);

CREATE INDEX idx_inventory_receipts_received_at
    ON inventory_receipts(received_at);

-- ===========================
-- Inventory Receipt Items
-- ===========================

CREATE TABLE inventory_receipt_items (
    id BINARY(16) NOT NULL,
    inventory_receipt_id BINARY(16) NOT NULL,
    medicine_id BINARY(16) NOT NULL,
    medicine_batch_id BINARY(16) NOT NULL,
    quantity INT NOT NULL,
    import_price DECIMAL(15, 2) NOT NULL,
    total_value DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_inventory_receipt_items PRIMARY KEY (id),
    CONSTRAINT fk_inventory_receipt_items_receipt
        FOREIGN KEY (inventory_receipt_id)
        REFERENCES inventory_receipts(id),
    CONSTRAINT fk_inventory_receipt_items_medicine
        FOREIGN KEY (medicine_id)
        REFERENCES medicines(id),
    CONSTRAINT fk_inventory_receipt_items_batch
        FOREIGN KEY (medicine_batch_id)
        REFERENCES medicine_batches(id),
    CONSTRAINT chk_inventory_receipt_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_inventory_receipt_items_import_price CHECK (import_price >= 0),
    CONSTRAINT chk_inventory_receipt_items_total_value CHECK (total_value >= 0)
);

CREATE INDEX idx_inventory_receipt_items_receipt
    ON inventory_receipt_items(inventory_receipt_id);

CREATE INDEX idx_inventory_receipt_items_medicine
    ON inventory_receipt_items(medicine_id);

CREATE INDEX idx_inventory_receipt_items_batch
    ON inventory_receipt_items(medicine_batch_id);


-- =====================================================
-- V21__create_dispense_allocation_and_stock_movements.sql
-- Dispense allocation / cấp phát theo lô và nhật ký biến động kho
-- MySQL 8.x
-- =====================================================

-- ===========================
-- Prescription Dispense Items
-- ===========================

CREATE TABLE prescription_dispense_items (
    id BINARY(16) NOT NULL,
    prescription_id BINARY(16) NOT NULL,
    prescription_item_id BINARY(16) NOT NULL,
    medicine_id BINARY(16) NOT NULL,
    medicine_batch_id BINARY(16) NOT NULL,
    dispensed_quantity INT NOT NULL,
    dispensed_by BINARY(16) NOT NULL,
    dispensed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_prescription_dispense_items PRIMARY KEY (id),
    CONSTRAINT fk_prescription_dispense_items_prescription
        FOREIGN KEY (prescription_id)
        REFERENCES prescriptions(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_prescription_dispense_items_prescription_item
        FOREIGN KEY (prescription_item_id)
        REFERENCES prescription_items(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_prescription_dispense_items_medicine
        FOREIGN KEY (medicine_id)
        REFERENCES medicines(id),
    CONSTRAINT fk_prescription_dispense_items_batch
        FOREIGN KEY (medicine_batch_id)
        REFERENCES medicine_batches(id),
    CONSTRAINT fk_prescription_dispense_items_dispensed_by
        FOREIGN KEY (dispensed_by)
        REFERENCES users(id),
    CONSTRAINT chk_prescription_dispense_items_quantity CHECK (dispensed_quantity > 0)
);

CREATE INDEX idx_prescription_dispense_items_prescription
    ON prescription_dispense_items(prescription_id);

CREATE INDEX idx_prescription_dispense_items_prescription_item
    ON prescription_dispense_items(prescription_item_id);

CREATE INDEX idx_prescription_dispense_items_medicine
    ON prescription_dispense_items(medicine_id);

CREATE INDEX idx_prescription_dispense_items_batch
    ON prescription_dispense_items(medicine_batch_id);

CREATE INDEX idx_prescription_dispense_items_dispensed_at
    ON prescription_dispense_items(dispensed_at);

-- ===========================
-- Stock Movements
-- ===========================

CREATE TABLE stock_movements (
    id BINARY(16) NOT NULL,
    medicine_id BINARY(16) NOT NULL,
    medicine_batch_id BINARY(16) NOT NULL,
    movement_type VARCHAR(30) NOT NULL,
    reference_type VARCHAR(30) NOT NULL,
    reference_id BINARY(16) NOT NULL,
    quantity_change INT NOT NULL,
    quantity_before INT NOT NULL,
    quantity_after INT NOT NULL,
    performed_by BINARY(16) NOT NULL,
    performed_at TIMESTAMP NOT NULL,
    note VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_stock_movements PRIMARY KEY (id),
    CONSTRAINT fk_stock_movements_medicine
        FOREIGN KEY (medicine_id)
        REFERENCES medicines(id),
    CONSTRAINT fk_stock_movements_batch
        FOREIGN KEY (medicine_batch_id)
        REFERENCES medicine_batches(id),
    CONSTRAINT fk_stock_movements_performed_by
        FOREIGN KEY (performed_by)
        REFERENCES users(id),
    CONSTRAINT chk_stock_movements_type CHECK (
        movement_type IN ('RECEIPT', 'DISPENSE', 'ADJUSTMENT', 'EXPIRE', 'RETURN')
    ),
    CONSTRAINT chk_stock_movements_reference_type CHECK (
        reference_type IN (
            'INVENTORY_RECEIPT',
            'PRESCRIPTION',
            'PRESCRIPTION_ITEM',
            'MANUAL_ADJUSTMENT',
            'EXPIRY_PROCESS',
            'RETURN'
        )
    ),
    CONSTRAINT chk_stock_movements_non_zero_change CHECK (quantity_change <> 0),
    CONSTRAINT chk_stock_movements_non_negative_before CHECK (quantity_before >= 0),
    CONSTRAINT chk_stock_movements_non_negative_after CHECK (quantity_after >= 0),
    CONSTRAINT chk_stock_movements_balance CHECK (quantity_after = quantity_before + quantity_change)
);

CREATE INDEX idx_stock_movements_medicine
    ON stock_movements(medicine_id);

CREATE INDEX idx_stock_movements_batch
    ON stock_movements(medicine_batch_id);

CREATE INDEX idx_stock_movements_type
    ON stock_movements(movement_type);

CREATE INDEX idx_stock_movements_reference
    ON stock_movements(reference_type, reference_id);

CREATE INDEX idx_stock_movements_performed_at
    ON stock_movements(performed_at);


CREATE TABLE inventory_alert_logs (
    id BINARY(16) NOT NULL,
    medicine_id BINARY(16) NOT NULL,
    alert_type VARCHAR(30) NOT NULL,
    threshold_value INT NOT NULL,
    observed_quantity INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP NULL,

    CONSTRAINT pk_inventory_alert_logs PRIMARY KEY (id),
    CONSTRAINT fk_inventory_alert_logs_medicine
        FOREIGN KEY (medicine_id)
        REFERENCES medicines(id),
    CONSTRAINT chk_inventory_alert_logs_type CHECK (
        alert_type IN ('LOW_STOCK')
    ),
    CONSTRAINT chk_inventory_alert_logs_threshold CHECK (threshold_value >= 0),
    CONSTRAINT chk_inventory_alert_logs_observed CHECK (observed_quantity >= 0),
    CONSTRAINT chk_inventory_alert_logs_resolved_at CHECK (
        resolved_at IS NULL OR resolved_at >= created_at
    )
);

CREATE INDEX idx_inventory_alert_logs_medicine_created
    ON inventory_alert_logs(medicine_id, created_at DESC);

CREATE INDEX idx_inventory_alert_logs_type_resolved
    ON inventory_alert_logs(alert_type, resolved_at);
