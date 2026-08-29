CREATE TABLE chat_threads (
    id UUID PRIMARY KEY,
    org_id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,
    title TEXT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_threads_org_updated ON chat_threads(org_id, updated_at DESC);

CREATE TABLE chat_messages (
    id UUID PRIMARY KEY,
    thread_id UUID NOT NULL,
    org_id UUID NOT NULL,
    role TEXT NOT NULL,
    content_text TEXT NOT NULL,
    content_structured JSONB NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT chk_chat_messages_role CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM')),
    
    CONSTRAINT fk_chat_messages_thread 
        FOREIGN KEY (thread_id) 
        REFERENCES chat_threads(id) 
        ON DELETE CASCADE
);

CREATE INDEX idx_chat_messages_thread_created ON chat_messages(thread_id, created_at);

CREATE TABLE ai_response_meta (
    id UUID PRIMARY KEY,
    org_id UUID NOT NULL,
    chat_message_id UUID NOT NULL UNIQUE,
    request_id UUID NOT NULL,
    as_of TIMESTAMP WITH TIME ZONE NOT NULL,
    model TEXT NOT NULL,
    context_hash TEXT NOT NULL,
    evidence JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_ai_response_meta_message 
        FOREIGN KEY (chat_message_id) 
        REFERENCES chat_messages(id) 
        ON DELETE CASCADE
);

CREATE INDEX idx_ai_response_meta_org_created ON ai_response_meta(org_id, created_at DESC);
