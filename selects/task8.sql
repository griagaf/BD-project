-- 8.1
SELECT
    unit_name,
    equipment_type,
    quantity
FROM v_unit_equipment
WHERE equipment_type = 'Т-72Б3'
  AND quantity > 5
ORDER BY unit_name;

-- 8.2
SELECT
    mu.name AS unit_name
FROM military_units mu
WHERE NOT EXISTS (
    SELECT 1
    FROM v_unit_equipment vue
    WHERE vue.unit_id = mu.unit_id
      AND vue.equipment_type = 'Т-72Б3'
)
ORDER BY mu.name;
