-- V1__create_shipments.sql
CREATE TABLE IF NOT EXISTS shipments (
    shipment_id VARCHAR(100) PRIMARY KEY,
    content_identifier VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    current_owner VARCHAR(42) NOT NULL,
    creator VARCHAR(42) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_events (
    id SERIAL PRIMARY KEY,
    shipment_id VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    content_identifier VARCHAR(255) NOT NULL,
    updater VARCHAR(42) NOT NULL,
    tx_hash VARCHAR(66),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
