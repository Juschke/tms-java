-- V12: Partner-Nummer (fortlaufend generiert ueber den Nummernkreis 'partner')

ALTER TABLE partner ADD COLUMN partner_number VARCHAR(50);

-- Bestehende Partner erhalten eine provisorische Nummer auf Basis ihrer Reihenfolge,
-- damit die Spalte spaeter eindeutig befuellt ist. Praefix 'L' entspricht der
-- Standardkonfiguration des Nummernkreises.
WITH numbered AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY tenant_id ORDER BY id) AS rn
    FROM partner
    WHERE partner_number IS NULL
)
UPDATE partner p
SET partner_number = 'L' || LPAD((70000 + n.rn)::text, 5, '0')
FROM numbered n
WHERE p.id = n.id;

-- Eindeutigkeit je Mandant sicherstellen
CREATE UNIQUE INDEX ux_partner_number_per_tenant
    ON partner (tenant_id, partner_number)
    WHERE partner_number IS NOT NULL;
