CREATE TABLE IF NOT EXISTS rank_attribute_types (
    attribute_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    data_type VARCHAR(20) NOT NULL CHECK (data_type IN ('text', 'number', 'date', 'boolean')),
    CHECK (name <> '')
);

CREATE TABLE IF NOT EXISTS rank_type_attributes (
    rank_id BIGINT NOT NULL REFERENCES military_ranks(rank_id) ON DELETE CASCADE,
    attribute_id BIGINT NOT NULL REFERENCES rank_attribute_types(attribute_id) ON DELETE CASCADE,
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (rank_id, attribute_id)
);

CREATE TABLE IF NOT EXISTS rank_attribute_values (
    personnel_id BIGINT NOT NULL REFERENCES personnel(personnel_id) ON DELETE CASCADE,
    attribute_id BIGINT NOT NULL REFERENCES rank_attribute_types(attribute_id) ON DELETE CASCADE,
    value_text TEXT,
    value_number NUMERIC,
    value_date DATE,
    value_boolean BOOLEAN,
    PRIMARY KEY (personnel_id, attribute_id)
);

CREATE TABLE IF NOT EXISTS equipment_attribute_types (
    attribute_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    data_type VARCHAR(20) NOT NULL CHECK (data_type IN ('text', 'number', 'date', 'boolean')),
    CHECK (name <> '')
);

CREATE TABLE IF NOT EXISTS equipment_category_attributes (
    category_id BIGINT NOT NULL REFERENCES equipment_categories(category_id) ON DELETE CASCADE,
    attribute_id BIGINT NOT NULL REFERENCES equipment_attribute_types(attribute_id) ON DELETE CASCADE,
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (category_id, attribute_id)
);

CREATE TABLE IF NOT EXISTS equipment_type_attribute_values (
    type_id BIGINT NOT NULL REFERENCES equipment_types(type_id) ON DELETE CASCADE,
    attribute_id BIGINT NOT NULL REFERENCES equipment_attribute_types(attribute_id) ON DELETE CASCADE,
    value_text TEXT,
    value_number NUMERIC,
    value_date DATE,
    value_boolean BOOLEAN,
    PRIMARY KEY (type_id, attribute_id)
);

CREATE TABLE IF NOT EXISTS weapon_attribute_types (
    attribute_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    data_type VARCHAR(20) NOT NULL CHECK (data_type IN ('text', 'number', 'date', 'boolean')),
    CHECK (name <> '')
);

CREATE TABLE IF NOT EXISTS weapon_category_attributes (
    category_id BIGINT NOT NULL REFERENCES weapon_categories(category_id) ON DELETE CASCADE,
    attribute_id BIGINT NOT NULL REFERENCES weapon_attribute_types(attribute_id) ON DELETE CASCADE,
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (category_id, attribute_id)
);

CREATE TABLE IF NOT EXISTS weapon_type_attribute_values (
    type_id BIGINT NOT NULL REFERENCES weapon_types(type_id) ON DELETE CASCADE,
    attribute_id BIGINT NOT NULL REFERENCES weapon_attribute_types(attribute_id) ON DELETE CASCADE,
    value_text TEXT,
    value_number NUMERIC,
    value_date DATE,
    value_boolean BOOLEAN,
    PRIMARY KEY (type_id, attribute_id)
);

CREATE INDEX IF NOT EXISTS idx_rank_attribute_values_personnel ON rank_attribute_values(personnel_id);
CREATE INDEX IF NOT EXISTS idx_equipment_type_attribute_values_type ON equipment_type_attribute_values(type_id);
CREATE INDEX IF NOT EXISTS idx_weapon_type_attribute_values_type ON weapon_type_attribute_values(type_id);

INSERT INTO equipment_attribute_types (name, data_type) VALUES
('назначение', 'text'),
('экипаж', 'number'),
('масса, т', 'number'),
('скорость, км/ч', 'number'),
('запас хода, км', 'number'),
('год принятия', 'number'),
('производитель', 'text'),
('описание', 'text')
ON CONFLICT (name) DO UPDATE SET data_type = EXCLUDED.data_type;

INSERT INTO weapon_attribute_types (name, data_type) VALUES
('назначение', 'text'),
('калибр', 'text'),
('дальность, м', 'number'),
('год принятия', 'number'),
('производитель', 'text'),
('описание', 'text')
ON CONFLICT (name) DO UPDATE SET data_type = EXCLUDED.data_type;

INSERT INTO rank_attribute_types (name, data_type) VALUES
('уровень командования', 'number'),
('категория допуска', 'text')
ON CONFLICT (name) DO UPDATE SET data_type = EXCLUDED.data_type;

INSERT INTO equipment_category_attributes (category_id, attribute_id, is_required)
SELECT ec.category_id, eat.attribute_id, eat.name IN ('назначение')
FROM equipment_categories ec
CROSS JOIN equipment_attribute_types eat
ON CONFLICT (category_id, attribute_id) DO UPDATE SET is_required = EXCLUDED.is_required;

INSERT INTO weapon_category_attributes (category_id, attribute_id, is_required)
SELECT wc.category_id, wat.attribute_id, wat.name IN ('назначение')
FROM weapon_categories wc
CROSS JOIN weapon_attribute_types wat
ON CONFLICT (category_id, attribute_id) DO UPDATE SET is_required = EXCLUDED.is_required;

INSERT INTO rank_type_attributes (rank_id, attribute_id, is_required)
SELECT mr.rank_id, rat.attribute_id, TRUE
FROM military_ranks mr
CROSS JOIN rank_attribute_types rat
ON CONFLICT (rank_id, attribute_id) DO UPDATE SET is_required = EXCLUDED.is_required;

INSERT INTO equipment_type_attribute_values (type_id, attribute_id, value_text, value_number)
SELECT et.type_id,
       eat.attribute_id,
       CASE eat.name
           WHEN 'назначение' THEN et.purpose
           WHEN 'производитель' THEN et.manufacturer
           WHEN 'описание' THEN et.description
           ELSE NULL
       END,
       CASE eat.name
           WHEN 'экипаж' THEN et.crew_size::NUMERIC
           WHEN 'масса, т' THEN et.weight_tons
           WHEN 'скорость, км/ч' THEN et.max_speed_kmh::NUMERIC
           WHEN 'запас хода, км' THEN et.operational_range_km::NUMERIC
           WHEN 'год принятия' THEN et.adoption_year::NUMERIC
           ELSE NULL
       END
FROM equipment_types et
JOIN equipment_attribute_types eat ON eat.name IN (
    'назначение', 'экипаж', 'масса, т', 'скорость, км/ч', 'запас хода, км', 'год принятия', 'производитель', 'описание'
)
WHERE et.archived = FALSE
ON CONFLICT (type_id, attribute_id) DO UPDATE SET
    value_text = EXCLUDED.value_text,
    value_number = EXCLUDED.value_number;

INSERT INTO weapon_type_attribute_values (type_id, attribute_id, value_text, value_number)
SELECT wt.type_id,
       wat.attribute_id,
       CASE wat.name
           WHEN 'назначение' THEN wt.purpose
           WHEN 'калибр' THEN wt.caliber
           WHEN 'производитель' THEN wt.manufacturer
           WHEN 'описание' THEN wt.description
           ELSE NULL
       END,
       CASE wat.name
           WHEN 'дальность, м' THEN wt.effective_range_m::NUMERIC
           WHEN 'год принятия' THEN wt.adoption_year::NUMERIC
           ELSE NULL
       END
FROM weapon_types wt
JOIN weapon_attribute_types wat ON wat.name IN (
    'назначение', 'калибр', 'дальность, м', 'год принятия', 'производитель', 'описание'
)
WHERE wt.archived = FALSE
ON CONFLICT (type_id, attribute_id) DO UPDATE SET
    value_text = EXCLUDED.value_text,
    value_number = EXCLUDED.value_number;

CREATE OR REPLACE FUNCTION tdc_rank_level(rank_name TEXT, rank_category TEXT)
RETURNS INTEGER
LANGUAGE plpgsql
IMMUTABLE
AS $$
BEGIN
    IF rank_name ILIKE '%генерал%' THEN
        RETURN 90;
    ELSIF rank_name ILIKE '%полковник%' AND rank_name NOT ILIKE '%подполковник%' THEN
        RETURN 80;
    ELSIF rank_name ILIKE '%подполковник%' THEN
        RETURN 70;
    ELSIF rank_name ILIKE '%майор%' THEN
        RETURN 60;
    ELSIF rank_name ILIKE '%капитан%' THEN
        RETURN 50;
    ELSIF rank_name ILIKE '%лейтенант%' THEN
        RETURN 40;
    ELSIF rank_name ILIKE '%прапорщик%' THEN
        RETURN 35;
    ELSIF rank_name ILIKE '%старшина%' THEN
        RETURN 30;
    ELSIF rank_name ILIKE '%сержант%' THEN
        RETURN 25;
    ELSIF rank_name ILIKE '%ефрейтор%' THEN
        RETURN 15;
    ELSIF rank_name ILIKE '%рядовой%' THEN
        RETURN 10;
    ELSIF rank_category = 'Офицерский' THEN
        RETURN 45;
    ELSE
        RETURN 10;
    END IF;
END;
$$;

INSERT INTO rank_attribute_values (personnel_id, attribute_id, value_text, value_number)
SELECT p.personnel_id,
       rat.attribute_id,
       CASE rat.name
           WHEN 'категория допуска' THEN mr.category
           ELSE NULL
       END,
       CASE rat.name
           WHEN 'уровень командования' THEN tdc_rank_level(mr.name, mr.category)::NUMERIC
           ELSE NULL
       END
FROM personnel p
JOIN personnel_ranks pr ON pr.personnel_id = p.personnel_id
JOIN military_ranks mr ON mr.rank_id = pr.rank_id
JOIN rank_attribute_types rat ON rat.name IN ('уровень командования', 'категория допуска')
ON CONFLICT (personnel_id, attribute_id) DO UPDATE SET
    value_text = EXCLUDED.value_text,
    value_number = EXCLUDED.value_number;

CREATE TABLE IF NOT EXISTS command_requirements (
    object_type VARCHAR(50) PRIMARY KEY,
    min_rank_level INTEGER NOT NULL CHECK (min_rank_level >= 0),
    description TEXT
);

INSERT INTO command_requirements (object_type, min_rank_level, description) VALUES
('Округ', 85, 'Командование военным округом'),
('Армия', 75, 'Командование армией'),
('Корпус', 70, 'Командование корпусом'),
('Дивизия', 65, 'Командование дивизией'),
('Бригада', 60, 'Командование бригадой'),
('MILITARY_UNIT', 45, 'Командование военной частью'),
('Батальон', 40, 'Командование батальоном'),
('Рота', 35, 'Командование ротой'),
('Взвод', 25, 'Командование взводом'),
('Отделение', 20, 'Командование отделением')
ON CONFLICT (object_type) DO UPDATE SET
    min_rank_level = EXCLUDED.min_rank_level,
    description = EXCLUDED.description;

CREATE OR REPLACE VIEW v_formation_closure AS
WITH RECURSIVE formation_closure AS (
    SELECT
        mf.formation_id AS root_formation_id,
        mf.name AS root_formation_name,
        mf.formation_type AS root_formation_type,
        mf.formation_id AS descendant_formation_id,
        mf.name AS descendant_formation_name,
        mf.formation_type AS descendant_formation_type,
        0 AS depth
    FROM military_formations mf
    UNION ALL
    SELECT
        fc.root_formation_id,
        fc.root_formation_name,
        fc.root_formation_type,
        child.formation_id,
        child.name,
        child.formation_type,
        fc.depth + 1
    FROM formation_closure fc
    JOIN military_formations child ON child.parent_id = fc.descendant_formation_id
)
SELECT *
FROM formation_closure;

CREATE OR REPLACE VIEW v_unit_equipment AS
SELECT
    mu.unit_id,
    mu.name AS unit_name,
    mf.formation_id,
    mf.name AS formation_name,
    ec.category_id,
    ec.name AS equipment_category,
    et.type_id,
    et.name AS equipment_type,
    eiu.quantity
FROM equipment_in_units eiu
JOIN military_units mu ON mu.unit_id = eiu.unit_id
JOIN military_formations mf ON mf.formation_id = mu.formation_id
JOIN equipment_types et ON et.type_id = eiu.type_id
JOIN equipment_categories ec ON ec.category_id = et.category_id
WHERE et.archived = FALSE AND ec.archived = FALSE;

CREATE OR REPLACE VIEW v_unit_weapons AS
SELECT
    mu.unit_id,
    mu.name AS unit_name,
    mf.formation_id,
    mf.name AS formation_name,
    wc.category_id,
    wc.name AS weapon_category,
    wt.type_id,
    wt.name AS weapon_type,
    wiu.quantity
FROM weapon_in_units wiu
JOIN military_units mu ON mu.unit_id = wiu.unit_id
JOIN military_formations mf ON mf.formation_id = mu.formation_id
JOIN weapon_types wt ON wt.type_id = wiu.type_id
JOIN weapon_categories wc ON wc.category_id = wt.category_id
WHERE wt.archived = FALSE AND wc.archived = FALSE;

CREATE OR REPLACE VIEW v_buildings_usage AS
SELECT
    b.building_id,
    b.name AS building_name,
    mu.unit_id,
    mu.name AS unit_name,
    COUNT(sb.subdivision_id) AS subdivisions_count,
    COALESCE(string_agg(s.name, ', ' ORDER BY s.name), '') AS subdivisions
FROM buildings b
JOIN military_units mu ON mu.unit_id = b.unit_id
LEFT JOIN subdivision_buildings sb ON sb.building_id = b.building_id
LEFT JOIN subdivisions s ON s.subdivision_id = sb.subdivision_id
GROUP BY b.building_id, b.name, mu.unit_id, mu.name;

CREATE OR REPLACE VIEW v_personnel_specialties AS
SELECT
    p.personnel_id,
    trim(concat(p.last_name, ' ', p.first_name, ' ', coalesce(p.middle_name, ''))) AS full_name,
    sp.specialty_id,
    sp.name AS specialty_name,
    s.subdivision_id,
    s.name AS subdivision_name,
    mu.unit_id,
    mu.name AS unit_name
FROM personnel p
JOIN subdivisions s ON s.subdivision_id = p.subdivision_id
JOIN military_units mu ON mu.unit_id = s.unit_id
JOIN personnel_specialties ps ON ps.personnel_id = p.personnel_id
JOIN specialties sp ON sp.specialty_id = ps.specialty_id;

CREATE OR REPLACE VIEW v_personnel_full AS
SELECT
    p.personnel_id,
    trim(concat(p.last_name, ' ', p.first_name, ' ', coalesce(p.middle_name, ''))) AS full_name,
    p.last_name,
    p.first_name,
    p.middle_name,
    p.personal_number,
    p.birth_date,
    p.service_start,
    mr.rank_id,
    mr.name AS rank_name,
    mr.category AS rank_category,
    s.subdivision_id,
    s.name AS subdivision_name,
    s.type AS subdivision_type,
    mu.unit_id,
    mu.name AS unit_name,
    mf.formation_id,
    mf.name AS formation_name,
    COALESCE(string_agg(DISTINCT sp.name, ', ' ORDER BY sp.name), '') AS specialties
FROM personnel p
JOIN subdivisions s ON s.subdivision_id = p.subdivision_id
JOIN military_units mu ON mu.unit_id = s.unit_id
JOIN military_formations mf ON mf.formation_id = mu.formation_id
LEFT JOIN personnel_ranks pr ON pr.personnel_id = p.personnel_id
LEFT JOIN military_ranks mr ON mr.rank_id = pr.rank_id
LEFT JOIN personnel_specialties ps ON ps.personnel_id = p.personnel_id
LEFT JOIN specialties sp ON sp.specialty_id = ps.specialty_id
GROUP BY p.personnel_id, p.last_name, p.first_name, p.middle_name, p.personal_number, p.birth_date,
         p.service_start, mr.rank_id, mr.name, mr.category, s.subdivision_id, s.name, s.type,
         mu.unit_id, mu.name, mf.formation_id, mf.name;

CREATE OR REPLACE FUNCTION tdc_validate_rank_attribute_value()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    attr_type TEXT;
    filled_count INTEGER;
BEGIN
    SELECT data_type INTO attr_type
    FROM rank_attribute_types
    WHERE attribute_id = NEW.attribute_id;

    filled_count :=
        CASE WHEN NEW.value_text IS NOT NULL THEN 1 ELSE 0 END +
        CASE WHEN NEW.value_number IS NOT NULL THEN 1 ELSE 0 END +
        CASE WHEN NEW.value_date IS NOT NULL THEN 1 ELSE 0 END +
        CASE WHEN NEW.value_boolean IS NOT NULL THEN 1 ELSE 0 END;

    IF filled_count <> 1 THEN
        RAISE EXCEPTION 'Exactly one rank attribute value column must be filled';
    END IF;

    IF attr_type = 'text' AND NEW.value_text IS NULL THEN
        RAISE EXCEPTION 'Rank attribute % requires text value', NEW.attribute_id;
    ELSIF attr_type = 'number' AND NEW.value_number IS NULL THEN
        RAISE EXCEPTION 'Rank attribute % requires number value', NEW.attribute_id;
    ELSIF attr_type = 'date' AND NEW.value_date IS NULL THEN
        RAISE EXCEPTION 'Rank attribute % requires date value', NEW.attribute_id;
    ELSIF attr_type = 'boolean' AND NEW.value_boolean IS NULL THEN
        RAISE EXCEPTION 'Rank attribute % requires boolean value', NEW.attribute_id;
    END IF;

    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION tdc_validate_equipment_attribute_value()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    attr_type TEXT;
    filled_count INTEGER;
BEGIN
    SELECT data_type INTO attr_type
    FROM equipment_attribute_types
    WHERE attribute_id = NEW.attribute_id;

    filled_count :=
        CASE WHEN NEW.value_text IS NOT NULL THEN 1 ELSE 0 END +
        CASE WHEN NEW.value_number IS NOT NULL THEN 1 ELSE 0 END +
        CASE WHEN NEW.value_date IS NOT NULL THEN 1 ELSE 0 END +
        CASE WHEN NEW.value_boolean IS NOT NULL THEN 1 ELSE 0 END;

    IF filled_count <> 1 THEN
        RAISE EXCEPTION 'Exactly one equipment attribute value column must be filled';
    END IF;

    IF attr_type = 'text' AND NEW.value_text IS NULL THEN
        RAISE EXCEPTION 'Equipment attribute % requires text value', NEW.attribute_id;
    ELSIF attr_type = 'number' AND NEW.value_number IS NULL THEN
        RAISE EXCEPTION 'Equipment attribute % requires number value', NEW.attribute_id;
    ELSIF attr_type = 'date' AND NEW.value_date IS NULL THEN
        RAISE EXCEPTION 'Equipment attribute % requires date value', NEW.attribute_id;
    ELSIF attr_type = 'boolean' AND NEW.value_boolean IS NULL THEN
        RAISE EXCEPTION 'Equipment attribute % requires boolean value', NEW.attribute_id;
    END IF;

    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION tdc_validate_weapon_attribute_value()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    attr_type TEXT;
    filled_count INTEGER;
BEGIN
    SELECT data_type INTO attr_type
    FROM weapon_attribute_types
    WHERE attribute_id = NEW.attribute_id;

    filled_count :=
        CASE WHEN NEW.value_text IS NOT NULL THEN 1 ELSE 0 END +
        CASE WHEN NEW.value_number IS NOT NULL THEN 1 ELSE 0 END +
        CASE WHEN NEW.value_date IS NOT NULL THEN 1 ELSE 0 END +
        CASE WHEN NEW.value_boolean IS NOT NULL THEN 1 ELSE 0 END;

    IF filled_count <> 1 THEN
        RAISE EXCEPTION 'Exactly one weapon attribute value column must be filled';
    END IF;

    IF attr_type = 'text' AND NEW.value_text IS NULL THEN
        RAISE EXCEPTION 'Weapon attribute % requires text value', NEW.attribute_id;
    ELSIF attr_type = 'number' AND NEW.value_number IS NULL THEN
        RAISE EXCEPTION 'Weapon attribute % requires number value', NEW.attribute_id;
    ELSIF attr_type = 'date' AND NEW.value_date IS NULL THEN
        RAISE EXCEPTION 'Weapon attribute % requires date value', NEW.attribute_id;
    ELSIF attr_type = 'boolean' AND NEW.value_boolean IS NULL THEN
        RAISE EXCEPTION 'Weapon attribute % requires boolean value', NEW.attribute_id;
    END IF;

    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION tdc_validate_rank_assignment_date()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    service_date DATE;
BEGIN
    SELECT service_start INTO service_date
    FROM personnel
    WHERE personnel_id = NEW.personnel_id;

    IF NEW.assignment_date < service_date THEN
        RAISE EXCEPTION 'Rank assignment date cannot be earlier than service start date';
    END IF;

    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION tdc_validate_subdivision_building_unit()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    subdivision_unit BIGINT;
    building_unit BIGINT;
BEGIN
    SELECT unit_id INTO subdivision_unit FROM subdivisions WHERE subdivision_id = NEW.subdivision_id;
    SELECT unit_id INTO building_unit FROM buildings WHERE building_id = NEW.building_id;

    IF subdivision_unit IS DISTINCT FROM building_unit THEN
        RAISE EXCEPTION 'Subdivision and building must belong to the same military unit';
    END IF;

    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION tdc_prevent_delete_commander()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM military_formations WHERE commander_id = OLD.personnel_id)
       OR EXISTS (SELECT 1 FROM military_units WHERE commander_id = OLD.personnel_id)
       OR EXISTS (SELECT 1 FROM subdivisions WHERE commander_id = OLD.personnel_id) THEN
        RAISE EXCEPTION 'Cannot delete personnel assigned as commander';
    END IF;
    RETURN OLD;
END;
$$;

CREATE OR REPLACE FUNCTION tdc_prevent_delete_rank_in_use()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM personnel_ranks WHERE rank_id = OLD.rank_id) THEN
        RAISE EXCEPTION 'Cannot delete rank used by personnel';
    END IF;
    RETURN OLD;
END;
$$;

CREATE OR REPLACE FUNCTION tdc_prevent_delete_unit_with_resources()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM equipment_in_units WHERE unit_id = OLD.unit_id)
       OR EXISTS (SELECT 1 FROM weapon_in_units WHERE unit_id = OLD.unit_id)
       OR EXISTS (SELECT 1 FROM buildings WHERE unit_id = OLD.unit_id) THEN
        RAISE EXCEPTION 'Cannot delete military unit with inventory or buildings';
    END IF;
    RETURN OLD;
END;
$$;

CREATE OR REPLACE FUNCTION tdc_validate_formation_commander_level()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    min_level INTEGER;
    actual_level INTEGER;
BEGIN
    IF NEW.commander_id IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT min_rank_level INTO min_level
    FROM command_requirements
    WHERE object_type = NEW.formation_type;

    SELECT tdc_rank_level(mr.name, mr.category) INTO actual_level
    FROM personnel_ranks pr
    JOIN military_ranks mr ON mr.rank_id = pr.rank_id
    WHERE pr.personnel_id = NEW.commander_id;

    IF min_level IS NOT NULL AND COALESCE(actual_level, 0) < min_level THEN
        RAISE EXCEPTION 'Commander rank level % is lower than required % for %', actual_level, min_level, NEW.formation_type;
    END IF;

    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION tdc_validate_unit_commander_level()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    min_level INTEGER;
    actual_level INTEGER;
BEGIN
    IF NEW.commander_id IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT min_rank_level INTO min_level
    FROM command_requirements
    WHERE object_type = 'MILITARY_UNIT';

    SELECT tdc_rank_level(mr.name, mr.category) INTO actual_level
    FROM personnel_ranks pr
    JOIN military_ranks mr ON mr.rank_id = pr.rank_id
    WHERE pr.personnel_id = NEW.commander_id;

    IF COALESCE(actual_level, 0) < min_level THEN
        RAISE EXCEPTION 'Commander rank level % is lower than required % for military unit', actual_level, min_level;
    END IF;

    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION tdc_validate_subdivision_commander_level()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    min_level INTEGER;
    actual_level INTEGER;
BEGIN
    IF NEW.commander_id IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT min_rank_level INTO min_level
    FROM command_requirements
    WHERE object_type = NEW.type;

    SELECT tdc_rank_level(mr.name, mr.category) INTO actual_level
    FROM personnel_ranks pr
    JOIN military_ranks mr ON mr.rank_id = pr.rank_id
    WHERE pr.personnel_id = NEW.commander_id;

    IF min_level IS NOT NULL AND COALESCE(actual_level, 0) < min_level THEN
        RAISE EXCEPTION 'Commander rank level % is lower than required % for %', actual_level, min_level, NEW.type;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_rank_attribute_value_guard ON rank_attribute_values;
CREATE TRIGGER trg_rank_attribute_value_guard
BEFORE INSERT OR UPDATE ON rank_attribute_values
FOR EACH ROW EXECUTE FUNCTION tdc_validate_rank_attribute_value();

DROP TRIGGER IF EXISTS trg_equipment_attribute_value_guard ON equipment_type_attribute_values;
CREATE TRIGGER trg_equipment_attribute_value_guard
BEFORE INSERT OR UPDATE ON equipment_type_attribute_values
FOR EACH ROW EXECUTE FUNCTION tdc_validate_equipment_attribute_value();

DROP TRIGGER IF EXISTS trg_weapon_attribute_value_guard ON weapon_type_attribute_values;
CREATE TRIGGER trg_weapon_attribute_value_guard
BEFORE INSERT OR UPDATE ON weapon_type_attribute_values
FOR EACH ROW EXECUTE FUNCTION tdc_validate_weapon_attribute_value();

DROP TRIGGER IF EXISTS trg_personnel_rank_date_guard ON personnel_ranks;
CREATE TRIGGER trg_personnel_rank_date_guard
BEFORE INSERT OR UPDATE ON personnel_ranks
FOR EACH ROW EXECUTE FUNCTION tdc_validate_rank_assignment_date();

DROP TRIGGER IF EXISTS trg_subdivision_building_unit_guard ON subdivision_buildings;
CREATE TRIGGER trg_subdivision_building_unit_guard
BEFORE INSERT OR UPDATE ON subdivision_buildings
FOR EACH ROW EXECUTE FUNCTION tdc_validate_subdivision_building_unit();

DROP TRIGGER IF EXISTS trg_prevent_delete_commander ON personnel;
CREATE TRIGGER trg_prevent_delete_commander
BEFORE DELETE ON personnel
FOR EACH ROW EXECUTE FUNCTION tdc_prevent_delete_commander();

DROP TRIGGER IF EXISTS trg_prevent_delete_rank_in_use ON military_ranks;
CREATE TRIGGER trg_prevent_delete_rank_in_use
BEFORE DELETE ON military_ranks
FOR EACH ROW EXECUTE FUNCTION tdc_prevent_delete_rank_in_use();

DROP TRIGGER IF EXISTS trg_prevent_delete_unit_with_resources ON military_units;
CREATE TRIGGER trg_prevent_delete_unit_with_resources
BEFORE DELETE ON military_units
FOR EACH ROW EXECUTE FUNCTION tdc_prevent_delete_unit_with_resources();

DROP TRIGGER IF EXISTS trg_validate_formation_commander_level ON military_formations;
CREATE TRIGGER trg_validate_formation_commander_level
BEFORE INSERT OR UPDATE OF commander_id, formation_type ON military_formations
FOR EACH ROW EXECUTE FUNCTION tdc_validate_formation_commander_level();

DROP TRIGGER IF EXISTS trg_validate_unit_commander_level ON military_units;
CREATE TRIGGER trg_validate_unit_commander_level
BEFORE INSERT OR UPDATE OF commander_id ON military_units
FOR EACH ROW EXECUTE FUNCTION tdc_validate_unit_commander_level();

DROP TRIGGER IF EXISTS trg_validate_subdivision_commander_level ON subdivisions;
CREATE TRIGGER trg_validate_subdivision_commander_level
BEFORE INSERT OR UPDATE OF commander_id, type ON subdivisions
FOR EACH ROW EXECUTE FUNCTION tdc_validate_subdivision_commander_level();
