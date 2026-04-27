-- ============================================================
-- V026 – Seed : types de pièce d'identité (ID_TYPE)
-- Utilisé pour le champ idType des demandes bailleur
-- ============================================================

INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign, created_at, updated_at)
VALUES
    ('410c91b2-0948-414c-8d24-d0033a74f75f', 'ID_TYPE', 'CNI',             'Carte Nationale d''Identité',   TRUE, now(), now()),
    ('1a453229-f604-4a30-9ccb-0016c93c3fba', 'ID_TYPE', 'PASSEPORT',       'Passeport',                     TRUE, now(), now()),
    ('72ddf48a-b116-4a41-8bd6-bd1520c9ac81', 'ID_TYPE', 'PERMIS_CONDUIRE', 'Permis de conduire',            TRUE, now(), now()),
    ('d12ad98d-a994-4649-948e-919bb97728b7', 'ID_TYPE', 'TITRE_SEJOUR',    'Titre de séjour',               TRUE, now(), now()),
    ('bcc5d681-a9ac-42db-9dd7-880ded2b0c03', 'ID_TYPE', 'CARTE_CONSULAIRE','Carte consulaire',              TRUE, now(), now());
