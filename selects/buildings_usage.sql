CREATE OR REPLACE VIEW v_buildings_usage AS
SELECT
    b.building_id,
    b.name AS building_name,
    b.unit_id,
    mu.name AS unit_name,
    COUNT(sb.subdivision_id) AS subdivisions_count
FROM buildings b
JOIN military_units mu ON mu.unit_id = b.unit_id
LEFT JOIN subdivision_buildings sb ON sb.building_id = b.building_id
GROUP BY b.building_id, b.name, b.unit_id, mu.name;
