CREATE OR REPLACE FUNCTION trg_prevent_delete_unit_with_resources()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM equipment_in_units WHERE unit_id = OLD.unit_id
    ) OR EXISTS (
        SELECT 1 FROM weapon_in_units WHERE unit_id = OLD.unit_id
    ) THEN
        RAISE EXCEPTION 'Нельзя удалить часть с техникой или вооружением';
    END IF;

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER prevent_delete_unit_resources
BEFORE DELETE ON military_units
FOR EACH ROW EXECUTE FUNCTION trg_prevent_delete_unit_with_resources();
