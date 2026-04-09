-- ============================================================
-- V010 — Dossiers locataires
-- ============================================================
CREATE TABLE IF NOT EXISTS administrative.tenants
(
    -- ── Identité ─────────────────────────────────────────────
    id                      UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id                 UUID         NOT NULL,

    -- ── Situation professionnelle ─────────────────────────────
    employment_status       VARCHAR(50),
    employer_name           VARCHAR(255),
    monthly_income          NUMERIC(15, 2),

    -- ── Garant ────────────────────────────────────────────────
    has_guarantor           BOOLEAN      NOT NULL DEFAULT FALSE,
    guarantor_name          VARCHAR(255),
    guarantor_phone         VARCHAR(20),
    guarantor_email         VARCHAR(255),

    -- ── Documents KYC (URLs MinIO – bucket tenant-documents) ──
    id_document_url         VARCHAR(500),
    id_document_type        VARCHAR(30),
    id_document_number      VARCHAR(100),
    id_document_expiry      DATE,
    income_proof_url        VARCHAR(500),
    address_proof_url       VARCHAR(500),

    -- ── Statut & qualification ────────────────────────────────
    status                  VARCHAR(20)  NOT NULL DEFAULT 'INCOMPLETE',
    is_qualified            BOOLEAN      NOT NULL DEFAULT FALSE,
    qualified_at            TIMESTAMP(6),
    qualified_by            UUID,

    -- ── Refus & notes ─────────────────────────────────────────
    rejection_reason        TEXT,
    internal_note           TEXT,

    -- ── Soft delete ───────────────────────────────────────────
    deleted_at              TIMESTAMP(6),

    -- ── Audit ─────────────────────────────────────────────────
    created_at              TIMESTAMP(6) NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP(6) NOT NULL DEFAULT now(),

    -- ── Contraintes ───────────────────────────────────────────
    CONSTRAINT pk_tenants PRIMARY KEY (id),
    CONSTRAINT uq_tenant_user_id UNIQUE (user_id),
    CONSTRAINT fk_tenant_user FOREIGN KEY (user_id)
        REFERENCES administrative.users (id),
    CONSTRAINT fk_tenant_qualified_by FOREIGN KEY (qualified_by)
        REFERENCES administrative.users (id)
);

-- ── Index ──────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_tenant_user        ON administrative.tenants (user_id);
CREATE INDEX IF NOT EXISTS idx_tenant_status      ON administrative.tenants (status);
CREATE INDEX IF NOT EXISTS idx_tenant_qualified   ON administrative.tenants (is_qualified);
CREATE INDEX IF NOT EXISTS idx_tenant_deleted_at  ON administrative.tenants (deleted_at);
