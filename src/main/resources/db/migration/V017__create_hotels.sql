-- V017 : Table des hôtels partenaires + rattachement sur users

CREATE TABLE administrative.hotels (
    id                       UUID         NOT NULL,
    name                     VARCHAR(150) NOT NULL,
    description              TEXT,
    registration_number      VARCHAR(100),
    logo_url                 VARCHAR(500),
    address                  TEXT,
    city                     VARCHAR(100),
    country                  VARCHAR(5)   NOT NULL,
    phone                    VARCHAR(20),
    email                    VARCHAR(150),
    website                  VARCHAR(300),
    stars                    SMALLINT     CHECK (stars BETWEEN 1 AND 5),
    total_rooms              INTEGER,
    subscription_plan        VARCHAR(20)  NOT NULL,
    subscription_expires_at  TIMESTAMP WITH TIME ZONE,
    is_verified              BOOLEAN      NOT NULL,
    verified_at              TIMESTAMP WITH TIME ZONE,
    is_active                BOOLEAN      NOT NULL,
    deleted_at               TIMESTAMP WITH TIME ZONE,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_hotels   PRIMARY KEY (id),
    CONSTRAINT uq_hotels_phone UNIQUE (phone),
    CONSTRAINT uq_hotels_email UNIQUE (email)
);

CREATE INDEX idx_hotels_city       ON administrative.hotels (city);
CREATE INDEX idx_hotels_verified   ON administrative.hotels (is_verified);
CREATE INDEX idx_hotels_active     ON administrative.hotels (is_active);
CREATE INDEX idx_hotels_deleted_at ON administrative.hotels (deleted_at);

-- Rattachement optionnel d'un utilisateur à un hôtel
ALTER TABLE administrative.users
    ADD COLUMN hotel_id UUID,
    ADD CONSTRAINT fk_user_hotel FOREIGN KEY (hotel_id)
        REFERENCES administrative.hotels (id) ON DELETE SET NULL;

CREATE INDEX idx_users_hotel_id ON administrative.users (hotel_id);
