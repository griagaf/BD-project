CREATE TABLE locations (
    location_id BIGSERIAL PRIMARY KEY,
    city VARCHAR(100) NOT NULL,
    address TEXT,
    UNIQUE (city, address),
    CHECK (city <> '')
);

CREATE TABLE military_formations (
    formation_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    formation_type VARCHAR(50) NOT NULL
        CHECK (formation_type IN ('Округ', 'Армия', 'Корпус', 'Дивизия', 'Бригада')),
    parent_id BIGINT REFERENCES military_formations(formation_id) ON DELETE SET NULL,
    formation_date DATE CHECK (formation_date <= CURRENT_DATE),
    status VARCHAR(30) NOT NULL DEFAULT 'Активна',
    commander_id BIGINT,
    CHECK (name <> ''),
    CHECK (parent_id IS NULL OR parent_id <> formation_id)
);

CREATE TABLE military_units (
    unit_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    formation_id BIGINT NOT NULL REFERENCES military_formations(formation_id) ON DELETE CASCADE,
    location_id BIGINT REFERENCES locations(location_id) ON DELETE SET NULL,
    commander_id BIGINT,
    CHECK (name <> '')
);

CREATE TABLE subdivisions (
    subdivision_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    type VARCHAR(50) NOT NULL
        CHECK (type IN ('Батальон', 'Рота', 'Взвод', 'Отделение')),
    unit_id BIGINT NOT NULL REFERENCES military_units(unit_id) ON DELETE CASCADE,
    parent_id BIGINT REFERENCES subdivisions(subdivision_id) ON DELETE SET NULL,
    commander_id BIGINT,
    UNIQUE (name, unit_id),
    CHECK (name <> ''),
    CHECK (parent_id IS NULL OR parent_id <> subdivision_id)
);

CREATE TABLE personnel (
    personnel_id BIGSERIAL PRIMARY KEY,
    last_name VARCHAR(100) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    personal_number VARCHAR(20) NOT NULL UNIQUE,
    birth_date DATE NOT NULL CHECK (birth_date <= CURRENT_DATE - INTERVAL '18 years'),
    service_start DATE NOT NULL CHECK (service_start <= CURRENT_DATE),
    subdivision_id BIGINT NOT NULL REFERENCES subdivisions(subdivision_id) ON DELETE RESTRICT,
    CHECK (last_name <> ''),
    CHECK (first_name <> '')
);

CREATE TABLE military_ranks (
    rank_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(50) NOT NULL
        CHECK (category IN ('Офицерский', 'Сержантский и Рядовой'))
);

CREATE TABLE personnel_ranks (
    personnel_id BIGINT PRIMARY KEY REFERENCES personnel(personnel_id) ON DELETE CASCADE,
    rank_id BIGINT NOT NULL REFERENCES military_ranks(rank_id),
    assignment_date DATE NOT NULL CHECK (assignment_date <= CURRENT_DATE)
);

CREATE TABLE specialties (
    specialty_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    CHECK (name <> '')
);

CREATE TABLE personnel_specialties (
    personnel_id BIGINT NOT NULL REFERENCES personnel(personnel_id) ON DELETE CASCADE,
    specialty_id BIGINT NOT NULL REFERENCES specialties(specialty_id) ON DELETE CASCADE,
    PRIMARY KEY (personnel_id, specialty_id)
);

CREATE TABLE audit_events (
    audit_event_id BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT,
    actor_username VARCHAR(64) NOT NULL,
    action VARCHAR(32) NOT NULL,
    object_type VARCHAR(64) NOT NULL,
    object_id BIGINT,
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

ALTER TABLE military_formations
ADD CONSTRAINT fk_military_formations_commander
FOREIGN KEY (commander_id) REFERENCES personnel(personnel_id);

ALTER TABLE military_units
ADD CONSTRAINT fk_military_units_commander
FOREIGN KEY (commander_id) REFERENCES personnel(personnel_id);

ALTER TABLE subdivisions
ADD CONSTRAINT fk_subdivisions_commander
FOREIGN KEY (commander_id) REFERENCES personnel(personnel_id);

CREATE OR REPLACE VIEW v_formation_closure AS
WITH RECURSIVE formation_closure AS (
    SELECT
        mf.formation_id AS root_formation_id,
        mf.name AS root_formation_name,
        mf.formation_type AS root_formation_type,
        mf.formation_id AS descendant_formation_id,
        mf.name AS descendant_formation_name,
        mf.formation_type AS descendant_formation_type
    FROM military_formations mf

    UNION ALL

    SELECT
        fc.root_formation_id,
        fc.root_formation_name,
        fc.root_formation_type,
        child.formation_id AS descendant_formation_id,
        child.name AS descendant_formation_name,
        child.formation_type AS descendant_formation_type
    FROM formation_closure fc
    JOIN military_formations child
        ON child.parent_id = fc.descendant_formation_id
)
SELECT *
FROM formation_closure;

CREATE INDEX idx_personnel_subdivision ON personnel(subdivision_id);
CREATE INDEX idx_personnel_search ON personnel(last_name, first_name, personal_number);
CREATE INDEX idx_subdivisions_unit ON subdivisions(unit_id);
CREATE INDEX idx_military_units_formation ON military_units(formation_id);
CREATE INDEX idx_audit_events_object ON audit_events(object_type, object_id);
