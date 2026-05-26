-- V060 : Rendre la colonne description obligatoire sur la table agencies
-- Étape 1 : backfill des agences existantes qui ont une description NULL
UPDATE administrative.agencies
SET description = 'Description non renseignée.'
WHERE description IS NULL;

-- Étape 2 : ajouter la contrainte NOT NULL
ALTER TABLE administrative.agencies
    ALTER COLUMN description SET NOT NULL;
