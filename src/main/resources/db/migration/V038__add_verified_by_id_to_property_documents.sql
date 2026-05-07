-- Ajout de la colonne verified_by_id sur property_documents si absente.
-- Nécessaire sur les serveurs déployés avec une version antérieure de V013.
ALTER TABLE administrative.property_documents
    ADD COLUMN IF NOT EXISTS verified_by_id UUID
        REFERENCES administrative.users (id);
