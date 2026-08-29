import { getToken, clearToken } from '../auth/token';


//wrapper peste fetch, adauga logica de auth (Bearer: ...), sterge token si face redirect la login daca e 401
export async function apiFetch(url: string, options: RequestInit = {}): Promise<Response> {
    const token = getToken();

    //normalizeaza in orice caz, gol daca nu are options 
    const headers = new Headers(options.headers || {});

    // adauga -H authorization..
    if (token && !headers.has('Authorization')) {
        headers.set('Authorization', `Bearer ${token}`);
    }

    // content type daca nu e form data, pentru SAFT import
    if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
        headers.set('Content-Type', 'application/json');
    }

    const response = await fetch(url, {...options,headers,});

    // in caz de 401 sau 403 (de ex. cand baza de date s-a resetat si tokenul vechi nu mai e valid)
    if (response.status === 401 || response.status === 403) {
        clearToken();
        window.location.href = '/login';
        throw new Error('Sesiune expirată. Vă rugăm autentificați-vă din nou.');
    }

    return response;
}
