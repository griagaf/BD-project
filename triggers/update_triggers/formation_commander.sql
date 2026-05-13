CREATE OR REPLACE FUNCTION trg_formation_commander()
RETURNS TRIGGER AS $$
DECLARE
    f_id INT;
BEGIN
    IF NEW.commander_id IS NULL THEN
        RETURN NEW;
    END IF;


    SELECT mf.formation_id INTO f_id
    FROM personnel p
    JOIN subdivisions s ON p.subdivision_id = s.subdivision_id
    JOIN military_units mu ON s.unit_id = mu.unit_id
    JOIN military_formations mf ON mu.formation_id = mf.formation_id
    WHERE p.personnel_id = NEW.commander_id;


    IF f_id IS NULL THEN
        RAISE EXCEPTION 'Командир не приписан ни к одному подразделению (нет цепочки подчинения)';
    END IF;

    IF f_id <> NEW.formation_id THEN
        RAISE EXCEPTION 'Командир не из этого формирования (служит в формировании %)', f_id;
    END IF;

    PERFORM trg_validate_commander(NEW.commander_id, 'formation', NEW.formation_type);

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER formation_commander
BEFORE INSERT OR UPDATE ON military_formations
FOR EACH ROW EXECUTE FUNCTION trg_formation_commander();
