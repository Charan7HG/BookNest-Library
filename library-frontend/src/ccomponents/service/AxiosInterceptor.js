import axios from "axios";
import { clearAuth } from "./AuthService";

axios.interceptors.request.use((config) => {

    const token = localStorage.getItem("token");

    if (token) {

        config.headers.Authorization = `Bearer ${token}`;

    }

    return config;

});

axios.interceptors.response.use(
    (response) => response,
    (error) => {
        const status = error.response?.status;
        const url = error.config?.url || "";
        const isAuthRequest =
            url.endsWith("/login") || url.endsWith("/forgot-password");

        if (status === 401 && !isAuthRequest) {
            clearAuth();

            if (window.location.pathname !== "/login") {
                window.location.replace("/login");
            }
        }

        return Promise.reject(error);
    }
);
