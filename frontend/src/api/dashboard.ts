import type { DashboardSummary } from '../types/dashboard';
import { apiFetch } from './client';

const API_BASE = '/api/v1';

//fetch dashboard summary din sevice
export async function getDashboardSummary(): Promise<DashboardSummary> {
    const response = await apiFetch(`${API_BASE}/dashboard/summary`);

    if (!response.ok) {
        throw new Error('Nu s-au putut încărca datele dashboard-ului.');
    }

    return response.json();
}
