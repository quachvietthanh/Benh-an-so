-- =====================================================
-- V11 - Service Catalog and Effective-dated Prices
-- =====================================================

CREATE TABLE service_catalog (
    id BINARY(16) NOT NULL,
    service_code VARCHAR(30) NOT NULL,
    service_name VARCHAR(150) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_service_catalog PRIMARY KEY (id),
    CONSTRAINT uk_service_catalog_code UNIQUE (service_code)
);

CREATE INDEX idx_service_catalog_name
    ON service_catalog(service_name);

CREATE INDEX idx_service_catalog_active
    ON service_catalog(active);

CREATE TABLE service_price (
    id BINARY(16) NOT NULL,
    service_catalog_id BINARY(16) NOT NULL,
    price DECIMAL(15, 2) NOT NULL,
    effective_from DATE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by BINARY(16) NOT NULL,

    CONSTRAINT pk_service_price PRIMARY KEY (id),

    CONSTRAINT uk_service_price_effective_from
        UNIQUE (service_catalog_id, effective_from),

    CONSTRAINT fk_service_price_catalog
        FOREIGN KEY (service_catalog_id)
        REFERENCES service_catalog(id),

    CONSTRAINT fk_service_price_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id),

    CONSTRAINT chk_service_price_non_negative
        CHECK (price >= 0)
);
