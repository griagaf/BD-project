CREATE OR REPLACE FUNCTION trg_validate_formation_hierarchy()
RETURNS TRIGGER AS $$
DECLARE parent_type TEXT;
BEGIN
    IF NEW.parent_id IS NULL THEN
        IF NEW.formation_type <> 'Округ' THEN
            RAISE EXCEPTION 'Только "Округ" может быть без родителя';
        END IF;
        RETURN NEW;
    END IF;

    IF NEW.formation_type = 'Округ' THEN
        RAISE EXCEPTION 'Округ не может иметь родителя';
    END IF;

    SELECT formation_type INTO parent_type
    FROM military_formations
    WHERE formation_id = NEW.parent_id;

    IF parent_type IS NULL THEN
        RAISE EXCEPTION 'Родительское формирование с ID % не существует', NEW.parent_id;
    END IF;

    IF NOT (
        (NEW.formation_type = 'Армия' AND parent_type = 'Округ') OR
        (NEW.formation_type = 'Корпус' AND parent_type = 'Армия') OR
        (NEW.formation_type = 'Дивизия' AND parent_type = 'Корпус') OR
        (NEW.formation_type = 'Бригада' AND parent_type = 'Дивизия')
    ) THEN
        RAISE EXCEPTION 'Неверная иерархия формирования: "%" не может быть дочерним для "%"',
            NEW.formation_type, parent_type;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER formation_hierarchy
BEFORE INSERT OR UPDATE ON military_formations
FOR EACH ROW EXECUTE FUNCTION trg_validate_formation_hierarchy();
