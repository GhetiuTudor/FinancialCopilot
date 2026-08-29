import React, { useState, useEffect } from 'react';
import { getProfile, updateProfile, type CompanyProfile } from '../api/profile';
import './profile.css';

const ENTITY_TYPES = [
    { value: 'SRL', label: 'SRL' },
    { value: 'PFA', label: 'PFA' },
];

const TAX_REGIMES = [
    { value: 'MICRO', label: 'Microîntreprindere' },
    { value: 'PROFIT', label: 'Impozit pe profit' },
    { value: 'REAL', label: 'Sistem real' },
    { value: 'NORMA', label: 'Normă de venit' },
];

const VAT_PERIODS = [
    { value: 'MONTHLY', label: 'Lunară' },
    { value: 'QUARTERLY', label: 'Trimestrială' },
];

export const ProfilePage: React.FC = () => {
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [success, setSuccess] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [form, setForm] = useState<CompanyProfile>({
        entity_type: 'SRL',
        tax_regime: 'MICRO',
        employees_count: 0,
        vat_registered: false,
        vat_cash_accounting: false,
        vat_period: null,
        estimated_annual_revenue: null,
        estimated_annual_expenses: null,
        norm_value: null,
        county: null,
        city: null,
        activity_start_date: null,
    });

    useEffect(() => {

        getProfile().then((data) => {
                setForm(data);
                setLoading(false);
            }).catch(() => {
                setError('Nu s-a putut încărca profilul.');
                setLoading(false);
            });
    }, []);

    const handleChange = (field: keyof CompanyProfile, value: unknown) => {
        setForm((prev) => ({ ...prev, [field]: value }));
        setSuccess(false);
    };

    const handleSave = async () => {
        setSaving(true);
        setError(null);
        setSuccess(false);
        try {
            const updated = await updateProfile(form);
            setForm(updated);
            setSuccess(true);
        } catch {
            setError('Eroare la salvarea profilului.');
        } finally {
            setSaving(false);
        }
    };

    const showNormValue = form.entity_type === 'PFA' && form.tax_regime === 'NORMA';

    if (loading) {
        return (
            <div className="profile__loading">
                <div className="profile__loading-spinner" />
            </div>
        );
    }

    return (
        <div className="profile">
            {/* Header */}
            <div className="profile__header">
                <h1 className="profile__title">Profil firmă</h1>
                <p className="profile__subtitle">
                    Configurează informațiile fiscale ale firmei tale
                </p>
            </div>

            <div className="profile__content">
                {/* ── Identitate ── */}
                <section className="profile-section">
                    <h2 className="profile-section__title">Identitate</h2>
                    <div className="profile-section__grid">
                        {/* Tip entitate */}
                        <div className="profile-field">
                            <label className="profile-field__label">Tip entitate</label>
                            <select
                                disabled={saving}
                                value={form.entity_type}
                                onChange={(e) => handleChange('entity_type', e.target.value)}
                                className="profile-field__select"
                            >
                                {ENTITY_TYPES.map((t) => (
                                    <option key={t.value} value={t.value}>{t.label}</option>
                                ))}
                            </select>
                        </div>

                        {/* Regim fiscal */}
                        <div className="profile-field">
                            <label className="profile-field__label">Regim fiscal</label>
                            <select
                                disabled={saving}
                                value={form.tax_regime}
                                onChange={(e) => handleChange('tax_regime', e.target.value)}
                                className="profile-field__select"
                            >
                                {TAX_REGIMES.map((t) => (
                                    <option key={t.value} value={t.value}>{t.label}</option>
                                ))}
                            </select>
                        </div>

                        {/* Nr angajați */}
                        <div className="profile-field">
                            <label className="profile-field__label">Nr. angajați</label>
                            <input
                                type="number"
                                min={0}
                                disabled={saving}
                                value={form.employees_count}
                                onChange={(e) => handleChange('employees_count', parseInt(e.target.value) || 0)}
                                className="profile-field__input"
                            />
                        </div>
                    </div>
                </section>

                {/* ── TVA ── */}
                <section className="profile-section">
                    <h2 className="profile-section__title">TVA</h2>
                    <div className="profile-section__checks">
                        <label className="profile-field profile-field--checkbox">
                            <input
                                type="checkbox"
                                disabled={saving}
                                checked={form.vat_registered}
                                onChange={(e) => handleChange('vat_registered', e.target.checked)}
                                className="profile-field__checkbox"
                            />
                            <span className="profile-field__label">Plătitor TVA</span>
                        </label>

                        <label className="profile-field profile-field--checkbox">
                            <input
                                type="checkbox"
                                disabled={saving}
                                checked={form.vat_cash_accounting}
                                onChange={(e) => handleChange('vat_cash_accounting', e.target.checked)}
                                className="profile-field__checkbox"
                            />
                            <span className="profile-field__label">TVA la încasare</span>
                        </label>

                        <div className="profile-section__narrow">
                            <div className="profile-field">
                                <label className="profile-field__label">Perioadă TVA</label>
                                <select
                                    disabled={saving}
                                    value={form.vat_period || ''}
                                    onChange={(e) => handleChange('vat_period', e.target.value || null)}
                                    className="profile-field__select"
                                >
                                    <option value="">—</option>
                                    {VAT_PERIODS.map((t) => (
                                        <option key={t.value} value={t.value}>{t.label}</option>
                                    ))}
                                </select>
                            </div>
                        </div>
                    </div>
                </section>

                {/* ── Parametri financiari ── */}
                <section className="profile-section">
                    <h2 className="profile-section__title">Parametri financiari</h2>
                    <div className="profile-section__grid">
                        <div className="profile-field">
                            <label className="profile-field__label">Venit anual estimat (RON)</label>
                            <input
                                type="number"
                                min={0}
                                step={0.01}
                                disabled={saving}
                                value={form.estimated_annual_revenue ?? ''}
                                onChange={(e) => handleChange('estimated_annual_revenue', e.target.value ? parseFloat(e.target.value) : null)}
                                className="profile-field__input"
                            />
                        </div>
                        <div className="profile-field">
                            <label className="profile-field__label">Cheltuieli anuale estimate (RON)</label>
                            <input
                                type="number"
                                min={0}
                                step={0.01}
                                disabled={saving}
                                value={form.estimated_annual_expenses ?? ''}
                                onChange={(e) => handleChange('estimated_annual_expenses', e.target.value ? parseFloat(e.target.value) : null)}
                                className="profile-field__input"
                            />
                        </div>

                        {showNormValue && (
                            <div className="profile-field">
                                <label className="profile-field__label">Normă de venit (RON)</label>
                                <input
                                    type="number"
                                    min={0}
                                    step={0.01}
                                    disabled={saving}
                                    value={form.norm_value ?? ''}
                                    onChange={(e) => handleChange('norm_value', e.target.value ? parseFloat(e.target.value) : null)}
                                    className="profile-field__input"
                                />
                            </div>
                        )}
                    </div>
                </section>

                {/* ── Localizare ── */}
                <section className="profile-section">
                    <h2 className="profile-section__title">Localizare</h2>
                    <div className="profile-section__grid">
                        <div className="profile-field">
                            <label className="profile-field__label">Județ</label>
                            <input
                                type="text"
                                disabled={saving}
                                value={form.county || ''}
                                onChange={(e) => handleChange('county', e.target.value || null)}
                                className="profile-field__input"
                            />
                        </div>
                        <div className="profile-field">
                            <label className="profile-field__label">Oraș</label>
                            <input
                                type="text"
                                disabled={saving}
                                value={form.city || ''}
                                onChange={(e) => handleChange('city', e.target.value || null)}
                                className="profile-field__input"
                            />
                        </div>
                        <div className="profile-field">
                            <label className="profile-field__label">Data început activitate</label>
                            <input
                                type="date"
                                disabled={saving}
                                value={form.activity_start_date || ''}
                                onChange={(e) => handleChange('activity_start_date', e.target.value || null)}
                                className="profile-field__input"
                            />
                        </div>
                    </div>
                </section>

                {/* ── Actions ── */}
                <div className="profile__actions">
                    <button
                        onClick={handleSave}
                        disabled={saving}
                        className="profile__save-btn"
                    >
                        {saving ? (
                            <>
                                <svg viewBox="0 0 24 24">
                                    <circle cx="12" cy="12" r="10"
                                        stroke="currentColor" strokeWidth="4" fill="none"
                                        style={{ opacity: 0.25 }} />
                                    <path fill="currentColor"
                                        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
                                        style={{ opacity: 0.75 }} />
                                </svg>
                                Se salvează…
                            </>
                        ) : 'Salvează'}
                    </button>

                    {success && (
                        <span className="profile__feedback profile__feedback--success">
                            ✅ Profil salvat cu succes
                        </span>
                    )}
                    {error && (
                        <span className="profile__feedback profile__feedback--error">
                            ❌ {error}
                        </span>
                    )}
                </div>
            </div>
        </div>
    );
};
