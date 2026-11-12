const rawBaseUrl = import.meta.env.VITE_API_BASE_URL;

// Development can fall back to the local backend. A production build must set
// VITE_API_BASE_URL; if it is missing, requests stay on the same origin so a
// reverse proxy can forward /books, /login, and the other API routes.
const fallbackBaseUrl = import.meta.env.DEV ? "http://localhost:8080" : "";

export const API_BASE_URL =
    rawBaseUrl && rawBaseUrl.trim() !== ""
        ? rawBaseUrl.replace(/\/+$/, "")
        : fallbackBaseUrl;
