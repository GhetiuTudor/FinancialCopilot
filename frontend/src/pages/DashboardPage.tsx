import React from 'react';
import { Dashboard } from '../components/Dashboard';
import { Chat } from '../components/Chat';

//split de 55-45 dashboard-chat
export const DashboardPage: React.FC = () => {
    return (
        <div className="dashboard-page">
            <div className="dashboard-page__left">
                <Dashboard />
            </div>
            <div className="dashboard-page__right">
                <Chat />
            </div>
        </div>
    );
};
