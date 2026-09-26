-- =====================================================
-- V75__create_medication_returns.sql
-- NCL-06-CN-009: Trả lại thuốc và hủy phiếu cấp phát.
-- Tracks how much of each dispense allocation has been returned and
-- stores an auditable return record (original batch, quantity, reason,
-- actor, timestamp). Stock is restored to the original batch.
-- =====================================================

ALTER TABLE prescription_dispense_items
    ADD COLUMN returned_quantity INT NOT NULL DEFAULT 0;

ALTER TABLE prescription_dispense_items
    ADD CONSTRAINT chk_prescription_dispense_items_returned_quantity
        CHECK (returned_quantity >= 0 AND returned_quantity <= dispensed_quantity);

CREATE TABLE medication_returns (
    id BINARY(16) NOT NULL,
    prescription_id BINARY(16) NOT NULL,
    prescription_item_id BINARY(16) NOT NULL,
    dispense_item_id BINARY(16) NOT NULL,
    medicine_id BINARY(16) NOT NULL,
    medicine_batch_id BINARY(16) NOT NULL,
    returned_quantity INT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    returned_by BINARY(16) NOT NULL,
    returned_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_medication_returns PRIMARY KEY (id),
    CONSTRAINT fk_medication_returns_prescription
        FOREIGN KEY (prescription_id) REFERENCES prescriptions(id),
    CONSTRAINT fk_medication_returns_prescription_item
        FOREIGN KEY (prescription_item_id) REFERENCES prescription_items(id),
    CONSTRAINT fk_medication_returns_dispense_item
        FOREIGN KEY (dispense_item_id) REFERENCES prescription_dispense_items(id),
    CONSTRAINT fk_medication_returns_medicine
        FOREIGN KEY (medicine_id) REFERENCES medicines(id),
    CONSTRAINT fk_medication_returns_batch
        FOREIGN KEY (medicine_batch_id) REFERENCES medicine_batches(id),
    CONSTRAINT fk_medication_returns_returned_by
        FOREIGN KEY (returned_by) REFERENCES users(id),
    CONSTRAINT chk_medication_returns_quantity CHECK (returned_quantity > 0),
    CONSTRAINT chk_medication_returns_reason CHECK (CHAR_LENGTH(TRIM(reason)) > 0)
);

CREATE INDEX idx_medication_returns_prescription
    ON medication_returns(prescription_id);

CREATE INDEX idx_medication_returns_dispense_item
    ON medication_returns(dispense_item_id);

CREATE INDEX idx_medication_returns_returned_at
    ON medication_returns(returned_at);
