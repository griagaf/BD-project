-- 5.1
SELECT DISTINCT
    mu.name AS unit_name,
    l.city,
    l.address
FROM military_units mu
JOIN locations l
    ON l.location_id = mu.location_id
JOIN v_formation_closure fc
    ON fc.descendant_formation_id = mu.formation_id
WHERE fc.root_formation_name = 'Западный военный округ'
ORDER BY l.city, l.address, mu.name;

-- 5.2
SELECT
    mu.name AS unit_name,
    l.city,
    l.address
FROM military_units mu
JOIN locations l
    ON l.location_id = mu.location_id
WHERE mu.name = '1-й танковый полк'
ORDER BY mu.name;
