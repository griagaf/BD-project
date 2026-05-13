CREATE OR REPLACE FUNCTION trg_validate_commander(
    p_personnel_id INT,
    p_scope TEXT,
    p_type TEXT
) RETURNS VOID AS $$
DECLARE
    r_level INT;
    min_lvl INT;
    max_lvl INT;
    rank_name TEXT;
BEGIN
    SELECT mr.rank_level, mr.name INTO r_level, rank_name
    FROM personnel_ranks pr
    JOIN military_ranks mr ON pr.rank_id = mr.rank_id
    WHERE pr.personnel_id = p_personnel_id;

    IF r_level IS NULL THEN
        RAISE EXCEPTION 'У военнослужащего ID % не указано воинское звание', p_personnel_id;
    END IF;

    SELECT min_rank_level, max_rank_level
    INTO min_lvl, max_lvl
    FROM command_requirements
    WHERE entity_scope = p_scope
      AND type_name = p_type;

    IF min_lvl IS NULL THEN
        RAISE EXCEPTION 'Неизвестная должность: scope=%, type=%', p_scope, p_type;
    END IF;

    IF r_level < min_lvl THEN
        RAISE EXCEPTION 'Звание "%" (уровень %) слишком низкое для командования "%" (требуется уровень >= %)',
            rank_name, r_level, p_type, min_lvl;
    END IF;

    IF max_lvl IS NOT NULL AND r_level > max_lvl THEN
        RAISE EXCEPTION 'Звание "%" (уровень %) слишком высокое для командования "%" (максимальный уровень %)',
            rank_name, r_level, p_type, max_lvl;
    END IF;
END;
$$ LANGUAGE plpgsql;
