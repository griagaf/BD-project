ALTER TABLE equipment_categories
    ADD COLUMN IF NOT EXISTS archived BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE weapon_categories
    ADD COLUMN IF NOT EXISTS archived BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE equipment_types
    ADD COLUMN IF NOT EXISTS purpose VARCHAR(255),
    ADD COLUMN IF NOT EXISTS crew_size INTEGER CHECK (crew_size IS NULL OR crew_size >= 0),
    ADD COLUMN IF NOT EXISTS weight_tons NUMERIC(8, 2) CHECK (weight_tons IS NULL OR weight_tons >= 0),
    ADD COLUMN IF NOT EXISTS max_speed_kmh INTEGER CHECK (max_speed_kmh IS NULL OR max_speed_kmh >= 0),
    ADD COLUMN IF NOT EXISTS operational_range_km INTEGER CHECK (operational_range_km IS NULL OR operational_range_km >= 0),
    ADD COLUMN IF NOT EXISTS adoption_year INTEGER CHECK (adoption_year IS NULL OR adoption_year BETWEEN 1900 AND 2100),
    ADD COLUMN IF NOT EXISTS manufacturer VARCHAR(160),
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS archived BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE weapon_types
    ADD COLUMN IF NOT EXISTS purpose VARCHAR(255),
    ADD COLUMN IF NOT EXISTS caliber VARCHAR(80),
    ADD COLUMN IF NOT EXISTS effective_range_m INTEGER CHECK (effective_range_m IS NULL OR effective_range_m >= 0),
    ADD COLUMN IF NOT EXISTS adoption_year INTEGER CHECK (adoption_year IS NULL OR adoption_year BETWEEN 1900 AND 2100),
    ADD COLUMN IF NOT EXISTS manufacturer VARCHAR(160),
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS archived BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_equipment_categories_archived ON equipment_categories(archived);
CREATE INDEX IF NOT EXISTS idx_equipment_types_archived ON equipment_types(archived);
CREATE INDEX IF NOT EXISTS idx_weapon_categories_archived ON weapon_categories(archived);
CREATE INDEX IF NOT EXISTS idx_weapon_types_archived ON weapon_types(archived);
