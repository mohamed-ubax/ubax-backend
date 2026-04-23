-- ============================================================
-- V020 – Sous-rôles applicatifs dans la_code_list
-- Types : ROLE_AGENCE · ROLE_HOTEL · ROLE_UBAX_INTERNAL
-- ROLE_UBAX_INTERNAL : accessible uniquement via /v1/code-list/admin/type/
-- ============================================================

-- ── ROLE_AGENCE ───────────────────────────────────────────────
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES
    ('19806777-513d-465f-93f3-bfd78a61ab2b', 'ROLE_AGENCE', 'DIRECTEUR_AGENCE',  'Directeur d''agence – accès complet + gestion équipe',       TRUE),
    ('fe1a12c4-8d95-4262-8ef7-543727e7773f', 'ROLE_AGENCE', 'COMMERCIAL',        'Commercial – prospects, rendez-vous, biens',                 TRUE),
    ('873512fd-9bfe-4364-8a10-5189c03d7d42', 'ROLE_AGENCE', 'COMPTABLE_AGENCE',  'Comptable agence – finances (solde masqué aux autres)',      TRUE),
    ('6346a20d-1420-483c-8944-58bde7c6b125', 'ROLE_AGENCE', 'AGENT_SAV',         'Agent SAV – tickets et interventions',                       TRUE);

-- ── ROLE_HOTEL ────────────────────────────────────────────────
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES
    ('775df92e-a31e-452b-92b1-b2e804279de9', 'ROLE_HOTEL', 'GERANT_HOTEL',             'Gérant hôtel – accès complet hôtel',                  TRUE),
    ('9e18d8fa-1fb8-4e45-9cc0-b597aafd0cac', 'ROLE_HOTEL', 'RECEPTIONNISTE',           'Réceptionniste – réservations, check-in/out',          TRUE),
    ('da89e8c5-6882-4e5e-9c16-286ae02f4ff7', 'ROLE_HOTEL', 'COMPTABLE_HOTEL',          'Comptable hôtel – facturation et revenus',             TRUE),
    ('373fde6c-010f-43c2-b3b0-6490cb8125e8', 'ROLE_HOTEL', 'RESPONSABLE_HEBERGEMENT',  'Responsable hébergement – espaces et chambres',        TRUE);

-- ── ROLE_UBAX_INTERNAL ────────────────────────────────────────
-- Accès restreint : GET /v1/code-list/admin/type/ROLE_UBAX_INTERNAL (ADMIN requis)
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES
    ('44d14fbe-9f87-4abe-89ec-7d423b34dc00', 'ROLE_UBAX_INTERNAL', 'DIRECTEUR_GENERAL', 'Directeur général – vision globale, accès complet back-office', TRUE),
    ('5fa31081-c6ee-4a48-a2e5-8b353eb9dca3', 'ROLE_UBAX_INTERNAL', 'SUPPORT_CLIENT',    'Support client – tickets, réclamations partenaires et clients', TRUE),
    ('864d0bab-bdac-4be7-a801-b279c49d7d3c', 'ROLE_UBAX_INTERNAL', 'OPERATIONS',        'Opérations – onboarding partenaires, modération annonces',     TRUE),
    ('13a8f6b3-8101-4c61-b81c-c484a475fea3', 'ROLE_UBAX_INTERNAL', 'FINANCE',           'Finance – abonnements, commissions, revenus, rapports',         TRUE),
    ('6a6f7c27-1a08-4849-98ec-a8dca9b4e44f', 'ROLE_UBAX_INTERNAL', 'COMMERCIAL',        'Commercial – acquisition partenaires, suivi commercial',        TRUE);
