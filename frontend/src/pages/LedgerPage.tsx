import React, { useState, useEffect, useCallback, useRef } from 'react';
import { getLedger } from '../api/ledger';
import { apiFetch } from '../api/client';
import type { LedgerRow, LedgerQueryParams } from '../types/ledger';
import './ledger.css';

type FilterType = 'ALL' | 'INVOICES' | 'UNPAID' | 'OVERDUE' | 'JOURNAL' | 'PAYMENTS';

const filters: { key: FilterType; label: string }[] = [
    { key: 'ALL', label: 'Toate' },
    { key: 'INVOICES', label: 'Facturi' },
    { key: 'UNPAID', label: 'Neplătite' },
    { key: 'OVERDUE', label: 'Restante' },
    { key: 'JOURNAL', label: 'Tranzacții contabile' },
    { key: 'PAYMENTS', label: 'Plăți' },
];

function formatCurrency(value: number, currency: string): string {
    const formatted = new Intl.NumberFormat('ro-RO', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    }).format(Math.abs(value));
    return `${value < 0 ? '-' : ''}${formatted} ${currency}`;
}

function formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleDateString('ro-RO', { day: 'numeric', month: 'short', year: 'numeric' });
}

function getStatusBadge(status: string) {
    switch (status) {
        case 'PAID':
            return <span className="badge badge-neutral">Plătită</span>;
        case 'ISSUED':
            return <span className="badge badge-neutral">Emisă</span>;
        case 'DRAFT':
            return <span className="badge badge-neutral">Ciornă</span>;
        case 'PARTIALLY_PAID':
            return <span className="badge badge-warning">Parțial plătită</span>;
        case 'UNPAID':
        case 'CANCELLED':
            return <span className="badge badge-warning">Neplătită</span>;
        case 'OVERDUE':
            return <span className="badge badge-error">Restantă</span>;
        case 'BOOKED':
            return <span className="badge badge-neutral">Înregistrată</span>;
        default:
            return <span className="badge badge-neutral">{status}</span>;
    }
}

function getTipLabel(tip: string): string {
    switch (tip) {
        case 'INVOICE': return 'Factură';
        case 'JOURNAL': return 'Tranzacție';
        case 'PAYMENT': return 'Plată';
        default: return tip;
    }
}

export const LedgerPage: React.FC = () => {
    const [activeFilter, setActiveFilter] = useState<FilterType>('ALL');
    const [searchQuery, setSearchQuery] = useState('');
    const [items, setItems] = useState<LedgerRow[]>([]);
    const [total, setTotal] = useState(0);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [clearing, setClearing] = useState(false);

    // Debounce timer ref
    const debounceRef = useRef<number | null>(null);

    // Fetch data function
    const fetchData = useCallback(async (params: LedgerQueryParams) => {
        setLoading(true);
        setError(null);
        try {
            const response = await getLedger(params);
            setItems(response.items);
            setTotal(response.total);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Nu s-au putut încărca datele.');
            setItems([]);
            setTotal(0);
        } finally {
            setLoading(false);
        }
    }, []);

    // Fetch on mount and filter change
    useEffect(() => {
        fetchData({ filter: activeFilter, q: searchQuery || undefined, limit: 100 });
    }, [activeFilter, fetchData]);

    
    useEffect(() => {
        if (debounceRef.current) {
            clearTimeout(debounceRef.current);
        }

        debounceRef.current = window.setTimeout(() => {
            fetchData({ filter: activeFilter, q: searchQuery || undefined, limit: 100 });
        }, 300);

        return () => {
            if (debounceRef.current) {
                clearTimeout(debounceRef.current);
            }
        };
    }, [searchQuery, activeFilter, fetchData]);

    const handleClearDatabase = async () => {
        if (!window.confirm('Ești sigur că vrei să ștergi toate datele? Acțiunea este ireversibilă.')) return;
        setClearing(true);
        try {
            await apiFetch('/api/v1/ledger/clear', { method: 'DELETE' });
            await fetchData({ filter: activeFilter, q: searchQuery || undefined, limit: 100 });
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Eroare la ștergerea datelor.');
        } finally {
            setClearing(false);
        }
    };

    return (
        <div className="ledger">
            {/* Header */}
            <div className="ledger__header">
                <h1 className="ledger__title">Registru (Ledger)</h1>
                <p className="ledger__subtitle">Vizualizează tranzacții și facturi</p>
            </div>

            {/* Controls row: Filters + Clear button */}
            <div className="ledger__controls">
                {/* Filter pills */}
                <div className="ledger__filter-group">
                    {filters.map((filter) => (
                        <button
                            key={filter.key}
                            onClick={() => setActiveFilter(filter.key)}
                            className={`filter-pill ${activeFilter === filter.key ? 'active' : ''}`}
                        >
                            {filter.label}
                        </button>
                    ))}
                </div>

                {/* Clear database button */}
                <button
                    onClick={handleClearDatabase}
                    disabled={clearing}
                    className="ledger__clear-btn"
                >
                    {clearing ? 'Se șterg…' : 'Golește baza de date'}
                </button>

                <div className="ledger__spacer" />

                {/* Search */}
                <div className="ledger__search-wrap">
                    <svg
                        className="ledger__search-icon"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                    >
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                            d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                    </svg>
                    <input
                        type="text"
                        placeholder="Caută după nume, sumă, descriere…"
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        className="ledger__search"
                    />
                </div>

                {/* Period button */}
                <button className="ledger__period-btn">
                    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                            d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                    </svg>
                    Perioada
                </button>

                {/* Total count */}
                {!loading && (
                    <span className="ledger__count">{total} rezultate</span>
                )}
            </div>

            {/* Table */}
            <div className="ledger__table-wrap">
                {/* Table Header */}
                <div className="ledger__table-head">
                    <div className="ledger__table-head-cell">Data</div>
                    <div className="ledger__table-head-cell">Tip</div>
                    <div className="ledger__table-head-cell">Partener</div>
                    <div className="ledger__table-head-cell ledger__table-head-cell--right">Suma</div>
                    <div className="ledger__table-head-cell ledger__table-head-cell--center">Status</div>
                    <div className="ledger__table-head-cell">Sursă</div>
                </div>

                {/* Loading State */}
                {loading && (
                    <div className="ledger__state">
                        <div className="ledger__state-loading">
                            <span className="spinner" />
                            <span>Se încarcă datele...</span>
                        </div>
                    </div>
                )}

                {/* Error State */}
                {!loading && error && (
                    <div className="ledger__state">
                        <p className="ledger__state-error">{error}</p>
                        <button
                            onClick={() => fetchData({ filter: activeFilter, q: searchQuery || undefined, limit: 100 })}
                            className="btn-secondary"
                        >
                            Încearcă din nou
                        </button>
                    </div>
                )}

                {/* Empty State */}
                {!loading && !error && items.length === 0 && (
                    <div className="ledger__state">
                        <p className="ledger__state-empty">Nu s-au găsit rezultate</p>
                    </div>
                )}

                {/* Table Body */}
                {!loading && !error && items.length > 0 && (
                    <div className="ledger__table-body">
                        {items.map((entry) => (
                            <div key={entry.id} className="ledger__table-row">
                                <div className="ledger__cell-date">{formatDate(entry.data)}</div>
                                <div className="ledger__cell-type">{getTipLabel(entry.tip)}</div>
                                <div className="ledger__cell-partner">{entry.partener}</div>
                                <div className={`ledger__cell-amount ${entry.suma < 0 ? 'ledger__cell-amount--negative' : ''}`}>
                                    {formatCurrency(entry.suma, entry.moneda)}
                                </div>
                                <div className="ledger__cell-status">
                                    {getStatusBadge(entry.status)}
                                </div>
                                <div className="ledger__cell-source">{entry.sursa}</div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
};
