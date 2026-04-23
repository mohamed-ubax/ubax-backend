-- ============================================================
-- V018 – Module Ticketing
-- Tables : tickets, ticket_messages, ticket_attachments
-- Schema  : administrative
-- ============================================================

-- ── Tickets ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS administrative.tickets
(
    id                       UUID         NOT NULL PRIMARY KEY,
    contract_id              UUID         NOT NULL REFERENCES administrative.contracts (id),
    reporter_id              UUID         NOT NULL REFERENCES administrative.users (id),
    assigned_to              UUID         REFERENCES administrative.users (id),
    category                 VARCHAR(30)  NOT NULL,
    title                    VARCHAR(255) NOT NULL,
    description              TEXT         NOT NULL,
    priority                 VARCHAR(20)  NOT NULL,
    status                   VARCHAR(20)  NOT NULL,
    technician_name          VARCHAR(200),
    technician_phone         VARCHAR(20),
    intervention_scheduled_at TIMESTAMP,
    resolved_at              TIMESTAMP,
    closed_at                TIMESTAMP,
    repair_cost              NUMERIC(12, 2),
    cost_imputed_to          VARCHAR(10),
    resolution_note          TEXT,
    rating                   INTEGER,
    rating_comment           TEXT,
    created_at               TIMESTAMP    NOT NULL,
    updated_at               TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_tickets_contract_id  ON administrative.tickets (contract_id);
CREATE INDEX IF NOT EXISTS idx_tickets_reporter_id  ON administrative.tickets (reporter_id);
CREATE INDEX IF NOT EXISTS idx_tickets_assigned_to  ON administrative.tickets (assigned_to);
CREATE INDEX IF NOT EXISTS idx_tickets_status       ON administrative.tickets (status);
CREATE INDEX IF NOT EXISTS idx_tickets_priority     ON administrative.tickets (priority);

-- ── Messages ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS administrative.ticket_messages
(
    id                   UUID        NOT NULL PRIMARY KEY,
    ticket_id            UUID        NOT NULL REFERENCES administrative.tickets (id),
    sender_id            UUID        NOT NULL REFERENCES administrative.users (id),
    message              TEXT        NOT NULL,
    message_type         VARCHAR(20) NOT NULL,
    attachment_url       VARCHAR(500),
    attachment_name      VARCHAR(255),
    attachment_mime_type VARCHAR(100),
    is_read              BOOLEAN     NOT NULL,
    created_at           TIMESTAMP   NOT NULL,
    updated_at           TIMESTAMP   NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ticket_messages_ticket_id ON administrative.ticket_messages (ticket_id);
CREATE INDEX IF NOT EXISTS idx_ticket_messages_sender_id ON administrative.ticket_messages (sender_id);

-- ── Pièces jointes ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS administrative.ticket_attachments
(
    id              UUID        NOT NULL,
    ticket_id       UUID        NOT NULL REFERENCES administrative.tickets (id),
    uploaded_by     UUID        NOT NULL REFERENCES administrative.users (id),
    file_url        VARCHAR(500) NOT NULL,
    file_name       VARCHAR(255),
    file_size       BIGINT,
    mime_type       VARCHAR(100),
    attachment_type VARCHAR(30) NOT NULL,
    caption         VARCHAR(500),
    created_at      TIMESTAMP   NOT NULL,
    updated_at      TIMESTAMP   NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ticket_attachments_ticket_id ON administrative.ticket_attachments (ticket_id);
