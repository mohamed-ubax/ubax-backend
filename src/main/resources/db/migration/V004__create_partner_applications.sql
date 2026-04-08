-- ============================================================
-- V4 : Table partner_applications
-- Demandes d'adhésion partenaire (hôtels, agences, restau…)
-- ============================================================

CREATE TABLE IF NOT EXISTS administrative.partner_applications (
    id                      UUID        NOT NULL DEFAULT gen_random_uuid(),

    -- Identification du partenaire
    partner_type            VARCHAR(40) NOT NULL,
    company_name            VARCHAR(200) NOT NULL,
    legal_representative    VARCHAR(200) NOT NULL,
    phone                   VARCHAR(25) NOT NULL,
    email                   VARCHAR(150) NOT NULL,

    -- Localisation
    country                 VARCHAR(5)  NOT NULL DEFAULT 'CI',
    city                    VARCHAR(100) NOT NULL,
    postal_address          TEXT,
    zone                    VARCHAR(150),

    -- Informations complémentaires
    description             TEXT,
    legal_status            VARCHAR(100),
    registration_number     VARCHAR(100),

    -- Documents (URLs MinIO bucket partner-documents)
    rccm_url                VARCHAR(500),
    dfe_url                 VARCHAR(500),
    bail_url                VARCHAR(500),
    logo_url                VARCHAR(500),

    -- Statut et workflow
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    submitted_at            TIMESTAMP   NOT NULL DEFAULT now(),
    reviewed_by_user_id     UUID,
    reviewed_at             TIMESTAMP,
    rejection_reason        TEXT,

    -- Audit (BaseEntity)
    created_at              TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP   NOT NULL DEFAULT now(),

    CONSTRAINT pk_partner_applications PRIMARY KEY (id),
    CONSTRAINT fk_partner_app_reviewed_by
        FOREIGN KEY (reviewed_by_user_id) REFERENCES administrative.users (id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_partner_app_status       ON administrative.partner_applications (status);
CREATE INDEX IF NOT EXISTS idx_partner_app_email        ON administrative.partner_applications (email);
CREATE INDEX IF NOT EXISTS idx_partner_app_phone        ON administrative.partner_applications (phone);
CREATE INDEX IF NOT EXISTS idx_partner_app_submitted_at ON administrative.partner_applications (submitted_at);
CREATE INDEX IF NOT EXISTS idx_partner_app_type         ON administrative.partner_applications (partner_type);
