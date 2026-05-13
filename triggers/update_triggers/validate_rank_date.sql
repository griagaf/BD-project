CREATE OR REPLACE FUNCTION trg_validate_rank_date()
RETURNS TRIGGER AS $$
DECLARE start_date DATE;
BEGIN
    SELECT service_start INTO start_date
    FROM personnel
    WHERE personnel_id = NEW.personnel_id;

    IF NEW.assignment_date < start_date THEN
        RAISE EXCEPTION 'Дата звания раньше начала службы';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER rank_date_check
BEFORE INSERT OR UPDATE ON personnel_ranks
FOR EACH ROW EXECUTE FUNCTION trg_validate_rank_date();
