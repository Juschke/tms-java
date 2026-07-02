-- V7: General tenant settings and text templates

CREATE TABLE tenant_settings (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL UNIQUE REFERENCES tenant(id) ON DELETE CASCADE,
    company_name VARCHAR(255),
    street VARCHAR(255),
    zip VARCHAR(50),
    city VARCHAR(100),
    country VARCHAR(50) DEFAULT 'DE',
    phone VARCHAR(100),
    email VARCHAR(255),
    website VARCHAR(255),
    tax_number VARCHAR(100),
    vat_id VARCHAR(100),
    default_currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    vat_percent DECIMAL(5,2) NOT NULL DEFAULT 19.00,
    default_payment_terms_days INT NOT NULL DEFAULT 14,
    default_quote_validity_days INT NOT NULL DEFAULT 14,
    default_delivery_method VARCHAR(50) NOT NULL DEFAULT 'EMAIL',
    email_sender_name VARCHAR(255),
    email_sender_address VARCHAR(255),
    bank_name VARCHAR(255),
    iban VARCHAR(100),
    bic VARCHAR(100),
    quote_footer TEXT,
    invoice_footer TEXT,
    order_footer TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE TABLE text_template (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    template_type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    subject VARCHAR(255),
    body TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_text_template_tenant ON text_template(tenant_id);
CREATE INDEX idx_text_template_type ON text_template(tenant_id, template_type);
