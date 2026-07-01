-- V3: Pricing, Inquiries and Quotes Schema

-- 1. Languages
CREATE TABLE language (
    id UUID PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

-- Seed basic languages
INSERT INTO language (id, code, name) VALUES 
('a838561d-79e5-4e67-8e65-8b832b84efad', 'de', 'Deutsch'),
('f88c83a7-5110-449e-b9b5-680cb709b1f6', 'en', 'Englisch'),
('c53b2be0-29c8-472e-8395-6b58348b61c9', 'fr', 'Französisch'),
('d12c8b09-cf8a-4d7a-ba92-d961cfb36e92', 'es', 'Spanisch'),
('e3b6e82a-df8a-41ab-bc93-a4e93bb281e0', 'it', 'Italienisch');

-- 2. Service Types
CREATE TABLE service_type (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500)
);

-- Seed basic service types
INSERT INTO service_type (id, name, description) VALUES
('b301f6c4-72ff-4b1f-bc87-d4bb1f6c561a', 'Fachübersetzung', 'Übersetzung von Fachtexten durch qualifizierte Übersetzer'),
('c412f7d5-8300-4b2c-ad98-e5cc2f7d672b', 'Dolmetschen', 'Mündliche Übersetzung bei Verhandlungen, Terminen oder Konferenzen'),
('d523f8e6-9411-4c3d-be09-f6dd3f8e783c', 'Beglaubigung', 'Beglaubigung von Übersetzungen durch beeidigte Übersetzer'),
('e634f9f7-0522-4d4e-cf1a-07ee4f9f894d', 'Apostille / Behördenservice', 'Unterstützung beim Einholen von Apostillen und Überbeglaubigungen');

-- 3. Price Rules
CREATE TABLE price_rule (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    source_language_id UUID REFERENCES language(id) ON DELETE SET NULL,
    target_language_id UUID REFERENCES language(id) ON DELETE SET NULL,
    service_type_id UUID NOT NULL REFERENCES service_type(id) ON DELETE CASCADE,
    rate_per_word DECIMAL(10,4),
    rate_per_page DECIMAL(10,2),
    minimum_fee DECIMAL(10,2),
    express_surcharge_percent DECIMAL(5,2) DEFAULT 0.00,
    certified_surcharge DECIMAL(10,2) DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- 4. Inquiry Table
CREATE TABLE inquiry (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES customer(id) ON DELETE CASCADE,
    contact_person_id UUID REFERENCES contact_person(id) ON DELETE SET NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'RECEIVED',
    source_language_id UUID REFERENCES language(id) ON DELETE SET NULL,
    target_language_id UUID REFERENCES language(id) ON DELETE SET NULL,
    service_type_id UUID REFERENCES service_type(id) ON DELETE SET NULL,
    word_count INT,
    page_count INT,
    is_certified BOOLEAN NOT NULL DEFAULT FALSE,
    is_express BOOLEAN NOT NULL DEFAULT FALSE,
    delivery_method VARCHAR(50) DEFAULT 'EMAIL',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- 5. Quote Table
CREATE TABLE quote (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    inquiry_id UUID UNIQUE REFERENCES inquiry(id) ON DELETE SET NULL,
    customer_id UUID NOT NULL REFERENCES customer(id) ON DELETE CASCADE,
    quote_number VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    net_amount DECIMAL(10,2) NOT NULL,
    vat_percent DECIMAL(5,2) NOT NULL DEFAULT 19.00,
    vat_amount DECIMAL(10,2) NOT NULL,
    gross_amount DECIMAL(10,2) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- 6. Quote Line Table
CREATE TABLE quote_line (
    id UUID PRIMARY KEY,
    quote_id UUID NOT NULL REFERENCES quote(id) ON DELETE CASCADE,
    description VARCHAR(255) NOT NULL,
    quantity DECIMAL(10,2) NOT NULL,
    unit VARCHAR(50) NOT NULL, -- e.g., 'WORDS', 'PAGES', 'HOURS', 'FLAT_RATE'
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL
);
