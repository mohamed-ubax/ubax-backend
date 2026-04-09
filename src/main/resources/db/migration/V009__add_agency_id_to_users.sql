-- ============================================================
-- V9 : Ajout de la colonne agency_id sur la table users
-- Permet de rattacher un agent (AGENT) ou un admin agence (AGENCY)
-- à son agence immobilière.
-- ============================================================

ALTER TABLE administrative.users
    ADD COLUMN IF NOT EXISTS agency_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = 'administrative'
          AND table_name        = 'users'
          AND constraint_name   = 'fk_user_agency'
    ) THEN
        ALTER TABLE administrative.users
            ADD CONSTRAINT fk_user_agency
                FOREIGN KEY (agency_id) REFERENCES administrative.agencies (id)
                ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_users_agency ON administrative.users (agency_id);
