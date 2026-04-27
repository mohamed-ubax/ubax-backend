-- ============================================================
-- V021 – Demandes d'adhésion bailleur
-- ============================================================

CREATE TABLE IF NOT EXISTS administrative.bailleur_applications
(
    id                  UUID         NOT NULL PRIMARY KEY,
    agency_id           UUID         NOT NULL REFERENCES administrative.agencies (id),

    -- Informations personnelles
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    phone               VARCHAR(25)  NOT NULL,
    email               VARCHAR(150) NOT NULL,
    id_type             VARCHAR(50)  NOT NULL,
    id_number           VARCHAR(100) NOT NULL,

    -- Workflow
    status              VARCHAR(20)  NOT NULL,
    rejection_reason    TEXT,
    conflict_detected   BOOLEAN      NOT NULL,
    conflict_note       TEXT,
    reviewed_by_user_id UUID         REFERENCES administrative.users (id),
    reviewed_at         TIMESTAMP,

    -- Audit
    created_at          TIMESTAMP    NOT NULL,
    updated_at          TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_bailleur_app_agency  ON administrative.bailleur_applications (agency_id);
CREATE INDEX IF NOT EXISTS idx_bailleur_app_phone   ON administrative.bailleur_applications (phone);
CREATE INDEX IF NOT EXISTS idx_bailleur_app_email   ON administrative.bailleur_applications (email);
CREATE INDEX IF NOT EXISTS idx_bailleur_app_status  ON administrative.bailleur_applications (status);

-- ── Biens décrits dans la demande ────────────────────────────

CREATE TABLE IF NOT EXISTS administrative.bailleur_application_properties
(
    id             UUID           NOT NULL PRIMARY KEY,
    application_id UUID           NOT NULL REFERENCES administrative.bailleur_applications (id) ON DELETE CASCADE,
    address        TEXT           NOT NULL,
    property_type  VARCHAR(50)    NOT NULL,
    rooms          INTEGER,
    surface        NUMERIC(10, 2),
    desired_rent   NUMERIC(15, 2),
    description    TEXT,
    latitude       NUMERIC(10, 8),
    longitude      NUMERIC(11, 8),
    geo_verified   BOOLEAN        NOT NULL,
    created_at     TIMESTAMP      NOT NULL,
    updated_at     TIMESTAMP      NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_bailleur_app_prop_app ON administrative.bailleur_application_properties (application_id);
