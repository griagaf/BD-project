-- 10.1
SELECT
    vps.specialty_name,
    COUNT(DISTINCT vps.personnel_id) AS specialists_count
FROM v_personnel_specialties vps
JOIN v_formation_closure fc
    ON fc.descendant_formation_id = vps.formation_id
WHERE fc.root_formation_name = 'Западный военный округ'
GROUP BY vps.specialty_id, vps.specialty_name
HAVING COUNT(DISTINCT vps.personnel_id) > 5
ORDER BY vps.specialty_name;

-- 10.2
WITH scoped_specialties AS (
    SELECT DISTINCT
        vps.specialty_id,
        vps.personnel_id
    FROM v_personnel_specialties vps
    JOIN v_formation_closure fc
        ON fc.descendant_formation_id = vps.formation_id
    WHERE fc.root_formation_name = 'Западный военный округ'
)
SELECT
    s.name AS specialty_name
FROM specialties s
LEFT JOIN scoped_specialties ss
    ON ss.specialty_id = s.specialty_id
GROUP BY s.specialty_id, s.name
HAVING COUNT(ss.personnel_id) = 0
ORDER BY s.name;

-- 10.3
SELECT
    vps.specialty_name,
    COUNT(DISTINCT vps.personnel_id) AS specialists_count
FROM v_personnel_specialties vps
WHERE vps.unit_name = '1-й танковый полк'
GROUP BY vps.specialty_id, vps.specialty_name
HAVING COUNT(DISTINCT vps.personnel_id) > 5
ORDER BY vps.specialty_name;

-- 10.4
WITH unit_specialties AS (
    SELECT DISTINCT
        specialty_id,
        personnel_id
    FROM v_personnel_specialties
    WHERE unit_name = '1-й танковый полк'
)
SELECT
    s.name AS specialty_name
FROM specialties s
LEFT JOIN unit_specialties us
    ON us.specialty_id = s.specialty_id
GROUP BY s.specialty_id, s.name
HAVING COUNT(us.personnel_id) = 0
ORDER BY s.name;
