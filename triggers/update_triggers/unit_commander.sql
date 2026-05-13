CREATE OR REPLACE FUNCTION trg_unit_commander()
RETURNS TRIGGER AS $$
DECLARE
    u_id INT;
BEGIN
    IF NEW.commander_id IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT s.unit_id INTO u_id
    FROM personnel p
    JOIN subdivisions s ON p.subdivision_id = s.subdivision_id
    WHERE p.personnel_id = NEW.commander_id;

    IF u_id IS NULL THEN
        RAISE EXCEPTION 'Командир не приписан ни к одному подразделению (не может командовать частью)';
    END IF;

    IF u_id <> NEW.unit_id THEN
        RAISE EXCEPTION 'Командир не из этой воинской части (служит в части %)', u_id;
    END IF;

    PERFORM trg_validate_commander(NEW.commander_id, 'unit', 'Воинская часть');

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER unit_commander
BEFORE INSERT OR UPDATE ON military_units
FOR EACH ROW EXECUTE FUNCTION trg_unit_commander();
