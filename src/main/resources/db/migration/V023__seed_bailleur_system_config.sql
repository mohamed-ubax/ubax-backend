-- ============================================================
-- V023 – Seed : paramètre système rayon de détection de conflit
-- Modifiable par SUPER_ADMIN via l'API la_code_list
-- ============================================================

INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign, created_at, updated_at)
VALUES (
    'e2dc56ea-8de4-462d-abf0-beec532ea1a0',
    'GEO_CONFLICT_RADIUS_METERS',
    '50',
    'Rayon en mètres utilisé pour détecter si un bien soumis est déjà géré par une autre agence',
    TRUE,
    now(),
    now()
);
