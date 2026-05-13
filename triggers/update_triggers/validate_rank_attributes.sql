CREATE OR REPLACE FUNCTION trg_validate_rank_attributes()
RETURNS TRIGGER AS $$
DECLARE r_id INT;
BEGIN
    SELECT rank_id INTO r_id
    FROM personnel_ranks
    WHERE personnel_id = NEW.personnel_id;

    IF NOT EXISTS (
        SELECT 1 FROM rank_type_attributes
        WHERE rank_id = r_id
        AND attribute_id = NEW.attribute_id
    ) THEN
        RAISE EXCEPTION 'Атрибут не соответствует званию';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER rank_attr_check
BEFORE INSERT OR UPDATE ON rank_attribute_values
FOR EACH ROW EXECUTE FUNCTION trg_validate_rank_attributes();
