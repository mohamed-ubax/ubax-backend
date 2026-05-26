-- V061 : Ajout de la colonne unit_count sur la table properties
-- Nombre d'unités disponibles pour un bien hôtelier (ex: 5 chambres identiques).
-- Pour les biens immobiliers classiques, la valeur est toujours 1.
ALTER TABLE administrative.properties
    ADD COLUMN unit_count INTEGER NOT NULL DEFAULT 1;

-- Contrainte : au minimum 1 unité
ALTER TABLE administrative.properties
    ADD CONSTRAINT chk_properties_unit_count_positive CHECK (unit_count >= 1);
