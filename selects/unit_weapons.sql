CREATE OR REPLACE VIEW v_unit_weapons AS
SELECT
    mu.unit_id,
    mu.name AS unit_name,
    mu.formation_id,
    mf.name AS formation_name,
    wc.category_id,
    wc.name AS weapon_category,
    wt.type_id,
    wt.name AS weapon_type,
    wiu.quantity
FROM military_units mu
JOIN military_formations mf ON mf.formation_id = mu.formation_id
JOIN weapon_in_units wiu ON wiu.unit_id = mu.unit_id
JOIN weapon_types wt ON wt.type_id = wiu.type_id
JOIN weapon_categories wc ON wc.category_id = wt.category_id;
