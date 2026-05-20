-- ============================================================
-- V055 : Ajout pièces d'identité recto/verso + email nullable
--        sur bailleur_applications
-- Le bailleur upload sa pièce d'identité avant soumission via
-- POST /v1/storage/upload?bucket=bailleur-documents
-- L'email est désormais optionnel en DB (extrait du compte ou
-- fourni dans le body si le compte n'en a pas).
-- ============================================================

-- 1. Email nullable (les clients inscrits par téléphone n'en ont pas)
ALTER TABLE administrative.bailleur_applications
  ALTER COLUMN email DROP NOT NULL;

-- 2. URLs des photos recto/verso de la pièce d'identité
ALTER TABLE administrative.bailleur_applications
  ADD COLUMN id_doc_recto_url TEXT,
  ADD COLUMN id_doc_verso_url TEXT;
