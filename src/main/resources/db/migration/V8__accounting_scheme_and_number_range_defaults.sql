-- V8: Accounting scheme settings and default number ranges

ALTER TABLE tenant_settings
    ADD COLUMN IF NOT EXISTS accounting_scheme VARCHAR(10) NOT NULL DEFAULT 'SKR03';

UPDATE tenant_settings
SET accounting_scheme = COALESCE(accounting_scheme, 'SKR03');
