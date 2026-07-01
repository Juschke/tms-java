-- V4: Orders, Partners and Billing Schema

-- 1. Partner Table
CREATE TABLE partner (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    company_name VARCHAR(255),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_at TIMESTAMP WITH TIME ZONE
);
-- 2. Translation Order Table
CREATE TABLE translation_order (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    quote_id UUID REFERENCES quote(id) ON DELETE SET NULL,
    customer_id UUID NOT NULL REFERENCES customer(id) ON DELETE CASCADE,
    order_number VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED', -- CREATED, ASSIGNED, IN_PROGRESS, QA_READY, READY_FOR_DELIVERY, DELIVERED, INVOICED, PAID, ARCHIVED, CANCELLED
    delivery_deadline TIMESTAMP WITH TIME ZONE,
    delivery_method VARCHAR(50) DEFAULT 'EMAIL',  -- EMAIL, POST, PICKUP
    net_amount DECIMAL(10,2) NOT NULL,
    vat_percent DECIMAL(5,2) NOT NULL DEFAULT 19.00,
    vat_amount DECIMAL(10,2) NOT NULL,
    gross_amount DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- 3. Translation Order Item Table
CREATE TABLE translation_order_item (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES translation_order(id) ON DELETE CASCADE,
    description VARCHAR(255) NOT NULL,
    quantity DECIMAL(10,2) NOT NULL,
    unit VARCHAR(50) NOT NULL, -- e.g., 'WORDS', 'PAGES', 'HOURS', 'FLAT_RATE'
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL
);

-- 4. Partner Assignment Table
CREATE TABLE partner_assignment (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES translation_order(id) ON DELETE CASCADE,
    partner_id UUID NOT NULL REFERENCES partner(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'OFFERED', -- OFFERED, ACCEPTED, REJECTED, IN_PROGRESS, SUBMITTED, APPROVED, DISPUTED
    fee DECIMAL(10,2) NOT NULL,
    deadline TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- 5. Invoice Table
CREATE TABLE invoice (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    order_id UUID REFERENCES translation_order(id) ON DELETE SET NULL,
    customer_id UUID NOT NULL REFERENCES customer(id) ON DELETE CASCADE,
    invoice_number VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, ISSUED, PARTIALLY_PAID, PAID, DUNNED, CANCELLED
    net_amount DECIMAL(10,2) NOT NULL,
    vat_percent DECIMAL(5,2) NOT NULL DEFAULT 19.00,
    vat_amount DECIMAL(10,2) NOT NULL,
    gross_amount DECIMAL(10,2) NOT NULL,
    issued_at TIMESTAMP WITH TIME ZONE,
    due_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- 6. Invoice Line Table
CREATE TABLE invoice_line (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoice(id) ON DELETE CASCADE,
    description VARCHAR(255) NOT NULL,
    quantity DECIMAL(10,2) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL
);

-- 7. Payment Table
CREATE TABLE payment (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoice(id) ON DELETE CASCADE,
    amount DECIMAL(10,2) NOT NULL,
    payment_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_method VARCHAR(50) NOT NULL, -- BANK_TRANSFER, CREDIT_CARD, PAYPAL, CASH
    transaction_reference VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);
