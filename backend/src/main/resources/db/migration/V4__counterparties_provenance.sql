ALTER TABLE counterparties ADD COLUMN source_system TEXT NULL;
ALTER TABLE counterparties ADD COLUMN external_id TEXT NULL;

ALTER TABLE counterparties ADD CONSTRAINT chk_counterparties_source
    CHECK (source_system IS NULL OR source_system IN ('MANUAL', 'SAFT', 'SMARTBILL', 'FGO', 'TINK', 'SEED'));

CREATE UNIQUE INDEX uq_counterparties_external
    ON counterparties(org_id, source_system, external_id)
    WHERE external_id IS NOT NULL;
