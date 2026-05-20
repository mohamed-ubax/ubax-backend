-- V056 : Table des contrats de mandat de gestion (agence ↔ bailleur)
-- Séparée de `contracts` car le mandat est un acte administratif inter-professionnel
-- qui n'implique aucun locataire ni bien commercial visible côté client.

CREATE TABLE administrative.management_contracts (
    id                     UUID         NOT NULL,
    agency_id              UUID         NOT NULL,
    owner_id               UUID         NOT NULL,
    created_by_id          UUID,
    reference_number       VARCHAR(100),
    status                 VARCHAR(30)  NOT NULL,
    start_date             DATE         NOT NULL,
    end_date               DATE,
    commission_rate        NUMERIC(5,2),
    special_clauses        TEXT,
    termination_conditions TEXT,
    file_url               TEXT,
    terminated_at          TIMESTAMP,
    termination_reason     TEXT,
    terminated_by_id       UUID,
    created_at             TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at             TIMESTAMP    NOT NULL DEFAULT now(),
    deleted_at             TIMESTAMP,

    CONSTRAINT pk_management_contracts PRIMARY KEY (id),
    CONSTRAINT fk_mgmt_contract_agency        FOREIGN KEY (agency_id)       REFERENCES administrative.agencies(id),
    CONSTRAINT fk_mgmt_contract_owner         FOREIGN KEY (owner_id)        REFERENCES administrative.users(id),
    CONSTRAINT fk_mgmt_contract_created_by    FOREIGN KEY (created_by_id)   REFERENCES administrative.users(id),
    CONSTRAINT fk_mgmt_contract_terminated_by FOREIGN KEY (terminated_by_id) REFERENCES administrative.users(id),
    CONSTRAINT chk_mgmt_contract_status CHECK (
        status IN ('DRAFT','PENDING_SIGNATURE','ACTIVE','TERMINATED','CANCELLED')
    )
);

CREATE INDEX idx_mgmt_contracts_agency  ON administrative.management_contracts(agency_id);
CREATE INDEX idx_mgmt_contracts_owner   ON administrative.management_contracts(owner_id);
CREATE INDEX idx_mgmt_contracts_status  ON administrative.management_contracts(status);
