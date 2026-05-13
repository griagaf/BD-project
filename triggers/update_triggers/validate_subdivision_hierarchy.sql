CREATE OR REPLACE FUNCTION trg_validate_subdivision_hierarchy()
RETURNS TRIGGER AS $$
DECLARE parent_type TEXT;
BEGIN
    IF NEW.parent_id IS NULL THEN
        IF NEW.type <> 'Батальон' THEN
            RAISE EXCEPTION 'Только "Батальон" может быть верхним уровнем';
        END IF;
        RETURN NEW;
    END IF;

    IF NEW.type = 'Батальон' THEN
        RAISE EXCEPTION 'Батальон не может иметь родителя';
    END IF;

    SELECT type INTO parent_type
    FROM subdivisions
    WHERE subdivision_id = NEW.parent_id;

    IF parent_type IS NULL THEN
        RAISE EXCEPTION 'Родительское подразделение с ID % не существует', NEW.parent_id;
    END IF;

    IF NOT (
        (NEW.type = 'Рота' AND parent_type = 'Батальон') OR
        (NEW.type = 'Взвод' AND parent_type = 'Рота') OR
        (NEW.type = 'Отделение' AND parent_type = 'Взвод')
    ) THEN
        RAISE EXCEPTION 'Неверная иерархия подразделения: "%" не может быть дочерним для "%"',
            NEW.type, parent_type;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER subdivision_hierarchy
BEFORE INSERT OR UPDATE ON subdivisions
FOR EACH ROW EXECUTE FUNCTION trg_validate_subdivision_hierarchy();
