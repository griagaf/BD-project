CREATE OR REPLACE FUNCTION tdc_object_exists(p_object_type VARCHAR, p_object_id BIGINT)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN CASE p_object_type
        WHEN 'DISTRICT' THEN EXISTS (
            SELECT 1 FROM military_formations
            WHERE formation_id = p_object_id AND formation_type = 'Округ'
        )
        WHEN 'ARMY' THEN EXISTS (
            SELECT 1 FROM military_formations
            WHERE formation_id = p_object_id AND formation_type = 'Армия'
        )
        WHEN 'FORMATION' THEN EXISTS (
            SELECT 1 FROM military_formations
            WHERE formation_id = p_object_id
        )
        WHEN 'CORPS' THEN EXISTS (
            SELECT 1 FROM military_formations
            WHERE formation_id = p_object_id AND formation_type = 'Корпус'
        )
        WHEN 'DIVISION' THEN EXISTS (
            SELECT 1 FROM military_formations
            WHERE formation_id = p_object_id AND formation_type = 'Дивизия'
        )
        WHEN 'BRIGADE' THEN EXISTS (
            SELECT 1 FROM military_formations
            WHERE formation_id = p_object_id AND formation_type = 'Бригада'
        )
        WHEN 'MILITARY_UNIT' THEN EXISTS (
            SELECT 1 FROM military_units
            WHERE unit_id = p_object_id
        )
        WHEN 'BATTALION' THEN EXISTS (
            SELECT 1 FROM subdivisions
            WHERE subdivision_id = p_object_id AND type = 'Батальон'
        )
        WHEN 'COMPANY' THEN EXISTS (
            SELECT 1 FROM subdivisions
            WHERE subdivision_id = p_object_id AND type = 'Рота'
        )
        WHEN 'PLATOON' THEN EXISTS (
            SELECT 1 FROM subdivisions
            WHERE subdivision_id = p_object_id AND type = 'Взвод'
        )
        WHEN 'SQUAD' THEN EXISTS (
            SELECT 1 FROM subdivisions
            WHERE subdivision_id = p_object_id AND type = 'Отделение'
        )
        WHEN 'SELF' THEN EXISTS (
            SELECT 1 FROM personnel
            WHERE personnel_id = p_object_id
        )
        ELSE FALSE
    END;
END;
$$;

CREATE OR REPLACE FUNCTION tdc_subdivision_level(p_type VARCHAR)
RETURNS INTEGER
LANGUAGE sql
IMMUTABLE
AS $$
    SELECT CASE p_type
        WHEN 'Батальон' THEN 1
        WHEN 'Рота' THEN 2
        WHEN 'Взвод' THEN 3
        WHEN 'Отделение' THEN 4
        ELSE NULL
    END
$$;

CREATE OR REPLACE FUNCTION tdc_validate_command_assignment()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM personnel WHERE personnel_id = NEW.soldier_id) THEN
        RAISE EXCEPTION 'Command assignment soldier % does not exist', NEW.soldier_id;
    END IF;

    IF NOT tdc_object_exists(NEW.object_type, NEW.object_id) THEN
        RAISE EXCEPTION 'Command assignment target %.% does not exist', NEW.object_type, NEW.object_id;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_command_assignments_validate ON command_assignments;
CREATE TRIGGER trg_command_assignments_validate
BEFORE INSERT OR UPDATE ON command_assignments
FOR EACH ROW
EXECUTE FUNCTION tdc_validate_command_assignment();

CREATE OR REPLACE FUNCTION tdc_validate_subdivision_parent()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    parent_unit_id BIGINT;
    parent_type VARCHAR(50);
BEGIN
    IF NEW.parent_id IS NULL THEN
        IF NEW.type NOT IN ('Батальон', 'Рота') THEN
            RAISE EXCEPTION 'Subdivision type % requires a parent subdivision', NEW.type;
        END IF;
        RETURN NEW;
    END IF;

    SELECT unit_id, type
    INTO parent_unit_id, parent_type
    FROM subdivisions
    WHERE subdivision_id = NEW.parent_id;

    IF parent_unit_id IS NULL THEN
        RAISE EXCEPTION 'Parent subdivision % does not exist', NEW.parent_id;
    END IF;

    IF parent_unit_id <> NEW.unit_id THEN
        RAISE EXCEPTION 'Subdivision parent % belongs to another military unit', NEW.parent_id;
    END IF;

    IF tdc_subdivision_level(NEW.type) <> tdc_subdivision_level(parent_type) + 1 THEN
        RAISE EXCEPTION 'Invalid subdivision hierarchy: parent type %, child type %', parent_type, NEW.type;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_subdivisions_validate_parent ON subdivisions;
CREATE TRIGGER trg_subdivisions_validate_parent
BEFORE INSERT OR UPDATE OF type, unit_id, parent_id ON subdivisions
FOR EACH ROW
EXECUTE FUNCTION tdc_validate_subdivision_parent();

CREATE OR REPLACE FUNCTION tdc_validate_unit_commander()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.commander_id IS NULL THEN
        RETURN NEW;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM personnel p
        JOIN subdivisions s ON s.subdivision_id = p.subdivision_id
        WHERE p.personnel_id = NEW.commander_id
          AND s.unit_id = NEW.unit_id
    ) THEN
        RAISE EXCEPTION 'Commander % is not assigned to military unit %', NEW.commander_id, NEW.unit_id;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_military_units_validate_commander ON military_units;
CREATE TRIGGER trg_military_units_validate_commander
BEFORE INSERT OR UPDATE OF commander_id ON military_units
FOR EACH ROW
EXECUTE FUNCTION tdc_validate_unit_commander();

CREATE OR REPLACE FUNCTION tdc_validate_subdivision_commander()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.commander_id IS NULL THEN
        RETURN NEW;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM personnel
        WHERE personnel_id = NEW.commander_id
          AND subdivision_id = NEW.subdivision_id
    ) THEN
        RAISE EXCEPTION 'Commander % is not assigned to subdivision %', NEW.commander_id, NEW.subdivision_id;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_subdivisions_validate_commander ON subdivisions;
CREATE TRIGGER trg_subdivisions_validate_commander
BEFORE INSERT OR UPDATE OF commander_id ON subdivisions
FOR EACH ROW
EXECUTE FUNCTION tdc_validate_subdivision_commander();

CREATE OR REPLACE FUNCTION tdc_validate_formation_commander()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.commander_id IS NULL THEN
        RETURN NEW;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM personnel p
        JOIN subdivisions s ON s.subdivision_id = p.subdivision_id
        JOIN military_units mu ON mu.unit_id = s.unit_id
        JOIN v_formation_closure fc ON fc.descendant_formation_id = mu.formation_id
        WHERE p.personnel_id = NEW.commander_id
          AND fc.root_formation_id = NEW.formation_id
    ) THEN
        RAISE EXCEPTION 'Commander % is outside formation % scope', NEW.commander_id, NEW.formation_id;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_military_formations_validate_commander ON military_formations;
CREATE TRIGGER trg_military_formations_validate_commander
BEFORE INSERT OR UPDATE OF commander_id ON military_formations
FOR EACH ROW
EXECUTE FUNCTION tdc_validate_formation_commander();

CREATE OR REPLACE FUNCTION tdc_touch_users_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_users_touch_updated_at ON users;
CREATE TRIGGER trg_users_touch_updated_at
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION tdc_touch_users_updated_at();
