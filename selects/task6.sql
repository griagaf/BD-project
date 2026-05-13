-- 6.1
SELECT
    vue.unit_name,
    vue.equipment_category,
    vue.equipment_type,
    vue.quantity
FROM v_unit_equipment vue
JOIN v_formation_closure fc
    ON fc.descendant_formation_id = vue.formation_id
WHERE fc.root_formation_name = 'Западный военный округ'
ORDER BY vue.unit_name, vue.equipment_category, vue.equipment_type;

-- 6.2
SELECT
    vue.unit_name,
    vue.equipment_category,
    vue.equipment_type,
    vue.quantity
FROM v_unit_equipment vue
JOIN v_formation_closure fc
    ON fc.descendant_formation_id = vue.formation_id
WHERE fc.root_formation_name = 'Западный военный округ'
  AND vue.equipment_category = 'Основные боевые танки'
ORDER BY vue.unit_name, vue.equipment_type;

-- 6.3
SELECT
    vue.unit_name,
    vue.equipment_category,
    vue.equipment_type,
    vue.quantity
FROM v_unit_equipment vue
JOIN v_formation_closure fc
    ON fc.descendant_formation_id = vue.formation_id
WHERE fc.root_formation_name = 'Западный военный округ'
  AND vue.equipment_type = 'Т-72Б3'
ORDER BY vue.unit_name;

-- 6.4
SELECT
    vue.unit_name,
    vue.equipment_category,
    vue.equipment_type,
    vue.quantity
FROM v_unit_equipment vue
WHERE vue.unit_name = '1-й танковый полк'
ORDER BY vue.equipment_category, vue.equipment_type;
