-- V043: Lie les demandes bailleur au compte CLIENT authentifié.
-- Nullable pour préserver les lignes existantes soumises via l'ancien formulaire public.
ALTER TABLE administrative.bailleur_applications
    ADD COLUMN user_id UUID REFERENCES administrative.users(id);
