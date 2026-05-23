-- V058 : Module Property Visit Requests (Réservation de visite)
-- Statuts : PENDING → CONFIRMED / REJECTED | CANCELLED

-- Table 1 : Demandes de visite (clients)
CREATE TABLE administrative.property_visit_requests
(
    id                       UUID         NOT NULL PRIMARY KEY,
    property_id              UUID         NOT NULL,
    client_id                UUID         NOT NULL,
    agent_id                 UUID,  -- Agent assigné de l'agence (optionnel)
    
    requested_date           DATE         NOT NULL,
    requested_time_slot      VARCHAR(20)  NOT NULL,  -- ex: "10:00-14:00"
    
    status                   VARCHAR(20)  NOT NULL,  -- PENDING, CONFIRMED, REJECTED, CANCELLED
    
    confirmed_date           DATE,
    confirmed_time_slot      VARCHAR(20),
    
    rejection_reason         TEXT,
    client_notes             TEXT,
    
    created_at               TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at               TIMESTAMP    NOT NULL DEFAULT now(),
    deleted_at               TIMESTAMP,

    CONSTRAINT fk_visit_request_property FOREIGN KEY (property_id)
        REFERENCES administrative.properties (id),
    CONSTRAINT fk_visit_request_client FOREIGN KEY (client_id)
        REFERENCES administrative.users (id),
    CONSTRAINT fk_visit_request_agent FOREIGN KEY (agent_id)
        REFERENCES administrative.users (id),
    CONSTRAINT chk_visit_request_status CHECK (status IN 
        ('PENDING', 'CONFIRMED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT chk_visit_request_confirmed_requires_slot CHECK 
        ((status = 'CONFIRMED' AND confirmed_date IS NOT NULL AND confirmed_time_slot IS NOT NULL)
         OR status != 'CONFIRMED')
);

CREATE INDEX idx_visit_requests_property ON administrative.property_visit_requests (property_id);
CREATE INDEX idx_visit_requests_client ON administrative.property_visit_requests (client_id);
CREATE INDEX idx_visit_requests_status ON administrative.property_visit_requests (status);
CREATE INDEX idx_visit_requests_dates ON administrative.property_visit_requests (requested_date);


-- Table 2 : Créneaux disponibilités agence par bien
CREATE TABLE administrative.agency_visit_availabilities
(
    id                  UUID         NOT NULL PRIMARY KEY,
    property_id         UUID         NOT NULL UNIQUE,
    agency_id           UUID         NOT NULL,
    
    -- Configuration par jour de semaine (0=Dim, 1=Lun, ..., 6=Sam)
    -- Stocké en JSON string pour simplifier : '{"1": ["10:00-14:00", "14:00-18:00"], "2": ["10:00-14:00"]}'
    time_slots_config   TEXT         NOT NULL,  -- JSON format
    
    -- Dates fermées (vacances, congés)
    blackout_dates      DATE[]       DEFAULT '{}',
    
    -- Limite de visites par créneau
    max_visits_per_slot INTEGER      DEFAULT 3,
    
    is_active           BOOLEAN      DEFAULT true,
    created_at          TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMP,

    CONSTRAINT fk_availability_property FOREIGN KEY (property_id)
        REFERENCES administrative.properties (id),
    CONSTRAINT fk_availability_agency FOREIGN KEY (agency_id)
        REFERENCES administrative.agencies (id)
);

CREATE INDEX idx_availabilities_property ON administrative.agency_visit_availabilities (property_id);
CREATE INDEX idx_availabilities_agency ON administrative.agency_visit_availabilities (agency_id);
CREATE INDEX idx_availabilities_active ON administrative.agency_visit_availabilities (is_active);


-- Table 3 : Cache du nombre de visites par créneau (optimisation requêtes)
CREATE TABLE administrative.visit_slot_occupancy
(
    id                  UUID         NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    availability_id     UUID         NOT NULL,
    visit_date          DATE         NOT NULL,
    time_slot           VARCHAR(20)  NOT NULL,
    
    current_bookings    INTEGER      DEFAULT 0,
    
    created_at          TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT fk_occupancy_availability FOREIGN KEY (availability_id)
        REFERENCES administrative.agency_visit_availabilities (id),
    CONSTRAINT unique_slot_occupancy UNIQUE (availability_id, visit_date, time_slot)
);

CREATE INDEX idx_occupancy_availability ON administrative.visit_slot_occupancy (availability_id);
CREATE INDEX idx_occupancy_date ON administrative.visit_slot_occupancy (visit_date);
