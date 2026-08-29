CREATE TABLE company_profiles (
    id                         UUID         PRIMARY KEY,
    org_id                     UUID         NOT NULL UNIQUE REFERENCES organizations(id),
    entity_type                TEXT         NOT NULL DEFAULT 'SRL',
    tax_regime                 TEXT         NOT NULL DEFAULT 'MICRO',
    employees_count            INTEGER      NOT NULL DEFAULT 0,
    vat_registered             BOOLEAN      NOT NULL DEFAULT false,
    vat_cash_accounting        BOOLEAN      NOT NULL DEFAULT false,
    vat_period                 TEXT         NULL,
    estimated_annual_revenue   NUMERIC(15,2) NULL,
    estimated_annual_expenses  NUMERIC(15,2) NULL,
    norm_value                 NUMERIC(15,2) NULL,
    county                     TEXT         NULL,
    city                       TEXT         NULL,
    activity_start_date        DATE         NULL,
    created_at                 TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                 TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_company_profiles_org_id ON company_profiles(org_id);
