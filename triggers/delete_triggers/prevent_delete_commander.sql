CREATE OR REPLACE FUNCTION trg_prevent_delete_commander()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM military_formations WHERE commander_id = OLD.personnel_id
        UNION
        SELECT 1 FROM military_units WHERE commander_id = OLD.personnel_id
        UNION
        SELECT 1 FROM subdivisions WHERE commander_id = OLD.personnel_id
    ) THEN
        RAISE EXCEPTION 'Нельзя удалить военнослужащего, являющегося командиром';
    END IF;

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER prevent_delete_commander
BEFORE DELETE ON personnel
FOR EACH ROW EXECUTE FUNCTION trg_prevent_delete_commander();
