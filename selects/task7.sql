-- 7.1
SELECT
    building_id,
    building_name,
    unit_name,
    subdivisions_count
FROM v_buildings_usage
WHERE unit_name = '1-й танковый полк'
ORDER BY building_name;

-- 7.2
SELECT
    building_id,
    building_name,
    unit_name,
    subdivisions_count
FROM v_buildings_usage
WHERE subdivisions_count > 1
ORDER BY unit_name, building_name;

-- 7.3
SELECT
    building_id,
    building_name,
    unit_name
FROM v_buildings_usage
WHERE subdivisions_count = 0
ORDER BY unit_name, building_name;
