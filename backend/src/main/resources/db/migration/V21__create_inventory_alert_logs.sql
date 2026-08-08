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
