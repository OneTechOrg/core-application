-- V19__add_keycloak_id_to_profiles.sql
-- Adds keycloak_id column to drivers and passengers tables for identity linking.

ALTER TABLE drivers ADD COLUMN keycloak_id VARCHAR(255) UNIQUE;
ALTER TABLE passengers ADD COLUMN keycloak_id VARCHAR(255) UNIQUE;

-- Add indexes for performance on profile resolution
CREATE INDEX idx_drivers_keycloak_id ON drivers(keycloak_id);
CREATE INDEX idx_passengers_keycloak_id ON passengers(keycloak_id);
