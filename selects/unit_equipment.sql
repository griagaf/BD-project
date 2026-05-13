CREATE OR REPLACE VIEW v_unit_equipment AS
SELECT
    mu.unit_id,
    mu.name AS unit_name,
    mu.formation_id,
    mf.name AS formation_name,
    ec.category_id,
    ec.name AS equipment_category,
    et.type_id,
    et.name AS equipment_type,
    eiu.quantity
FROM military_units mu
JOIN military_formations mf ON mf.formation_id = mu.formation_id
JOIN equipment_in_units eiu ON eiu.unit_id = mu.unit_id
JOIN equipment_types et ON et.type_id = eiu.type_id
JOIN equipment_categories ec ON ec.category_id = et.category_id;
