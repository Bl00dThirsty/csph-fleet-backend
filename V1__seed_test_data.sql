-- ============================================================
-- SCRIPT DE SEED DATA DE TEST - CSPH GPL LIVRAISONS
-- Compatible avec l'Architecture V2 (Séparation Person/User, Tiers)
-- ============================================================

-- 1. ORGANISATIONS (Ajout du champ TIER)
INSERT INTO organizations (id, tier, name, type, registration_number, tax_id, is_active, operational_site_count, client_site_count, vehicle_count, driver_count, user_count)
VALUES 
('11111111-1111-1111-1111-111111111111', 'TIER_1_GOVERNANCE', 'CSPH - Comité de Stabilisation des Prix des Hydrocarbures', 'REGULATEUR', 'RC/YAO/2020/B/001', 'M01200001', true, 1, 0, 0, 0, 4),
('22222222-2222-2222-2222-222222222222', 'TIER_3_OPERATIONS', 'TOTAL Cameroun SA', 'MARKETEUR', 'RC/DLA/2015/B/055', 'M02150002', true, 2, 0, 0, 0, 2),
('33333333-3333-3333-3333-333333333333', 'TIER_3_OPERATIONS', 'Trans-GPL Express SARL', 'TRANSPORTEUR', 'RC/DLA/2018/B/102', 'M03180003', true, 1, 0, 2, 2, 3),
('44444444-4444-4444-4444-444444444444', 'TIER_4_CONSUMPTION', 'GazPro SARL', 'CLIENT', 'RC/DLA/2021/B/304', 'M04210004', true, 0, 1, 0, 0, 1)
ON CONFLICT (id) DO NOTHING;

-- 2. SYSTEM ROLES
INSERT INTO system_roles (id, name, description, hierarchy_level, can_create_subroles, can_assign_roles)
VALUES
('00000000-0000-0000-0000-000000000001', 'SUPERADMIN', 'Supervision totale — carte ultra-détaillée, tous modules', 100, true, true),
('00000000-0000-0000-0000-000000000002', 'ADMIN', 'Staff CSPH / RH — gestion utilisateurs, agents, marketeurs, rapports', 80, true, true),
('00000000-0000-0000-0000-000000000003', 'SUPERVISOR', 'DevOps / monitoring — Prometheus, Grafana, alertes, scores de risque', 60, false, false),
('00000000-0000-0000-0000-000000000004', 'INTEGRATEUR', 'Spécialiste domaine — activation, authentification, maintenance matériel PDA+GPS+RFID', 60, false, false),
('00000000-0000-0000-0000-000000000005', 'AGENT', 'Validateur terrain — suivi marketeurs, reset passwords, validation déclarations', 60, false, false),
('00000000-0000-0000-0000-000000000006', 'MARKETEUR', 'Société pétrolière — flotte, tournées, quotas, chauffeurs, règles personnalisées', 40, false, false),
('00000000-0000-0000-0000-000000000007', 'TRANSPORTEUR', 'Transporteur — flotte, tournées, scans RFID/PDA, points de contrôle et chauffeurs', 40, false, false),
('00000000-0000-0000-0000-000000000008', 'LIVREUR', 'Application PDA mobile — missions, scans RFID et livraisons (sans interface web)', 20, false, false)
ON CONFLICT (name) DO NOTHING;

-- 3. PERSONS (Nouvelle table)
INSERT INTO persons (id, person_id, organization_id, first_name, last_name, email, is_active)
VALUES
-- CSPH
('p1111111-1111-1111-1111-111111111111', 'P-CSPH-001', '11111111-1111-1111-1111-111111111111', 'Emmanuel', 'Mbarga', 'superadmin.csphq@csphq.cm', true),
('p1111111-1111-1111-1111-222222222222', 'P-CSPH-002', '11111111-1111-1111-1111-111111111111', 'Admin', 'CSPH', 'admin.csphq@csphq.cm', true),
('p1111111-1111-1111-1111-333333333333', 'P-CSPH-003', '11111111-1111-1111-1111-111111111111', 'Robert', 'Atangana', 'superviseur.csphq@csphq.cm', true),
-- TOTAL
('p2222222-2222-2222-2222-111111111111', 'P-TOT-001', '22222222-2222-2222-2222-222222222222', 'Alice', 'Fouda', 'gest.gpl@total-cam.cm', true),
-- TRANS-GPL
('p3333333-3333-3333-3333-111111111111', 'P-TRG-001', '33333333-3333-3333-3333-333333333333', 'Jacques', 'Tabi', 'resp.abc@transgpl.cm', true),
('p3333333-3333-3333-3333-222222222222', 'P-TRG-002', '33333333-3333-3333-3333-333333333333', 'Pierre', 'Essomba', 'chauffeur.abc1@transgpl.cm', true)
ON CONFLICT (person_id) DO NOTHING;

-- 4. USERS (Séparé de Persons)
-- Mots de passe hashés avec BCrypt pour "Password123!"
INSERT INTO auth_users (id, person_id, username, email, password_hash, system_role, org_id, is_active, mfa_status)
VALUES
-- CSPH Users
('a1111111-1111-1111-1111-111111111111', 'p1111111-1111-1111-1111-111111111111', 'embarga', 'superadmin.csphq@csphq.cm', '$2a$10$e8WpX4Osh.1LgQnZtI6b0uK403b9P5i9aXG.qF9x4pWq3j', 'SUPERADMIN', '11111111-1111-1111-1111-111111111111', true, 'DISABLED'),
('a1111111-1111-1111-1111-222222222222', 'p1111111-1111-1111-1111-222222222222', 'admin', 'admin.csphq@csphq.cm', '$2a$10$e8WpX4Osh.1LgQnZtI6b0uK403b9P5i9aXG.qF9x4pWq3j', 'ADMIN', '11111111-1111-1111-1111-111111111111', true, 'DISABLED'),
('a1111111-1111-1111-1111-333333333333', 'p1111111-1111-1111-1111-333333333333', 'ratangana', 'superviseur.csphq@csphq.cm', '$2a$10$e8WpX4Osh.1LgQnZtI6b0uK403b9P5i9aXG.qF9x4pWq3j', 'SUPERVISOR', '11111111-1111-1111-1111-111111111111', true, 'DISABLED'),
-- TOTAL User
('a2222222-2222-2222-2222-111111111111', 'p2222222-2222-2222-2222-111111111111', 'afouda', 'gest.gpl@total-cam.cm', '$2a$10$e8WpX4Osh.1LgQnZtI6b0uK403b9P5i9aXG.qF9x4pWq3j', 'MARKETEUR', '22222222-2222-2222-2222-222222222222', true, 'DISABLED')
ON CONFLICT (username) DO NOTHING;

-- 5. GROUPES D'UTILISATEURS (User Groups)
INSERT INTO user_groups (id, code, name, description, organization_id, is_active, is_system_group)
VALUES
('h1111111-1111-1111-1111-111111111111', 'GRP_DIR_CSPH', 'Direction CSPH', 'Membres du top management', '11111111-1111-1111-1111-111111111111', true, false),
('h1111111-1111-1111-1111-222222222222', 'GRP_SUP_TERRAIN', 'Supervision Terrain CSPH', 'Agents de terrain et auditeurs', '11111111-1111-1111-1111-111111111111', true, false)
ON CONFLICT (code) DO NOTHING;

INSERT INTO user_group_memberships (group_id, user_id)
VALUES
('h1111111-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-111111111111'),
('h1111111-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-222222222222'),
('h1111111-1111-1111-1111-222222222222', 'a1111111-1111-1111-1111-333333333333')
ON CONFLICT (group_id, user_id) DO NOTHING;
