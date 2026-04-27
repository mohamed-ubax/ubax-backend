-- ============================================================
-- V019 : Ajout des coordonnées GPS sur partner_applications
-- Permet la localisation précise de l'agence ou de l'hôtel
-- ============================================================

ALTER TABLE administrative.partner_applications
    ADD COLUMN IF NOT EXISTS latitude  NUMERIC(10, 8),
    ADD COLUMN IF NOT EXISTS longitude NUMERIC(11, 8);
