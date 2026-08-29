import type { CreateThreadResponse, SendMessageRequest, SendMessageResponse, Thread } from '../types/chat';
import { apiFetch } from './client';

const API_BASE = '/api/v1/chat';

//thread nou 
export async function createThread(): Promise<CreateThreadResponse> {
    const response = await apiFetch(`${API_BASE}/threads`, {
        method: 'POST',
    });

    if (!response.ok) {
        throw new Error('Nu s-a putut crea conversația. Vă rugăm încercați din nou.');
    }

    return response.json();
}

//get thread by id (cu mesaje in el)
export async function getThread(threadId: string): Promise<Thread> {
    const response = await apiFetch(`${API_BASE}/threads/${threadId}`);

    if (!response.ok) {
        throw new Error('Nu s-a putut încărca conversația. Vă rugăm încercați din nou.');
    }

    return response.json();
}

//trimite mesaj + primeste mesaj AI 
export async function sendMessage(threadId: string,content: string): Promise<SendMessageResponse> {
    const body: SendMessageRequest = { content };

    const response = await apiFetch(`${API_BASE}/threads/${threadId}/messages`, {
        method: 'POST',
        body: JSON.stringify(body),
    });

    if (!response.ok) {
        throw new Error('Nu s-a putut trimite mesajul. Vă rugăm încercați din nou.');
    }

    return response.json();
}
