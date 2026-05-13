-- 13.1
WITH units_count AS (
    SELECT
        fc.root_formation_id,
        fc.root_formation_name,
        fc.root_formation_type,
        COUNT(mu.unit_id) AS units_count
    FROM v_formation_closure fc
    LEFT JOIN military_units mu
        ON mu.formation_id = fc.descendant_formation_id
    WHERE fc.root_formation_type IN ('Армия', 'Дивизия', 'Корпус')
    GROUP BY
        fc.root_formation_id,
        fc.root_formation_name,
        fc.root_formation_type
),
ranked AS (
    SELECT
        uc.*,
        RANK() OVER (ORDER BY uc.units_count DESC) AS rnk
    FROM units_count uc
)
SELECT
    root_formation_type AS formation_type,
    root_formation_name AS formation_name,
    units_count
FROM ranked
WHERE rnk = 1
ORDER BY formation_type, formation_name;

-- 13.2
WITH units_count AS (
    SELECT
        fc.root_formation_id,
        fc.root_formation_name,
        fc.root_formation_type,
        COUNT(mu.unit_id) AS units_count
    FROM v_formation_closure fc
    LEFT JOIN military_units mu
        ON mu.formation_id = fc.descendant_formation_id
    WHERE fc.root_formation_type IN ('Армия', 'Дивизия', 'Корпус')
    GROUP BY
        fc.root_formation_id,
        fc.root_formation_name,
        fc.root_formation_type
),
ranked AS (
    SELECT
        uc.*,
        RANK() OVER (ORDER BY uc.units_count ASC) AS rnk
    FROM units_count uc
)
SELECT
    root_formation_type AS formation_type,
    root_formation_name AS formation_name,
    units_count
FROM ranked
WHERE rnk = 1
ORDER BY formation_type, formation_name;
