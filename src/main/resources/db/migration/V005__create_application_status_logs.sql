-- ============================================================
-- V5 : Table application_status_logs
-- Journal d'audit des transitions de statut des demandes partenaire
-- ============================================================

CREATE TABLE IF NOT EXISTS administrative.application_status_logs (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),

    -- Relations
    application_id      UUID        NOT NULL,
    changed_by_user_id  UUID,

    -- Transition
    previous_status     VARCHAR(20),
    new_status          VARCHAR(20) NOT NULL,
    comment             TEXT,
    changed_at          TIMESTAMP   NOT NULL DEFAULT now(),

    -- Audit (BaseEntity)
    created_at          TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP   NOT NULL DEFAULT now(),

    CONSTRAINT pk_application_status_logs PRIMARY KEY (id),
    CONSTRAINT fk_status_log_application
        FOREIGN KEY (application_id) REFERENCES administrative.partner_applications (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_status_log_changed_by
        FOREIGN KEY (changed_by_user_id) REFERENCES administrative.users (id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_status_log_application ON administrative.application_status_logs (application_id);
CREATE INDEX IF NOT EXISTS idx_status_log_changed_at  ON administrative.application_status_logs (changed_at);
