-- V6: Number Ranges, Dunning, and Vendor Invoices & External Costs Schema

-- 1. Number Range Table
CREATE TABLE number_range (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    entity_type VARCHAR(50) NOT NULL,
    prefix VARCHAR(100),
    current_number INT NOT NULL DEFAULT 0,
    increment INT NOT NULL DEFAULT 1,
    padding INT NOT NULL DEFAULT 4,
    year_based BOOLEAN DEFAULT FALSE,
    reset_yearly BOOLEAN DEFAULT FALSE,
    last_year INT,
    separator VARCHAR(10) DEFAULT '-',
    CONSTRAINT uk_tenant_entity UNIQUE (tenant_id, entity_type)
);

-- 2. Dunning Tables
CREATE TABLE dunning_setting (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL UNIQUE REFERENCES tenant(id) ON DELETE CASCADE,
    enabled BOOLEAN DEFAULT TRUE,
    fee_per_level DECIMAL(10,2) DEFAULT 5.00,
    days_overdue_level1 INT DEFAULT 3,
    days_overdue_level2 INT DEFAULT 10,
    days_overdue_level3 INT DEFAULT 20
);

CREATE TABLE dunning_log (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    invoice_id UUID NOT NULL REFERENCES invoice(id) ON DELETE CASCADE,
    level INT NOT NULL,
    fee_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    sent_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    pdf_path VARCHAR(500)
);

-- Add tracking columns to Invoice
ALTER TABLE invoice ADD COLUMN reminder_level INT DEFAULT 0;
ALTER TABLE invoice ADD COLUMN last_reminder_date TIMESTAMP WITH TIME ZONE;

-- 3. Vendor Invoice and External Cost Tables
CREATE TABLE vendor_invoice (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    partner_id UUID NOT NULL REFERENCES partner(id) ON DELETE CASCADE,
    order_id UUID REFERENCES translation_order(id) ON DELETE SET NULL,
    invoice_number VARCHAR(100) NOT NULL,
    reference VARCHAR(255),
    date DATE NOT NULL,
    due_date DATE,
    received_at DATE,
    amount_net DECIMAL(10,2) NOT NULL,
    tax_rate DECIMAL(5,2) DEFAULT 19.00,
    amount_tax DECIMAL(10,2) NOT NULL,
    amount_gross DECIMAL(10,2) NOT NULL,
    paid_amount DECIMAL(10,2) DEFAULT 0.00,
    status VARCHAR(50) DEFAULT 'open',
    notes TEXT
);

CREATE TABLE external_cost (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    order_id UUID REFERENCES translation_order(id) ON DELETE SET NULL,
    description VARCHAR(255) NOT NULL,
    cost_type VARCHAR(100),
    amount DECIMAL(10,2) NOT NULL,
    date DATE NOT NULL,
    supplier VARCHAR(255),
    notes TEXT
);
