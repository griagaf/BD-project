ALTER TABLE buildings
    ADD COLUMN IF NOT EXISTS assignable BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE buildings
SET assignable = CASE
    WHEN lower(name) LIKE '%склад%'
      OR lower(name) LIKE '%хранилищ%'
      OR lower(name) LIKE '%ангар%'
      OR lower(name) LIKE '%парк%'
      OR lower(name) LIKE '%гараж%'
      OR lower(name) LIKE '%ремонт%'
    THEN FALSE
    ELSE TRUE
END;

CREATE OR REPLACE FUNCTION tdc_validate_building_assignment()
RETURNS TRIGGER AS $$
DECLARE
    allowed BOOLEAN;
BEGIN
    SELECT assignable
    INTO allowed
    FROM buildings
    WHERE building_id = NEW.building_id;

    IF allowed IS DISTINCT FROM TRUE THEN
        RAISE EXCEPTION 'BUILDING_NOT_ASSIGNABLE';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_building_assignment_assignable_guard ON subdivision_buildings;
CREATE TRIGGER trg_building_assignment_assignable_guard
BEFORE INSERT OR UPDATE ON subdivision_buildings
FOR EACH ROW
EXECUTE FUNCTION tdc_validate_building_assignment();
