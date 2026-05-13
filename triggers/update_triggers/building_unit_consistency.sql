CREATE OR REPLACE FUNCTION trg_building_unit_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.unit_id <> NEW.unit_id THEN
        RAISE EXCEPTION 'Нельзя изменить принадлежность здания к воинской части';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER building_unit_check
BEFORE UPDATE OF unit_id ON buildings
FOR EACH ROW EXECUTE FUNCTION trg_building_unit_change();
