-- ============================================================================
-- CSPH LPG FLEET MANAGEMENT — SCRIPT DE SEED CARTE & ITINÉRAIRES VRAC
-- Compatible avec schema PostgreSQL csph_gpl_schema_v6_2.sql et JPA Hibernate
-- ============================================================================

-- 1. ORGANISATIONS (MARKETEUR, DEPOT, TRANSPORTEUR, CLIENT)
INSERT INTO organizations (id, name, code, type, status, row_stamp, created_at, created_by)
VALUES 
    ('11111111-1111-1111-1111-111111111111', 'TotalEnergies Marketing Cameroun', 'TOTAL_CM', 'MARKETEUR', 'ACTIVE', 1, NOW(), 'system'),
    ('22222222-2222-2222-2222-222222222222', 'SCDP Douala (Société Camerounaise des Dépôts Pétroliers)', 'SCDP_DLA', 'DEPOT', 'ACTIVE', 1, NOW(), 'system'),
    ('33333333-3333-3333-3333-333333333333', 'CAMTRANS GPL Logistique', 'CAMTRANS', 'TRANSPORTEUR', 'ACTIVE', 1, NOW(), 'system'),
    ('44444444-4444-4444-4444-444444444444', 'Groupe Hôtelier Akwa Palace', 'AKWA_CORP', 'CLIENT', 'ACTIVE', 1, NOW(), 'system')
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name;

-- 2. SITES MARCHANDS / CENTRES D'EMPLISSAGE (Coordonnées réelles Douala & Yaoundé)
INSERT INTO sites (id, organization_id, name, code, type, region, city, address_line1, latitude, longitude, is_operational, status, row_stamp, created_at, created_by)
VALUES
    ('aaaaaaaa-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 'Dépôt SCDP Bonabéri', 'SCDP_BON', 'CENTREEMPLISSEUR', 'LITTORAL', 'Douala', 'Zone Industrielle de Bonabéri, Douala', 4.0700, 9.6800, true, 'ACTIVE', 1, NOW(), 'system'),
    ('aaaaaaaa-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'Total Bonabéri (Centre Emplisseur & Vrac)', 'TOT_BON', 'CENTREEMPLISSEUR', 'LITTORAL', 'Douala', 'Route Nationale 3, Bonabéri, Douala', 4.0650, 9.6750, true, 'ACTIVE', 1, NOW(), 'system'),
    ('aaaaaaaa-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222222', 'Dépôt SCDP Nsam Yaoundé', 'SCDP_NSAM', 'CENTREEMPLISSEUR', 'CENTRE', 'Yaoundé', 'Nsam, Yaoundé, Cameroun', 3.8350, 11.5050, true, 'ACTIVE', 1, NOW(), 'system')
ON CONFLICT (id) DO UPDATE SET latitude = EXCLUDED.latitude, longitude = EXCLUDED.longitude;

-- 3. SITES CLIENTS (Points de livraison VRAC à Douala)
INSERT INTO client_sites (id, organization_id, name, code, type, region, address, city, latitude, longitude, status, row_stamp, created_at, created_by)
VALUES
    ('bbbbbbbb-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'Akwa Palace Hotel (Cuves GPL Vrac)', 'AKWA_PALACE', 'HOTEL', 'LITTORAL', 'Boulevard de la Liberté, Akwa, Douala, Cameroun', 'Douala', 4.0500, 9.7000, 'ACTIVE', 1, NOW(), 'system'),
    ('bbbbbbbb-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'Hôtel Sawa Douala', 'SAWA_HOTEL', 'HOTEL', 'LITTORAL', 'Avenue des Cocotiers, Bonanjo, Douala, Cameroun', 'Douala', 4.0450, 9.6950, 'ACTIVE', 1, NOW(), 'system')
ON CONFLICT (id) DO UPDATE SET latitude = EXCLUDED.latitude, longitude = EXCLUDED.longitude;

-- 4. VÉHICULES CITERNE VRAC
INSERT INTO vehicles (id, organization_id, license_plate, type, max_volume, status, row_stamp, created_at, created_by)
VALUES
    ('cccccccc-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', 'LT-982-AA', 'VRAC', 25.0, 'ACTIVE', 1, NOW(), 'system'),
    ('cccccccc-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'CE-415-BX', 'VRAC', 20.0, 'ACTIVE', 1, NOW(), 'system')
ON CONFLICT (id) DO UPDATE SET license_plate = EXCLUDED.license_plate;

-- 5. TOURNÉE DE LIVRAISON VRAC (Total Bonabéri -> Akwa Palace Hotel)
INSERT INTO delivery_tours (id, tour_code, execution_mode, type, status, marketeur_org_id, transporter_org_id, vehicle_id, requested_quantity, loaded_quantity, started_at, row_stamp, created_at, created_by)
VALUES
    ('dddddddd-1111-1111-1111-111111111111', 'TR-VRAC-DLA-001', 'EXTERNAL', 'VRAC', 'INPROGRESS', '11111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', 'cccccccc-1111-1111-1111-111111111111', 18.5, 18.5, NOW() - INTERVAL '45 minutes', 1, NOW(), 'system')
ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status;

-- 6. POINTS DE CONTRÔLE (CHECKPOINTS) DE LA TOURNÉE VRAC
INSERT INTO checkpoints (id, tour_id, sequence, site_id, status, expected_arrival, actual_arrival, row_stamp, created_at, created_by)
VALUES
    ('eeeeeeee-1111-1111-1111-111111111111', 'dddddddd-1111-1111-1111-111111111111', 1, 'aaaaaaaa-2222-2222-2222-222222222222', 'COMPLETED', NOW() - INTERVAL '45 minutes', NOW() - INTERVAL '30 minutes', 1, NOW(), 'system'),
    ('eeeeeeee-2222-2222-2222-222222222222', 'dddddddd-1111-1111-1111-111111111111', 2, 'bbbbbbbb-1111-1111-1111-111111111111', 'PENDING', NOW() + INTERVAL '15 minutes', NULL, 1, NOW(), 'system')
ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status;

-- 7. ANOMALIE GÉOLOCALISÉE SUR LE PARCOURS
INSERT INTO anomalies (id, type, category, severity, status, entity_type, entity_id, site_id, created_at, created_by)
VALUES
    ('ffffffff-1111-1111-1111-111111111111', 'VOLUMEGAP', 'INVESTIGATION', 'ELEVE', 'NOUVEAU', 'SITE', 'aaaaaaaa-2222-2222-2222-222222222222', 'aaaaaaaa-2222-2222-2222-222222222222', NOW() - INTERVAL '20 minutes', 'system')
ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status;
