-- 12.1
SELECT
    unit_name,
    weapon_type,
    quantity
FROM v_unit_weapons
WHERE weapon_type = 'АК-74М'
  AND quantity > 10
ORDER BY unit_name;

-- 12.2
SELECT
    mu.name AS unit_name
FROM military_units mu
WHERE NOT EXISTS (
    SELECT 1
    FROM v_unit_weapons vuw
    WHERE vuw.unit_id = mu.unit_id
      AND vuw.weapon_type = 'АК-74М'
)
ORDER BY mu.name;
