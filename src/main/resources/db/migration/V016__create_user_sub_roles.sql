-- V016 : Table des sous-rôles applicatifs (non portés par Keycloak)
-- Portée (scope) : ubax_internal (admins), agence (partenaires agence), hotel (partenaires hôtel)

CREATE TABLE administrative.user_sub_roles (
    id         UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    role       VARCHAR(60) NOT NULL,
    scope      VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_user_sub_roles PRIMARY KEY (id),
    CONSTRAINT uq_user_sub_roles UNIQUE (user_id, role, scope),
    CONSTRAINT fk_user_sub_roles_user FOREIGN KEY (user_id)
        REFERENCES administrative.users (id) ON DELETE CASCADE,
    CONSTRAINT chk_user_sub_roles_scope
        CHECK (scope IN ('ubax_internal', 'agence', 'hotel'))
);

CREATE INDEX idx_user_sub_roles_user_id ON administrative.user_sub_roles (user_id);
CREATE INDEX idx_user_sub_roles_scope   ON administrative.user_sub_roles (scope);
