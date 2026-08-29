import React, { useEffect, useState } from 'react';
import { DashboardCard } from './DashboardCard';
import { getDashboardSummary } from '../api/dashboard';
import type { DashboardSummary } from '../types/dashboard';



const WalletIcon = () => (
    <svg className="dashboard-card__icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
    </svg>
);

const TrendUpIcon = () => (
    <svg className="dashboard-card__icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
    </svg>
);

const TrendDownIcon = () => (
    <svg className="dashboard-card__icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M13 17h8m0 0V9m0 8l-8-8-4 4-6-6" />
    </svg>
);

const DatabaseIcon = () => (
    <svg className="dashboard-card__icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4" />
    </svg>
);

const PlugIcon = () => (
    <svg className="dashboard-card__icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
    </svg>
);

const TaxIcon = () => (
    <svg className="dashboard-card__icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 14l6-6m-5.5.5h.01m4.99 5h.01M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16l3.5-2 3.5 2 3.5-2 3.5 2z" />
    </svg>
);

const CalendarIcon = () => (
    <svg className="dashboard-card__icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
    </svg>
);

const ChartIcon = () => (
    <svg className="dashboard-card__icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
    </svg>
);

const ShieldIcon = () => (
    <svg className="dashboard-card__icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
    </svg>
);



interface DataSource {
    name: string;
    status: 'connected' | 'disconnected';
}


const dataSources: DataSource[] = [
    { name: 'SmartBill', status: 'disconnected' },
    { name: 'Bancă', status: 'disconnected' },
    { name: 'SAF-T', status: 'disconnected' },
];


//numar -> RON
function formatCurrency(value: number, currency: string): string {
    return new Intl.NumberFormat('ro-RO', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    }).format(value) + ' ' + currency;
}

//data range pt display
function formatDateRange(from: string, to: string): string {
    const fromDate = new Date(from);
    const toDate = new Date(to);
    const options: Intl.DateTimeFormatOptions = { day: 'numeric', month: 'short', year: 'numeric' };
    return `${fromDate.toLocaleDateString('ro-RO', options)} – ${toDate.toLocaleDateString('ro-RO', options)}`;
}

//YYYY-MM-DD pt data
function formatDate(dateStr: string): string {
    const d = new Date(dateStr);
    return d.toLocaleDateString('ro-RO', { day: 'numeric', month: 'short', year: 'numeric' });
}

function formatRatio(value: number): string {
    return new Intl.NumberFormat('ro-RO', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    }).format(value);
}

/* ─── Dashboard Component ──────────────────────────────────────── */

export const Dashboard: React.FC = () => {
    const [summary, setSummary] = useState<DashboardSummary | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        let cancelled = false;

        async function fetchData() {
            try {
                setLoading(true);
                setError(null);

                const data = await getDashboardSummary();
                if (!cancelled) {
                    setSummary(data);
                }
            } catch (err) {
                if (!cancelled) {
                    setError(err instanceof Error ? err.message : 'Nu s-au putut încărca datele dashboard-ului.');
                }
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        }

        fetchData();

        return () => {
            cancelled = true;
        };
    }, []);

    // loading ui
    if (loading) {
        return (
            <div className="dashboard__state">
                <span className="spinner" />
                <p className="dashboard__state-text">
                    Se încarcă datele financiare…
                </p>
            </div>
        );
    }

    //error ui
    if (error) {
        return (
            <div className="dashboard__state">
                <p className="dashboard__state-error-title">
                    Nu s-au putut încărca datele dashboard-ului.
                </p>
                <p className="dashboard__state-error-body">
                    Verificați conexiunea la server și încercați din nou.
                </p>
            </div>
        );
    }

    // edge case
    if (!summary) {
        return (
            <div className="dashboard__state">
                <p className="dashboard__state-text">
                    Nu există date disponibile.
                </p>
            </div>
        );
    }

    const currency = summary.currency;
    const hasSources = summary.acoperire.sources.length > 0;
    const taxes = summary.taxes;

    return (
        <div className="dashboard">
            {/* Header */}
            <div className="dashboard__header">
                <h1 className="dashboard__title">Tablou de bord</h1>
                <p className="dashboard__subtitle">Prezentare generală a datelor financiare</p>
            </div>

            {/* Cards Grid */}
            <div className="dashboard__grid">
                {/* Sold disponibil */}
                <DashboardCard
                    title="Sold disponibil"
                    value={formatCurrency(summary.soldDisponibil, currency)}
                    subtitle="Balanță din registru"
                >
                    <div className="dashboard-card__footer">
                        <WalletIcon />
                    </div>
                </DashboardCard>


                {/* Încasări (30 zile) */}
                <DashboardCard
                    title="Încasări (30 zile)"
                    value={formatCurrency(summary.incasari30Zile, currency)}
                    subtitle="Ultimele 30 de zile"
                >
                    <div className="dashboard-card__footer">
                        <TrendUpIcon />
                        <span className="dashboard-card__indicator dashboard-card__indicator--positive">
                            Încasări
                        </span>
                    </div>
                </DashboardCard>

                {/* Plăți (30 zile) */}
                <DashboardCard
                    title="Plăți (30 zile)"
                    value={formatCurrency(summary.plati30Zile, currency)}
                    subtitle="Ultimele 30 de zile"
                >
                    <div className="dashboard-card__footer">
                        <TrendDownIcon />
                        <span className="dashboard-card__indicator dashboard-card__indicator--negative">
                            Plăți
                        </span>
                    </div>
                </DashboardCard>


                {/* Venituri */}
                <DashboardCard
                    title="Venituri (30 zile)"
                    value={formatCurrency(summary.revenue30d, currency)}
                    subtitle="Din facturi emise"
                >
                    <div className="dashboard-card__footer">
                        <TrendUpIcon />
                    </div>
                </DashboardCard>

                {/* Cheltuieli */}
                <DashboardCard
                    title="Cheltuieli (30 zile)"
                    value={formatCurrency(summary.expenses30d, currency)}
                    subtitle="Din facturi primite"
                >
                    <div className="dashboard-card__footer">
                        <TrendDownIcon />
                    </div>
                </DashboardCard>

                {/* Profit */}
                <DashboardCard
                    title="Profit (30 zile)"
                    value={formatCurrency(summary.profit30d, currency)}
                    subtitle="Venituri − Cheltuieli"
                >
                    <div className="dashboard-card__footer">
                        <ChartIcon />
                        <span className={`dashboard-card__indicator ${summary.profit30d >= 0
                            ? 'dashboard-card__indicator--positive'
                            : 'dashboard-card__indicator--negative'}`}
                        >
                            {summary.profit30d >= 0 ? 'Profitabil' : 'Pierdere'}
                        </span>
                    </div>
                </DashboardCard>

                {/* Impozit estimat */}
                <DashboardCard
                    title="Impozit estimat"
                    value={taxes?.selected_tax_amount != null
                        ? formatCurrency(taxes.selected_tax_amount, currency)
                        : 'N/A'}
                    subtitle={taxes?.selected_regime
                        ? `${taxes.selected_regime === 'MICRO' ? 'Micro' : 'Profit'}${taxes.due_date ? ' · scadent ' + formatDate(taxes.due_date) : ''}`
                        : 'Configurează profilul firmei'}
                >
                    <div className="dashboard-card__footer">
                        <TaxIcon />
                    </div>
                </DashboardCard>

                {/* TVA estimat */}
                <DashboardCard
                    title="TVA estimat"
                    value={
                        taxes?.vat_payable_estimate != null
                            ? formatCurrency(taxes.vat_payable_estimate, currency)
                            : 'N/A'
                    }
                    subtitle={
                        taxes?.vat_status === 'DE_PLATA'
                            ? `De plată · scadent ${taxes?.due_date ? formatDate(taxes.due_date) : ''}`
                            : taxes?.vat_status === 'DE_RECUPERAT'
                                ? 'De recuperat'
                                : taxes?.vat_status === 'NEUTRU'
                                    ? 'Neutru'
                                    : 'N/A'
                    }
                >
                    <div className="dashboard-card__footer">
                        <TaxIcon />
                    </div>
                </DashboardCard>

                {/* Acoperire facturi */}
                <DashboardCard
                    title="Acoperire facturi"
                    value={formatRatio(summary.coverageRatio)}
                    subtitle="Sold / Facturi restante"
                >
                    <div className="dashboard-card__footer">
                        <ShieldIcon />
                        <span className={`dashboard-card__indicator ${summary.coverageRatio >= 1
                            ? 'dashboard-card__indicator--positive'
                            : 'dashboard-card__indicator--negative'}`}
                        >
                            {summary.coverageRatio >= 1 ? 'Acoperire suficientă' : 'Atenție'}
                        </span>
                    </div>
                </DashboardCard>


                {/* Scadențe */}
                {taxes && taxes.deadlines && taxes.deadlines.length > 0 && (
                    <DashboardCard title="Scadențe">
                        <div style={{ marginTop: '0.5rem' }}>
                            {taxes.deadlines.map((dl, i) => (
                                <div key={i} className="dashboard-card__deadline-row">
                                    <CalendarIcon />
                                    <div className="dashboard-card__deadline-content">
                                        <p className="dashboard-card__deadline-label">
                                            {dl.label}
                                        </p>
                                        <p className="dashboard-card__deadline-date">
                                            {formatDate(dl.due_date)}
                                        </p>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </DashboardCard>
                )}

                {/* Acoperire date */}
                <DashboardCard title="Acoperire date">
                    <div className="dashboard-card__coverage-row">
                        <DatabaseIcon />
                        <div>
                            <p className="dashboard-card__coverage-label">
                                {hasSources ? summary.acoperire.sources.join(', ') : 'Fără date'}
                            </p>
                            <p className="dashboard-card__coverage-dates">
                                {hasSources
                                    ? formatDateRange(summary.acoperire.from, summary.acoperire.to)
                                    : 'Conectați sursele pentru date reale'}
                            </p>
                        </div>
                    </div>
                </DashboardCard>

                {/* Surse de date */}
                <DashboardCard title="Surse de date">
                    <div style={{ marginTop: '0.5rem' }}>
                        {dataSources.map((source) => (
                            <div key={source.name} className="dashboard-card__source-row">
                                <div className="dashboard-card__source-left">
                                    <PlugIcon />
                                    <span className="dashboard-card__source-name">
                                        {source.name}
                                    </span>
                                </div>
                                <span className={`dashboard-card__source-badge ${source.status === 'connected'
                                    ? 'dashboard-card__source-badge--connected'
                                    : 'dashboard-card__source-badge--disconnected'}`}
                                >
                                    {source.status === 'connected' ? 'Conectat' : 'Neconectat'}
                                </span>
                            </div>
                        ))}
                    </div>
                </DashboardCard>
            </div>

            {/* Tax disclaimer */}
            {taxes?.disclaimer && (
                <p className="dashboard__disclaimer">{taxes.disclaimer}</p>
            )}
        </div>
    );
};
