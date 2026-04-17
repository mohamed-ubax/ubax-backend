-- ============================================================
-- V012 : Supprime la colonne "role" parasite dans la table users
--
-- Cette colonne a été ajoutée par Hibernate ddl-auto=update sur
-- certains environnements. Les rôles sont stockés dans user_roles
-- via @ElementCollection — cette colonne est donc inutile et
-- viole la contrainte NOT NULL lors des inserts.
-- ============================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'administrative'
          AND table_name   = 'users'
          AND column_name  = 'role'
    ) THEN
        ALTER TABLE administrative.users DROP COLUMN role;
        RAISE NOTICE 'Colonne "role" supprimée de administrative.users';
    ELSE
        RAISE NOTICE 'Colonne "role" absente de administrative.users — rien à faire';
    END IF;
END $$;
