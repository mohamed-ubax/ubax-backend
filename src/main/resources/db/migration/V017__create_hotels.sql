-- V017 : Création de la table hotels et lien FK sur users
-- Symétrique avec la table agencies pour les partenaires hôteliers.

CREATE TABLE administrative.hotels (
    id              UUID PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    description     TEXT,
    registration_number VARCHAR(100),
    logo_url        VARCHAR(500),
    address         TEXT,
    city            VARCHAR(100),
    country         VARCHAR(5)   NOT NULL DEFAULT 'SN',
    phone           VARCHAR(20),
    email           VARCHAR(150),
    website         VARCHAR(300),
    subscription_plan       VARCHAR(20)  NOT NULL DEFAULT 'BASIC',
    subscription_expires_at TIMESTAMP,
    is_verified     BOOLEAN      NOT NULL DEFAULT FALSE,
    verified_at     TIMESTAMP,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted_at      TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);

COMMENT ON TABLE administrative.hotels IS
    'Hôtels partenaires de la plateforme UBAX. '
    'Symétrique avec la table agencies pour les structures hôtelières.';

ALTER TABLE administrative.users
    ADD COLUMN IF NOT EXISTS hotel_id UUID
        REFERENCES administrative.hotels (id)
        ON DELETE SET NULL;

COMMENT ON COLUMN administrative.users.hotel_id IS
    'Hôtel auquel est rattaché le membre (non null uniquement pour les rôles GERANT_HOTEL, '
    'RECEPTIONNISTE, COMPTABLE_HOTEL, RESPONSABLE_HEBERGEMENT).';

CREATE INDEX IF NOT EXISTS idx_users_hotel_id ON administrative.users (hotel_id);
