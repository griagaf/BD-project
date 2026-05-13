CREATE OR REPLACE FUNCTION trg_subdivision_commander()
RETURNS TRIGGER AS $$
DECLARE
    p_sub INT;
BEGIN
    IF NEW.commander_id IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT subdivision_id INTO p_sub
    FROM personnel
    WHERE personnel_id = NEW.commander_id;

    IF p_sub IS NULL THEN
        RAISE EXCEPTION 'Командир не приписан ни к одному подразделению';
    END IF;

    IF p_sub <> NEW.subdivision_id THEN
        RAISE EXCEPTION 'Командир не из этого подразделения (служит в подразделении %)', p_sub;
    END IF;

    PERFORM trg_validate_commander(NEW.commander_id, 'subdivision', NEW.type);

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER subdivision_commander
BEFORE INSERT OR UPDATE ON subdivisions
FOR EACH ROW EXECUTE FUNCTION trg_subdivision_commander();
