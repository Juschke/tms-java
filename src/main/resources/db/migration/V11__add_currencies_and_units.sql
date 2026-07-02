CREATE TABLE IF NOT EXISTS currency (
    id UUID PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    symbol VARCHAR(10),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS unit (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Initiale Währungen
INSERT INTO currency (id, code, name, symbol) VALUES (gen_random_uuid(), 'EUR', 'Euro', '€') ON CONFLICT (code) DO NOTHING;
INSERT INTO currency (id, code, name, symbol) VALUES (gen_random_uuid(), 'USD', 'US Dollar', '$') ON CONFLICT (code) DO NOTHING;
INSERT INTO currency (id, code, name, symbol) VALUES (gen_random_uuid(), 'CHF', 'Schweizer Franken', 'CHF') ON CONFLICT (code) DO NOTHING;
INSERT INTO currency (id, code, name, symbol) VALUES (gen_random_uuid(), 'GBP', 'Britisches Pfund', '£') ON CONFLICT (code) DO NOTHING;

-- Initiale Einheiten
INSERT INTO unit (id, code, name) VALUES (gen_random_uuid(), 'WORDS', 'Wörter') ON CONFLICT (code) DO NOTHING;
INSERT INTO unit (id, code, name) VALUES (gen_random_uuid(), 'LINES', 'Normzeilen') ON CONFLICT (code) DO NOTHING;
INSERT INTO unit (id, code, name) VALUES (gen_random_uuid(), 'PAGES', 'Normseiten') ON CONFLICT (code) DO NOTHING;
INSERT INTO unit (id, code, name) VALUES (gen_random_uuid(), 'HOURS', 'Stunden') ON CONFLICT (code) DO NOTHING;
INSERT INTO unit (id, code, name) VALUES (gen_random_uuid(), 'FLAT_RATE', 'Pauschal') ON CONFLICT (code) DO NOTHING;
