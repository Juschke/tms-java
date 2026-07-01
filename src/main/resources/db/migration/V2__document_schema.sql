-- V2: Document Management Schema

CREATE TABLE document (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    customer_id UUID REFERENCES customer(id) ON DELETE SET NULL,
    associated_entity_type VARCHAR(50), -- e.g., 'INQUIRY', 'ORDER', 'PARTNER'
    associated_entity_id UUID,        -- ID of the related Inquiry, Order, etc.
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL,    -- e.g., 'SOURCE_FILE', 'TRANSLATION', 'INVOICE', 'QUOTE', 'SUPPORTING'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE document_version (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100),
    file_hash VARCHAR(64),
    storage_path VARCHAR(512) NOT NULL, -- Logical path in filesystem or S3 key
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

CREATE UNIQUE INDEX idx_doc_version_unique ON document_version(document_id, version_number);
