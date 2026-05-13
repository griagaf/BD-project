-- 9.1
SELECT
    vuw.unit_name,
    vuw.weapon_category,
    vuw.weapon_type,
    vuw.quantity
FROM v_unit_weapons vuw
JOIN v_formation_closure fc
    ON fc.descendant_formation_id = vuw.formation_id
WHERE fc.root_formation_name = 'Западный военный округ'
ORDER BY vuw.unit_name, vuw.weapon_category, vuw.weapon_type;

-- 9.2
SELECT
    vuw.unit_name,
    vuw.weapon_category,
    vuw.weapon_type,
    vuw.quantity
FROM v_unit_weapons vuw
JOIN v_formation_closure fc
    ON fc.descendant_formation_id = vuw.formation_id
WHERE fc.root_formation_name = 'Западный военный округ'
  AND vuw.weapon_category = 'Автоматическое оружие'
ORDER BY vuw.unit_name, vuw.weapon_type;

-- 9.3
SELECT
    vuw.unit_name,
    vuw.weapon_category,
    vuw.weapon_type,
    vuw.quantity
FROM v_unit_weapons vuw
JOIN v_formation_closure fc
    ON fc.descendant_formation_id = vuw.formation_id
WHERE fc.root_formation_name = 'Западный военный округ'
  AND vuw.weapon_type = 'АК-74М'
ORDER BY vuw.unit_name;

-- 9.4
SELECT
    vuw.unit_name,
    vuw.weapon_category,
    vuw.weapon_type,
    vuw.quantity
FROM v_unit_weapons vuw
WHERE vuw.unit_name = '1-й танковый полк'
ORDER BY vuw.weapon_category, vuw.weapon_type;
