CREATE TABLE IF NOT EXISTS equipment_categories (
    category_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    CHECK (name <> '')
);

CREATE TABLE IF NOT EXISTS equipment_types (
    type_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category_id BIGINT NOT NULL REFERENCES equipment_categories(category_id) ON DELETE CASCADE,
    UNIQUE (name, category_id),
    CHECK (name <> '')
);

CREATE TABLE IF NOT EXISTS equipment_in_units (
    unit_id BIGINT NOT NULL REFERENCES military_units(unit_id) ON DELETE CASCADE,
    type_id BIGINT NOT NULL REFERENCES equipment_types(type_id),
    quantity INTEGER NOT NULL CHECK (quantity >= 0),
    PRIMARY KEY (unit_id, type_id)
);

CREATE TABLE IF NOT EXISTS weapon_categories (
    category_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    CHECK (name <> '')
);

CREATE TABLE IF NOT EXISTS weapon_types (
    type_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category_id BIGINT NOT NULL REFERENCES weapon_categories(category_id) ON DELETE CASCADE,
    UNIQUE (name, category_id),
    CHECK (name <> '')
);

CREATE TABLE IF NOT EXISTS weapon_in_units (
    unit_id BIGINT NOT NULL REFERENCES military_units(unit_id) ON DELETE CASCADE,
    type_id BIGINT NOT NULL REFERENCES weapon_types(type_id),
    quantity INTEGER NOT NULL CHECK (quantity >= 0),
    PRIMARY KEY (unit_id, type_id)
);

CREATE TABLE IF NOT EXISTS buildings (
    building_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    unit_id BIGINT NOT NULL REFERENCES military_units(unit_id) ON DELETE CASCADE,
    CHECK (name <> '')
);

CREATE TABLE IF NOT EXISTS subdivision_buildings (
    subdivision_id BIGINT NOT NULL REFERENCES subdivisions(subdivision_id) ON DELETE CASCADE,
    building_id BIGINT NOT NULL REFERENCES buildings(building_id) ON DELETE CASCADE,
    PRIMARY KEY (subdivision_id, building_id)
);

CREATE INDEX IF NOT EXISTS idx_equipment_types_category ON equipment_types(category_id);
CREATE INDEX IF NOT EXISTS idx_equipment_in_units_type ON equipment_in_units(type_id);
CREATE INDEX IF NOT EXISTS idx_weapon_types_category ON weapon_types(category_id);
CREATE INDEX IF NOT EXISTS idx_weapon_in_units_type ON weapon_in_units(type_id);
CREATE INDEX IF NOT EXISTS idx_buildings_unit ON buildings(unit_id);
CREATE INDEX IF NOT EXISTS idx_subdivision_buildings_building ON subdivision_buildings(building_id);

INSERT INTO equipment_categories (category_id, name) VALUES
(1, 'Armored vehicles'),
(2, 'Transport'),
(3, 'Communications')
ON CONFLICT (category_id) DO NOTHING;

INSERT INTO equipment_types (type_id, name, category_id) VALUES
(1, 'BMP-2', 1),
(2, 'BTR-82A', 1),
(3, 'Ural-4320', 2),
(4, 'R-168 Radio Station', 3)
ON CONFLICT (type_id) DO NOTHING;

INSERT INTO weapon_categories (category_id, name) VALUES
(1, 'Small arms'),
(2, 'Anti-tank weapons'),
(3, 'Mortars')
ON CONFLICT (category_id) DO NOTHING;

INSERT INTO weapon_types (type_id, name, category_id) VALUES
(1, 'AK-74M', 1),
(2, 'PKM', 1),
(3, 'RPG-7', 2),
(4, '2B14 Podnos', 3)
ON CONFLICT (type_id) DO NOTHING;

INSERT INTO equipment_in_units (unit_id, type_id, quantity) VALUES
(1, 1, 14),
(1, 3, 18),
(1, 4, 9),
(2, 2, 6),
(2, 3, 12)
ON CONFLICT (unit_id, type_id) DO UPDATE SET quantity = EXCLUDED.quantity;

INSERT INTO weapon_in_units (unit_id, type_id, quantity) VALUES
(1, 1, 320),
(1, 2, 42),
(1, 3, 18),
(2, 1, 90),
(2, 4, 6)
ON CONFLICT (unit_id, type_id) DO UPDATE SET quantity = EXCLUDED.quantity;

INSERT INTO buildings (building_id, name, unit_id) VALUES
(1, 'Command Post', 1),
(2, 'Vehicle Park', 1),
(3, 'Weapons Storage', 1),
(4, 'Barracks Alpha', 1),
(5, 'Vehicle Park', 2),
(6, 'Barracks Bravo', 2)
ON CONFLICT (building_id) DO NOTHING;

INSERT INTO subdivision_buildings (subdivision_id, building_id) VALUES
(10, 1),
(10, 4),
(11, 4),
(12, 4),
(20, 6)
ON CONFLICT (subdivision_id, building_id) DO NOTHING;

SELECT setval('equipment_categories_category_id_seq', (SELECT MAX(category_id) FROM equipment_categories));
SELECT setval('equipment_types_type_id_seq', (SELECT MAX(type_id) FROM equipment_types));
SELECT setval('weapon_categories_category_id_seq', (SELECT MAX(category_id) FROM weapon_categories));
SELECT setval('weapon_types_type_id_seq', (SELECT MAX(type_id) FROM weapon_types));
SELECT setval('buildings_building_id_seq', (SELECT MAX(building_id) FROM buildings));
