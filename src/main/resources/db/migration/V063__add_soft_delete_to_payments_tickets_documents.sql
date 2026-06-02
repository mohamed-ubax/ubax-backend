-- V063: Add soft-delete (deleted_at) to payments, tickets, property_documents

ALTER TABLE administrative.payments
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE administrative.tickets
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE administrative.property_documents
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITHOUT TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_payments_deleted_at
    ON administrative.payments (deleted_at);

CREATE INDEX IF NOT EXISTS idx_tickets_deleted_at
    ON administrative.tickets (deleted_at);

CREATE INDEX IF NOT EXISTS idx_property_documents_deleted_at
    ON administrative.property_documents (deleted_at);
