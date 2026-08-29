import React, { useState, useRef, useEffect } from 'react';
import { uploadSaft, type SaftImportResult } from '../api/saft';
import { apiFetch } from '../api/client';
import './integrations.css';


interface Integration {
    id: string;
    name: string;
    description: string;
    status: 'connected' | 'disconnected' | 'error';
    icon: React.ReactNode;
}


const SmartBillIcon = () => (
    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
            d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
    </svg>
);

const FGOIcon = () => (
    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
            d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
    </svg>
);

const BankIcon = () => (
    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
            d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
    </svg>
);

const SAFTIcon = () => (
    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
            d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
    </svg>
);

//hardcoded, trebuie luate din db prin request la java
const otherIntegrations: Integration[] = [
    {
        id: 'smartbill',
        name: 'SmartBill',
        description: 'Sincronizează facturile emise și primite din SmartBill.',
        status: 'disconnected',
        icon: <SmartBillIcon />,
    },
    {
        id: 'fgo',
        name: 'FGO',
        description: 'Conectează-te la platforma FGO pentru raportări fiscale.',
        status: 'disconnected',
        icon: <FGOIcon />,
    },
    {
        id: 'bank',
        name: 'Bancă (PSD2 / Tink)',
        description: 'Importă tranzacțiile bancare automat prin Open Banking.',
        status: 'disconnected',
        icon: <BankIcon />,
    },
];

//icon pt status 
function getStatusBadge(status: Integration['status']) {
    switch (status) {
        case 'connected':
            return <span className="badge badge-success">Conectat</span>;
        case 'disconnected':
            return <span className="badge badge-neutral">Neconectat</span>;
        case 'error':
            return <span className="badge badge-error">Eroare</span>;
    }
}

export const IntegrationsPage: React.FC = () => {
    const [integrations, setIntegrations] = useState(otherIntegrations);

    const [saftFile, setSaftFile] = useState<File | null>(null);
    const [saftUploading, setSaftUploading] = useState(false);
    const [saftResult, setSaftResult] = useState<SaftImportResult | null>(null);
    const [saftError, setSaftError] = useState<string | null>(null);
    const [saftStatus, setSaftStatus] = useState<'disconnected' | 'connected' | 'error'>('disconnected');
    const fileInputRef = useRef<HTMLInputElement>(null);

    // badge update pentru saft connection 
    useEffect(() => {
        apiFetch('/api/v1/integrations').then(res => res.json())
            .then((connections: Array<{ provider: string; status: string; lastSyncStatus?: string }>) => {
                const saftConn = connections.find(c => c.provider === 'SAFT');
                if (saftConn) {
                    setSaftStatus(
                        saftConn.status === 'CONNECTED' ? 'connected' : saftConn.status === 'ERROR' ? 'error' : 'disconnected'
                    );
                }
            })
            //sa nu arate error in consola 
            .catch(() => { });
    }, []);

    //cand se incarca un file nou in input
    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        //null daca userul da cancel 
        const file = e.target.files?.[0] || null;
        setSaftFile(file);
        setSaftResult(null);
        setSaftError(null);
    };

    const handleUpload = async () => {
        if (!saftFile) return;

        setSaftUploading(true);
        setSaftResult(null);
        setSaftError(null);

        try {
            const result = await uploadSaft(saftFile);
            if (result.status === 'SUCCESS') {
                setSaftResult(result);
                setSaftStatus('connected');
                setSaftError(null);
            } else {
                setSaftError(result.error || 'Import eșuat.');
                setSaftStatus('error');

            }
        } catch (err: unknown) {
            const message = err instanceof Error ? err.message : 'Eroare la importul fișierului.';
            setSaftError(message);
            setSaftStatus('error');
        } finally {
            setSaftUploading(false);
            setSaftFile(null);
            if (fileInputRef.current) {
                fileInputRef.current.value = '';
            }
        }
    };

    // mock pt alte integrari 
    const toggleConnection = (id: string) => {
        setIntegrations((prev) => prev.map((integration) => integration.id === id ? {
            ...integration,
            status: integration.status === 'connected' ? 'disconnected' : 'connected',
        } : integration
        )
        );
    };

    return (
        <div className="integrations">
            {/* Header */}
            <div className="integrations__header">
                <h1 className="integrations__title">Integrări</h1>
                <p className="integrations__subtitle">
                    Conectează sursele de date pentru a completa registrul
                </p>
            </div>

            {/* Integration Cards Grid */}
            <div className="integrations__grid">
                {/*SAF-T Card */}
                <div className="integration-card integration-card__grid-delay">
                    <div className="integration-card__inner">
                        {/* Icon */}
                        <div className="integration-card__icon-wrap">
                            <SAFTIcon />
                        </div>

                        {/* Content */}
                        <div className="integration-card__content">
                            <div className="integration-card__title-row">
                                <h3 className="integration-card__name">SAF-T (D406)</h3>
                                {getStatusBadge(saftStatus)}
                            </div>
                            <p className="integration-card__description">
                                Importă fișierul SAF-T pentru a încărca istoricul contabil.
                            </p>

                            {/* File Input + Upload Button */}
                            <div className="integration-card__file-row">
                                <input
                                    ref={fileInputRef}
                                    type="file"
                                    accept=".xml"
                                    onChange={handleFileChange}
                                    className="integration-card__file-input"
                                />
                                <button
                                    onClick={handleUpload}
                                    disabled={!saftFile || saftUploading}
                                    className="btn-secondary"
                                >
                                    {saftUploading ? (
                                        <span className="integration-card__upload-spinner">
                                            <svg viewBox="0 0 24 24">
                                                <circle className="opacity-25" cx="12" cy="12" r="10"
                                                    stroke="currentColor" strokeWidth="4" fill="none" />
                                                <path fill="currentColor"
                                                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                                            </svg>
                                            Se importă…
                                        </span>
                                    ) : 'Importă fișier'}
                                </button>
                            </div>

                            {/* Success */}
                            {saftResult && (
                                <div className="integration-card__toast integration-card__toast--success">
                                    <p className="integration-card__toast-title">
                                        Import realizat cu succes
                                    </p>
                                    <p className="integration-card__toast-body">
                                        {saftResult.stats?.accounts ?? 0} conturi •{' '}
                                        {(saftResult.stats?.customers ?? 0) + (saftResult.stats?.suppliers ?? 0)} parteneri •{' '}
                                        {saftResult.stats?.invoices ?? 0} facturi •{' '}
                                        {saftResult.stats?.journal_entries ?? 0} tranzacții •{' '}
                                        {saftResult.stats?.payments ?? 0} plăți
                                        {saftResult.period_from && saftResult.period_to && (
                                            <> • Perioada: {saftResult.period_from} – {saftResult.period_to}</>
                                        )}
                                    </p>
                                </div>
                            )}

                            {/* Error Toast */}
                            {saftError && (
                                <div className="integration-card__toast integration-card__toast--error">
                                    <p className="integration-card__toast-title">Eroare la import</p>
                                    <p className="integration-card__toast-body">{saftError}</p>
                                </div>
                            )}
                        </div>
                    </div>
                </div>


                {integrations.map((integration) => (
                    <div key={integration.id} className="integration-card integration-card__grid-delay">
                        <div className="integration-card__inner">
                            {/* Icon */}
                            <div className="integration-card__icon-wrap">
                                {integration.icon}
                            </div>

                            {/* Content */}
                            <div className="integration-card__content">
                                <div className="integration-card__title-row">
                                    <h3 className="integration-card__name">{integration.name}</h3>
                                    {getStatusBadge(integration.status)}
                                </div>
                                <p className="integration-card__description">
                                    {integration.description}
                                </p>
                                <button
                                    onClick={() => toggleConnection(integration.id)}
                                    className="btn-secondary"
                                >
                                    {integration.status === 'connected' ? 'Gestionează' : 'Conectează'}
                                </button>
                            </div>
                        </div>
                    </div>
                ))}
            </div>

            {/* Info note */}
            <div className="integrations__note">
                <svg className="integrations__note-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                        d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <p className="integrations__note-text">
                    Integrarea SAF-T este funcțională. Celelalte integrări sunt în curs de dezvoltare.
                </p>
            </div>
        </div>
    );
};
