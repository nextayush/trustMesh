-- V2__add_merkle_root.sql
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS merkle_root BYTEA;
ALTER TABLE audit_events ADD COLUMN IF NOT EXISTS merkle_root BYTEA;
