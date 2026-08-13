import re

file_path = "csph_gpl_schema_v6_2.sql"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Update organizations table to add parent_id and tier
org_replacement = """CREATE TABLE organizations (
    id                     UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    parent_organization_id UUID,
    tier                   VARCHAR(50),
    name                   VARCHAR(255) NOT NULL,
    type                   org_type NOT NULL,"""
content = re.sub(r'CREATE TABLE organizations \(\n\s*id\s+UUID PRIMARY KEY DEFAULT uuid_generate_v4\(\),\n\s*name\s+VARCHAR\(255\) NOT NULL,\n\s*type\s+org_type NOT NULL,', org_replacement, content)

# 2. Update users table
users_replacement = """CREATE TABLE users (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    person_id            UUID NOT NULL UNIQUE,
    username             VARCHAR(255) NOT NULL UNIQUE,
    email                VARCHAR(255),
    password_hash        VARCHAR(255) NOT NULL,
    system_role          system_role NOT NULL,"""
content = re.sub(r'CREATE TABLE users \(\n\s*id\s+UUID PRIMARY KEY DEFAULT uuid_generate_v4\(\),\n\s*email\s+VARCHAR\(255\) NOT NULL UNIQUE,\n\s*password_hash\s+VARCHAR\(255\) NOT NULL,\n\s*first_name\s+VARCHAR\(100\) NOT NULL,\n\s*last_name\s+VARCHAR\(100\) NOT NULL,\n\s*system_role\s+system_role NOT NULL,', users_replacement, content)

# 3. Update client_sites table
client_sites_replacement = """CREATE TABLE client_sites (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    site_id                 UUID NOT NULL UNIQUE,
    client_org_id           UUID NOT NULL,
    site_contact_person_id  UUID,
    delivery_instructions   TEXT,
    specific_requirements   TEXT,
    requires_authorization  BOOLEAN NOT NULL DEFAULT false,
    operating_constraints   TEXT,"""
content = re.sub(r'CREATE TABLE client_sites \(\n\s*id\s+UUID PRIMARY KEY DEFAULT uuid_generate_v4\(\),\n\s*client_org_id\s+UUID NOT NULL,\n\s*region\s+region NOT NULL,\n\s*name\s+VARCHAR\(255\) NOT NULL,\n\s*address\s+TEXT,\n\s*geo_point\s+GEOMETRY\(POINT, 4326\),', client_sites_replacement, content)

# 4. Append new tables
new_tables = """

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
"""

if "CREATE TABLE persons" not in content:
    content += new_tables

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Schema updated successfully.")
