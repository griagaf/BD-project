CREATE OR REPLACE FUNCTION trg_protect_command_requirements()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Удаление требований запрещено';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER protect_command_requirements
BEFORE DELETE ON command_requirements
FOR EACH ROW EXECUTE FUNCTION trg_protect_command_requirements();
