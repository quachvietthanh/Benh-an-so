-- =====================================================
-- V19__create_dispense_allocation_and_stock_movements.sql
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
