-- ============================================================
-- V027 – Seed : liste des pays (COUNTRY)
-- Utilisé pour le champ country des utilisateurs et formulaires
-- ============================================================

INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign, created_at, updated_at)
VALUES
    ('a1f3e2b4-1111-4c2a-8e01-000000000001', 'COUNTRY', 'SN', 'Sénégal',       TRUE, now(), now()),
    ('a1f3e2b4-1111-4c2a-8e01-000000000002', 'COUNTRY', 'CI', 'Côte d''Ivoire', TRUE, now(), now()),
    ('a1f3e2b4-1111-4c2a-8e01-000000000003', 'COUNTRY', 'ML', 'Mali',           TRUE, now(), now()),
    ('a1f3e2b4-1111-4c2a-8e01-000000000004', 'COUNTRY', 'BF', 'Burkina Faso',   TRUE, now(), now()),
    ('a1f3e2b4-1111-4c2a-8e01-000000000005', 'COUNTRY', 'GN', 'Guinée',         TRUE, now(), now()),
    ('a1f3e2b4-1111-4c2a-8e01-000000000013', 'COUNTRY', 'GW', 'Guinée-Bissau', TRUE, now(), now()),
    ('a1f3e2b4-1111-4c2a-8e01-000000000006', 'COUNTRY', 'NE', 'Niger',          TRUE, now(), now()),
    ('a1f3e2b4-1111-4c2a-8e01-000000000007', 'COUNTRY', 'TG', 'Togo',           TRUE, now(), now()),
    ('a1f3e2b4-1111-4c2a-8e01-000000000008', 'COUNTRY', 'BJ', 'Bénin',          TRUE, now(), now()),
    ('a1f3e2b4-1111-4c2a-8e01-000000000009', 'COUNTRY', 'NG', 'Nigeria',        TRUE, now(), now()),
    ('a1f3e2b4-1111-4c2a-8e01-000000000010', 'COUNTRY', 'GH', 'Ghana',          TRUE, now(), now()),
    ('a1f3e2b4-1111-4c2a-8e01-000000000011', 'COUNTRY', 'CM', 'Cameroun',       TRUE, now(), now()),
    ('a1f3e2b4-1111-4c2a-8e01-000000000012', 'COUNTRY', 'MA', 'Maroc',          TRUE, now(), now());
