BEGIN;

CREATE TABLE IF NOT EXISTS grading_systems (
    id UUID PRIMARY KEY,
    gym_id UUID REFERENCES gyms (id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    visibility VARCHAR(7) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT grading_system_scope CHECK (
        (visibility = 'PUBLIC' AND gym_id IS NULL)
        OR (visibility = 'PRIVATE' AND gym_id IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS grading_systems_gym_id_idx ON grading_systems (gym_id);

CREATE TABLE IF NOT EXISTS grades (
    id UUID PRIMARY KEY,
    grading_system_id UUID NOT NULL REFERENCES grading_systems (id),
    label VARCHAR(255) NOT NULL,
    description TEXT,
    sort_order INTEGER NOT NULL CHECK (sort_order >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT grades_system_label_unique UNIQUE (grading_system_id, label)
);

INSERT INTO grading_systems (id, name, description, visibility, created_at)
VALUES
    ('b9e31050-3b21-4fc9-a5e4-3fca253664c6', 'V-scale', 'American bouldering grades', 'PUBLIC', CURRENT_TIMESTAMP),
    ('974eb84f-2844-4805-823e-ba92a16553d4', 'Fontainebleau', 'French bouldering grades', 'PUBLIC', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

INSERT INTO grades (id, grading_system_id, label, sort_order, created_at)
SELECT gen_random_uuid(), CAST('b9e31050-3b21-4fc9-a5e4-3fca253664c6' AS UUID), seed.label, seed.sort_order, CURRENT_TIMESTAMP
FROM (VALUES
    ('VB', 0), ('V0', 1), ('V1', 2), ('V2', 3), ('V3', 4),
    ('V4', 5), ('V5', 6), ('V6', 7), ('V7', 8), ('V8', 9),
    ('V9', 10), ('V10', 11), ('V11', 12), ('V12', 13), ('V13', 14),
    ('V14', 15), ('V15', 16), ('V16', 17), ('V17', 18)
) AS seed(label, sort_order)
ON CONFLICT DO NOTHING;

INSERT INTO grades (id, grading_system_id, label, sort_order, created_at)
SELECT gen_random_uuid(), CAST('974eb84f-2844-4805-823e-ba92a16553d4' AS UUID), seed.label, seed.sort_order, CURRENT_TIMESTAMP
FROM (VALUES
    ('1', 0), ('2', 1), ('3', 2), ('4', 3), ('4+', 4),
    ('5', 5), ('5+', 6), ('6A', 7), ('6A+', 8), ('6B', 9),
    ('6B+', 10), ('6C', 11), ('6C+', 12), ('7A', 13), ('7A+', 14),
    ('7B', 15), ('7B+', 16), ('7C', 17), ('7C+', 18), ('8A', 19),
    ('8A+', 20), ('8B', 21), ('8B+', 22), ('8C', 23), ('8C+', 24),
    ('9A', 25)
) AS seed(label, sort_order)
ON CONFLICT DO NOTHING;

COMMIT;
