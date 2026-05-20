-- ============================================================
-- V054 : Simplification du module bailleur
-- Le formulaire de demande ne contient plus la liste des biens.
-- Le bailleur décrit son besoin en texte libre ; l'agence vérifie
-- les biens en dehors de l'app avant approbation.
-- ============================================================

-- 1. Ajout de la description libre du bailleur
ALTER TABLE administrative.bailleur_applications
  ADD COLUMN description TEXT;

-- 2. Suppression des colonnes de conflit géographique (plus de biens à l'étape demande)
ALTER TABLE administrative.bailleur_applications
  DROP COLUMN IF EXISTS conflict_detected;

ALTER TABLE administrative.bailleur_applications
  DROP COLUMN IF EXISTS conflict_note;

-- 3. Suppression de la table des biens liés à la demande (plus utilisée)
DROP TABLE IF EXISTS administrative.bailleur_application_properties;
