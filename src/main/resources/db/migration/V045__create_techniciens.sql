-- ============================================================
-- V045 : Table des techniciens / prestataires SAV
-- Techniciens externes gérés par les agences et hôtels.
-- Ils n'ont pas de compte sur la plateforme.
-- ============================================================

CREATE TABLE administrative.techniciens (
  id           UUID          NOT NULL,
  first_name   VARCHAR(100)  NOT NULL,
  last_name    VARCHAR(100)  NOT NULL,
  phone        VARCHAR(20)   NOT NULL,
  email        VARCHAR(150),
  avatar_url   VARCHAR(500),
  profession   VARCHAR(100)  NOT NULL,
  address      VARCHAR(255),
  available    BOOLEAN       NOT NULL,
  agency_id    UUID          REFERENCES administrative.agencies(id) ON DELETE SET NULL,
  hotel_id     UUID          REFERENCES administrative.hotels(id)   ON DELETE SET NULL,
  deleted_at   TIMESTAMP,
  created_at   TIMESTAMP     NOT NULL DEFAULT now(),
  updated_at   TIMESTAMP     NOT NULL DEFAULT now(),
  CONSTRAINT pk_techniciens PRIMARY KEY (id),
  CONSTRAINT chk_technicien_structure CHECK (
    (agency_id IS NOT NULL AND hotel_id IS NULL)
    OR (agency_id IS NULL  AND hotel_id IS NOT NULL)
  )
);

CREATE INDEX IF NOT EXISTS idx_techniciens_agency_id ON administrative.techniciens(agency_id);
CREATE INDEX IF NOT EXISTS idx_techniciens_hotel_id  ON administrative.techniciens(hotel_id);
