import { apiFetch } from './client';

const API_BASE = '/api/v1';

export interface SaftImportResult {
    status: string;
    importId?: string;
    stats?: {
        accounts: number;
        customers: number;
        suppliers: number;
        journal_entries: number;
        invoices: number;
        payments: number;
    };
    period_from?: string;
    period_to?: string;
    error?: string;
}

//upload saft 
export async function uploadSaft(file: File): Promise<SaftImportResult> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await apiFetch(`${API_BASE}/integrations/SAFT/import`, {
        method: 'POST',
        body: formData,
    });

    const data: SaftImportResult = await response.json();

    if (!response.ok && !data.error) {
        throw new Error('Eroare la importul fișierului SAF-T.');
    }

    return data;
}
