WITH RECURSIVE subdivision_chain AS (
    SELECT
        s.subdivision_id,
        s.name,
        s.type,
        s.parent_id,
        s.commander_id,
        1 AS level_no
    FROM personnel p
    JOIN subdivisions s
        ON s.subdivision_id = p.subdivision_id
    WHERE p.personnel_id = 44

    UNION ALL

    SELECT
        parent_s.subdivision_id,
        parent_s.name,
        parent_s.type,
        parent_s.parent_id,
        parent_s.commander_id,
        sc.level_no + 1
    FROM subdivisions parent_s
    JOIN subdivision_chain sc
        ON sc.parent_id = parent_s.subdivision_id
),
person_unit AS (
    SELECT
        mu.unit_id,
        mu.name AS unit_name,
        mu.formation_id,
        mu.commander_id
    FROM personnel p
    JOIN subdivisions s
        ON s.subdivision_id = p.subdivision_id
    JOIN military_units mu
        ON mu.unit_id = s.unit_id
    WHERE p.personnel_id = 44
),
formation_chain AS (
    SELECT
        mf.formation_id,
        mf.name,
        mf.formation_type,
        mf.parent_id,
        mf.commander_id,
        1 AS level_no
    FROM person_unit pu
    JOIN military_formations mf
        ON mf.formation_id = pu.formation_id

    UNION ALL

    SELECT
        parent_f.formation_id,
        parent_f.name,
        parent_f.formation_type,
        parent_f.parent_id,
        parent_f.commander_id,
        fc.level_no + 1
    FROM military_formations parent_f
    JOIN formation_chain fc
        ON fc.parent_id = parent_f.formation_id
)
SELECT
    0 AS sort_group,
    0 AS level_no,
    'Военнослужащий' AS object_type,
    CONCAT(p.last_name, ' ', p.first_name, COALESCE(' ' || p.middle_name, '')) AS object_name,
    mr.name AS extra_info
FROM personnel p
LEFT JOIN personnel_ranks pr
    ON pr.personnel_id = p.personnel_id
LEFT JOIN military_ranks mr
    ON mr.rank_id = pr.rank_id
WHERE p.personnel_id = 44

UNION ALL

SELECT
    1 AS sort_group,
    sc.level_no,
    sc.type AS object_type,
    sc.name AS object_name,
    CONCAT(cp.last_name, ' ', cp.first_name, COALESCE(' ' || cp.middle_name, '')) AS extra_info
FROM subdivision_chain sc
LEFT JOIN personnel cp
    ON cp.personnel_id = sc.commander_id

UNION ALL

SELECT
    2 AS sort_group,
    1 AS level_no,
    'Часть' AS object_type,
    pu.unit_name AS object_name,
    CONCAT(cp.last_name, ' ', cp.first_name, COALESCE(' ' || cp.middle_name, '')) AS extra_info
FROM person_unit pu
LEFT JOIN personnel cp
    ON cp.personnel_id = pu.commander_id

UNION ALL

SELECT
    3 AS sort_group,
    fc.level_no,
    fc.formation_type AS object_type,
    fc.name AS object_name,
    CONCAT(cp.last_name, ' ', cp.first_name, COALESCE(' ' || cp.middle_name, '')) AS extra_info
FROM formation_chain fc
LEFT JOIN personnel cp
    ON cp.personnel_id = fc.commander_id

ORDER BY sort_group, level_no;
