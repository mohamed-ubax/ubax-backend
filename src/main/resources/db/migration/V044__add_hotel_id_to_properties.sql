-- V044 : Lien bien → hôtel
-- Permet de filtrer les biens (chambres/suites) par hôtel depuis l'API.

ALTER TABLE administrative.properties
    ADD COLUMN hotel_id UUID
        REFERENCES administrative.hotels(id)
        ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_properties_hotel_id ON administrative.properties(hotel_id);
