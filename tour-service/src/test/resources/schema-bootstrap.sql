-- ============================================================
-- TourLifecycleIT schema bootstrap.
--
-- Brings the running gpl_tour_db (currently on the legacy Hibernate-
-- generated schema) up to a state compatible with the v6_2 entities
-- shipped in src/main, so Hibernate's ddl-auto can complete without
-- DDL failures during the IT.
--
-- This file is sourced once before the Spring context boots
-- (see TourLifecycleIT.schemaBootstrap). It is idempotent — each
-- statement uses IF NOT EXISTS / DO blocks so re-runs are safe.
-- ============================================================

-- 1) Enum types expected by the entity model (Hibernate emits these
--    in CREATE TABLE DDL; if absent, the EntityManagerFactory fails to
--    build with "type X does not exist").
CREATE TYPE IF NOT EXISTS tournee_status AS ENUM (
    'DRAFT', 'PLANNED', 'PENDINGTRANSPORTERACK', 'ACKNOWLEDGED',
    'INPROGRESS', 'CHECKPOINTACTIVE', 'CLOSED', 'CANCELLED'
);
CREATE TYPE IF NOT EXISTS checkpoint_status AS ENUM (
    'PENDING', 'REACHED', 'COMPLETED', 'SKIPPED'
);
DO $$ BEGIN
    CREATE TYPE scan_direction AS ENUM ('IN', 'OUT');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- 2) PostGIS — the geo_point column on scan_events is GEOMETRY(Point, 4326).
--    Skip silently if PostGIS is unavailable; the IT will then skip the
--    scan_events fan-out assertion (see TourLifecycleIT.skipScanEventsIfNoPostgis).
CREATE EXTENSION IF NOT EXISTS postgis;

-- 3) New columns added by Task 1.4 (Tour + Checkpoint alignment).
--    Old rows may exist (the running tour-service has been writing since
--    Sep 27); backfill with safe defaults so the ALTER ... NOT NULL succeeds.
ALTER TABLE delivery_tours
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE delivery_tours
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE delivery_tours
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(36);
ALTER TABLE delivery_tours
    ADD COLUMN IF NOT EXISTS livreur_user_id UUID;

ALTER TABLE checkpoints
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE checkpoints
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE checkpoints
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(36);

-- 4) scan_events table — created here so the IT does not depend on
--    Hibernate's auto-DDL succeeding against the running cluster
--    (which has PostGIS installed manually, not bundled with the
--    Hibernate dialect defaults).
CREATE TABLE IF NOT EXISTS scan_events (
    id               UUID NOT NULL,
    checkpoint_id    UUID NOT NULL,
    livreur_user_id  UUID NOT NULL,
    rfid_tag_id      UUID,
    direction        scan_direction NOT NULL,
    geo_point        GEOMETRY(Point, 4326) NOT NULL,
    timestamp        TIMESTAMPTZ NOT NULL DEFAULT now(),
    meter_reading    DOUBLE PRECISION CHECK (meter_reading >= 0),
    photo_url        TEXT,
    pda_sync_id      VARCHAR(100),
    conflict_status  VARCHAR(20),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by       UUID,
    PRIMARY KEY (id, timestamp)
);
