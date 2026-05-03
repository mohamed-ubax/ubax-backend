-- ============================================================
-- V027 – Ajout du sous-rôle AGENT_IMMOBILIER dans ROLE_AGENCE
-- ============================================================

INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES
    ('d3b01821-b245-48a6-9d76-3f742536f93a', 'ROLE_AGENCE', 'AGENT_IMMOBILIER', 'Agent immobilier – gestion des biens, visites et dossiers locataires/acheteurs', TRUE);
