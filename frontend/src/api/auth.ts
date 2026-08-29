import { setToken } from '../auth/token';

const API_BASE = '/api/v1/auth';

//AuthResponse din java pentru response din java
export interface AuthResponse {
    accessToken: string;
    user: { id: string; email: string; role: string };
    org: { id: string; name: string };
}
//response catre java, RegisterRequest din /dto
export interface RegisterPayload {
    orgName: string;
    email: string;
    password: string;
}

//payload catre login
export interface LoginPayload {
    email: string;
    password: string;
}

export async function register(payload: RegisterPayload): Promise<AuthResponse> {
    const res = await fetch(`${API_BASE}/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
    });
//200-299
    const data = await res.json();
    if (!res.ok) {
        throw new Error(data.error || 'Eroare la înregistrare.');
    }

    setToken(data.accessToken);
    return data;
}

export async function login(payload: LoginPayload): Promise<AuthResponse> {
    const res = await fetch(`${API_BASE}/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
    });

    const data = await res.json();
    if (!res.ok) {
        throw new Error(data.error || 'Email sau parolă incorectă.');
    }

    setToken(data.accessToken);
    return data;
}
