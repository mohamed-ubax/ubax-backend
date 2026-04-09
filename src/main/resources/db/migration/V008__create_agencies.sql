-- ============================================================
-- V8 : Table agencies – agences immobilières de la plateforme UBAX
-- ============================================================

CREATE TABLE IF NOT EXISTS administrative.agencies (
    id                      UUID         NOT NULL DEFAULT gen_random_uuid(),
    name                    VARCHAR(150) NOT NULL,
    description             TEXT,
    registration_number     VARCHAR(100),
    logo_url                VARCHAR(500),
    address                 TEXT,
    city                    VARCHAR(100),
    country                 VARCHAR(5)   NOT NULL DEFAULT 'SN',
    phone                   VARCHAR(20),
    email                   VARCHAR(150),
    website                 VARCHAR(300),
    subscription_plan       VARCHAR(20)  NOT NULL DEFAULT 'BASIC',
    subscription_expires_at TIMESTAMP,
    is_verified             BOOLEAN      NOT NULL DEFAULT FALSE,
    verified_at             TIMESTAMP,
    is_active               BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted_at              TIMESTAMP,
    created_at              TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT pk_agencies          PRIMARY KEY (id),
    CONSTRAINT uq_agencies_email    UNIQUE (email),
    CONSTRAINT uq_agencies_phone    UNIQUE (phone)
);

CREATE INDEX IF NOT EXISTS idx_agencies_verified   ON administrative.agencies (is_verified);
CREATE INDEX IF NOT EXISTS idx_agencies_active     ON administrative.agencies (is_active);
CREATE INDEX IF NOT EXISTS idx_agencies_deleted_at ON administrative.agencies (deleted_at);
CREATE INDEX IF NOT EXISTS idx_agencies_city       ON administrative.agencies (city);
