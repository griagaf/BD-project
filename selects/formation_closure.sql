CREATE OR REPLACE VIEW v_formation_closure AS
WITH RECURSIVE formation_closure AS (
    SELECT
        mf.formation_id AS root_formation_id,
        mf.name AS root_formation_name,
        mf.formation_type AS root_formation_type,
        mf.formation_id AS descendant_formation_id,
        mf.name AS descendant_formation_name,
        mf.formation_type AS descendant_formation_type
    FROM military_formations mf

    UNION ALL

    SELECT
        fc.root_formation_id,
        fc.root_formation_name,
        fc.root_formation_type,
        child.formation_id AS descendant_formation_id,
        child.name AS descendant_formation_name,
        child.formation_type AS descendant_formation_type
    FROM formation_closure fc
    JOIN military_formations child
        ON child.parent_id = fc.descendant_formation_id
)
SELECT *
FROM formation_closure;
