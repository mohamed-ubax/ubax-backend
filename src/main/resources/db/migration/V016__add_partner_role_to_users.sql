-- V016 : Ajout du rôle interne partenaire sur les utilisateurs
-- Permet de distinguer les sous-rôles au sein d'une agence ou d'un hôtel
-- (DIRECTEUR_AGENCE, COMMERCIAL, COMPTABLE_AGENCE, AGENT_SAV, GERANT_HOTEL, …)
-- indépendamment du rôle Keycloak (UBAX_PARTNER / UBAX_AGENT).

ALTER TABLE administrative.users
    ADD COLUMN IF NOT EXISTS partner_role VARCHAR(40);

COMMENT ON COLUMN administrative.users.partner_role IS
    'Rôle interne partenaire (com.africa.ubaxplatform.auth.codeList.PartnerRole). '
    'NULL pour les utilisateurs non rattachés à une agence ou hôtel partenaire.';
