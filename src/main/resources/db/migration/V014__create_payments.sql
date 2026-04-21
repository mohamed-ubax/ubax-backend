-- ─────────────────────────────────────────────────────────────────────
-- V014 – Module Paiements : payments
-- Règles : pas de DEFAULT sur la PK, pas de DEFAULT sur les colonnes
--          (valeurs gérées par l'application / Hibernate)
-- ─────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS administrative.payments
(
    -- ── Identité ─────────────────────────────────────────────────
    id              UUID            NOT NULL,

    -- ── Relations ────────────────────────────────────────────────
    contract_id     UUID,
    tenant_id       UUID,
    agency_id       UUID,
    property_id     UUID,
    recorded_by     UUID            NOT NULL,

    -- ── Classification ───────────────────────────────────────────
    payment_type    VARCHAR(30)     NOT NULL,
    payment_method  VARCHAR(30),
    status          VARCHAR(30)     NOT NULL,

    -- ── Montants ─────────────────────────────────────────────────
    amount          NUMERIC(15, 2)  NOT NULL,
    amount_paid     NUMERIC(15, 2),

    -- ── Dates ────────────────────────────────────────────────────
    due_date        DATE            NOT NULL,
    paid_date       DATE,

    -- ── Libellé de période (ex : "Octobre 2025") ─────────────────
    period_label    VARCHAR(50),

    -- ── Référence & justificatif ──────────────────────────────────
    reference       VARCHAR(100),
    receipt_url     VARCHAR(500),
    note            TEXT,

    -- ── Audit ────────────────────────────────────────────────────
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP       NOT NULL,

    -- ── Contraintes ──────────────────────────────────────────────
    CONSTRAINT pk_payments          PRIMARY KEY (id),
    CONSTRAINT uq_payment_reference UNIQUE (reference),

    CONSTRAINT fk_payment_contract  FOREIGN KEY (contract_id)
        REFERENCES administrative.contracts (id),
    CONSTRAINT fk_payment_tenant    FOREIGN KEY (tenant_id)
        REFERENCES administrative.tenants (id),
    CONSTRAINT fk_payment_agency    FOREIGN KEY (agency_id)
        REFERENCES administrative.agencies (id),
    CONSTRAINT fk_payment_property  FOREIGN KEY (property_id)
        REFERENCES administrative.properties (id),
    CONSTRAINT fk_payment_recorded_by FOREIGN KEY (recorded_by)
        REFERENCES administrative.users (id)
);

-- ── Index ──────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_payment_contract    ON administrative.payments (contract_id);
CREATE INDEX IF NOT EXISTS idx_payment_tenant      ON administrative.payments (tenant_id);
CREATE INDEX IF NOT EXISTS idx_payment_agency      ON administrative.payments (agency_id);
CREATE INDEX IF NOT EXISTS idx_payment_property    ON administrative.payments (property_id);
CREATE INDEX IF NOT EXISTS idx_payment_status      ON administrative.payments (status);
CREATE INDEX IF NOT EXISTS idx_payment_type        ON administrative.payments (payment_type);
CREATE INDEX IF NOT EXISTS idx_payment_due_date    ON administrative.payments (due_date);
CREATE INDEX IF NOT EXISTS idx_payment_paid_date   ON administrative.payments (paid_date);
CREATE INDEX IF NOT EXISTS idx_payment_period      ON administrative.payments (agency_id, due_date);
