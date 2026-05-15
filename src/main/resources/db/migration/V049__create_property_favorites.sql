-- V049 : table des biens immobiliers mis en favoris par les clients mobiles

CREATE TABLE administrative.property_favorites
(
    id          UUID        NOT NULL,
    user_id     UUID        NOT NULL,
    property_id UUID        NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT now(),

    CONSTRAINT pk_property_favorites PRIMARY KEY (id),
    CONSTRAINT fk_favorite_user
        FOREIGN KEY (user_id) REFERENCES administrative.users (id),
    CONSTRAINT fk_favorite_property
        FOREIGN KEY (property_id) REFERENCES administrative.properties (id),
    CONSTRAINT uq_favorite_user_property
        UNIQUE (user_id, property_id)
);

CREATE INDEX idx_favorites_user_id ON administrative.property_favorites (user_id);
