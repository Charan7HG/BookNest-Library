import axios from "axios";

import { API_BASE_URL } from "./apiConfig";

const BASE_URL = API_BASE_URL;

export const login = (email, password) => {
    return axios.post(`${BASE_URL}/login`, {
        email,
        password
    });
};

export const saveAuth = (token, user) => {
    localStorage.setItem("token", token);
    localStorage.setItem("user", JSON.stringify(user));
};

export const clearAuth = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
};

export const getToken = () => {
    return localStorage.getItem("token");
};

export const getStoredUser = () => {
    const storedUser = localStorage.getItem("user");

    if (!storedUser) {
        return null;
    }

    try {
        return JSON.parse(storedUser);
    } catch {
        return null;
    }
};

export const hasAnyRole = (...roles) => {
    const user = getStoredUser();
    return Boolean(user?.role) && roles.includes(user.role);
};

// Authenticated self-service password change. The backend identifies the user
// from the JWT, so no user id is ever sent from the browser.
export const changePassword = (currentPassword, newPassword) => {
    return axios.post(`${BASE_URL}/change-password`, {
        currentPassword,
        newPassword
    });
};

// The forced-change flag is stored with the session so the router can keep a
// temporary-password user on the Change Password page. It is cleared only
// after the backend confirms the password was changed.
export const isPasswordChangeRequired = () => {
    return getStoredUser()?.passwordChangeRequired === true;
};

export const clearPasswordChangeRequired = () => {
    const user = getStoredUser();

    if (!user) {
        return;
    }

    localStorage.setItem("user", JSON.stringify({
        ...user,
        passwordChangeRequired: false
    }));
};

const isTokenExpired = (token) => {
    try {
        const payload = token.split(".")[1];

        if (!payload) {
            return true;
        }

        const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
        const padded = normalized + "=".repeat((4 - (normalized.length % 4)) % 4);
        const decoded = JSON.parse(atob(padded));

        if (typeof decoded.exp !== "number") {
            return true;
        }

        return decoded.exp * 1000 <= Date.now();
    } catch {
        return true;
    }
};

export const isAuthenticated = () => {
    const token = getToken();
    const user = getStoredUser();

    if (!token || !user || isTokenExpired(token)) {
        clearAuth();
        return false;
    }

    return true;
};
