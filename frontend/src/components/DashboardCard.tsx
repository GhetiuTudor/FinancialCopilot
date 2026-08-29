import React from 'react';

interface DashboardCardProps {
    title: string;
    value?: string;
    subtitle?: string;
    children?: React.ReactNode;
    className?: string;
}

export const DashboardCard: React.FC<DashboardCardProps> = ({
    title,
    value,
    subtitle,
    children,
    className = '',
}) => {
    return (
        <div className={`dashboard-card ${className}`}>
            <h3 className="dashboard-card__title">{title}</h3>
            {value && (
                <p className="dashboard-card__value">{value}</p>
            )}
            {subtitle && (
                <p className="dashboard-card__subtitle">{subtitle}</p>
            )}
            {children}
        </div>
    );
};
