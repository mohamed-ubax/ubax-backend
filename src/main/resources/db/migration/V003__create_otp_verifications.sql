-- ============================================================
-- V3 : Table otp_verifications (inscription et reset MDP)
-- ============================================================

CREATE TABLE IF NOT EXISTS administrative.otp_verifications (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    phone       VARCHAR(20) NOT NULL,
    purpose     VARCHAR(20) NOT NULL DEFAULT 'REGISTRATION',
    code        VARCHAR(6)  NOT NULL,
    expires_at  TIMESTAMP   NOT NULL,
    used        BOOLEAN     NOT NULL DEFAULT FALSE,
    -- Audit (BaseEntity)
    created_at  TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT now(),

    CONSTRAINT pk_otp_verifications PRIMARY KEY (id),
    CONSTRAINT chk_otp_purpose CHECK (purpose IN ('REGISTRATION', 'PASSWORD_RESET'))
);

CREATE INDEX IF NOT EXISTS idx_otp_phone      ON administrative.otp_verifications (phone);
CREATE INDEX IF NOT EXISTS idx_otp_expires_at ON administrative.otp_verifications (expires_at);