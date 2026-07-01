-- V5: Extend Entities for Partner details, Language pairs, Order items and Documents

-- 1. Extend Partner Table
ALTER TABLE partner ADD COLUMN salutation VARCHAR(50);
ALTER TABLE partner ADD COLUMN title VARCHAR(50);
ALTER TABLE partner ADD COLUMN display_name VARCHAR(100);
ALTER TABLE partner ADD COLUMN organization VARCHAR(255);
ALTER TABLE partner ADD COLUMN organization_unit VARCHAR(255);
ALTER TABLE partner ADD COLUMN is_translator BOOLEAN DEFAULT TRUE;
ALTER TABLE partner ADD COLUMN is_interpreter BOOLEAN DEFAULT FALSE;
ALTER TABLE partner ADD COLUMN is_active BOOLEAN DEFAULT TRUE;
ALTER TABLE partner ADD COLUMN is_recommended BOOLEAN DEFAULT FALSE;
ALTER TABLE partner ADD COLUMN classification VARCHAR(50) DEFAULT 'extern';
ALTER TABLE partner ADD COLUMN street VARCHAR(255);
ALTER TABLE partner ADD COLUMN zip VARCHAR(50);
ALTER TABLE partner ADD COLUMN city VARCHAR(100);
ALTER TABLE partner ADD COLUMN country VARCHAR(50);
ALTER TABLE partner ADD COLUMN notes TEXT;

-- 2. Create Partner Language Pair Table
CREATE TABLE partner_language_pair (
    id UUID PRIMARY KEY,
    partner_id UUID NOT NULL REFERENCES partner(id) ON DELETE CASCADE,
    source_language_id UUID REFERENCES language(id) ON DELETE SET NULL,
    target_language_id UUID REFERENCES language(id) ON DELETE SET NULL,
    activity VARCHAR(100) NOT NULL,
    subject_area VARCHAR(255),
    price_per_line DECIMAL(10,2) NOT NULL DEFAULT 0.00
);

-- 3. Extend Translation Order Item Table
ALTER TABLE translation_order_item ADD COLUMN source_language_id UUID REFERENCES language(id) ON DELETE SET NULL;
ALTER TABLE translation_order_item ADD COLUMN target_language_id UUID REFERENCES language(id) ON DELETE SET NULL;
ALTER TABLE translation_order_item ADD COLUMN activity VARCHAR(100);
ALTER TABLE translation_order_item ADD COLUMN assigned_partner_id UUID REFERENCES partner(id) ON DELETE SET NULL;
ALTER TABLE translation_order_item ADD COLUMN status VARCHAR(50) DEFAULT 'CREATED';
ALTER TABLE translation_order_item ADD COLUMN word_count INT;
ALTER TABLE translation_order_item ADD COLUMN page_count DECIMAL(10,2);
ALTER TABLE translation_order_item ADD COLUMN price_per_line DECIMAL(10,2);
ALTER TABLE translation_order_item ADD COLUMN surcharge_or_discount DECIMAL(10,2);

-- 4. Extend Document Table
ALTER TABLE document ADD COLUMN do_not_translate BOOLEAN DEFAULT FALSE;
ALTER TABLE document ADD COLUMN contains_personal_data BOOLEAN DEFAULT FALSE;
