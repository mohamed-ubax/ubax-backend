-- ============================================================
-- V046 : Lien ticket → technicien + prix d'intervention
-- Remplace les champs libres technician_name/phone par une FK
-- vers la table techniciens. Les champs libres sont conservés
-- pour les techniciens non enregistrés (compatibilité).
-- ============================================================

ALTER TABLE administrative.tickets
  ADD COLUMN technicien_id      UUID       REFERENCES administrative.techniciens(id) ON DELETE SET NULL,
  ADD COLUMN intervention_price DECIMAL(15, 2);

CREATE INDEX IF NOT EXISTS idx_tickets_technicien_id ON administrative.tickets(technicien_id);
