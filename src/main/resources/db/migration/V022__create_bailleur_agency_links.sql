-- ============================================================
-- V022 – Liens bailleur ↔ agence
-- ============================================================

CREATE TABLE IF NOT EXISTS administrative.bailleur_agency_links
(
    id               UUID      NOT NULL PRIMARY KEY,
    bailleur_user_id UUID      NOT NULL REFERENCES administrative.users (id),
    agency_id        UUID      NOT NULL REFERENCES administrative.agencies (id),
    joined_at        TIMESTAMP NOT NULL,
    created_at       TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP NOT NULL,

    CONSTRAINT uq_bailleur_agency UNIQUE (bailleur_user_id, agency_id)
);

CREATE INDEX IF NOT EXISTS idx_bailleur_link_user   ON administrative.bailleur_agency_links (bailleur_user_id);
CREATE INDEX IF NOT EXISTS idx_bailleur_link_agency ON administrative.bailleur_agency_links (agency_id);
