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
