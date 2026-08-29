import { apiFetch } from './client';

const API_BASE = '/api/v1';

export interface CompanyProfile {
    entity_type: string;
    tax_regime: string;
    employees_count: number;
    vat_registered: boolean;
    vat_cash_accounting: boolean;
    vat_period: string | null;
    estimated_annual_revenue: number | null;
    estimated_annual_expenses: number | null;
    norm_value: number | null;
    county: string | null;
    city: string | null;
    activity_start_date: string | null;
}


export async function getProfile(): Promise<CompanyProfile> {
    const response = await apiFetch(`${API_BASE}/profile`);

    if (!response.ok) {
        throw new Error('Nu s-a putut încărca profilul firmei.');
    }

    return response.json();
}


export async function updateProfile(data: Partial<CompanyProfile>): Promise<CompanyProfile> {
    const response = await apiFetch(`${API_BASE}/profile`, {
        method: 'PUT',
        body: JSON.stringify(data),
    });

    if (!response.ok) {
        throw new Error('Nu s-a putut salva profilul firmei.');
    }

    return response.json();
}
