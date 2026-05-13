CREATE OR REPLACE VIEW v_personnel_full AS
SELECT
    p.personnel_id,
    p.last_name,
    p.first_name,
    p.middle_name,
    p.personal_number,
    p.birth_date,
    p.service_start,

    mr.rank_id,
    mr.name AS rank_name,
    mr.category AS rank_category,

    s.subdivision_id,
    s.name AS subdivision_name,
    s.type AS subdivision_type,
    s.parent_id AS subdivision_parent_id,
    s.commander_id AS subdivision_commander_id,

    mu.unit_id,
    mu.name AS unit_name,
    mu.commander_id AS unit_commander_id,
    mu.location_id,
    mu.formation_id,

    mf.name AS formation_name,
    mf.formation_type,
    mf.parent_id AS formation_parent_id,
    mf.commander_id AS formation_commander_id
FROM personnel p
JOIN subdivisions s ON s.subdivision_id = p.subdivision_id
JOIN military_units mu ON mu.unit_id = s.unit_id
JOIN military_formations mf ON mf.formation_id = mu.formation_id
LEFT JOIN personnel_ranks pr ON pr.personnel_id = p.personnel_id
LEFT JOIN military_ranks mr ON mr.rank_id = pr.rank_id;
