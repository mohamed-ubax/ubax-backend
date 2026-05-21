-- ============================================================
-- V057 — Fix tenants_employment_status_check constraint
--
-- Cause racine :
--   La contrainte CHECK "tenants_employment_status_check" n'est définie
--   dans aucune migration Flyway. Elle a été créée lors d'une ancienne
--   session de dev (ddl-auto non "none") avec les valeurs issues de la
--   code list V007 : EMPLOYEE, SELF_EMPLOYED, STUDENT, RETIRED, UNEMPLOYED, OTHER.
--
-- Problème :
--   Le mobile envoie "EMPLOYED" (sémantiquement correct) mais la contrainte
--   n'autorise que "EMPLOYEE" → erreur 500 au lieu d'un 400 clair.
--
-- Solution :
--   1. Supprimer la contrainte orpheline (le champ VARCHAR n'en a pas besoin,
--      la validation est gérée côté application via la_code_list).
--   2. Ajouter "EMPLOYED" dans la code list et désactiver "EMPLOYEE"
--      pour eviter la confusion future.
--   3. Migrer les enregistrements existants EMPLOYEE → EMPLOYED.
-- ============================================================

-- ── 1. Supprimer la contrainte orpheline ─────────────────────
ALTER TABLE administrative.tenants
    DROP CONSTRAINT IF EXISTS tenants_employment_status_check;

-- ── 2. Ajouter EMPLOYED dans la code list (valeur mobile) ────
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign)
VALUES ('d4e7f812-3a1b-4c9d-8e2f-5b6c7d8e9f0a', 'EMPLOYMENT_STATUS', 'EMPLOYED',
        'Salarié (CDI ou CDD)', TRUE)
ON CONFLICT DO NOTHING;

-- ── 3. Migrer les enregistrements existants EMPLOYEE → EMPLOYED ──
UPDATE administrative.tenants
SET employment_status = 'EMPLOYED'
WHERE employment_status = 'EMPLOYEE';

-- ── 4. Désactiver l'ancienne valeur EMPLOYEE (remplacée par EMPLOYED) ──
-- Conservée pour la traçabilité mais masquée des listes déroulantes.
UPDATE administrative.la_code_list
SET is_system_assign = FALSE
WHERE type = 'EMPLOYMENT_STATUS'
  AND value = 'EMPLOYEE';
