CREATE OR REPLACE FUNCTION trg_subdivision_unit_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.unit_id <> NEW.unit_id THEN
        IF EXISTS (
            SELECT 1
            FROM subdivision_buildings sb
            JOIN buildings b ON sb.building_id = b.building_id
            WHERE sb.subdivision_id = NEW.subdivision_id
            AND b.unit_id <> NEW.unit_id
        ) THEN
            RAISE EXCEPTION 'Нельзя изменить часть подразделения: есть привязанные здания из другой части';
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER subdivision_unit_check
BEFORE UPDATE OF unit_id ON subdivisions
FOR EACH ROW EXECUTE FUNCTION trg_subdivision_unit_change();
