import React from 'react';
import { Navigate } from 'react-router-dom';
import { getToken } from './token';

interface AuthGuardProps {
    children: React.ReactNode;
    //orice e bagat in <AuthGuard /> ca la TW
}

export const AuthGuard: React.FC<AuthGuardProps> = ({ children }) => {
    const token = getToken();

    if (!token) {
        //replace ca sa nu ajunga userul inapoi la pagina protejata cu Back
        return <Navigate to="/login" replace />;
    }

    return <>{children}</>;
};
