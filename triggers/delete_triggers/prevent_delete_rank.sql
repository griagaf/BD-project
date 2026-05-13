CREATE OR REPLACE FUNCTION trg_prevent_delete_rank()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM personnel_ranks
        WHERE rank_id = OLD.rank_id
    ) THEN
        RAISE EXCEPTION 'Нельзя удалить звание, назначенное военнослужащим';
    END IF;

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER prevent_delete_rank
BEFORE DELETE ON military_ranks
FOR EACH ROW EXECUTE FUNCTION trg_prevent_delete_rank();
