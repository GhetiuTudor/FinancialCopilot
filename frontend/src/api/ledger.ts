import type { LedgerListResponse, LedgerQueryParams } from '../types/ledger';
import { apiFetch } from './client';

const API_BASE = '/api/v1';

//fetch din ledger
export async function getLedger(params: LedgerQueryParams = {}): Promise<LedgerListResponse> {
    //browser api pt query 
    //construit automat 
    const searchParams = new URLSearchParams();

    if (params.filter) {
        searchParams.set('filter', params.filter);
    }
    if (params.q) {
        searchParams.set('q', params.q);
    }
    if (params.from) {
        searchParams.set('from', params.from);
    }
    if (params.to) {
        searchParams.set('to', params.to);
    }


    if (params.limit !== undefined) {
        searchParams.set('limit', params.limit.toString());
    }
    if (params.offset !== undefined) {
        searchParams.set('offset', params.offset.toString());
    }

    const queryString = searchParams.toString();
    const url = `${API_BASE}/ledger${queryString ? '?' + queryString : ''}`;

    const response = await apiFetch(url);

    if (!response.ok) {
        throw new Error(`Eroare la încărcarea registrului: ${response.status}`);
    }

    return response.json();
}
