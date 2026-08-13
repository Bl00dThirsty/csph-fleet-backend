import sys

file_path = "csph_gpl_schema_v6_2.sql"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

new_tables = """
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
"""

if "CREATE TABLE user_groups" not in content:
    with open(file_path, "a", encoding="utf-8") as f:
        f.write("\n" + new_tables)
    print("Tables user_groups added.")
else:
    print("Tables user_groups already exist.")
