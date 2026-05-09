-- V041 : Rendre recorded_by nullable sur payments
-- Nécessaire pour les paiements générés automatiquement par le scheduler (aucun acteur humain)

ALTER TABLE administrative.payments ALTER COLUMN recorded_by DROP NOT NULL;
