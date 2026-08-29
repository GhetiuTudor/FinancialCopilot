CREATE TABLE integration_connections (
    id UUID PRIMARY KEY,
    org_id UUID NOT NULL,
    provider VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    display_name VARCHAR(128),
    connected_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    last_sync_at TIMESTAMPTZ,
    last_sync_status VARCHAR(32),
    last_error_code VARCHAR(64),
    last_error_message TEXT,
    config JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT chk_integration_connections_provider 
        CHECK (provider IN ('SAFT', 'SMARTBILL', 'FGO', 'TINK')),
    
    CONSTRAINT chk_integration_connections_status 
        CHECK (status IN ('DISCONNECTED', 'CONNECTED', 'ERROR')),
    
    CONSTRAINT chk_integration_connections_last_sync_status 
        CHECK (last_sync_status IS NULL OR last_sync_status IN ('SUCCESS', 'FAILED', 'PARTIAL')),
    
    CONSTRAINT uq_integration_connections_org_provider 
        UNIQUE (org_id, provider)
);

CREATE INDEX idx_integration_connections_org ON integration_connections(org_id);
CREATE INDEX idx_integration_connections_provider ON integration_connections(provider);
CREATE INDEX idx_integration_connections_status ON integration_connections(status);

CREATE TABLE integration_sync_runs (
    id UUID PRIMARY KEY,
    org_id UUID NOT NULL,
    provider VARCHAR(32) NOT NULL,
    connection_id UUID,
    trigger VARCHAR(32) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL,
    stats JSONB NOT NULL DEFAULT '{}'::jsonb,
    error_code VARCHAR(64),
    error_message TEXT,
    
    CONSTRAINT chk_integration_sync_runs_provider 
        CHECK (provider IN ('SAFT', 'SMARTBILL', 'FGO', 'TINK')),
    
    CONSTRAINT chk_integration_sync_runs_trigger 
        CHECK (trigger IN ('MANUAL', 'SCHEDULED')),
    
    CONSTRAINT chk_integration_sync_runs_status 
        CHECK (status IN ('RUNNING', 'SUCCESS', 'FAILED', 'PARTIAL')),
    
    CONSTRAINT fk_integration_sync_runs_connection 
        FOREIGN KEY (connection_id) REFERENCES integration_connections(id) ON DELETE SET NULL
);

CREATE INDEX idx_integration_sync_runs_org_provider_started 
    ON integration_sync_runs(org_id, provider, started_at DESC);
CREATE INDEX idx_integration_sync_runs_status ON integration_sync_runs(status);
