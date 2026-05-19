-- ============================================================
-- V052 : Ajout de la colonne description sur property_amenities
-- Permet d'associer une description à chaque commodité d'un bien
-- (standard via code ou personnalisée via custom_value)
-- ============================================================

ALTER TABLE administrative.property_amenities
  ADD COLUMN description VARCHAR(500);
