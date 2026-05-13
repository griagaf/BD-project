-- 11.1
SELECT
    vps.last_name,
    vps.first_name,
    vps.middle_name,
    vps.rank_name,
    vps.specialty_name,
    vps.unit_name,
    vps.subdivision_name
FROM v_personnel_specialties vps
JOIN v_formation_closure fc
    ON fc.descendant_formation_id = vps.formation_id
WHERE fc.root_formation_name = 'Западный военный округ'
  AND vps.specialty_name = 'Связист'
ORDER BY vps.last_name, vps.first_name, vps.middle_name;

-- 11.2
SELECT
    vps.last_name,
    vps.first_name,
    vps.middle_name,
    vps.rank_name,
    vps.specialty_name,
    vps.unit_name,
    vps.subdivision_name
FROM v_personnel_specialties vps
WHERE vps.unit_name = '1-й танковый полк'
  AND vps.specialty_name = 'Связист'
ORDER BY vps.last_name, vps.first_name, vps.middle_name;

-- 11.3
SELECT
    vps.last_name,
    vps.first_name,
    vps.middle_name,
    vps.rank_name,
    vps.specialty_name,
    vps.unit_name,
    vps.subdivision_name
FROM v_personnel_specialties vps
WHERE vps.unit_name = '1-й танковый полк'
  AND vps.subdivision_name = '1-я рота'
  AND vps.specialty_name = 'Связист'
ORDER BY vps.last_name, vps.first_name, vps.middle_name;
