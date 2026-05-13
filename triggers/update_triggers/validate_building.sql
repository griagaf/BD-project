CREATE OR REPLACE FUNCTION trg_validate_building()
RETURNS TRIGGER AS $$
DECLARE
    sub_unit INT;
    b_unit INT;
BEGIN
    SELECT unit_id INTO sub_unit
    FROM subdivisions
    WHERE subdivision_id = NEW.subdivision_id;

    SELECT unit_id INTO b_unit
    FROM buildings
    WHERE building_id = NEW.building_id;

    IF sub_unit IS NULL THEN
        RAISE EXCEPTION 'Подразделение с ID % не существует', NEW.subdivision_id;
    END IF;

    IF b_unit IS NULL THEN
        RAISE EXCEPTION 'Здание с ID % не существует', NEW.building_id;
    END IF;

    IF sub_unit <> b_unit THEN
        RAISE EXCEPTION 'Подразделение (часть %) и здание (часть %) должны быть в одной воинской части',
            sub_unit, b_unit;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER building_check
BEFORE INSERT OR UPDATE ON subdivision_buildings
FOR EACH ROW EXECUTE FUNCTION trg_validate_building();
