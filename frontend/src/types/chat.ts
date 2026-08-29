export interface Message {
    id: string;
    thread_id: string;
    role: 'USER' | 'ASSISTANT';
    content: string;
    created_at: string;
}

export interface Thread {
    thread_id: string;
    messages: Message[];
    created_at?: string;
}

export interface CreateThreadResponse {
    thread_id: string;
}

export interface SendMessageRequest {
    content: string;
}

export interface SendMessageResponse {
    id: string;
    thread_id: string;
    role: 'ASSISTANT';
    content: string;
    created_at: string;
}
