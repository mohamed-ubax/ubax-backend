-- ============================================================
-- V2 : Table users et user_roles (sans relation agency)
-- ============================================================

CREATE TABLE IF NOT EXISTS administrative.users (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    -- Identité Keycloak
    keycloak_id         VARCHAR(100) NOT NULL,
    -- Informations personnelles
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    email               VARCHAR(150),
    phone               VARCHAR(20),
    date_of_birth       DATE,
    address             TEXT,
    city                VARCHAR(100),
    country             VARCHAR(5)  NOT NULL DEFAULT 'SN',
    language            VARCHAR(5)  NOT NULL DEFAULT 'fr',
    avatar_url          VARCHAR(500),
    -- Statut
    last_login_at       TIMESTAMP,
    is_email_verified   BOOLEAN     NOT NULL DEFAULT FALSE,
    is_phone_verified   BOOLEAN     NOT NULL DEFAULT FALSE,
    is_identity_verified BOOLEAN    NOT NULL DEFAULT FALSE,
    is_active           BOOLEAN     NOT NULL DEFAULT TRUE,
    deleted_at          TIMESTAMP,
    -- Audit (BaseEntity)
    created_at          TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP   NOT NULL DEFAULT now(),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_keycloak_id UNIQUE (keycloak_id),
    CONSTRAINT uq_users_email       UNIQUE (email),
    CONSTRAINT uq_users_phone       UNIQUE (phone)
);

CREATE INDEX IF NOT EXISTS idx_users_active     ON administrative.users (is_active);
CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON administrative.users (deleted_at);

-- ── Rôles utilisateur (collection) ─────────────────────────────────
CREATE TABLE IF NOT EXISTS administrative.user_roles (
    user_id UUID        NOT NULL,
    role    VARCHAR(30) NOT NULL,

    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id)
        REFERENCES administrative.users (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_roles_role ON administrative.user_roles (role);