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
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('7dd53f9f-19a7-4a82-8d2f-050652432c47', 'PROPERTY_TYPE', 'APARTMENT',  'Appartement',      TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('3623ce46-dfaa-4bf1-a079-e808f7243c33', 'PROPERTY_TYPE', 'VILLA',      'Villa',            TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('3297f318-88b6-4a78-9be1-367af4e67849', 'PROPERTY_TYPE', 'HOUSE',      'Maison',           TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('774845d2-e313-47cd-9025-858757b18ef6', 'PROPERTY_TYPE', 'LAND',       'Terrain',          TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('3d2eee98-5276-47ec-9b4d-b86c29a37283', 'PROPERTY_TYPE', 'OFFICE',     'Bureau',           TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('4153d989-6e54-443e-b905-ddc9bff2683f', 'PROPERTY_TYPE', 'WAREHOUSE',  'Entrepôt',         TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('a1e38ed5-57ac-43d3-8a5e-83c08875c141', 'PROPERTY_TYPE', 'STORE',      'Local commercial', TRUE);

-- ── TRANSACTION_TYPE ──────────────────────────────────────────
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('6e0a3f46-e0c6-4ae5-8ce8-3c11662c46a7', 'TRANSACTION_TYPE', 'SALE',           'Vente',                TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('61227366-af40-4f06-b86c-27141cf01080', 'TRANSACTION_TYPE', 'RENT',           'Location non meublée', TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('249f7a3c-3371-4604-a8a8-fd583fc8599d', 'TRANSACTION_TYPE', 'RENT_FURNISHED', 'Location meublée',     TRUE);

-- ── PROPERTY_CONDITION ────────────────────────────────────────
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('13d3adc1-10d6-40aa-ab9d-d22747d6be8d', 'PROPERTY_CONDITION', 'NEW',      'Neuf, jamais habité',                   TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('d2c52527-d132-46ca-b1bc-260d5bdcdc34', 'PROPERTY_CONDITION', 'GOOD',     'Bon état, prêt à habiter',              TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('42af63d8-a192-499c-9ecb-c60f11df9fb3', 'PROPERTY_CONDITION', 'RENOVATE', 'Nécessite des travaux de rénovation',   TRUE);

-- ── PROPERTY_DOCUMENT_TYPE ────────────────────────────────────
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('b6f8c160-ad8a-4388-a1f9-4fccc3acbed0', 'PROPERTY_DOCUMENT_TYPE', 'TITLE_DEED',             'Titre foncier',            TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('d5f986bc-40f9-4b18-9a6e-11776b51ed39', 'PROPERTY_DOCUMENT_TYPE', 'BUILDING_PERMIT',        'Permis de construire',     TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('7c1e7f9a-7d38-45cc-85a8-7ac572f3aa67', 'PROPERTY_DOCUMENT_TYPE', 'DIAGNOSTIC',             'Diagnostics techniques',   TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('f7d29cf3-6678-485d-a2fd-6251c9d06631', 'PROPERTY_DOCUMENT_TYPE', 'CADASTRAL_PLAN',         'Plan cadastral',           TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('9329bfc7-a991-4224-a14f-c1f7e199bdff', 'PROPERTY_DOCUMENT_TYPE', 'INSURANCE',              'Attestation d''assurance', TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('95017fbd-d130-4dc4-9353-fa52af24d1b9', 'PROPERTY_DOCUMENT_TYPE', 'CONFORMITY_CERTIFICATE', 'Certificat de conformité', TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('7595184d-bf2d-4e06-9794-6cab8a044580', 'PROPERTY_DOCUMENT_TYPE', 'OTHER',                  'Autre document',           TRUE);

-- ── MEDIA_TYPE ────────────────────────────────────────────────
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('9bfb8104-20c5-49b6-b0e4-5e57cdb31f4d', 'MEDIA_TYPE', 'PHOTO',     'Photo du bien',          TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('a4498623-7473-4c14-9755-11b51eb9e14f', 'MEDIA_TYPE', 'VIDEO',     'Vidéo de présentation',  TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('16b1f149-ae31-447f-9dff-4f3dae9d7341', 'MEDIA_TYPE', 'PLAN_2D',   'Plan architectural 2D',  TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('d097dd55-14be-46bf-8e0b-290072d9ba70', 'MEDIA_TYPE', 'PLAN_3D',   'Modélisation 3D',         TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('ab0b78ef-5e79-46d0-8a77-307f4aa99851', 'MEDIA_TYPE', 'VISIT_360', 'Visite virtuelle 360°',  TRUE);

-- ── PARTNER_TYPE ──────────────────────────────────────────────
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('fd19adb2-7cf5-4004-9703-6894a18fee39', 'PARTNER_TYPE', 'AGENCE_IMMOBILIERE', 'Agence immobilière',     TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('907fefcf-402a-4fd5-aba6-8767ba15d360', 'PARTNER_TYPE', 'HOTEL',              'Établissement hôtelier', TRUE);

-- ── EMPLOYMENT_STATUS ─────────────────────────────────────────
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('21f39c44-0110-4c55-880c-6a31b7dcd1aa', 'EMPLOYMENT_STATUS', 'EMPLOYEE',      'Salarié (CDI ou CDD)',                TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('0fe58901-6d2d-42cf-9c72-8fe1664b2f86', 'EMPLOYMENT_STATUS', 'SELF_EMPLOYED', 'Travailleur indépendant / Freelance',  TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('3c8390f6-df1f-4589-a4fd-40284f57fe95', 'EMPLOYMENT_STATUS', 'STUDENT',       'Étudiant',                            TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('f19d2878-44fa-4c95-8405-1c3121467381', 'EMPLOYMENT_STATUS', 'RETIRED',       'Retraité',                            TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('77996df6-472a-4e5b-a816-9f85d6466149', 'EMPLOYMENT_STATUS', 'UNEMPLOYED',    'Sans emploi',                         TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('1b497063-6cfb-4999-9538-cd547d93071d', 'EMPLOYMENT_STATUS', 'OTHER',         'Autre',                               TRUE);

-- ── ID_DOCUMENT_TYPE ──────────────────────────────────────────
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('a57f21fc-3fca-4f0d-9842-e39261aba207', 'ID_DOCUMENT_TYPE', 'CNI',              'Carte nationale d''identité',         TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('e86d911e-0ff5-4741-a767-087de07812bd', 'ID_DOCUMENT_TYPE', 'PASSPORT',         'Passeport biométrique',               TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('3c2268df-5147-46e0-b73e-aeed9655f37d', 'ID_DOCUMENT_TYPE', 'RESIDENCE_PERMIT', 'Titre de séjour / Carte de résident', TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('6feda4b7-e9c6-4d35-8360-e824b98d4513', 'ID_DOCUMENT_TYPE', 'DRIVER_LICENSE',   'Permis de conduire',                  TRUE);

-- ── TICKET_CATEGORY ───────────────────────────────────────────
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('41c94212-3e4b-4145-a222-b812c4153a25', 'TICKET_CATEGORY', 'LEAK',        'Fuite d''eau',           TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('1842a7b0-aa3d-46e2-9304-9e8545654341', 'TICKET_CATEGORY', 'ELECTRICAL',  'Problème électrique',    TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('0ee8a9b7-1dc2-462f-8c9b-5f0c550693b9', 'TICKET_CATEGORY', 'LOCK',        'Serrure / Fermeture',    TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('cbb71389-b8f5-4f27-8751-369c98a3262b', 'TICKET_CATEGORY', 'PLUMBING',    'Plomberie',              TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('55c65a31-50c2-4607-bc4e-32379a114937', 'TICKET_CATEGORY', 'APPLIANCE',   'Électroménager',         TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('68e2cc8d-dabb-4dcf-a642-41e9212be1ba', 'TICKET_CATEGORY', 'STRUCTURE',   'Structure / Gros œuvre', TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('7c9a524f-f7c9-4f8e-bcad-fc69bf0b49c8', 'TICKET_CATEGORY', 'PEST',        'Nuisibles / Insectes',   TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('221a6513-d03e-4f2a-9eb4-009214637e5c', 'TICKET_CATEGORY', 'COMMON_AREA', 'Parties communes',       TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('c42fce8b-95f9-4d7a-8f52-5a03b7fedb9c', 'TICKET_CATEGORY', 'OTHER',       'Autre',                  TRUE);

-- ── TICKET_PRIORITY ───────────────────────────────────────────
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('faef8eb2-3318-4833-852b-9c0424798445', 'TICKET_PRIORITY', 'LOW',    'Faible', TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('e9e2b4b5-0367-40f2-9b26-1ac616b3b330', 'TICKET_PRIORITY', 'NORMAL', 'Normal', TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('06ba9314-0130-4f46-a0bc-0dad4d6a2d61', 'TICKET_PRIORITY', 'HIGH',   'Élevé',  TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('e7cd71b4-fe81-47a3-86e2-a90ef952b04f', 'TICKET_PRIORITY', 'URGENT', 'Urgent', TRUE);

-- ── TICKET_ATTACHMENT_TYPE ────────────────────────────────────
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('9eb5226e-862f-418f-8a66-5cff79fc8617', 'TICKET_ATTACHMENT_TYPE', 'INCIDENT_PHOTO',      'Photo de l''incident',    TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('0ea4538f-32f9-4e23-ad36-32b473e1ac26', 'TICKET_ATTACHMENT_TYPE', 'INCIDENT_VIDEO',      'Vidéo de l''incident',    TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('7e8c5e0f-269d-4bb1-aa15-d517d3cac4fd', 'TICKET_ATTACHMENT_TYPE', 'INTERVENTION_REPORT', 'Rapport d''intervention', TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('6ab602da-f64d-4765-9317-d82ab546acf1', 'TICKET_ATTACHMENT_TYPE', 'INVOICE',             'Facture',                 TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('056609da-aef0-4d5b-ae52-2d98c82fcf76', 'TICKET_ATTACHMENT_TYPE', 'OTHER',               'Autre',                   TRUE);

-- ── COST_IMPUTED_TO ───────────────────────────────────────────
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('ad37a993-bb00-44bc-9646-941822a92562', 'COST_IMPUTED_TO', 'OWNER',  'À la charge du propriétaire',             TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('5b3fe543-a84d-4185-bfd8-17e728733ce8', 'COST_IMPUTED_TO', 'TENANT', 'À la charge du locataire',                TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('e1d0618b-055f-46e3-b74d-a9038b854609', 'COST_IMPUTED_TO', 'SHARED', 'Partagé entre propriétaire et locataire', TRUE);

-- ── CONTRACT_TYPE ─────────────────────────────────────────────
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('2fb17789-68ba-45b8-a19e-9b84419109fb', 'CONTRACT_TYPE', 'LEASE',       'Bail de location',           TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('5a952bac-f11d-4f82-a76a-3e6acbadf0a0', 'CONTRACT_TYPE', 'SALE',        'Acte de vente',              TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('ad60bb64-8596-4a0c-9ffb-dd6197d35d6a', 'CONTRACT_TYPE', 'RESERVATION', 'Contrat de réservation',     TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('517c0ffb-b315-4030-a4b8-4763497c830f', 'CONTRACT_TYPE', 'MANDATE',     'Mandat de gestion locative', TRUE);

-- ── SUBSCRIPTION_PLAN ─────────────────────────────────────────
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('8a07562f-072d-4f85-a20e-ab1c14dd9fd5', 'SUBSCRIPTION_PLAN', 'BASIC',   'Accès limité (annonces restreintes, sans outils avancés)', TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('6f6b4b4c-d352-4bb7-9119-01efd859683d', 'SUBSCRIPTION_PLAN', 'PRO',     'Annonces illimitées + statistiques avancées + CRM',        TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('ca495ee9-9f0c-46ba-9f9d-cb6d4d694255', 'SUBSCRIPTION_PLAN', 'PREMIUM', 'Multi-utilisateurs + branding personnalisé + rapports',    TRUE);

-- ── ALERT_FREQUENCY ───────────────────────────────────────────
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('b0dbc636-7596-40f1-bb21-fb32f4cc621b', 'ALERT_FREQUENCY', 'REALTIME', 'Notification immédiate',     TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('d78d4d62-0e8d-4fd4-a77a-0012fac46b16', 'ALERT_FREQUENCY', 'DAILY',    'Récapitulatif quotidien',    TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('efb3d2e5-2863-40ad-8c35-d329aecd79a2', 'ALERT_FREQUENCY', 'WEEKLY',   'Récapitulatif hebdomadaire', TRUE);

-- ── NEWSLETTER_FREQUENCY ──────────────────────────────────────
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('efb1023c-f7c4-49c0-80c4-70324f2eb9ae', 'NEWSLETTER_FREQUENCY', 'WEEKLY',  'Newsletter hebdomadaire', TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('1019b85c-aa60-474c-928f-c2cce4e4f66f', 'NEWSLETTER_FREQUENCY', 'MONTHLY', 'Newsletter mensuelle',    TRUE);

-- ── DISPLAY_MODE ──────────────────────────────────────────────
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('7cf269d6-908b-4100-813b-54306c8c243f', 'DISPLAY_MODE', 'LIST',  'Affichage en liste',         TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('820e9364-0560-47e6-8a41-bd7555bdc086', 'DISPLAY_MODE', 'MAP',   'Carte interactive',          TRUE);
INSERT INTO administrative.la_code_list (id, type, value, description, is_system_assign) VALUES ('518ddf43-3c2d-4241-93eb-e77e9a170f12', 'DISPLAY_MODE', 'SPLIT', 'Liste et carte côte à côte', TRUE);
