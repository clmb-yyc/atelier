CREATE TABLE IF NOT EXISTS organizations (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS gyms (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name TEXT NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE,
    UNIQUE (organization_id, id)
);

CREATE TABLE IF NOT EXISTS organization_admins (
    organization_id UUID NOT NULL REFERENCES organizations(id),
    account_id UUID NOT NULL,
    PRIMARY KEY (organization_id, account_id)
);

CREATE TABLE IF NOT EXISTS gym_staff (
    gym_id UUID NOT NULL REFERENCES gyms(id),
    account_id UUID NOT NULL,
    PRIMARY KEY (gym_id, account_id)
);

CREATE TABLE IF NOT EXISTS gym_admins (
    gym_id UUID NOT NULL REFERENCES gyms(id),
    account_id UUID NOT NULL,
    PRIMARY KEY (gym_id, account_id)
);

CREATE TABLE IF NOT EXISTS walls (
    id UUID PRIMARY KEY,
    gym_id UUID NOT NULL REFERENCES gyms(id),
    name TEXT NOT NULL,
    discipline VARCHAR(16) NOT NULL CHECK (discipline IN ('BOULDER', 'ROPE')),
    archived_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS routes (
    id UUID PRIMARY KEY,
    wall_id UUID NOT NULL REFERENCES walls(id),
    name TEXT,
    grade TEXT NOT NULL,
    set_at TIMESTAMP WITH TIME ZONE NOT NULL,
    removed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS access_plans (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    gym_id UUID,
    name TEXT NOT NULL,
    valid_until TIMESTAMP WITH TIME ZONE,
    visit_limit INTEGER CHECK (visit_limit > 0),
    archived_at TIMESTAMP WITH TIME ZONE,
    FOREIGN KEY (organization_id, gym_id) REFERENCES gyms (organization_id, id)
);

-- Existing plans stay unclassified until an admin replaces them with a typed plan.
ALTER TABLE access_plans ADD COLUMN IF NOT EXISTS plan_type VARCHAR(16)
    CHECK (plan_type IN ('MEMBERSHIP', 'DAY_PASS', 'VISIT_PASS'));

CREATE TABLE IF NOT EXISTS member_passes (
    id UUID PRIMARY KEY,
    access_plan_id UUID NOT NULL REFERENCES access_plans(id),
    account_id UUID NOT NULL,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    CHECK (expires_at > starts_at)
);

CREATE TABLE IF NOT EXISTS check_ins (
    id UUID PRIMARY KEY,
    gym_id UUID NOT NULL REFERENCES gyms(id),
    member_pass_id UUID NOT NULL REFERENCES member_passes(id),
    checked_in_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS check_ins_member_pass_id_idx ON check_ins (member_pass_id);
CREATE INDEX IF NOT EXISTS member_passes_account_id_idx ON member_passes (account_id);
CREATE INDEX IF NOT EXISTS walls_gym_id_idx ON walls (gym_id);
CREATE INDEX IF NOT EXISTS routes_wall_id_idx ON routes (wall_id);
