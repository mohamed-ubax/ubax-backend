-- V031 : Remplace la colonne legal_representative par legal_rep_first_name + legal_rep_last_name
--        dans partner_applications. Migration des données existantes par split sur le premier espace.

ALTER TABLE administrative.partner_applications
    ADD COLUMN legal_rep_first_name VARCHAR(100),
    ADD COLUMN legal_rep_last_name  VARCHAR(100);

UPDATE administrative.partner_applications
SET legal_rep_first_name = SPLIT_PART(legal_representative, ' ', 1),
    legal_rep_last_name  = CASE
        WHEN POSITION(' ' IN legal_representative) > 0
            THEN TRIM(SUBSTRING(legal_representative FROM POSITION(' ' IN legal_representative) + 1))
        ELSE legal_representative
    END;

ALTER TABLE administrative.partner_applications
    ALTER COLUMN legal_rep_first_name SET NOT NULL,
    ALTER COLUMN legal_rep_last_name  SET NOT NULL;

ALTER TABLE administrative.partner_applications
    DROP COLUMN legal_representative;
