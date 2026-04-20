-- ─────────────────────────────────────────────────────────────────────
-- V013 – Biens immobiliers : properties / property_media / property_documents
-- ─────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS administrative.properties
(
    id               UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    owner_id         UUID         NOT NULL REFERENCES administrative.users (id),
    agency_id        UUID                  REFERENCES administrative.agencies (id),
    title            VARCHAR(255) NOT NULL,
    description      TEXT,
    property_type    VARCHAR(50)  NOT NULL,
    transaction_type VARCHAR(30)  NOT NULL,
    price            NUMERIC(15, 2) NOT NULL,
    condition        VARCHAR(20),
    year_built       INTEGER,
    surface_total    NUMERIC(10, 2),
    surface_living   NUMERIC(10, 2),
    rooms            INTEGER,
    bedrooms         INTEGER,
    bathrooms        INTEGER,
    balconies        INTEGER,
    floor            INTEGER,
    total_floors     INTEGER,
    address          TEXT,
    city             VARCHAR(100) NOT NULL,
    district         VARCHAR(100),
    street           VARCHAR(200),
    latitude         NUMERIC(10, 8),
    longitude        NUMERIC(11, 8),
    has_pool         BOOLEAN      NOT NULL,
    has_generator    BOOLEAN      NOT NULL,
    has_water_tank   BOOLEAN      NOT NULL,
    has_ac           BOOLEAN      NOT NULL,
    has_security     BOOLEAN      NOT NULL,
    has_parking      BOOLEAN      NOT NULL,
    has_elevator     BOOLEAN      NOT NULL,
    has_garden       BOOLEAN      NOT NULL,
    is_furnished     BOOLEAN      NOT NULL,
    accepts_pets     BOOLEAN      NOT NULL,
    is_pmr_accessible BOOLEAN     NOT NULL,
    is_boosted       BOOLEAN      NOT NULL,
    boost_expires_at TIMESTAMP,
    status           VARCHAR(30)  NOT NULL,
    rejection_reason TEXT,
    published_at     TIMESTAMP,
    expires_at       TIMESTAMP,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_properties_status       ON administrative.properties (status);
CREATE INDEX IF NOT EXISTS idx_properties_city         ON administrative.properties (city);
CREATE INDEX IF NOT EXISTS idx_properties_owner        ON administrative.properties (owner_id);
CREATE INDEX IF NOT EXISTS idx_properties_agency       ON administrative.properties (agency_id);
CREATE INDEX IF NOT EXISTS idx_properties_price        ON administrative.properties (price);
CREATE INDEX IF NOT EXISTS idx_properties_type_tx      ON administrative.properties (property_type, transaction_type);
CREATE INDEX IF NOT EXISTS idx_properties_published_at ON administrative.properties (published_at);

-- ─────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS administrative.property_media
(
    id          UUID        NOT NULL PRIMARY KEY,
    property_id UUID        NOT NULL REFERENCES administrative.properties (id) ON DELETE CASCADE,
    media_type  VARCHAR(20) NOT NULL,
    file_url    TEXT        NOT NULL,
    file_name   VARCHAR(255),
    file_size   BIGINT,
    mime_type   VARCHAR(100),
    is_cover    BOOLEAN     NOT NULL,
    sort_order  INTEGER     NOT NULL,
    created_at  TIMESTAMP   NOT NULL,
    updated_at  TIMESTAMP   NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_property_media_property ON administrative.property_media (property_id);
CREATE INDEX IF NOT EXISTS idx_property_media_type     ON administrative.property_media (media_type);
CREATE INDEX IF NOT EXISTS idx_property_media_cover    ON administrative.property_media (property_id, is_cover);
CREATE INDEX IF NOT EXISTS idx_property_media_sort     ON administrative.property_media (property_id, sort_order);

-- ─────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS administrative.property_documents
(
    id                  UUID        NOT NULL PRIMARY KEY,
    property_id         UUID        NOT NULL REFERENCES administrative.properties (id) ON DELETE CASCADE,
    verified_by_id      UUID                 REFERENCES administrative.users (id),
    doc_type            VARCHAR(50) NOT NULL,
    title               VARCHAR(255),
    file_url            TEXT        NOT NULL,
    file_name           VARCHAR(255),
    file_size           BIGINT,
    mime_type           VARCHAR(100),
    is_visible_to_public BOOLEAN   NOT NULL,
    is_verified         BOOLEAN     NOT NULL,
    verified_at         TIMESTAMP,
    verification_note   TEXT,
    created_at          TIMESTAMP   NOT NULL,
    updated_at          TIMESTAMP   NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_property_docs_property ON administrative.property_documents (property_id);
CREATE INDEX IF NOT EXISTS idx_property_docs_type     ON administrative.property_documents (doc_type);
CREATE INDEX IF NOT EXISTS idx_property_docs_verified ON administrative.property_documents (is_verified);
