-- ============================================================
-- V7 : Table la_code_list + seed initial (17 types)
-- Référentiels de valeurs persistées pour les selects frontend
-- ============================================================

CREATE TABLE IF NOT EXISTS administrative.la_code_list (
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    type              VARCHAR(100) NOT NULL,
    value             VARCHAR(100) NOT NULL,
    description       VARCHAR(500) NOT NULL,
    is_system_assign  BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_la_code_list PRIMARY KEY (id),
    CONSTRAINT uk_codelist_type_value UNIQUE (type, value)
);

CREATE INDEX IF NOT EXISTS idx_codelist_type ON administrative.la_code_list (type);

-- ── PROPERTY_TYPE ─────────────────────────────────────────────
INSERT INTO administrative.la_code_list (type, value, description, is_system_assign) VALUES
    ('PROPERTY_TYPE', 'APARTMENT',  'Appartement',            TRUE),
    ('PROPERTY_TYPE', 'VILLA',      'Villa',                  TRUE),
    ('PROPERTY_TYPE', 'HOUSE',      'Maison',                 TRUE),
    ('PROPERTY_TYPE', 'LAND',       'Terrain',                TRUE),
    ('PROPERTY_TYPE', 'OFFICE',     'Bureau',                 TRUE),
    ('PROPERTY_TYPE', 'WAREHOUSE',  'Entrepôt',               TRUE),
    ('PROPERTY_TYPE', 'STORE',      'Local commercial',       TRUE);

-- ── TRANSACTION_TYPE ──────────────────────────────────────────
INSERT INTO administrative.la_code_list (type, value, description, is_system_assign) VALUES
    ('TRANSACTION_TYPE', 'SALE',           'Vente',                TRUE),
    ('TRANSACTION_TYPE', 'RENT',           'Location non meublée', TRUE),
    ('TRANSACTION_TYPE', 'RENT_FURNISHED', 'Location meublée',     TRUE);

-- ── PROPERTY_CONDITION ────────────────────────────────────────
INSERT INTO administrative.la_code_list (type, value, description, is_system_assign) VALUES
    ('PROPERTY_CONDITION', 'NEW',      'Neuf, jamais habité',              TRUE),
    ('PROPERTY_CONDITION', 'GOOD',     'Bon état, prêt à habiter',         TRUE),
    ('PROPERTY_CONDITION', 'RENOVATE', 'Nécessite des travaux de rénovation', TRUE);

-- ── PROPERTY_DOCUMENT_TYPE ────────────────────────────────────
INSERT INTO administrative.la_code_list (type, value, description, is_system_assign) VALUES
    ('PROPERTY_DOCUMENT_TYPE', 'TITLE_DEED',             'Titre foncier',                TRUE),
    ('PROPERTY_DOCUMENT_TYPE', 'BUILDING_PERMIT',        'Permis de construire',         TRUE),
    ('PROPERTY_DOCUMENT_TYPE', 'DIAGNOSTIC',             'Diagnostics techniques',       TRUE),
    ('PROPERTY_DOCUMENT_TYPE', 'CADASTRAL_PLAN',         'Plan cadastral',               TRUE),
    ('PROPERTY_DOCUMENT_TYPE', 'INSURANCE',              'Attestation d''assurance',     TRUE),
    ('PROPERTY_DOCUMENT_TYPE', 'CONFORMITY_CERTIFICATE', 'Certificat de conformité',     TRUE),
    ('PROPERTY_DOCUMENT_TYPE', 'OTHER',                  'Autre document',               TRUE);

-- ── MEDIA_TYPE ────────────────────────────────────────────────
INSERT INTO administrative.la_code_list (type, value, description, is_system_assign) VALUES
    ('MEDIA_TYPE', 'PHOTO',     'Photo du bien',                       TRUE),
    ('MEDIA_TYPE', 'VIDEO',     'Vidéo de présentation',               TRUE),
    ('MEDIA_TYPE', 'PLAN_2D',   'Plan architectural 2D',               TRUE),
    ('MEDIA_TYPE', 'PLAN_3D',   'Modélisation 3D',                     TRUE),
    ('MEDIA_TYPE', 'VISIT_360', 'Visite virtuelle 360°',               TRUE);

-- ── PARTNER_TYPE ──────────────────────────────────────────────
INSERT INTO administrative.la_code_list (type, value, description, is_system_assign) VALUES
    ('PARTNER_TYPE', 'AGENCE_IMMOBILIERE', 'Agence immobilière',      TRUE),
    ('PARTNER_TYPE', 'HOTEL',              'Établissement hôtelier',  TRUE);

-- ── EMPLOYMENT_STATUS ─────────────────────────────────────────
INSERT INTO administrative.la_code_list (type, value, description, is_system_assign) VALUES
    ('EMPLOYMENT_STATUS', 'EMPLOYEE',      'Salarié (CDI ou CDD)',             TRUE),
    ('EMPLOYMENT_STATUS', 'SELF_EMPLOYED', 'Travailleur indépendant / Freelance', TRUE),
    ('EMPLOYMENT_STATUS', 'STUDENT',       'Étudiant',                         TRUE),
    ('EMPLOYMENT_STATUS', 'RETIRED',       'Retraité',                         TRUE),
    ('EMPLOYMENT_STATUS', 'UNEMPLOYED',    'Sans emploi',                      TRUE),
    ('EMPLOYMENT_STATUS', 'OTHER',         'Autre',                            TRUE);

-- ── ID_DOCUMENT_TYPE ──────────────────────────────────────────
INSERT INTO administrative.la_code_list (type, value, description, is_system_assign) VALUES
    ('ID_DOCUMENT_TYPE', 'CNI',              'Carte nationale d''identité',         TRUE),
    ('ID_DOCUMENT_TYPE', 'PASSPORT',         'Passeport biométrique',               TRUE),
    ('ID_DOCUMENT_TYPE', 'RESIDENCE_PERMIT', 'Titre de séjour / Carte de résident', TRUE),
    ('ID_DOCUMENT_TYPE', 'DRIVER_LICENSE',   'Permis de conduire',                  TRUE);

-- ── TICKET_CATEGORY ───────────────────────────────────────────
INSERT INTO administrative.la_code_list (type, value, description, is_system_assign) VALUES
    ('TICKET_CATEGORY', 'LEAK',        'Fuite d''eau',              TRUE),
    ('TICKET_CATEGORY', 'ELECTRICAL',  'Problème électrique',       TRUE),
    ('TICKET_CATEGORY', 'LOCK',        'Serrure / Fermeture',       TRUE),
    ('TICKET_CATEGORY', 'PLUMBING',    'Plomberie',                 TRUE),
    ('TICKET_CATEGORY', 'APPLIANCE',   'Électroménager',            TRUE),
    ('TICKET_CATEGORY', 'STRUCTURE',   'Structure / Gros œuvre',    TRUE),
    ('TICKET_CATEGORY', 'PEST',        'Nuisibles / Insectes',      TRUE),
    ('TICKET_CATEGORY', 'COMMON_AREA', 'Parties communes',          TRUE),
    ('TICKET_CATEGORY', 'OTHER',       'Autre',                     TRUE);

-- ── TICKET_PRIORITY ───────────────────────────────────────────
INSERT INTO administrative.la_code_list (type, value, description, is_system_assign) VALUES
    ('TICKET_PRIORITY', 'LOW',    'Faible',  TRUE),
    ('TICKET_PRIORITY', 'NORMAL', 'Normal',  TRUE),
    ('TICKET_PRIORITY', 'HIGH',   'Élevé',   TRUE),
    ('TICKET_PRIORITY', 'URGENT', 'Urgent',  TRUE);

-- ── TICKET_ATTACHMENT_TYPE ────────────────────────────────────
INSERT INTO administrative.la_code_list (type, value, description, is_system_assign) VALUES
    ('TICKET_ATTACHMENT_TYPE', 'INCIDENT_PHOTO',      'Photo de l''incident',     TRUE),
    ('TICKET_ATTACHMENT_TYPE', 'INCIDENT_VIDEO',      'Vidéo de l''incident',     TRUE),
    ('TICKET_ATTACHMENT_TYPE', 'INTERVENTION_REPORT', 'Rapport d''intervention',  TRUE),
    ('TICKET_ATTACHMENT_TYPE', 'INVOICE',             'Facture',                  TRUE),
    ('TICKET_ATTACHMENT_TYPE', 'OTHER',               'Autre',                    TRUE);

-- ── COST_IMPUTED_TO ───────────────────────────────────────────
INSERT INTO administrative.la_code_list (type, value, description, is_system_assign) VALUES
    ('COST_IMPUTED_TO', 'OWNER',  'À la charge du propriétaire',              TRUE),
    ('COST_IMPUTED_TO', 'TENANT', 'À la charge du locataire',                 TRUE),
    ('COST_IMPUTED_TO', 'SHARED', 'Partagé entre propriétaire et locataire',  TRUE);

-- ── CONTRACT_TYPE ─────────────────────────────────────────────
INSERT INTO administrative.la_code_list (type, value, description, is_system_assign) VALUES
    ('CONTRACT_TYPE', 'LEASE',       'Bail de location',               TRUE),
    ('CONTRACT_TYPE', 'SALE',        'Acte de vente',                  TRUE),
    ('CONTRACT_TYPE', 'RESERVATION', 'Contrat de réservation',         TRUE),
    ('CONTRACT_TYPE', 'MANDATE',     'Mandat de gestion locative',     TRUE);

-- ── SUBSCRIPTION_PLAN ─────────────────────────────────────────
INSERT INTO administrative.la_code_list (type, value, description, is_system_assign) VALUES
    ('SUBSCRIPTION_PLAN', 'BASIC',   'Accès limité (annonces restreintes, sans outils avancés)', TRUE),
    ('SUBSCRIPTION_PLAN', 'PRO',     'Annonces illimitées + statistiques avancées + CRM',        TRUE),
    ('SUBSCRIPTION_PLAN', 'PREMIUM', 'Multi-utilisateurs + branding personnalisé + rapports',    TRUE);

-- ── ALERT_FREQUENCY ───────────────────────────────────────────
INSERT INTO administrative.la_code_list (type, value, description, is_system_assign) VALUES
    ('ALERT_FREQUENCY', 'REALTIME', 'Notification immédiate',      TRUE),
    ('ALERT_FREQUENCY', 'DAILY',    'Récapitulatif quotidien',      TRUE),
    ('ALERT_FREQUENCY', 'WEEKLY',   'Récapitulatif hebdomadaire',   TRUE);

-- ── NEWSLETTER_FREQUENCY ──────────────────────────────────────
INSERT INTO administrative.la_code_list (type, value, description, is_system_assign) VALUES
    ('NEWSLETTER_FREQUENCY', 'WEEKLY',  'Newsletter hebdomadaire', TRUE),
    ('NEWSLETTER_FREQUENCY', 'MONTHLY', 'Newsletter mensuelle',    TRUE);

-- ── DISPLAY_MODE ──────────────────────────────────────────────
INSERT INTO administrative.la_code_list (type, value, description, is_system_assign) VALUES
    ('DISPLAY_MODE', 'LIST',  'Affichage en liste',                TRUE),
    ('DISPLAY_MODE', 'MAP',   'Carte interactive',                 TRUE),
    ('DISPLAY_MODE', 'SPLIT', 'Liste et carte côte à côte',        TRUE);
