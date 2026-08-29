//rand din ledger in UI 
export interface LedgerRow {
    id: string;
    tip: 'INVOICE' | 'JOURNAL' | 'PAYMENT';
    data: string; 
    partener: string;
    suma: number;
    moneda: string;
    status: string;
    sursa: string;
    referinta: string | null;
}

// Response de la GET /api/v1/ledger

export interface LedgerListResponse {
    items: LedgerRow[];
    total: number;
    currency: string;
}

//query parameters 
export interface LedgerQueryParams {
    filter?: 'ALL' | 'INVOICES' | 'UNPAID' | 'OVERDUE' | 'JOURNAL' | 'PAYMENTS';
    q?: string;
    from?: string;
    to?: string;
    limit?: number;
    offset?: number;
}
