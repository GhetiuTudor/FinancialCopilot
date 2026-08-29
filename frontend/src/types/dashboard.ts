/**
 * Dashboard data types - matches backend GET /api/v1/dashboard/summary response
 */

export interface DashboardAcoperire {
    from: string;
    to: string;
    sources: string[];
}

export interface DashboardDeadline {
    label: string;
    due_date: string;
}

export interface DashboardTaxEstimates {
    micro_tax_estimate: number | null;
    profit_tax_estimate: number | null;
    vat_payable_estimate: number | null;
    vat_status: string | null;
    selected_regime: string | null;
    selected_tax_amount: number | null;
    due_date: string;
    deadlines: DashboardDeadline[];
    disclaimer: string;
}

export interface DashboardSummary {
    orgId: string;
    currency: string;

    // Financial metrics
    soldDisponibil: number;
    incasari30Zile: number;
    plati30Zile: number;
    netCashflow30d: number;
    revenue30d: number;
    expenses30d: number;
    profit30d: number;
    facturiRestante: number;
    unpaidInvoiceCount: number;
    coverageRatio: number;
    runwayMonths: number;

    // Coverage & taxes
    acoperire: DashboardAcoperire;
    taxes: DashboardTaxEstimates | null;
}
