-- ============================================================
-- V6 : Ajout de storage_slug sur partner_applications
-- Slug MinIO du répertoire partenaire dans partner-documents
-- Exemple : "Hôtel La Téranga" → "hotel-la-teranga"
-- ============================================================

ALTER TABLE administrative.partner_applications
    ADD COLUMN IF NOT EXISTS storage_slug VARCHAR(200);

CREATE INDEX IF NOT EXISTS idx_partner_app_storage_slug
    ON administrative.partner_applications (storage_slug);
