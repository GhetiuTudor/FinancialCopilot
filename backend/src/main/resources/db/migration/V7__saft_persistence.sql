CREATE TABLE saft_imports (
    id              UUID PRIMARY KEY,
    org_id          UUID NOT NULL,
    file_name       TEXT NULL,
    imported_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    period_start    TEXT NULL,
    period_end      TEXT NULL,
    company_name    TEXT NULL,
    registration_number TEXT NULL,
    total_transactions  INTEGER NULL,
    total_invoices      INTEGER NULL,
    total_payments      INTEGER NULL,
    status          TEXT NOT NULL DEFAULT 'COMPLETED',

    CONSTRAINT chk_saft_imports_status CHECK (status IN ('COMPLETED', 'FAILED'))
);

CREATE INDEX idx_saft_imports_org ON saft_imports(org_id);
CREATE INDEX idx_saft_imports_org_imported ON saft_imports(org_id, imported_at);

CREATE TABLE payments (
    id                  UUID PRIMARY KEY,
    org_id              UUID NOT NULL,
    saft_import_id      UUID NULL,
    payment_external_id TEXT NULL,
    source_document_id  TEXT NULL,
    amount              NUMERIC(18, 2) NOT NULL,
    source_system       TEXT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_payments_source CHECK (source_system IN ('MANUAL', 'SAFT', 'SMARTBILL', 'FGO', 'TINK', 'SEED')),
    CONSTRAINT fk_payments_saft_import FOREIGN KEY (saft_import_id) REFERENCES saft_imports(id)
);

CREATE INDEX idx_payments_org ON payments(org_id);
CREATE INDEX idx_payments_saft_import ON payments(saft_import_id);
CREATE INDEX idx_payments_org_source_doc ON payments(org_id, source_document_id);

CREATE TABLE tax_codes (
    id              UUID PRIMARY KEY,
    org_id          UUID NOT NULL,
    saft_import_id  UUID NULL,
    tax_type        TEXT NOT NULL,
    tax_code        TEXT NOT NULL,
    tax_percentage  NUMERIC(6, 4) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_tax_codes_saft_import FOREIGN KEY (saft_import_id) REFERENCES saft_imports(id)
);

CREATE INDEX idx_tax_codes_org ON tax_codes(org_id);
CREATE INDEX idx_tax_codes_saft_import ON tax_codes(saft_import_id);

ALTER TABLE accounts ADD COLUMN standard_account_id TEXT NULL;
ALTER TABLE accounts ADD COLUMN saft_import_id UUID NULL;

ALTER TABLE accounts ADD CONSTRAINT fk_accounts_saft_import
    FOREIGN KEY (saft_import_id) REFERENCES saft_imports(id);

CREATE INDEX idx_accounts_saft_import ON accounts(saft_import_id);

ALTER TABLE counterparties ADD COLUMN counterparty_type TEXT NULL;
ALTER TABLE counterparties ADD COLUMN saft_import_id UUID NULL;

ALTER TABLE counterparties ADD CONSTRAINT chk_counterparties_type
    CHECK (counterparty_type IS NULL OR counterparty_type IN ('CUSTOMER', 'SUPPLIER'));
ALTER TABLE counterparties ADD CONSTRAINT fk_counterparties_saft_import
    FOREIGN KEY (saft_import_id) REFERENCES saft_imports(id);

CREATE INDEX idx_counterparties_saft_import ON counterparties(saft_import_id);

ALTER TABLE journal_entries ADD COLUMN journal_id TEXT NULL;
ALTER TABLE journal_entries ADD COLUMN saft_import_id UUID NULL;

ALTER TABLE journal_entries ADD CONSTRAINT fk_journal_entries_saft_import
    FOREIGN KEY (saft_import_id) REFERENCES saft_imports(id);

CREATE INDEX idx_journal_entries_saft_import ON journal_entries(saft_import_id);

ALTER TABLE invoices ADD COLUMN saft_import_id UUID NULL;

ALTER TABLE invoices ADD CONSTRAINT fk_invoices_saft_import
    FOREIGN KEY (saft_import_id) REFERENCES saft_imports(id);

CREATE INDEX idx_invoices_saft_import ON invoices(saft_import_id);

ALTER TABLE invoice_lines ADD COLUMN product_id TEXT NULL;
