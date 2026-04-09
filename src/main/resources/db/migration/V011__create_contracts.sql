-- ============================================================
-- V011 — Contrats immobiliers
-- NOTE : La contrainte FK vers administrative.properties sera ajoutée
--        lors de la migration V0XX__create_properties.sql.
--        Le champ property_id est présent mais non contraint pour l'instant.
-- ============================================================
CREATE TABLE IF NOT EXISTS administrative.contracts
(
    -- ── Identité ─────────────────────────────────────────────
    id                          UUID         NOT NULL DEFAULT gen_random_uuid(),

    -- ── Parties & bien ────────────────────────────────────────
    property_id                 UUID         NOT NULL,   -- FK ajoutée à la création de properties
    tenant_id                   UUID,
    owner_id                    UUID         NOT NULL,
    created_by                  UUID         NOT NULL,

    -- ── Type & référence ──────────────────────────────────────
    contract_type               VARCHAR(30)  NOT NULL,
    reference_number            VARCHAR(100) NOT NULL,

    -- ── Dates ─────────────────────────────────────────────────
    start_date                  DATE         NOT NULL,
    end_date                    DATE,
    reservation_duration_days   INTEGER,

    -- ── Financier – location ───────────────────────────────────
    monthly_rent                NUMERIC(15, 2),
    monthly_charges             NUMERIC(15, 2),
    deposit_amount              NUMERIC(15, 2),
    deposit_returned            BOOLEAN      NOT NULL DEFAULT FALSE,

    -- ── Financier – vente ─────────────────────────────────────
    sale_price                  NUMERIC(15, 2),
    reservation_deposit         NUMERIC(15, 2),

    -- ── Commission agence ─────────────────────────────────────
    agency_commission_rate      NUMERIC(5, 2),

    -- ── Conditions ────────────────────────────────────────────
    payment_day                 INTEGER      NOT NULL DEFAULT 5,
    special_clauses             TEXT,
    termination_conditions      TEXT,

    -- ── Statut & documents ────────────────────────────────────
    status                      VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    file_url                    VARCHAR(500),
    signed_file_url             VARCHAR(500),

    -- ── Résiliation ───────────────────────────────────────────
    terminated_at               TIMESTAMP(6),
    termination_reason          TEXT,
    terminated_by               UUID,

    -- ── Audit ─────────────────────────────────────────────────
    created_at                  TIMESTAMP(6) NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMP(6) NOT NULL DEFAULT now(),

    -- ── Contraintes ───────────────────────────────────────────
    CONSTRAINT pk_contracts PRIMARY KEY (id),
    CONSTRAINT uq_contracts_reference UNIQUE (reference_number),
    CONSTRAINT fk_contract_tenant FOREIGN KEY (tenant_id)
        REFERENCES administrative.tenants (id),
    CONSTRAINT fk_contract_owner FOREIGN KEY (owner_id)
        REFERENCES administrative.users (id),
    CONSTRAINT fk_contract_created_by FOREIGN KEY (created_by)
        REFERENCES administrative.users (id),
    CONSTRAINT fk_contract_terminated_by FOREIGN KEY (terminated_by)
        REFERENCES administrative.users (id)
);

-- ── Index ──────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_contract_property    ON administrative.contracts (property_id);
CREATE INDEX IF NOT EXISTS idx_contract_tenant      ON administrative.contracts (tenant_id);
CREATE INDEX IF NOT EXISTS idx_contract_owner       ON administrative.contracts (owner_id);
CREATE INDEX IF NOT EXISTS idx_contract_status      ON administrative.contracts (status);
CREATE INDEX IF NOT EXISTS idx_contract_type        ON administrative.contracts (contract_type);
CREATE INDEX IF NOT EXISTS idx_contract_start_date  ON administrative.contracts (start_date);
CREATE INDEX IF NOT EXISTS idx_contract_end_date    ON administrative.contracts (end_date);
