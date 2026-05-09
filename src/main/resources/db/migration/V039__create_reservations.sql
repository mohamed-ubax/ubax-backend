-- V039 : Module Reservations (hotel + agence mobile)
-- Statuts : PENDING → CONFIRMED → COMPLETED | CANCELLED | NO_SHOW

CREATE TABLE administrative.reservations
(
    id                  UUID         NOT NULL PRIMARY KEY,
    property_id         UUID         NOT NULL,
    client_id           UUID         NOT NULL,
    check_in_date       DATE         NOT NULL,
    check_out_date      DATE         NOT NULL,
    number_of_nights    INTEGER      NOT NULL,
    guest_count         INTEGER      NOT NULL,
    -- Snapshot du prix à la date de réservation (le tarif hôtel peut évoluer)
    price_per_night     DECIMAL(15, 2) NOT NULL,
    total_amount        DECIMAL(15, 2) NOT NULL,
    status              VARCHAR(30)  NOT NULL,
    notes               TEXT,
    cancellation_reason TEXT,
    cancelled_by_id     UUID,
    confirmed_at        TIMESTAMP,
    cancelled_at        TIMESTAMP,
    completed_at        TIMESTAMP,
    created_at          TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMP,

    CONSTRAINT fk_reservation_property FOREIGN KEY (property_id)
        REFERENCES administrative.properties (id),
    CONSTRAINT fk_reservation_client FOREIGN KEY (client_id)
        REFERENCES administrative.users (id),
    CONSTRAINT fk_reservation_cancelled_by FOREIGN KEY (cancelled_by_id)
        REFERENCES administrative.users (id),
    CONSTRAINT chk_reservation_dates CHECK (check_out_date > check_in_date),
    CONSTRAINT chk_reservation_nights CHECK (number_of_nights > 0),
    CONSTRAINT chk_reservation_guests CHECK (guest_count > 0),
    CONSTRAINT chk_reservation_status CHECK (status IN
        ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW'))
);

CREATE INDEX idx_reservations_property ON administrative.reservations (property_id);
CREATE INDEX idx_reservations_client ON administrative.reservations (client_id);
CREATE INDEX idx_reservations_status ON administrative.reservations (status);
CREATE INDEX idx_reservations_dates ON administrative.reservations (check_in_date, check_out_date);
