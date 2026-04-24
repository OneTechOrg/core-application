-- Create Keycloak database
CREATE DATABASE keycloak;

-- Grant permissions to rappidrive user
GRANT ALL PRIVILEGES ON DATABASE keycloak TO rappidrive;

-- Connect to keycloak database and enable extensions
\c keycloak;

-- Enable required extensions for Keycloak
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";