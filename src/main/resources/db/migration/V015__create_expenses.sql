-- ─────────────────────────────────────────────────────────────────────
-- V015 – Module Paiements : expenses (dépenses comptables)
-- Règles : pas de DEFAULT sur la PK, pas de DEFAULT sur les colonnes
--          (valeurs gérées par l'application / Hibernate)
-- ─────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS administrative.expenses
(
    -- ── Identité ─────────────────────────────────────────────────
    id                  UUID            NOT NULL,

    -- ── Relations ────────────────────────────────────────────────
    agency_id           UUID            NOT NULL,
    property_id         UUID,
    created_by          UUID            NOT NULL,

    -- ── Classification ───────────────────────────────────────────
    category            VARCHAR(50)     NOT NULL,
    cost_center         VARCHAR(30)     NOT NULL,

    -- ── Description ──────────────────────────────────────────────
    label               VARCHAR(255)    NOT NULL,
    amount              NUMERIC(15, 2)  NOT NULL,
    payment_method      VARCHAR(30),
    expense_date        DATE            NOT NULL,

    -- ── Fournisseur & référence ───────────────────────────────────
    provider            VARCHAR(255),
    invoice_reference   VARCHAR(100),
    justification_url   VARCHAR(500),
    note                TEXT,

    -- ── Audit ────────────────────────────────────────────────────
    created_at          TIMESTAMP       NOT NULL,
    updated_at          TIMESTAMP       NOT NULL,

    -- ── Contraintes ──────────────────────────────────────────────
    CONSTRAINT pk_expenses              PRIMARY KEY (id),

    CONSTRAINT fk_expense_agency        FOREIGN KEY (agency_id)
        REFERENCES administrative.agencies (id),
    CONSTRAINT fk_expense_property      FOREIGN KEY (property_id)
        REFERENCES administrative.properties (id),
    CONSTRAINT fk_expense_created_by    FOREIGN KEY (created_by)
        REFERENCES administrative.users (id)
);

-- ── Index ──────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_expense_agency       ON administrative.expenses (agency_id);
CREATE INDEX IF NOT EXISTS idx_expense_property     ON administrative.expenses (property_id);
CREATE INDEX IF NOT EXISTS idx_expense_category     ON administrative.expenses (category);
CREATE INDEX IF NOT EXISTS idx_expense_date         ON administrative.expenses (expense_date);
CREATE INDEX IF NOT EXISTS idx_expense_cost_center  ON administrative.expenses (agency_id, cost_center);
CREATE INDEX IF NOT EXISTS idx_expense_period       ON administrative.expenses (agency_id, expense_date);
