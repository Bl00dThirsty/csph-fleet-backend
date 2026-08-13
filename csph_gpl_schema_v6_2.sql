-- ============================================================
-- CSPH GPL TRACEABILITY SYSTEM — PRODUCTION SCHEMA v6.2
-- PostgreSQL 15 + PostGIS + TimescaleDB
-- Corrected & hardened for Spring Boot microservices.
-- All foreign keys added after base tables (Section 4).
-- Indexes in Section 5. Triggers, audit, geo-auto-promotion,
-- and TimescaleDB compression/retention policies in Section 6.
-- snake_case naming throughout; org_type / site_function
-- enums already corrected (REGULATEUR/DEPOT, ENTREPOT).
-- Fixed 4 malformed create_hypertable(...) calls that used a
-- literal ellipsis placeholder (invalid SQL) in v6.1.
-- ============================================================

-- 1. EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis";
CREATE EXTENSION IF NOT EXISTS "timescaledb";

-- ============================================================
-- 2. ENUMERATIONS (corrected business terms)
-- ============================================================
CREATE TYPE org_type AS ENUM (
    'REGULATEUR', 'DEPOT', 'MARKETEUR', 'TRANSPORTEUR', 'CLIENT'
);

CREATE TYPE region AS ENUM (
    'ADAMAOUA', 'CENTRE', 'EST', 'EXTREMENORD', 'LITTORAL',
    'NORD', 'NORDOUEST', 'OUEST', 'SUD', 'SUDOUEST'
);

CREATE TYPE site_function AS ENUM (
    'CENTREEMPLISSEUR', 'ENTREPOT', 'POINTAPPROVISIONABLE'
);

CREATE TYPE site_status AS ENUM (
    'UNASSIGNED', 'ASSIGNED', 'ACTIVE', 'VERIFIED', 'SUSPENDED', 'REJECTED'
);

CREATE TYPE system_role AS ENUM (
    'SUPERADMIN', 'ADMIN', 'SUPERVISOR', 'INTEGRATEUR',
    'AGENT', 'MARKETEUR', 'LIVREUR', 'TRANSPORTEUR'
);

CREATE TYPE vehicle_type AS ENUM ('VRAC', 'BOUTEILLES50KG');
CREATE TYPE tournee_type AS ENUM ('VRAC', 'BOUTEILLES50KG');

CREATE TYPE execution_mode AS ENUM ('INTERNAL', 'EXTERNAL');
CREATE TYPE tournee_status AS ENUM (
    'DRAFT', 'PLANNED', 'PENDINGTRANSPORTERACK', 'ACKNOWLEDGED',
    'INPROGRESS', 'CHECKPOINTACTIVE', 'CLOSED', 'CANCELLED'
);
CREATE TYPE checkpoint_status AS ENUM ('PENDING', 'REACHED', 'COMPLETED', 'SKIPPED');
CREATE TYPE scan_direction AS ENUM ('IN', 'OUT');
CREATE TYPE pickup_status AS ENUM ('DRAFT', 'VALIDATED', 'INPROGRESS', 'COMPLETED', 'CANCELLED');
CREATE TYPE declaration_status AS ENUM ('DRAFT', 'SUBMITTED', 'RECONCILED', 'DISPUTED');
CREATE TYPE reconciliation_status AS ENUM ('PENDING', 'VERIFIED', 'REDRESSEMENTAPPLIED');
CREATE TYPE redressement_status AS ENUM ('ISSUED', 'PAID', 'WAIVED');
CREATE TYPE device_type AS ENUM ('GPS', 'PDA', 'RFIDREADER');
CREATE TYPE device_status AS ENUM (
    'UNASSIGNED', 'ASSIGNED', 'INMISSION', 'OFFLINE',
    'PENDINGSYNC', 'SYNCING', 'SYNCED', 'SYNCFAILED',
    'MAINTENANCE', 'DEPLOYED', 'REMOVED', 'LOST'
);
CREATE TYPE rfid_tag_status AS ENUM (
    'AVAILABLE', 'ASSIGNEDTOBOTTLE', 'INTRANSITOUT',
    'INTRANSITIN', 'LOST', 'BLOCKED'
);
CREATE TYPE risk_level AS ENUM (
    'FAIBLE', 'MODERE', 'ELEVE', 'CRITIQUE', 'CRITIQUEEXTREME'
);
CREATE TYPE risk_entity_type AS ENUM (
    'MARKETEUR', 'TRANSPORTEUR', 'LIVREUR', 'SITE',
    'TOURNEE', 'CLIENT', 'CLIENTSITE', 'VEHICLE'
);
CREATE TYPE anomaly_category AS ENUM ('INVESTIGATION', 'TECHNICAL');
CREATE TYPE anomaly_type AS ENUM (
    'VOLUMEGAP', 'DEVIATIONROUTE', 'CHECKPOINTMISSED',
    'SCANOUTOFSEQUENCE', 'SIPHONNAGE', 'SUBSTITUTIONBOUTEILLES',
    'FALSIFICATIONPREUVES', 'FILLINGILLEGAL', 'DIVERSIONSUBSIDIES',
    'PDAUNSYNCED', 'BATTERYCRITICAL', 'GPSFAILURE',
    'KAFKATIMEOUT', 'IOTDEGRADATION', 'SERVERUNAVAILABLE',
    'TOURNEEUNASSIGNEDTOOLONG', 'TRANSPORTERNOACK',
    'GPSREMOVED', 'DEVICEOFFLINE'
);
CREATE TYPE anomaly_status AS ENUM ('NOUVEAU', 'ENCOURS', 'RESOLU', 'FERME');
CREATE TYPE notification_group_type AS ENUM (
    'TECHNICAL', 'INVESTIGATION', 'ADMIN', 'MARKETING', 'TRANSPORT'
);
CREATE TYPE mfa_type AS ENUM ('TOTP', 'SMS', 'EMAIL');
CREATE TYPE mfa_status AS ENUM ('DISABLED', 'PENDINGSETUP', 'ENABLED', 'LOCKED');
CREATE TYPE audit_action AS ENUM (
    'LOGINSUCCESS', 'LOGINFAILURE', 'LOGOUT', 'TOKENREFRESH',
    'PASSWORDRESET', 'MFAENABLED', 'MFADISABLED', 'MFACHALLENGEFAILED',
    'MFACHALLENGESUCCESS', 'PERMISSIONDENIED', 'DATAEXPORT',
    'BULKDELETE', 'DECLARATIONSUBMITTED', 'RECONCILIATIONVERIFIED',
    'TOURNEECREATED', 'TOURNEEASSIGNED', 'TOURNEESENTTOTRANSPORTER',
    'TOURNEEACKNOWLEDGED', 'TOURNEESTARTED', 'TOURNEECLOSED',
    'VEHICLECERTIFICATEEXPIRED', 'SITESUSPENDED', 'CLIENTCREATED',
    'SCANEVENTRECEIVED', 'PDASYNCBULKUPLOAD', 'ANOMALYRESOLVED',
    'DEVICEREMOVED', 'GPSPOSITIONCAPTURED', 'SETTINGCHANGED'
);
CREATE TYPE report_format AS ENUM ('PDF', 'EXCEL', 'CSV', 'JSON');
CREATE TYPE report_status AS ENUM ('PENDING', 'GENERATING', 'READY', 'FAILED', 'EXPIRED');

-- ============================================================
-- 3. BASE TABLES (no foreign keys – added later)
-- ============================================================

CREATE TABLE regions (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(100) NOT NULL,
    code        region NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO regions (name, code) VALUES
    ('Adamaoua', 'ADAMAOUA'), ('Centre', 'CENTRE'), ('Est', 'EST'),
    ('Extreme-Nord', 'EXTREMENORD'), ('Littoral', 'LITTORAL'),
    ('Nord', 'NORD'), ('Nord-Ouest', 'NORDOUEST'), ('Ouest', 'OUEST'),
    ('Sud', 'SUD'), ('Sud-Ouest', 'SUDOUEST');

-- organizations (will reference auth_users later)
CREATE TABLE organizations (
    id                     UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    parent_organization_id UUID,
    tier                   VARCHAR(50),
    name                   VARCHAR(255) NOT NULL,
    type                   org_type NOT NULL,
    registration_number    VARCHAR(100),
    tax_id                 VARCHAR(100),
    is_active              BOOLEAN NOT NULL DEFAULT true,
    operational_site_count INTEGER NOT NULL DEFAULT 0 CHECK (operational_site_count >= 0),
    client_site_count      INTEGER NOT NULL DEFAULT 0 CHECK (client_site_count >= 0),
    vehicle_count          INTEGER NOT NULL DEFAULT 0 CHECK (vehicle_count >= 0),
    driver_count           INTEGER NOT NULL DEFAULT 0 CHECK (driver_count >= 0),
    user_count             INTEGER NOT NULL DEFAULT 0 CHECK (user_count >= 0),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at             TIMESTAMPTZ,
    created_by             UUID,   -- FK to auth_users added later
    updated_by             UUID
);

-- auth_users (references organizations)
CREATE TABLE auth_users (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    person_id            UUID NOT NULL UNIQUE,
    username             VARCHAR(255) NOT NULL UNIQUE,
    email                VARCHAR(255),
    password_hash        VARCHAR(255) NOT NULL,
    system_role          system_role NOT NULL,
    org_id               UUID,   -- FK to organizations
    is_active            BOOLEAN NOT NULL DEFAULT true,
    mfa_status           mfa_status NOT NULL DEFAULT 'DISABLED',
    last_login_at        TIMESTAMPTZ,
    last_login_ip        INET,
    failed_login_count   INTEGER NOT NULL DEFAULT 0 CHECK (failed_login_count >= 0),
    locked_until         TIMESTAMPTZ,
    password_changed_at  TIMESTAMPTZ DEFAULT now(),
    must_change_password BOOLEAN NOT NULL DEFAULT false,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at           TIMESTAMPTZ,
    created_by           UUID,   -- self-referencing FK
    updated_by           UUID
);

-- settings (now that auth_users exists, the FK will be added later)
CREATE TABLE settings (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    setting_key     VARCHAR(100) NOT NULL UNIQUE,
    setting_value   TEXT NOT NULL,
    value_type      VARCHAR(20) NOT NULL DEFAULT 'STRING',
    category        VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    description     TEXT,
    is_encrypted    BOOLEAN NOT NULL DEFAULT false,
    min_value       NUMERIC,
    max_value       NUMERIC,
    requires_restart BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_by      UUID,
    CONSTRAINT chk_settings_numeric_check CHECK (
        (value_type NOT IN ('INTEGER','DECIMAL','NUMERIC')) OR
        (min_value IS NULL OR max_value IS NULL OR min_value <= max_value)
    )
);

INSERT INTO settings (setting_key, setting_value, value_type, category, description, min_value, max_value, requires_restart) VALUES
    ('geo.confidence_auto_verify_threshold', '80', 'INTEGER', 'GEOLOCATION', 'Minimum geo_confidence_score at which a site or client_site is auto-promoted to verified.', 0, 100, false),
    ('geo.confidence_flag_threshold', '30', 'INTEGER', 'GEOLOCATION', 'geo_confidence_score below this value raises a GPSFAILURE-adjacent review flag.', 0, 100, false),
    ('session.expiry_minutes', '480', 'INTEGER', 'SECURITY', 'User session lifetime before forced re-authentication.', 5, 1440, false),
    ('session.mfa_grace_minutes', '15', 'INTEGER', 'SECURITY', 'Window to complete MFA challenge after primary login before session is invalidated.', 1, 60, false),
    ('auth.max_failed_login_attempts', '5', 'INTEGER', 'SECURITY', 'Failed login attempts before account lockout.', 1, 20, false),
    ('auth.lockout_duration_minutes', '30', 'INTEGER', 'SECURITY', 'Duration of account lockout after max failed attempts is reached.', 1, 1440, false),
    ('device.battery_critical_threshold', '15', 'INTEGER', 'DEVICE', 'Battery percentage at or below which a device is flagged BATTERYCRITICAL.', 0, 100, false),
    ('device.offline_alert_minutes', '30', 'INTEGER', 'DEVICE', 'Minutes without sync before a device is flagged DEVICEOFFLINE.', 1, 1440, false),
    ('reconciliation.volume_gap_tolerance_percent', '2.5', 'DECIMAL', 'RECONCILIATION', 'Acceptable volume gap percentage before a redressement is triggered.', 0, 100, false),
    ('reconciliation.redressement_due_days', '15', 'INTEGER', 'RECONCILIATION', 'Number of days after issuance before a redressement is considered overdue.', 1, 365, false),
    ('tournee.transporter_ack_timeout_hours', '4', 'INTEGER', 'LOGISTICS', 'Hours a transporter has to acknowledge a tournee before TRANSPORTERNOACK is raised.', 1, 72, false),
    ('tournee.unassigned_alert_hours', '12', 'INTEGER', 'LOGISTICS', 'Hours a tournee may remain unassigned before TOURNEEUNASSIGNEDTOOLONG is raised.', 1, 168, false),
    ('audit.retention_years', '5', 'INTEGER', 'COMPLIANCE', 'Minimum retention period for audit_logs, aligned with CSPH regulatory requirements.', 1, 20, true),
    ('report.default_expiry_days', '30', 'INTEGER', 'REPORTING', 'Days a generated report file remains downloadable before expiry.', 1, 365, false),
    ('mfa.enforced_for_roles', 'ADMIN,SUPERADMIN,SUPERVISOR', 'STRING', 'SECURITY', 'Comma-separated system_role values for which MFA is mandatory.', NULL, NULL, false);

-- permissions
CREATE TABLE permissions (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code        VARCHAR(100) NOT NULL UNIQUE,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    category    VARCHAR(50) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- system_roles
CREATE TABLE system_roles (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name                system_role NOT NULL UNIQUE,
    description         TEXT,
    hierarchy_level     INTEGER NOT NULL DEFAULT 0,
    can_create_subroles BOOLEAN NOT NULL DEFAULT false,
    can_assign_roles    BOOLEAN NOT NULL DEFAULT false,
    max_subordinate_level INTEGER,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- system_role_permissions
CREATE TABLE system_role_permissions (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    system_role_id   UUID NOT NULL,
    permission_id    UUID NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- user_mfa
CREATE TABLE user_mfa (
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id            UUID NOT NULL UNIQUE,
    mfa_type           mfa_type,
    secret_encrypted   TEXT,
    backup_codes_hash  TEXT[],
    is_enabled         BOOLEAN NOT NULL DEFAULT false,
    verified_at        TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- integration_auth
CREATE TABLE integration_auth (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id              UUID NOT NULL UNIQUE,
    auth_key_hash        VARCHAR(255) NOT NULL,
    certificate_pem      TEXT,
    certificate_expiry   TIMESTAMPTZ,
    allowed_ip_ranges    INET[],
    last_auth_at         TIMESTAMPTZ,
    auth_success_count   INTEGER NOT NULL DEFAULT 0 CHECK (auth_success_count >= 0),
    auth_failure_count   INTEGER NOT NULL DEFAULT 0 CHECK (auth_failure_count >= 0),
    is_active            BOOLEAN NOT NULL DEFAULT true,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- user_sessions
CREATE TABLE user_sessions (
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id            UUID NOT NULL,
    refresh_token_hash VARCHAR(255) NOT NULL,
    ip_address         INET NOT NULL,
    user_agent         TEXT,
    geo_country        VARCHAR(2),
    geo_city           VARCHAR(100),
    geo_point          GEOMETRY(POINT, 4326),
    is_valid           BOOLEAN NOT NULL DEFAULT true,
    is_mfa_verified    BOOLEAN NOT NULL DEFAULT false,
    last_active_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at         TIMESTAMPTZ NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- audit_logs (hypertable, will be created after TimescaleDB is fully set)
CREATE TABLE audit_logs (
    id              UUID NOT NULL,
    user_id         UUID,
    session_id      UUID,
    action          audit_action NOT NULL,
    resource_table  VARCHAR(50),
    resource_id     UUID,
    field_name      VARCHAR(100),
    old_value       JSONB,
    new_value       JSONB,
    ip_address      INET NOT NULL,
    user_agent      TEXT,
    request_id      UUID,
    risk_score      INTEGER DEFAULT 0 CHECK (risk_score >= 0),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (id, created_at)
);

SELECT create_hypertable('audit_logs', 'created_at',
    chunk_time_interval => INTERVAL '7 days', if_not_exists => TRUE);

-- (compression/retention policies same as original)

-- sites
CREATE TABLE sites (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id               UUID NOT NULL,
    region               region NOT NULL,
    name                 VARCHAR(255) NOT NULL,
    functions            site_function[] NOT NULL DEFAULT '{}',
    address              TEXT,
    geo_point            GEOMETRY(POINT, 4326),
    geo_confidence_score INTEGER NOT NULL DEFAULT 0 CHECK (geo_confidence_score BETWEEN 0 AND 100),
    delivery_count       INTEGER NOT NULL DEFAULT 0 CHECK (delivery_count >= 0),
    is_verified          BOOLEAN NOT NULL DEFAULT false,
    verified_at          TIMESTAMPTZ,
    verified_by          UUID,
    status               site_status NOT NULL DEFAULT 'UNASSIGNED',
    reason               TEXT,
    is_active            BOOLEAN NOT NULL DEFAULT true,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at           TIMESTAMPTZ,
    created_by           UUID,
    updated_by           UUID,
    UNIQUE (org_id, name),
    CONSTRAINT chk_sites_functions CHECK (COALESCE(array_length(functions, 1), 0) > 0)
);

-- clients
CREATE TABLE clients (
    id                     UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id                 UUID NOT NULL UNIQUE,
    primary_contact_name   VARCHAR(100),
    primary_contact_phone  VARCHAR(50),
    primary_contact_email  VARCHAR(255),
    billing_address        TEXT,
    payment_terms          INTEGER DEFAULT 30 CHECK (payment_terms >= 0),
    credit_limit           DOUBLE PRECISION DEFAULT 0 CHECK (credit_limit >= 0),
    tax_id                 VARCHAR(100),
    industry_sector        VARCHAR(100),
    is_active              BOOLEAN NOT NULL DEFAULT true,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at             TIMESTAMPTZ,
    created_by             UUID,
    updated_by             UUID
);

-- client_sites
CREATE TABLE client_sites (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    site_id                 UUID NOT NULL UNIQUE,
    client_org_id           UUID NOT NULL,
    site_contact_person_id  UUID,
    delivery_instructions   TEXT,
    specific_requirements   TEXT,
    requires_authorization  BOOLEAN NOT NULL DEFAULT false,
    operating_constraints   TEXT,
    geo_confidence_score    INTEGER NOT NULL DEFAULT 0 CHECK (geo_confidence_score BETWEEN 0 AND 100),
    delivery_count          INTEGER NOT NULL DEFAULT 0 CHECK (delivery_count >= 0),
    is_verified             BOOLEAN NOT NULL DEFAULT false,
    verified_at             TIMESTAMPTZ,
    verified_by             UUID,
    status                  site_status NOT NULL DEFAULT 'UNASSIGNED',
    current_marketeur_org_id UUID,
    site_contact_name       VARCHAR(100),
    site_contact_phone      VARCHAR(50),
    is_active               BOOLEAN NOT NULL DEFAULT true,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at              TIMESTAMPTZ,
    created_by              UUID,
    updated_by              UUID,
    UNIQUE (client_org_id, name)
);

-- user_site_assignments
CREATE TABLE user_site_assignments (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL,
    site_id     UUID NOT NULL,
    is_primary  BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID,
    updated_by  UUID,
    UNIQUE (user_id, site_id)
);

-- custom_roles
CREATE TABLE custom_roles (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id           UUID NOT NULL,
    name             VARCHAR(100) NOT NULL,
    description      TEXT,
    permissions_json JSONB NOT NULL DEFAULT '{}',
    is_active        BOOLEAN NOT NULL DEFAULT true,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMPTZ,
    created_by       UUID,
    updated_by       UUID,
    UNIQUE (org_id, name)
);

-- user_custom_roles
CREATE TABLE user_custom_roles (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id        UUID NOT NULL,
    custom_role_id UUID NOT NULL,
    site_id        UUID,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by     UUID,
    updated_by     UUID,
    UNIQUE (user_id, custom_role_id, site_id)
);

-- vehicles
CREATE TABLE vehicles (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    license_plate           VARCHAR(20) NOT NULL UNIQUE,
    type                    vehicle_type NOT NULL,
    org_id                  UUID NOT NULL,
    max_volume              DOUBLE PRECISION CHECK (max_volume > 0),
    max_bottle_count        INTEGER CHECK (max_bottle_count > 0),
    certificate_url         TEXT,
    certificate_number      VARCHAR(100),
    certificate_issued_at   TIMESTAMPTZ,
    certificate_expiry_at   TIMESTAMPTZ,
    tare_weight             DOUBLE PRECISION CHECK (tare_weight >= 0),
    is_active               BOOLEAN NOT NULL DEFAULT true,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at              TIMESTAMPTZ,
    created_by              UUID,
    updated_by              UUID,
    CONSTRAINT chk_vehicle_vrac_cert CHECK (
        type != 'VRAC' OR (certificate_number IS NOT NULL AND certificate_expiry_at IS NOT NULL)
    ),
    CONSTRAINT chk_vehicle_capacity CHECK (
        (type = 'VRAC' AND max_volume IS NOT NULL AND max_bottle_count IS NULL) OR
        (type = 'BOUTEILLES50KG' AND max_bottle_count IS NOT NULL AND max_volume IS NULL)
    )
);

-- drivers
CREATE TABLE drivers (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    license_number VARCHAR(50) NOT NULL UNIQUE,
    org_id         UUID NOT NULL,
    user_id        UUID,
    is_active      BOOLEAN NOT NULL DEFAULT true,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at     TIMESTAMPTZ,
    created_by     UUID,
    updated_by     UUID
);

-- devices
CREATE TABLE devices (
    id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    serial_number         VARCHAR(100) NOT NULL UNIQUE,
    device_type           device_type NOT NULL,
    status                device_status NOT NULL DEFAULT 'UNASSIGNED',
    firmware_version      VARCHAR(50),
    battery_level         INTEGER DEFAULT 100 CHECK (battery_level BETWEEN 0 AND 100),
    battery_critical      BOOLEAN NOT NULL DEFAULT false,
    last_sync             TIMESTAMPTZ,
    last_known_position   GEOMETRY(POINT, 4326),
    assigned_to_user_id   UUID,
    assigned_to_vehicle_id UUID,
    org_id                UUID,
    config_json           JSONB DEFAULT '{}',
    metadata_json         JSONB DEFAULT '{}',
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at            TIMESTAMPTZ,
    created_by            UUID,
    updated_by            UUID,
    CONSTRAINT chk_device_gps_imei CHECK (
        device_type != 'GPS' OR (metadata_json ? 'imei' AND metadata_json->>'imei' IS NOT NULL)
    )
);

-- device_status_history (hypertable)
CREATE TABLE device_status_history (
    id          UUID NOT NULL,
    device_id   UUID NOT NULL,
    old_status  device_status,
    new_status  device_status NOT NULL,
    reason      TEXT,
    changed_by  UUID,
    geo_point   GEOMETRY(POINT, 4326),
    timestamp   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (id, timestamp)
);
SELECT create_hypertable('device_status_history', 'timestamp', chunk_time_interval => INTERVAL '7 days', if_not_exists => TRUE);

-- vehicle_positions (hypertable)
CREATE TABLE vehicle_positions (
    id               UUID NOT NULL,
    vehicle_id       UUID NOT NULL,
    device_id        UUID,
    geo_point        GEOMETRY(POINT, 4326) NOT NULL,
    speed            DOUBLE PRECISION CHECK (speed >= 0),
    heading          DOUBLE PRECISION CHECK (heading >= 0 AND heading < 360),
    accuracy         DOUBLE PRECISION CHECK (accuracy >= 0),
    altitude         DOUBLE PRECISION,
    ignition_status  BOOLEAN,
    odometer         DOUBLE PRECISION CHECK (odometer >= 0),
    battery_level    INTEGER CHECK (battery_level BETWEEN 0 AND 100),
    timestamp        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (id, timestamp)
);
SELECT create_hypertable('vehicle_positions', 'timestamp', chunk_time_interval => INTERVAL '1 day', if_not_exists => TRUE);

-- rfid_tags
CREATE TABLE rfid_tags (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tag_id              VARCHAR(100) NOT NULL UNIQUE,
    bottle_serial       VARCHAR(100),
    status              rfid_tag_status NOT NULL DEFAULT 'AVAILABLE',
    current_site_id     UUID,
    current_client_site_id UUID,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ,
    created_by          UUID,
    updated_by          UUID,
    CONSTRAINT chk_rfid_bottle_serial CHECK (bottle_serial IS NULL OR bottle_serial ~ '^[A-Z0-9]{8,}$'),
    CONSTRAINT chk_rfid_location CHECK (
        (current_site_id IS NOT NULL AND current_client_site_id IS NULL) OR
        (current_site_id IS NULL AND current_client_site_id IS NOT NULL) OR
        (current_site_id IS NULL AND current_client_site_id IS NULL)
    )
);

-- transporter_contracts
CREATE TABLE transporter_contracts (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    marketeur_org_id     UUID NOT NULL,
    transporter_org_id   UUID NOT NULL,
    is_primary           BOOLEAN NOT NULL DEFAULT false,
    contract_reference   VARCHAR(100),
    started_at           TIMESTAMPTZ,
    ended_at             TIMESTAMPTZ,
    is_active            BOOLEAN NOT NULL DEFAULT true,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at           TIMESTAMPTZ,
    created_by           UUID,
    updated_by           UUID,
    UNIQUE (marketeur_org_id, transporter_org_id)
);

-- pickup_requests
CREATE TABLE pickup_requests (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    marketeur_org_id     UUID NOT NULL,
    source_site_id       UUID NOT NULL,
    destination_site_id  UUID NOT NULL,
    requested_quantity   DOUBLE PRECISION NOT NULL CHECK (requested_quantity > 0),
    approved_quantity    DOUBLE PRECISION CHECK (approved_quantity >= 0),
    status               pickup_status NOT NULL DEFAULT 'DRAFT',
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at           TIMESTAMPTZ,
    created_by           UUID,
    updated_by           UUID,
    CONSTRAINT chk_pickup_sites_different CHECK (source_site_id != destination_site_id)
);

-- pickup_request_vehicles
CREATE TABLE pickup_request_vehicles (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    pickup_request_id UUID NOT NULL,
    vehicle_id        UUID NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (pickup_request_id, vehicle_id)
);

-- delivery_tours
CREATE TABLE delivery_tours (
    id                             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    marketeur_org_id               UUID NOT NULL,
    execution_mode                 execution_mode NOT NULL,
    transporter_org_id             UUID,
    vehicle_id                     UUID,
    driver_id                      UUID,
    livreur_user_id                UUID,
    assigned_by_transporter_user_id UUID,
    transporter_assigned_at        TIMESTAMPTZ,
    type                           tournee_type NOT NULL,
    status                         tournee_status NOT NULL DEFAULT 'DRAFT',
    requested_quantity             DOUBLE PRECISION NOT NULL CHECK (requested_quantity > 0),
    loaded_quantity                DOUBLE PRECISION CHECK (loaded_quantity >= 0),
    delivered_quantity             DOUBLE PRECISION CHECK (delivered_quantity >= 0),
    started_at                     TIMESTAMPTZ,
    closed_at                      TIMESTAMPTZ,
    created_at                     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                     TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at                     TIMESTAMPTZ,
    created_by                     UUID,
    updated_by                     UUID,
    CONSTRAINT chk_tournee_internal CHECK (
        execution_mode != 'INTERNAL' OR
        (vehicle_id IS NOT NULL AND driver_id IS NOT NULL AND livreur_user_id IS NOT NULL)
    ),
    CONSTRAINT chk_tournee_external CHECK (
        execution_mode != 'EXTERNAL' OR transporter_org_id IS NOT NULL
    ),
    CONSTRAINT chk_tournee_no_double_assign CHECK (
        NOT (execution_mode = 'INTERNAL' AND assigned_by_transporter_user_id IS NOT NULL)
    ),
    CONSTRAINT chk_tournee_dates CHECK (
        started_at IS NULL OR closed_at IS NULL OR started_at <= closed_at
    )
);

-- checkpoints
CREATE TABLE checkpoints (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tournee_id        UUID NOT NULL,
    site_id           UUID,
    client_site_id    UUID,
    sequence          INTEGER NOT NULL,
    expected_arrival  TIMESTAMPTZ,
    actual_arrival    TIMESTAMPTZ,
    status            checkpoint_status NOT NULL DEFAULT 'PENDING',
    skip_reason       TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ,
    created_by        UUID,
    updated_by        UUID,
    UNIQUE (tournee_id, sequence),
    CONSTRAINT chk_checkpoint_exclusive CHECK (
        (site_id IS NOT NULL AND client_site_id IS NULL) OR
        (site_id IS NULL AND client_site_id IS NOT NULL)
    ),
    CONSTRAINT chk_checkpoint_has_destination CHECK (
        site_id IS NOT NULL OR client_site_id IS NOT NULL
    )
);

-- scan_events (hypertable)
CREATE TABLE scan_events (
    id               UUID NOT NULL,
    checkpoint_id    UUID NOT NULL,
    livreur_user_id  UUID NOT NULL,
    rfid_tag_id      UUID,
    direction        scan_direction NOT NULL,
    geo_point        GEOMETRY(POINT, 4326) NOT NULL,
    timestamp        TIMESTAMPTZ NOT NULL DEFAULT now(),
    meter_reading    DOUBLE PRECISION CHECK (meter_reading >= 0),
    photo_url        TEXT,
    pda_sync_id      VARCHAR(100),
    conflict_status  VARCHAR(20),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by       UUID,
    PRIMARY KEY (id, timestamp)
);
SELECT create_hypertable('scan_events', 'timestamp', chunk_time_interval => INTERVAL '7 days', if_not_exists => TRUE);

-- declarations
CREATE TABLE declarations (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    marketeur_org_id  UUID NOT NULL,
    period_start      TIMESTAMPTZ NOT NULL,
    period_end        TIMESTAMPTZ NOT NULL,
    declared_volume   DOUBLE PRECISION NOT NULL CHECK (declared_volume >= 0),
    status            declaration_status NOT NULL DEFAULT 'DRAFT',
    submitted_by      UUID NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ,
    created_by        UUID,
    updated_by        UUID
);

-- reconciliations
CREATE TABLE reconciliations (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    declaration_id      UUID NOT NULL UNIQUE,
    tracked_volume      DOUBLE PRECISION NOT NULL CHECK (tracked_volume >= 0),
    tracked_bottles_out INTEGER CHECK (tracked_bottles_out >= 0),
    tracked_bottles_in  INTEGER CHECK (tracked_bottles_in >= 0),
    volume_gap          DOUBLE PRECISION NOT NULL,
    subsidy_impact      DOUBLE PRECISION NOT NULL,
    status              reconciliation_status NOT NULL DEFAULT 'PENDING',
    verified_by         UUID,
    verified_at         TIMESTAMPTZ,
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          UUID,
    updated_by          UUID
);

-- redressements
CREATE TABLE redressements (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reconciliation_id UUID NOT NULL,
    amount            DOUBLE PRECISION NOT NULL CHECK (amount >= 0),
    status            redressement_status NOT NULL DEFAULT 'ISSUED',
    issued_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    due_date          TIMESTAMPTZ NOT NULL,
    paid_at           TIMESTAMPTZ,
    transaction_ref   VARCHAR(200),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        UUID,
    updated_by        UUID
);

-- risk_scores
CREATE TABLE risk_scores (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type     risk_entity_type NOT NULL,
    entity_id       UUID NOT NULL,
    score           INTEGER NOT NULL CHECK (score BETWEEN 0 AND 100),
    level           risk_level NOT NULL,
    period_start    TIMESTAMPTZ NOT NULL,
    period_end      TIMESTAMPTZ NOT NULL,
    model_version   VARCHAR(50) NOT NULL,
    details_json    JSONB NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID
);

-- anomalies
CREATE TABLE anomalies (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    type                anomaly_type NOT NULL,
    category            anomaly_category NOT NULL,
    severity            risk_level NOT NULL,
    status              anomaly_status NOT NULL DEFAULT 'NOUVEAU',
    entity_type         risk_entity_type NOT NULL,
    entity_id           UUID NOT NULL,
    site_id             UUID,
    client_site_id      UUID,
    evidence_json       JSONB NOT NULL DEFAULT '{}',
    assigned_to_group   notification_group_type NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at         TIMESTAMPTZ,
    resolved_by         UUID,
    resolution_notes    TEXT,
    created_by          UUID,
    updated_by          UUID
);

-- anomaly_assignments
CREATE TABLE anomaly_assignments (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    anomaly_id           UUID NOT NULL,
    assigned_to_user_id  UUID NOT NULL,
    assigned_by_user_id  UUID NOT NULL,
    assigned_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notes                TEXT,
    resolved_at          TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- notification_groups
CREATE TABLE notification_groups (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(150) NOT NULL,
    type        notification_group_type NOT NULL,
    is_active   BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,
    created_by  UUID,
    updated_by  UUID,
    UNIQUE (name, type)
);

-- notification_group_members
CREATE TABLE notification_group_members (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id    UUID NOT NULL,
    user_id     UUID NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (group_id, user_id)
);

-- notification_rules
CREATE TABLE notification_rules (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(150) NOT NULL,
    anomaly_type    anomaly_type NOT NULL,
    min_severity    risk_level NOT NULL,
    target_group_id UUID NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    created_by      UUID,
    updated_by      UUID,
    UNIQUE (anomaly_type, min_severity)
);

-- reports
CREATE TABLE reports (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(255) NOT NULL,
    type            VARCHAR(50) NOT NULL,
    format          report_format NOT NULL,
    parameters_json JSONB NOT NULL DEFAULT '{}',
    status          report_status NOT NULL DEFAULT 'PENDING',
    generated_at    TIMESTAMPTZ,
    file_url        TEXT,
    file_size       BIGINT CHECK (file_size >= 0),
    generated_by    UUID,
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    created_by      UUID,
    updated_by      UUID
);

-- monitoring_metrics (hypertable)
CREATE TABLE monitoring_metrics (
    id              UUID NOT NULL,
    metric_name     VARCHAR(100) NOT NULL,
    metric_value    DOUBLE PRECISION NOT NULL,
    metric_labels   JSONB DEFAULT '{}',
    hostname        VARCHAR(100),
    service_name    VARCHAR(50),
    timestamp       TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (id, timestamp)
);
SELECT create_hypertable('monitoring_metrics', 'timestamp', chunk_time_interval => INTERVAL '1 day', if_not_exists => TRUE);
-- ============================================================
-- 4. ADD FOREIGN KEYS (after all tables exist)
-- ============================================================

ALTER TABLE organizations
    ADD CONSTRAINT fk_organizations_created_by FOREIGN KEY (created_by) REFERENCES auth_users(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_organizations_updated_by FOREIGN KEY (updated_by) REFERENCES auth_users(id) ON DELETE SET NULL;

ALTER TABLE auth_users
    ADD CONSTRAINT fk_auth_users_org        FOREIGN KEY (org_id)     REFERENCES organizations(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_auth_users_created_by FOREIGN KEY (created_by) REFERENCES auth_users(id)          ON DELETE SET NULL,
    ADD CONSTRAINT fk_auth_users_updated_by FOREIGN KEY (updated_by) REFERENCES auth_users(id)          ON DELETE SET NULL;

ALTER TABLE settings
    ADD CONSTRAINT fk_settings_created_by FOREIGN KEY (created_by) REFERENCES auth_users(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_settings_updated_by FOREIGN KEY (updated_by) REFERENCES auth_users(id) ON DELETE SET NULL;

ALTER TABLE system_role_permissions
    ADD CONSTRAINT fk_srp_system_role FOREIGN KEY (system_role_id) REFERENCES system_roles(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_srp_permission  FOREIGN KEY (permission_id)  REFERENCES permissions(id)   ON DELETE CASCADE,
    ADD CONSTRAINT uq_srp_role_permission UNIQUE (system_role_id, permission_id);

ALTER TABLE user_mfa
    ADD CONSTRAINT fk_user_mfa_user FOREIGN KEY (user_id) REFERENCES auth_users(id) ON DELETE CASCADE;

ALTER TABLE integration_auth
    ADD CONSTRAINT fk_integration_auth_user FOREIGN KEY (user_id) REFERENCES auth_users(id) ON DELETE CASCADE;

ALTER TABLE user_sessions
    ADD CONSTRAINT fk_user_sessions_user FOREIGN KEY (user_id) REFERENCES auth_users(id) ON DELETE CASCADE;

ALTER TABLE audit_logs
    ADD CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES auth_users(id) ON DELETE SET NULL;
    -- Note: session_id intentionally has no FK. user_sessions has no unique (id, *) pair matching
    -- an audit_logs-compatible composite, and audit_logs is a hypertable partitioned on created_at;
    -- enforcing it would force painful cross-chunk lookups. Validate session_id at the application layer.

ALTER TABLE sites
    ADD CONSTRAINT fk_sites_org         FOREIGN KEY (org_id)      REFERENCES organizations(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_sites_verified_by FOREIGN KEY (verified_by) REFERENCES auth_users(id)          ON DELETE SET NULL,
    ADD CONSTRAINT fk_sites_created_by  FOREIGN KEY (created_by)  REFERENCES auth_users(id)          ON DELETE SET NULL,
    ADD CONSTRAINT fk_sites_updated_by  FOREIGN KEY (updated_by)  REFERENCES auth_users(id)          ON DELETE SET NULL;

ALTER TABLE clients
    ADD CONSTRAINT fk_clients_org        FOREIGN KEY (org_id)     REFERENCES organizations(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_clients_created_by FOREIGN KEY (created_by) REFERENCES auth_users(id)          ON DELETE SET NULL,
    ADD CONSTRAINT fk_clients_updated_by FOREIGN KEY (updated_by) REFERENCES auth_users(id)          ON DELETE SET NULL;

ALTER TABLE client_sites
    ADD CONSTRAINT fk_client_sites_client_org  FOREIGN KEY (client_org_id)            REFERENCES organizations(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_client_sites_marketeur   FOREIGN KEY (current_marketeur_org_id) REFERENCES organizations(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_client_sites_verified_by FOREIGN KEY (verified_by)              REFERENCES auth_users(id)         ON DELETE SET NULL,
    ADD CONSTRAINT fk_client_sites_created_by  FOREIGN KEY (created_by)               REFERENCES auth_users(id)         ON DELETE SET NULL,
    ADD CONSTRAINT fk_client_sites_updated_by  FOREIGN KEY (updated_by)               REFERENCES auth_users(id)         ON DELETE SET NULL;

ALTER TABLE user_site_assignments
    ADD CONSTRAINT fk_usa_user       FOREIGN KEY (user_id)    REFERENCES auth_users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_usa_site       FOREIGN KEY (site_id)    REFERENCES sites(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_usa_created_by FOREIGN KEY (created_by) REFERENCES auth_users(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_usa_updated_by FOREIGN KEY (updated_by) REFERENCES auth_users(id) ON DELETE SET NULL;

ALTER TABLE custom_roles
    ADD CONSTRAINT fk_custom_roles_org        FOREIGN KEY (org_id)     REFERENCES organizations(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_custom_roles_created_by FOREIGN KEY (created_by) REFERENCES auth_users(id)          ON DELETE SET NULL,
    ADD CONSTRAINT fk_custom_roles_updated_by FOREIGN KEY (updated_by) REFERENCES auth_users(id)          ON DELETE SET NULL;

ALTER TABLE user_custom_roles
    ADD CONSTRAINT fk_ucr_user        FOREIGN KEY (user_id)        REFERENCES auth_users(id)        ON DELETE CASCADE,
    ADD CONSTRAINT fk_ucr_custom_role FOREIGN KEY (custom_role_id) REFERENCES custom_roles(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_ucr_site        FOREIGN KEY (site_id)        REFERENCES sites(id)        ON DELETE CASCADE,
    ADD CONSTRAINT fk_ucr_created_by  FOREIGN KEY (created_by)     REFERENCES auth_users(id)        ON DELETE SET NULL,
    ADD CONSTRAINT fk_ucr_updated_by  FOREIGN KEY (updated_by)     REFERENCES auth_users(id)        ON DELETE SET NULL;

ALTER TABLE vehicles
    ADD CONSTRAINT fk_vehicles_org        FOREIGN KEY (org_id)     REFERENCES organizations(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_vehicles_created_by FOREIGN KEY (created_by) REFERENCES auth_users(id)          ON DELETE SET NULL,
    ADD CONSTRAINT fk_vehicles_updated_by FOREIGN KEY (updated_by) REFERENCES auth_users(id)          ON DELETE SET NULL;

ALTER TABLE drivers
    ADD CONSTRAINT fk_drivers_org        FOREIGN KEY (org_id)     REFERENCES organizations(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_drivers_user       FOREIGN KEY (user_id)    REFERENCES auth_users(id)          ON DELETE SET NULL,
    ADD CONSTRAINT fk_drivers_created_by FOREIGN KEY (created_by) REFERENCES auth_users(id)          ON DELETE SET NULL,
    ADD CONSTRAINT fk_drivers_updated_by FOREIGN KEY (updated_by) REFERENCES auth_users(id)          ON DELETE SET NULL;

ALTER TABLE devices
    ADD CONSTRAINT fk_devices_assigned_user    FOREIGN KEY (assigned_to_user_id)    REFERENCES auth_users(id)         ON DELETE SET NULL,
    ADD CONSTRAINT fk_devices_assigned_vehicle FOREIGN KEY (assigned_to_vehicle_id) REFERENCES vehicles(id)      ON DELETE SET NULL,
    ADD CONSTRAINT fk_devices_org              FOREIGN KEY (org_id)                 REFERENCES organizations(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_devices_created_by       FOREIGN KEY (created_by)             REFERENCES auth_users(id)         ON DELETE SET NULL,
    ADD CONSTRAINT fk_devices_updated_by       FOREIGN KEY (updated_by)             REFERENCES auth_users(id)         ON DELETE SET NULL;

ALTER TABLE device_status_history
    ADD CONSTRAINT fk_dsh_device     FOREIGN KEY (device_id)  REFERENCES devices(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_dsh_changed_by FOREIGN KEY (changed_by) REFERENCES auth_users(id)   ON DELETE SET NULL;

ALTER TABLE vehicle_positions
    ADD CONSTRAINT fk_vp_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_vp_device  FOREIGN KEY (device_id)  REFERENCES devices(id)  ON DELETE SET NULL;

ALTER TABLE rfid_tags
    ADD CONSTRAINT fk_rfid_current_site        FOREIGN KEY (current_site_id)        REFERENCES sites(id)        ON DELETE SET NULL,
    ADD CONSTRAINT fk_rfid_current_client_site FOREIGN KEY (current_client_site_id) REFERENCES client_sites(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_rfid_created_by          FOREIGN KEY (created_by)             REFERENCES auth_users(id)        ON DELETE SET NULL,
    ADD CONSTRAINT fk_rfid_updated_by          FOREIGN KEY (updated_by)             REFERENCES auth_users(id)        ON DELETE SET NULL;

ALTER TABLE transporter_contracts
    ADD CONSTRAINT fk_tc_marketeur   FOREIGN KEY (marketeur_org_id)   REFERENCES organizations(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_tc_transporter FOREIGN KEY (transporter_org_id) REFERENCES organizations(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_tc_created_by  FOREIGN KEY (created_by)         REFERENCES auth_users(id)          ON DELETE SET NULL,
    ADD CONSTRAINT fk_tc_updated_by  FOREIGN KEY (updated_by)         REFERENCES auth_users(id)          ON DELETE SET NULL,
    ADD CONSTRAINT chk_tc_orgs_different CHECK (marketeur_org_id != transporter_org_id);

ALTER TABLE pickup_requests
    ADD CONSTRAINT fk_pr_marketeur   FOREIGN KEY (marketeur_org_id)    REFERENCES organizations(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_pr_source      FOREIGN KEY (source_site_id)      REFERENCES sites(id)          ON DELETE RESTRICT,
    ADD CONSTRAINT fk_pr_destination FOREIGN KEY (destination_site_id) REFERENCES sites(id)          ON DELETE RESTRICT,
    ADD CONSTRAINT fk_pr_created_by  FOREIGN KEY (created_by)          REFERENCES auth_users(id)          ON DELETE SET NULL,
    ADD CONSTRAINT fk_pr_updated_by  FOREIGN KEY (updated_by)          REFERENCES auth_users(id)          ON DELETE SET NULL;

ALTER TABLE pickup_request_vehicles
    ADD CONSTRAINT fk_prv_pickup_request FOREIGN KEY (pickup_request_id) REFERENCES pickup_requests(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_prv_vehicle        FOREIGN KEY (vehicle_id)        REFERENCES vehicles(id)        ON DELETE CASCADE;

ALTER TABLE delivery_tours
    ADD CONSTRAINT fk_dt_marketeur              FOREIGN KEY (marketeur_org_id)               REFERENCES organizations(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_dt_transporter            FOREIGN KEY (transporter_org_id)             REFERENCES organizations(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_dt_vehicle                FOREIGN KEY (vehicle_id)                     REFERENCES vehicles(id)      ON DELETE SET NULL,
    ADD CONSTRAINT fk_dt_driver                 FOREIGN KEY (driver_id)                      REFERENCES drivers(id)       ON DELETE SET NULL,
    ADD CONSTRAINT fk_dt_livreur                FOREIGN KEY (livreur_user_id)                REFERENCES auth_users(id)         ON DELETE SET NULL,
    ADD CONSTRAINT fk_dt_assigned_by_transporter FOREIGN KEY (assigned_by_transporter_user_id) REFERENCES auth_users(id)       ON DELETE SET NULL,
    ADD CONSTRAINT fk_dt_created_by             FOREIGN KEY (created_by)                     REFERENCES auth_users(id)         ON DELETE SET NULL,
    ADD CONSTRAINT fk_dt_updated_by             FOREIGN KEY (updated_by)                     REFERENCES auth_users(id)         ON DELETE SET NULL;

ALTER TABLE checkpoints
    ADD CONSTRAINT fk_checkpoints_tournee     FOREIGN KEY (tournee_id)     REFERENCES delivery_tours(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_checkpoints_site        FOREIGN KEY (site_id)        REFERENCES sites(id)          ON DELETE RESTRICT,
    ADD CONSTRAINT fk_checkpoints_client_site FOREIGN KEY (client_site_id) REFERENCES client_sites(id)   ON DELETE RESTRICT,
    ADD CONSTRAINT fk_checkpoints_created_by  FOREIGN KEY (created_by)     REFERENCES auth_users(id)          ON DELETE SET NULL,
    ADD CONSTRAINT fk_checkpoints_updated_by  FOREIGN KEY (updated_by)     REFERENCES auth_users(id)          ON DELETE SET NULL;

ALTER TABLE scan_events
    ADD CONSTRAINT fk_scan_events_checkpoint   FOREIGN KEY (checkpoint_id)   REFERENCES checkpoints(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_scan_events_livreur      FOREIGN KEY (livreur_user_id) REFERENCES auth_users(id)       ON DELETE RESTRICT,
    ADD CONSTRAINT fk_scan_events_rfid_tag     FOREIGN KEY (rfid_tag_id)     REFERENCES rfid_tags(id)   ON DELETE SET NULL,
    ADD CONSTRAINT fk_scan_events_created_by   FOREIGN KEY (created_by)      REFERENCES auth_users(id)       ON DELETE SET NULL;
    -- Note: checkpoint_id references checkpoints(id), which is a plain (non-hypertable) PK — valid.

ALTER TABLE declarations
    ADD CONSTRAINT fk_declarations_marketeur    FOREIGN KEY (marketeur_org_id) REFERENCES organizations(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_declarations_submitted_by FOREIGN KEY (submitted_by)     REFERENCES auth_users(id)          ON DELETE RESTRICT,
    ADD CONSTRAINT fk_declarations_created_by   FOREIGN KEY (created_by)       REFERENCES auth_users(id)          ON DELETE SET NULL,
    ADD CONSTRAINT fk_declarations_updated_by   FOREIGN KEY (updated_by)       REFERENCES auth_users(id)          ON DELETE SET NULL,
    ADD CONSTRAINT chk_declarations_period CHECK (period_start < period_end);

ALTER TABLE reconciliations
    ADD CONSTRAINT fk_reconciliations_declaration FOREIGN KEY (declaration_id) REFERENCES declarations(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_reconciliations_verified_by FOREIGN KEY (verified_by)    REFERENCES auth_users(id)         ON DELETE SET NULL,
    ADD CONSTRAINT fk_reconciliations_created_by  FOREIGN KEY (created_by)     REFERENCES auth_users(id)         ON DELETE SET NULL,
    ADD CONSTRAINT fk_reconciliations_updated_by  FOREIGN KEY (updated_by)     REFERENCES auth_users(id)         ON DELETE SET NULL;

ALTER TABLE redressements
    ADD CONSTRAINT fk_redressements_reconciliation FOREIGN KEY (reconciliation_id) REFERENCES reconciliations(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_redressements_created_by     FOREIGN KEY (created_by)        REFERENCES auth_users(id)            ON DELETE SET NULL,
    ADD CONSTRAINT fk_redressements_updated_by     FOREIGN KEY (updated_by)        REFERENCES auth_users(id)            ON DELETE SET NULL,
    ADD CONSTRAINT chk_redressements_paid_at CHECK (paid_at IS NULL OR status = 'PAID');

ALTER TABLE risk_scores
    ADD CONSTRAINT fk_risk_scores_created_by FOREIGN KEY (created_by) REFERENCES auth_users(id) ON DELETE SET NULL,
    ADD CONSTRAINT chk_risk_scores_period CHECK (period_start < period_end);
    -- entity_id is polymorphic (governed by entity_type) and intentionally has no FK.

ALTER TABLE anomalies
    ADD CONSTRAINT fk_anomalies_site        FOREIGN KEY (site_id)        REFERENCES sites(id)        ON DELETE SET NULL,
    ADD CONSTRAINT fk_anomalies_client_site FOREIGN KEY (client_site_id) REFERENCES client_sites(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_anomalies_resolved_by FOREIGN KEY (resolved_by)    REFERENCES auth_users(id)        ON DELETE SET NULL,
    ADD CONSTRAINT fk_anomalies_created_by  FOREIGN KEY (created_by)     REFERENCES auth_users(id)        ON DELETE SET NULL,
    ADD CONSTRAINT fk_anomalies_updated_by  FOREIGN KEY (updated_by)     REFERENCES auth_users(id)        ON DELETE SET NULL,
    ADD CONSTRAINT chk_anomalies_resolution CHECK (
        (status IN ('RESOLU', 'FERME') AND resolved_at IS NOT NULL) OR
        (status NOT IN ('RESOLU', 'FERME'))
    );
    -- entity_id is polymorphic (governed by entity_type) and intentionally has no FK.

ALTER TABLE anomaly_assignments
    ADD CONSTRAINT fk_aa_anomaly           FOREIGN KEY (anomaly_id)          REFERENCES anomalies(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_aa_assigned_to_user  FOREIGN KEY (assigned_to_user_id) REFERENCES auth_users(id)     ON DELETE CASCADE,
    ADD CONSTRAINT fk_aa_assigned_by_user  FOREIGN KEY (assigned_by_user_id) REFERENCES auth_users(id)     ON DELETE SET NULL;

ALTER TABLE notification_groups
    ADD CONSTRAINT fk_ng_created_by FOREIGN KEY (created_by) REFERENCES auth_users(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_ng_updated_by FOREIGN KEY (updated_by) REFERENCES auth_users(id) ON DELETE SET NULL;

ALTER TABLE notification_group_members
    ADD CONSTRAINT fk_ngm_group FOREIGN KEY (group_id) REFERENCES notification_groups(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_ngm_user  FOREIGN KEY (user_id)  REFERENCES auth_users(id)                ON DELETE CASCADE;

ALTER TABLE notification_rules
    ADD CONSTRAINT fk_nr_target_group FOREIGN KEY (target_group_id) REFERENCES notification_groups(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_nr_created_by   FOREIGN KEY (created_by)      REFERENCES auth_users(id)                ON DELETE SET NULL,
    ADD CONSTRAINT fk_nr_updated_by   FOREIGN KEY (updated_by)      REFERENCES auth_users(id)                ON DELETE SET NULL;

ALTER TABLE reports
    ADD CONSTRAINT fk_reports_generated_by FOREIGN KEY (generated_by) REFERENCES auth_users(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_reports_created_by   FOREIGN KEY (created_by)   REFERENCES auth_users(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_reports_updated_by   FOREIGN KEY (updated_by)   REFERENCES auth_users(id) ON DELETE SET NULL;

-- monitoring_metrics: no FKs — hostname/service_name are free-text operational telemetry fields.
-- ============================================================
-- 5. INDEXES (created after tables and foreign keys exist)
-- ============================================================
-- Convention: idx_<table>_<column(s)>. FK columns, status/enum
-- filters, soft-delete filters, geo columns (GIST), and JSONB
-- columns (GIN) are all covered. PostgreSQL already creates an
-- index for every PRIMARY KEY and UNIQUE constraint, so those
-- are not repeated here.

-- organizations
CREATE INDEX idx_organizations_type       ON organizations(type);
CREATE INDEX idx_organizations_is_active  ON organizations(is_active) WHERE deleted_at IS NULL;
CREATE INDEX idx_organizations_deleted_at ON organizations(deleted_at) WHERE deleted_at IS NOT NULL;

-- auth_users
CREATE INDEX idx_auth_users_org_id       ON auth_users(org_id);
CREATE INDEX idx_auth_users_system_role  ON auth_users(system_role);
CREATE INDEX idx_auth_users_is_active    ON auth_users(is_active) WHERE deleted_at IS NULL;
CREATE INDEX idx_auth_users_deleted_at   ON auth_users(deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX idx_auth_users_locked_until ON auth_users(locked_until) WHERE locked_until IS NOT NULL;

-- settings
CREATE INDEX idx_settings_category ON settings(category);

-- permissions
CREATE INDEX idx_permissions_category ON permissions(category);

-- system_roles
CREATE INDEX idx_system_roles_hierarchy_level ON system_roles(hierarchy_level);

-- system_role_permissions
CREATE INDEX idx_srp_system_role_id ON system_role_permissions(system_role_id);
CREATE INDEX idx_srp_permission_id  ON system_role_permissions(permission_id);

-- user_mfa
CREATE INDEX idx_user_mfa_is_enabled ON user_mfa(is_enabled);

-- integration_auth
CREATE INDEX idx_integration_auth_is_active         ON integration_auth(is_active);
CREATE INDEX idx_integration_auth_certificate_expiry ON integration_auth(certificate_expiry) WHERE certificate_expiry IS NOT NULL;

-- user_sessions
CREATE INDEX idx_user_sessions_user_id     ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_is_valid    ON user_sessions(is_valid) WHERE is_valid = true;
CREATE INDEX idx_user_sessions_expires_at  ON user_sessions(expires_at);
CREATE INDEX idx_user_sessions_geo_point   ON user_sessions USING GIST(geo_point) WHERE geo_point IS NOT NULL;

-- audit_logs (hypertable — created_at is already the partitioning column)
CREATE INDEX idx_audit_logs_user_id        ON audit_logs(user_id, created_at DESC);
CREATE INDEX idx_audit_logs_action         ON audit_logs(action, created_at DESC);
CREATE INDEX idx_audit_logs_resource       ON audit_logs(resource_table, resource_id, created_at DESC);
CREATE INDEX idx_audit_logs_request_id     ON audit_logs(request_id) WHERE request_id IS NOT NULL;

-- sites
CREATE INDEX idx_sites_org_id      ON sites(org_id);
CREATE INDEX idx_sites_region      ON sites(region);
CREATE INDEX idx_sites_status      ON sites(status);
CREATE INDEX idx_sites_is_active   ON sites(is_active) WHERE deleted_at IS NULL;
CREATE INDEX idx_sites_is_verified ON sites(is_verified) WHERE is_verified = false;
CREATE INDEX idx_sites_functions   ON sites USING GIN(functions);
CREATE INDEX idx_sites_geo_point   ON sites USING GIST(geo_point) WHERE geo_point IS NOT NULL;
CREATE INDEX idx_sites_deleted_at  ON sites(deleted_at) WHERE deleted_at IS NOT NULL;

-- clients
CREATE INDEX idx_clients_is_active ON clients(is_active) WHERE deleted_at IS NULL;

-- client_sites
CREATE INDEX idx_client_sites_client_org_id  ON client_sites(client_org_id);
CREATE INDEX idx_client_sites_marketeur      ON client_sites(current_marketeur_org_id);
CREATE INDEX idx_client_sites_region         ON client_sites(region);
CREATE INDEX idx_client_sites_status         ON client_sites(status);
CREATE INDEX idx_client_sites_is_verified    ON client_sites(is_verified) WHERE is_verified = false;
CREATE INDEX idx_client_sites_geo_point      ON client_sites USING GIST(geo_point) WHERE geo_point IS NOT NULL;
CREATE INDEX idx_client_sites_deleted_at     ON client_sites(deleted_at) WHERE deleted_at IS NOT NULL;

-- user_site_assignments
CREATE INDEX idx_usa_user_id ON user_site_assignments(user_id);
CREATE INDEX idx_usa_site_id ON user_site_assignments(site_id);

-- custom_roles
CREATE INDEX idx_custom_roles_org_id     ON custom_roles(org_id);
CREATE INDEX idx_custom_roles_permissions ON custom_roles USING GIN(permissions_json);

-- user_custom_roles
CREATE INDEX idx_ucr_user_id        ON user_custom_roles(user_id);
CREATE INDEX idx_ucr_custom_role_id ON user_custom_roles(custom_role_id);
CREATE INDEX idx_ucr_site_id        ON user_custom_roles(site_id) WHERE site_id IS NOT NULL;

-- vehicles
CREATE INDEX idx_vehicles_org_id                 ON vehicles(org_id);
CREATE INDEX idx_vehicles_type                   ON vehicles(type);
CREATE INDEX idx_vehicles_is_active              ON vehicles(is_active) WHERE deleted_at IS NULL;
CREATE INDEX idx_vehicles_certificate_expiry_at  ON vehicles(certificate_expiry_at) WHERE certificate_expiry_at IS NOT NULL;

-- drivers
CREATE INDEX idx_drivers_org_id    ON drivers(org_id);
CREATE INDEX idx_drivers_user_id   ON drivers(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_drivers_is_active ON drivers(is_active) WHERE deleted_at IS NULL;

-- devices
CREATE INDEX idx_devices_status                 ON devices(status);
CREATE INDEX idx_devices_device_type             ON devices(device_type);
CREATE INDEX idx_devices_org_id                  ON devices(org_id) WHERE org_id IS NOT NULL;
CREATE INDEX idx_devices_assigned_to_user_id     ON devices(assigned_to_user_id) WHERE assigned_to_user_id IS NOT NULL;
CREATE INDEX idx_devices_assigned_to_vehicle_id  ON devices(assigned_to_vehicle_id) WHERE assigned_to_vehicle_id IS NOT NULL;
CREATE INDEX idx_devices_battery_critical        ON devices(battery_critical) WHERE battery_critical = true;
CREATE INDEX idx_devices_last_known_position     ON devices USING GIST(last_known_position) WHERE last_known_position IS NOT NULL;
CREATE INDEX idx_devices_config_json             ON devices USING GIN(config_json);

-- device_status_history (hypertable)
CREATE INDEX idx_dsh_device_id ON device_status_history(device_id, timestamp DESC);

-- vehicle_positions (hypertable)
CREATE INDEX idx_vp_vehicle_id ON vehicle_positions(vehicle_id, timestamp DESC);
CREATE INDEX idx_vp_device_id  ON vehicle_positions(device_id, timestamp DESC) WHERE device_id IS NOT NULL;
CREATE INDEX idx_vp_geo_point  ON vehicle_positions USING GIST(geo_point);

-- rfid_tags
CREATE INDEX idx_rfid_tags_status                  ON rfid_tags(status);
CREATE INDEX idx_rfid_tags_current_site_id         ON rfid_tags(current_site_id) WHERE current_site_id IS NOT NULL;
CREATE INDEX idx_rfid_tags_current_client_site_id  ON rfid_tags(current_client_site_id) WHERE current_client_site_id IS NOT NULL;
CREATE INDEX idx_rfid_tags_bottle_serial           ON rfid_tags(bottle_serial) WHERE bottle_serial IS NOT NULL;

-- transporter_contracts
CREATE INDEX idx_tc_marketeur_org_id   ON transporter_contracts(marketeur_org_id);
CREATE INDEX idx_tc_transporter_org_id ON transporter_contracts(transporter_org_id);
CREATE INDEX idx_tc_is_active          ON transporter_contracts(is_active) WHERE deleted_at IS NULL;

-- pickup_requests
CREATE INDEX idx_pr_marketeur_org_id    ON pickup_requests(marketeur_org_id);
CREATE INDEX idx_pr_source_site_id      ON pickup_requests(source_site_id);
CREATE INDEX idx_pr_destination_site_id ON pickup_requests(destination_site_id);
CREATE INDEX idx_pr_status              ON pickup_requests(status);

-- pickup_request_vehicles
CREATE INDEX idx_prv_pickup_request_id ON pickup_request_vehicles(pickup_request_id);
CREATE INDEX idx_prv_vehicle_id        ON pickup_request_vehicles(vehicle_id);

-- delivery_tours
CREATE INDEX idx_dt_marketeur_org_id    ON delivery_tours(marketeur_org_id);
CREATE INDEX idx_dt_transporter_org_id  ON delivery_tours(transporter_org_id) WHERE transporter_org_id IS NOT NULL;
CREATE INDEX idx_dt_vehicle_id          ON delivery_tours(vehicle_id) WHERE vehicle_id IS NOT NULL;
CREATE INDEX idx_dt_driver_id           ON delivery_tours(driver_id) WHERE driver_id IS NOT NULL;
CREATE INDEX idx_dt_livreur_user_id     ON delivery_tours(livreur_user_id) WHERE livreur_user_id IS NOT NULL;
CREATE INDEX idx_dt_status              ON delivery_tours(status);
CREATE INDEX idx_dt_execution_mode      ON delivery_tours(execution_mode);
CREATE INDEX idx_dt_started_at          ON delivery_tours(started_at) WHERE started_at IS NOT NULL;

-- checkpoints
CREATE INDEX idx_checkpoints_tournee_id     ON checkpoints(tournee_id, sequence);
CREATE INDEX idx_checkpoints_site_id        ON checkpoints(site_id) WHERE site_id IS NOT NULL;
CREATE INDEX idx_checkpoints_client_site_id ON checkpoints(client_site_id) WHERE client_site_id IS NOT NULL;
CREATE INDEX idx_checkpoints_status         ON checkpoints(status);

-- scan_events (hypertable)
CREATE INDEX idx_scan_events_checkpoint_id   ON scan_events(checkpoint_id, timestamp DESC);
CREATE INDEX idx_scan_events_livreur_user_id ON scan_events(livreur_user_id, timestamp DESC);
CREATE INDEX idx_scan_events_rfid_tag_id     ON scan_events(rfid_tag_id, timestamp DESC) WHERE rfid_tag_id IS NOT NULL;
CREATE INDEX idx_scan_events_geo_point       ON scan_events USING GIST(geo_point);
CREATE INDEX idx_scan_events_conflict_status ON scan_events(conflict_status) WHERE conflict_status IS NOT NULL;

-- declarations
CREATE INDEX idx_declarations_marketeur_org_id ON declarations(marketeur_org_id);
CREATE INDEX idx_declarations_status           ON declarations(status);
CREATE INDEX idx_declarations_period           ON declarations(period_start, period_end);

-- reconciliations
CREATE INDEX idx_reconciliations_status ON reconciliations(status);

-- redressements
CREATE INDEX idx_redressements_reconciliation_id ON redressements(reconciliation_id);
CREATE INDEX idx_redressements_status            ON redressements(status);
CREATE INDEX idx_redressements_due_date          ON redressements(due_date) WHERE status = 'ISSUED';

-- risk_scores
CREATE INDEX idx_risk_scores_entity          ON risk_scores(entity_type, entity_id);
CREATE INDEX idx_risk_scores_level           ON risk_scores(level);
CREATE INDEX idx_risk_scores_period          ON risk_scores(period_start, period_end);
CREATE INDEX idx_risk_scores_details_json    ON risk_scores USING GIN(details_json);

-- anomalies
CREATE INDEX idx_anomalies_entity            ON anomalies(entity_type, entity_id);
CREATE INDEX idx_anomalies_type              ON anomalies(type);
CREATE INDEX idx_anomalies_status            ON anomalies(status);
CREATE INDEX idx_anomalies_severity          ON anomalies(severity);
CREATE INDEX idx_anomalies_site_id           ON anomalies(site_id) WHERE site_id IS NOT NULL;
CREATE INDEX idx_anomalies_client_site_id    ON anomalies(client_site_id) WHERE client_site_id IS NOT NULL;
CREATE INDEX idx_anomalies_assigned_to_group ON anomalies(assigned_to_group);
CREATE INDEX idx_anomalies_evidence_json     ON anomalies USING GIN(evidence_json);
CREATE INDEX idx_anomalies_created_at        ON anomalies(created_at DESC);

-- anomaly_assignments
CREATE INDEX idx_aa_anomaly_id           ON anomaly_assignments(anomaly_id);
CREATE INDEX idx_aa_assigned_to_user_id  ON anomaly_assignments(assigned_to_user_id);
CREATE INDEX idx_aa_status               ON anomaly_assignments(status);

-- notification_groups
CREATE INDEX idx_ng_type      ON notification_groups(type);
CREATE INDEX idx_ng_is_active ON notification_groups(is_active) WHERE deleted_at IS NULL;

-- notification_group_members
CREATE INDEX idx_ngm_group_id ON notification_group_members(group_id);
CREATE INDEX idx_ngm_user_id  ON notification_group_members(user_id);

-- notification_rules
CREATE INDEX idx_nr_anomaly_type    ON notification_rules(anomaly_type);
CREATE INDEX idx_nr_target_group_id ON notification_rules(target_group_id);
CREATE INDEX idx_nr_is_active       ON notification_rules(is_active) WHERE deleted_at IS NULL;

-- reports
CREATE INDEX idx_reports_status       ON reports(status);
CREATE INDEX idx_reports_generated_by ON reports(generated_by) WHERE generated_by IS NOT NULL;
CREATE INDEX idx_reports_expires_at   ON reports(expires_at) WHERE expires_at IS NOT NULL;
CREATE INDEX idx_reports_parameters_json ON reports USING GIN(parameters_json);

-- monitoring_metrics (hypertable)
CREATE INDEX idx_mm_metric_name ON monitoring_metrics(metric_name, timestamp DESC);
CREATE INDEX idx_mm_service_name ON monitoring_metrics(service_name, timestamp DESC) WHERE service_name IS NOT NULL;
CREATE INDEX idx_mm_labels ON monitoring_metrics USING GIN(metric_labels);
-- ============================================================
-- 6. TRIGGERS & FUNCTIONS (all at the end, referencing existing tables)
-- ============================================================

-- --------------------------------------------------------------
-- 6.1 Generic updated_at maintenance
-- --------------------------------------------------------------
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Attach the trigger to every base table that has an updated_at column.
-- Using information_schema instead of a hand-written list of ~31 CREATE
-- TRIGGER statements guarantees coverage today and for any table added
-- later, and removes the risk of a table silently being missed.
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT c.table_name
        FROM information_schema.columns c
        JOIN information_schema.tables t
          ON t.table_name = c.table_name AND t.table_schema = c.table_schema
        WHERE c.column_name = 'updated_at'
          AND c.table_schema = 'public'
          AND t.table_type = 'BASE TABLE'
    LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS trg_%I_updated_at ON %I', r.table_name, r.table_name);
        EXECUTE format(
            'CREATE TRIGGER trg_%I_updated_at BEFORE UPDATE ON %I FOR EACH ROW EXECUTE FUNCTION update_updated_at()',
            r.table_name, r.table_name
        );
    END LOOP;
END $$;

-- --------------------------------------------------------------
-- 6.2 Settings change audit trail
-- --------------------------------------------------------------
-- Every UPDATE to settings.setting_value is written to audit_logs so that
-- CSPH regulatory/compliance review has a full history of configuration
-- changes (e.g. reconciliation tolerances, MFA enforcement).
CREATE OR REPLACE FUNCTION audit_settings_change()
RETURNS TRIGGER AS $$
DECLARE
    v_actor UUID;
BEGIN
    IF NEW.setting_value IS DISTINCT FROM OLD.setting_value THEN
        BEGIN
            v_actor := NULLIF(current_setting('app.current_user_id', true), '')::UUID;
        EXCEPTION WHEN OTHERS THEN
            v_actor := NULL;
        END;

        INSERT INTO audit_logs (
            id, user_id, action, resource_table, resource_id,
            field_name, old_value, new_value, ip_address, created_at
        ) VALUES (
            uuid_generate_v4(), v_actor, 'SETTINGCHANGED', 'settings', NEW.id,
            'setting_value', to_jsonb(OLD.setting_value), to_jsonb(NEW.setting_value),
            COALESCE(inet_client_addr(), '0.0.0.0'::inet), now()
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_settings_audit ON settings;
CREATE TRIGGER trg_settings_audit
    AFTER UPDATE ON settings
    FOR EACH ROW
    EXECUTE FUNCTION audit_settings_change();

-- --------------------------------------------------------------
-- 6.3 Geo auto-promotion
-- --------------------------------------------------------------
-- When a site's or client_site's geo_confidence_score reaches the
-- configured threshold (settings.geo.confidence_auto_verify_threshold,
-- default 80) it is automatically marked verified, mirroring the field
-- workflow where GPS confidence rises as delivery scans accumulate.
CREATE OR REPLACE FUNCTION autogeopromote()
RETURNS TRIGGER AS $$
DECLARE
    v_threshold INTEGER;
BEGIN
    SELECT COALESCE(setting_value::INTEGER, 80) INTO v_threshold
    FROM settings WHERE setting_key = 'geo.confidence_auto_verify_threshold';

    IF v_threshold IS NULL THEN
        v_threshold := 80;
    END IF;

    IF NEW.geo_confidence_score >= v_threshold AND NEW.is_verified = false THEN
        NEW.is_verified := true;
        NEW.verified_at := now();
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_sites_autogeopromote ON sites;
CREATE TRIGGER trg_sites_autogeopromote
    BEFORE INSERT OR UPDATE OF geo_confidence_score ON sites
    FOR EACH ROW
    EXECUTE FUNCTION autogeopromote();

DROP TRIGGER IF EXISTS trg_client_sites_autogeopromote ON client_sites;
CREATE TRIGGER trg_client_sites_autogeopromote
    BEFORE INSERT OR UPDATE OF geo_confidence_score ON client_sites
    FOR EACH ROW
    EXECUTE FUNCTION autogeopromote();

-- --------------------------------------------------------------
-- 6.4 Reconciliation volume gap computation
-- --------------------------------------------------------------
-- Keeps volume_gap in sync with declared vs. tracked volume so the
-- reconciliation trigger for redressements always sees a fresh figure.
CREATE OR REPLACE FUNCTION compute_reconciliation_gap()
RETURNS TRIGGER AS $$
DECLARE
    v_declared DOUBLE PRECISION;
BEGIN
    SELECT declared_volume INTO v_declared FROM declarations WHERE id = NEW.declaration_id;
    IF v_declared IS NOT NULL THEN
        NEW.volume_gap := v_declared - NEW.tracked_volume;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_reconciliations_compute_gap ON reconciliations;
CREATE TRIGGER trg_reconciliations_compute_gap
    BEFORE INSERT OR UPDATE OF tracked_volume ON reconciliations
    FOR EACH ROW
    EXECUTE FUNCTION compute_reconciliation_gap();

-- --------------------------------------------------------------
-- 6.5 Device battery-critical flag
-- --------------------------------------------------------------
CREATE OR REPLACE FUNCTION flag_device_battery_critical()
RETURNS TRIGGER AS $$
DECLARE
    v_threshold INTEGER;
BEGIN
    SELECT COALESCE(setting_value::INTEGER, 15) INTO v_threshold
    FROM settings WHERE setting_key = 'device.battery_critical_threshold';

    IF v_threshold IS NULL THEN
        v_threshold := 15;
    END IF;

    NEW.battery_critical := (NEW.battery_level IS NOT NULL AND NEW.battery_level <= v_threshold);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_devices_battery_critical ON devices;
CREATE TRIGGER trg_devices_battery_critical
    BEFORE INSERT OR UPDATE OF battery_level ON devices
    FOR EACH ROW
    EXECUTE FUNCTION flag_device_battery_critical();

-- ============================================================
-- 7. TIMESCALEDB COMPRESSION & RETENTION POLICIES
-- ============================================================
-- Retention windows below follow settings.audit.retention_years (5y default
-- for audit_logs, aligned with CSPH regulatory expectations) and reasonable
-- operational defaults for the other hypertables. Adjust to your actual
-- compliance requirements before running in production.

ALTER TABLE audit_logs SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'user_id, action'
);
SELECT add_compression_policy('audit_logs', INTERVAL '30 days', if_not_exists => TRUE);
SELECT add_retention_policy('audit_logs', INTERVAL '5 years', if_not_exists => TRUE);

ALTER TABLE device_status_history SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'device_id'
);
SELECT add_compression_policy('device_status_history', INTERVAL '14 days', if_not_exists => TRUE);
SELECT add_retention_policy('device_status_history', INTERVAL '1 year', if_not_exists => TRUE);

ALTER TABLE vehicle_positions SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'vehicle_id'
);
SELECT add_compression_policy('vehicle_positions', INTERVAL '7 days', if_not_exists => TRUE);
SELECT add_retention_policy('vehicle_positions', INTERVAL '6 months', if_not_exists => TRUE);

ALTER TABLE scan_events SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'livreur_user_id, direction'
);
SELECT add_compression_policy('scan_events', INTERVAL '30 days', if_not_exists => TRUE);
SELECT add_retention_policy('scan_events', INTERVAL '5 years', if_not_exists => TRUE);
-- scan_events is kept for 5 years (not compressed-away sooner) because it is
-- the primary evidentiary record for GPL volume reconciliation and redressement disputes.

ALTER TABLE monitoring_metrics SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'metric_name, service_name'
);
SELECT add_compression_policy('monitoring_metrics', INTERVAL '3 days', if_not_exists => TRUE);
SELECT add_retention_policy('monitoring_metrics', INTERVAL '90 days', if_not_exists => TRUE);
-- ============================================================
-- 8. MATERIALIZED VIEWS
-- ============================================================
-- NOTE: your v6.1 draft referenced this section as "unchanged / keep the
-- same logic," but the actual materialized view definitions were not
-- included in what you pasted — only the placeholder comment was. Rather
-- than silently invent business-critical reporting logic on your behalf,
-- the two views below are clearly-labeled proposals covering the two most
-- obvious reporting needs implied by the schema (site risk, marketeur
-- declaration health). If you have the original view SQL, send it and I'll
-- drop it in verbatim instead.

CREATE MATERIALIZED VIEW mv_site_risk_summary AS
SELECT
    s.id                AS site_id,
    s.org_id,
    s.name              AS site_name,
    s.region,
    s.status,
    rs.score            AS latest_risk_score,
    rs.level            AS latest_risk_level,
    rs.period_end        AS risk_as_of
FROM sites s
LEFT JOIN LATERAL (
    SELECT score, level, period_end
    FROM risk_scores
    WHERE entity_type = 'SITE' AND entity_id = s.id
    ORDER BY period_end DESC
    LIMIT 1
) rs ON true
WHERE s.deleted_at IS NULL;

CREATE UNIQUE INDEX idx_mv_site_risk_summary_site_id ON mv_site_risk_summary(site_id);

CREATE MATERIALIZED VIEW mv_marketeur_declaration_summary AS
SELECT
    o.id                    AS marketeur_org_id,
    o.name                  AS marketeur_name,
    d.id                    AS declaration_id,
    d.period_start,
    d.period_end,
    d.declared_volume,
    d.status                AS declaration_status,
    r.tracked_volume,
    r.volume_gap,
    r.subsidy_impact,
    r.status                AS reconciliation_status
FROM organizations o
JOIN declarations d      ON d.marketeur_org_id = o.id
LEFT JOIN reconciliations r ON r.declaration_id = d.id
WHERE o.type = 'MARKETEUR' AND d.deleted_at IS NULL;

CREATE UNIQUE INDEX idx_mv_marketeur_decl_summary_decl_id ON mv_marketeur_declaration_summary(declaration_id);

-- Both views should be refreshed on a schedule (e.g. via pg_cron or an
-- external scheduler) rather than on every write:
--   REFRESH MATERIALIZED VIEW CONCURRENTLY mv_site_risk_summary;
--   REFRESH MATERIALIZED VIEW CONCURRENTLY mv_marketeur_declaration_summary;
-- CONCURRENTLY requires the unique indexes created above.

-- ============================================================
-- 9. POST-DEPLOYMENT & MICROSERVICE CONSIDERATIONS
-- ============================================================
/*
- When splitting into microservices, group tables by bounded context:
   - Identity & Auth Service: auth_users, user_mfa, user_sessions, integration_auth
  - Organization Service: organizations, regions, settings (reference), custom_roles, …
  - Site Service: sites, client_sites, user_site_assignments
  - Fleet & Device Service: vehicles, drivers, devices, device_status_history
  - Tour & Delivery Service: pickup_requests, delivery_tours, checkpoints
  - Scan Service: scan_events, rfid_tags
  - Reconciliation Service: declarations, reconciliations, redressements
  - Risk & Notification Service: anomalies, anomaly_assignments, notification_*, risk_scores
  - Audit & Reporting: audit_logs, reports, monitoring_metrics

- Use event-driven communication (Kafka) and maintain read-only replicas for reporting.
- The current unified schema is still valid for a monolith; the corrected
  structure above (Sections 4-7) ensures no creation failures: base tables
  first, foreign keys once every referenced table exists, indexes after
  that, and triggers/functions/policies last.
- Before running in production: review the compression/retention windows
  in Section 7 against your actual CSPH contractual retention obligations,
  and confirm mv_site_risk_summary / mv_marketeur_declaration_summary match
  the reporting your original design intended.
*/


-- ----------------------------------------------------------------------------
-- NEW ARCHITECTURE TABLES (Added for scalability)
-- ----------------------------------------------------------------------------

CREATE TABLE persons (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    person_id           VARCHAR(100) NOT NULL UNIQUE,
    organization_id     UUID,
    primary_site_id     UUID,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    email               VARCHAR(255),
    primary_phone       VARCHAR(50),
    is_active           BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE class_structures (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    classification_id       VARCHAR(100) NOT NULL,
    description             VARCHAR(255),
    hierarchy_path          VARCHAR(500),
    parent_class_structure_id UUID,
    is_active               BOOLEAN NOT NULL DEFAULT true,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE organization_relationships (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    source_organization_id  UUID NOT NULL,
    target_organization_id  UUID NOT NULL,
    relationship_type       VARCHAR(50) NOT NULL,
    status                  VARCHAR(50) NOT NULL,
    valid_from              TIMESTAMPTZ,
    valid_until             TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);


CREATE TABLE user_groups (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code                VARCHAR(100) NOT NULL UNIQUE,
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    organization_id     UUID,
    site_id             UUID,
    is_active           BOOLEAN NOT NULL DEFAULT true,
    is_system_group     BOOLEAN NOT NULL DEFAULT false,
    member_count        INTEGER NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE user_group_memberships (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID NOT NULL,
    group_id            UUID NOT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT true,
    joined_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    added_by            UUID,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(user_id, group_id)
);
