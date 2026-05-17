-- V050 : lien direct entre un dossier locataire et le bien visé
-- Le champ est nullable pour ne pas bloquer les données existantes.
ALTER TABLE administrative.tenants
    ADD COLUMN property_id UUID
        REFERENCES administrative.properties(id)
        ON DELETE SET NULL;

CREATE INDEX idx_tenants_property_id ON administrative.tenants(property_id);
