-- ============================================================
-- V035 : Commodités des biens (property_amenities)
-- Remplace les colonnes booléennes par une table dédiée
-- permettant commodités standard (code_list) + personnalisées
-- ============================================================

-- ── Création de la table ──────────────────────────────────────
CREATE TABLE administrative.property_amenities (
  id                  UUID         NOT NULL,
  property_id         UUID         NOT NULL,
  code                VARCHAR(100),
  custom_value        VARCHAR(100),
  custom_description  VARCHAR(255),
  created_at          TIMESTAMP    NOT NULL DEFAULT now(),
  CONSTRAINT pk_property_amenities  PRIMARY KEY (id),
  CONSTRAINT fk_amenity_property    FOREIGN KEY (property_id)
    REFERENCES administrative.properties(id) ON DELETE CASCADE,
  CONSTRAINT chk_amenity_type CHECK (
    (code IS NOT NULL AND custom_value IS NULL)
    OR (code IS NULL AND custom_value IS NOT NULL)
  )
);

CREATE INDEX idx_property_amenities_property_id ON administrative.property_amenities(property_id);

-- ── Migration des données booléennes existantes ───────────────
INSERT INTO administrative.property_amenities (id, property_id, code)
SELECT '062c050b-da75-4c94-bfa3-e0fcc1f81127', id, 'POOL'
FROM administrative.properties WHERE has_pool = TRUE;

INSERT INTO administrative.property_amenities (id, property_id, code)
SELECT 'fe7f5190-89cf-4e9d-936e-f0a92a20427c', id, 'GENERATOR'
FROM administrative.properties WHERE has_generator = TRUE;

INSERT INTO administrative.property_amenities (id, property_id, code)
SELECT '137febee-f88a-427a-be58-764e66d8f636', id, 'WATER_TANK'
FROM administrative.properties WHERE has_water_tank = TRUE;

INSERT INTO administrative.property_amenities (id, property_id, code)
SELECT '6c93ce26-f726-48b9-94a0-caebd8ccf7d1', id, 'AC'
FROM administrative.properties WHERE has_ac = TRUE;

INSERT INTO administrative.property_amenities (id, property_id, code)
SELECT 'c89e60b8-f304-440f-8079-4eb792826404', id, 'SECURITY'
FROM administrative.properties WHERE has_security = TRUE;

INSERT INTO administrative.property_amenities (id, property_id, code)
SELECT '58f18d06-5c18-4913-9bf2-a6d14a3867cf', id, 'PARKING'
FROM administrative.properties WHERE has_parking = TRUE;

INSERT INTO administrative.property_amenities (id, property_id, code)
SELECT 'd6d2ac64-a40d-4834-95ab-96a09a9808a3', id, 'ELEVATOR'
FROM administrative.properties WHERE has_elevator = TRUE;

INSERT INTO administrative.property_amenities (id, property_id, code)
SELECT '082a29a0-5c38-47ff-bfcc-5a7cc26ae621', id, 'GARDEN'
FROM administrative.properties WHERE has_garden = TRUE;

INSERT INTO administrative.property_amenities (id, property_id, code)
SELECT '1c68d8dc-2d0a-4ab8-b8d0-fbdabd3a71df', id, 'FURNISHED'
FROM administrative.properties WHERE is_furnished = TRUE;

INSERT INTO administrative.property_amenities (id, property_id, code)
SELECT '5312df61-fe07-4825-b1be-f0d9456f9aad', id, 'PETS'
FROM administrative.properties WHERE accepts_pets = TRUE;

-- ── Suppression des colonnes booléennes obsolètes ─────────────
ALTER TABLE administrative.properties
  DROP COLUMN IF EXISTS has_pool,
  DROP COLUMN IF EXISTS has_generator,
  DROP COLUMN IF EXISTS has_water_tank,
  DROP COLUMN IF EXISTS has_ac,
  DROP COLUMN IF EXISTS has_security,
  DROP COLUMN IF EXISTS has_parking,
  DROP COLUMN IF EXISTS has_elevator,
  DROP COLUMN IF EXISTS has_garden,
  DROP COLUMN IF EXISTS is_furnished,
  DROP COLUMN IF EXISTS accepts_pets,
  DROP COLUMN IF EXISTS is_pmr_accessible;
