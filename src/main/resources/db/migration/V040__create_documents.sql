-- V040 : Table centrale des documents générés par la plateforme (contrats, factures, reçus)
-- Couvre : CONTRACT, INVOICE, RECEIPT, LEASE, REPORT, STATEMENT, INVENTORY, OTHER
-- Fichiers physiques dans MinIO bucket : documents-generated

CREATE TABLE administrative.documents
(
    id               UUID         NOT NULL PRIMARY KEY,
    ref_id           UUID,
    ref_type         VARCHAR(30),
    doc_type         VARCHAR(30)  NOT NULL,
    title            VARCHAR(255),
    reference_number VARCHAR(100) UNIQUE,
    file_url         VARCHAR(500) NOT NULL,
    file_name        VARCHAR(255),
    file_size        BIGINT,
    status           VARCHAR(20)  NOT NULL,
    error_message    TEXT,
    generated_by     UUID,
    created_at       TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT fk_document_generated_by FOREIGN KEY (generated_by)
        REFERENCES administrative.users (id),
    CONSTRAINT chk_document_doc_type CHECK (doc_type IN
        ('INVOICE', 'RECEIPT', 'CONTRACT', 'LEASE', 'INVENTORY', 'REPORT', 'STATEMENT', 'OTHER')),
    CONSTRAINT chk_document_status CHECK (status IN
        ('PENDING', 'GENERATED', 'SENT', 'FAILED')),
    CONSTRAINT chk_document_ref_type CHECK (ref_type IS NULL OR ref_type IN
        ('PAYMENT', 'CONTRACT', 'TICKET', 'PROPERTY', 'OFFER'))
);

CREATE INDEX idx_documents_ref ON administrative.documents (ref_id, ref_type);
CREATE INDEX idx_documents_status ON administrative.documents (status);
CREATE INDEX idx_documents_generated_by ON administrative.documents (generated_by);
