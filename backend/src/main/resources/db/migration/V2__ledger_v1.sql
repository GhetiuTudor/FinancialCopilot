CREATE TABLE counterparties (
    id UUID PRIMARY KEY,
    org_id UUID NOT NULL,
    name TEXT NOT NULL,
    tax_id TEXT NULL,
    country_code TEXT NOT NULL DEFAULT 'RO',
    email TEXT NULL,
    phone TEXT NULL,
    address TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_counterparties_org_name ON counterparties(org_id, name);
CREATE INDEX idx_counterparties_org_tax_id ON counterparties(org_id, tax_id);

CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    org_id UUID NOT NULL,
    code TEXT NOT NULL,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'RON',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT chk_accounts_type CHECK (type IN ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE')),
    CONSTRAINT uq_accounts_org_code UNIQUE (org_id, code)
);

CREATE INDEX idx_accounts_org_type ON accounts(org_id, type);

CREATE TABLE journal_entries (
    id UUID PRIMARY KEY,
    org_id UUID NOT NULL,
    entry_date DATE NOT NULL,
    description TEXT NULL,
    source_system TEXT NOT NULL,
    external_id TEXT NULL,
    source_ref JSONB NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT chk_journal_entries_source CHECK (source_system IN ('MANUAL', 'SAFT', 'SMARTBILL', 'FGO', 'TINK', 'SEED'))
);

CREATE UNIQUE INDEX uq_journal_entries_external 
    ON journal_entries(org_id, source_system, external_id) 
    WHERE external_id IS NOT NULL;

CREATE INDEX idx_journal_entries_org_date ON journal_entries(org_id, entry_date);

CREATE TABLE journal_lines (
    id UUID PRIMARY KEY,
    org_id UUID NOT NULL,
    entry_id UUID NOT NULL,
    account_id UUID NOT NULL,
    counterparty_id UUID NULL,
    description TEXT NULL,
    debit NUMERIC(18, 2) NOT NULL DEFAULT 0,
    credit NUMERIC(18, 2) NOT NULL DEFAULT 0,
    currency CHAR(3) NOT NULL DEFAULT 'RON',
    fx_rate NUMERIC(18, 8) NULL,
    amount_original NUMERIC(18, 2) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT chk_journal_lines_non_negative CHECK (debit >= 0 AND credit >= 0),
    CONSTRAINT chk_journal_lines_mutex CHECK (NOT (debit > 0 AND credit > 0)),
    CONSTRAINT chk_journal_lines_has_amount CHECK (debit > 0 OR credit > 0),
    
    CONSTRAINT fk_journal_lines_entry 
        FOREIGN KEY (entry_id) REFERENCES journal_entries(id) ON DELETE CASCADE,
    CONSTRAINT fk_journal_lines_account 
        FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT fk_journal_lines_counterparty 
        FOREIGN KEY (counterparty_id) REFERENCES counterparties(id)
);

CREATE INDEX idx_journal_lines_entry ON journal_lines(entry_id);
CREATE INDEX idx_journal_lines_org_account ON journal_lines(org_id, account_id);
CREATE INDEX idx_journal_lines_org_counterparty ON journal_lines(org_id, counterparty_id);

CREATE TABLE bank_connections (
    id UUID PRIMARY KEY,
    org_id UUID NOT NULL,
    provider TEXT NOT NULL,
    status TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT chk_bank_connections_status CHECK (status IN ('ACTIVE', 'REVOKED', 'ERROR'))
);

CREATE INDEX idx_bank_connections_org_provider ON bank_connections(org_id, provider);

CREATE TABLE bank_accounts (
    id UUID PRIMARY KEY,
    org_id UUID NOT NULL,
    connection_id UUID NULL,
    bank_name TEXT NULL,
    iban TEXT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'RON',
    display_name TEXT NULL,
    external_id TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT fk_bank_accounts_connection 
        FOREIGN KEY (connection_id) REFERENCES bank_connections(id)
);

CREATE INDEX idx_bank_accounts_org ON bank_accounts(org_id);
CREATE INDEX idx_bank_accounts_org_iban ON bank_accounts(org_id, iban);
CREATE INDEX idx_bank_accounts_org_external ON bank_accounts(org_id, external_id);

CREATE TABLE bank_transactions (
    id UUID PRIMARY KEY,
    org_id UUID NOT NULL,
    account_id UUID NOT NULL,
    booking_date DATE NOT NULL,
    value_date DATE NULL,
    amount NUMERIC(18, 2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'RON',
    description TEXT NULL,
    counterparty_name TEXT NULL,
    reference TEXT NULL,
    category TEXT NULL,
    source_system TEXT NOT NULL DEFAULT 'TINK',
    external_id TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT chk_bank_transactions_source CHECK (source_system IN ('TINK', 'SEED')),
    
    CONSTRAINT fk_bank_transactions_account 
        FOREIGN KEY (account_id) REFERENCES bank_accounts(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_bank_transactions_external 
    ON bank_transactions(org_id, account_id, source_system, external_id) 
    WHERE external_id IS NOT NULL;

CREATE INDEX idx_bank_transactions_org_date ON bank_transactions(org_id, booking_date);
CREATE INDEX idx_bank_transactions_account_date ON bank_transactions(account_id, booking_date);

CREATE TABLE invoices (
    id UUID PRIMARY KEY,
    org_id UUID NOT NULL,
    counterparty_id UUID NOT NULL,
    direction TEXT NOT NULL,
    invoice_number TEXT NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NULL,
    currency CHAR(3) NOT NULL DEFAULT 'RON',
    subtotal NUMERIC(18, 2) NOT NULL,
    vat_total NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total NUMERIC(18, 2) NOT NULL,
    status TEXT NOT NULL,
    source_system TEXT NOT NULL,
    external_id TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT chk_invoices_direction CHECK (direction IN ('SALES', 'PURCHASE')),
    CONSTRAINT chk_invoices_status CHECK (status IN ('DRAFT', 'ISSUED', 'PAID', 'PARTIALLY_PAID', 'OVERDUE', 'CANCELLED')),
    CONSTRAINT chk_invoices_source CHECK (source_system IN ('SAFT', 'SMARTBILL', 'FGO', 'SEED', 'MANUAL')),
    
    CONSTRAINT fk_invoices_counterparty 
        FOREIGN KEY (counterparty_id) REFERENCES counterparties(id)
);

CREATE UNIQUE INDEX uq_invoices_external 
    ON invoices(org_id, source_system, external_id) 
    WHERE external_id IS NOT NULL;

CREATE INDEX idx_invoices_org_issue_date ON invoices(org_id, issue_date);
CREATE INDEX idx_invoices_org_due_date ON invoices(org_id, due_date);
CREATE INDEX idx_invoices_org_status ON invoices(org_id, status);

CREATE TABLE invoice_lines (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL,
    description TEXT NOT NULL,
    quantity NUMERIC(18, 4) NOT NULL DEFAULT 1,
    unit_price NUMERIC(18, 4) NOT NULL,
    vat_rate NUMERIC(6, 4) NOT NULL DEFAULT 0,
    line_total NUMERIC(18, 2) NOT NULL,
    account_hint TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT fk_invoice_lines_invoice 
        FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);

CREATE INDEX idx_invoice_lines_invoice ON invoice_lines(invoice_id);

CREATE TABLE matches (
    id UUID PRIMARY KEY,
    org_id UUID NOT NULL,
    bank_transaction_id UUID NOT NULL,
    invoice_id UUID NULL,
    journal_entry_id UUID NULL,
    match_type TEXT NOT NULL,
    confidence NUMERIC(4, 3) NOT NULL,
    status TEXT NOT NULL,
    explanation TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT chk_matches_type CHECK (match_type IN ('INVOICE_PAYMENT', 'EXPENSE', 'TRANSFER', 'OTHER')),
    CONSTRAINT chk_matches_status CHECK (status IN ('SUGGESTED', 'CONFIRMED', 'REJECTED')),
    CONSTRAINT chk_matches_has_target CHECK (invoice_id IS NOT NULL OR journal_entry_id IS NOT NULL),
    
    CONSTRAINT fk_matches_bank_transaction 
        FOREIGN KEY (bank_transaction_id) REFERENCES bank_transactions(id) ON DELETE CASCADE,
    CONSTRAINT fk_matches_invoice 
        FOREIGN KEY (invoice_id) REFERENCES invoices(id),
    CONSTRAINT fk_matches_journal_entry 
        FOREIGN KEY (journal_entry_id) REFERENCES journal_entries(id)
);

CREATE INDEX idx_matches_org_status ON matches(org_id, status);
CREATE INDEX idx_matches_org_bank_transaction ON matches(org_id, bank_transaction_id);
