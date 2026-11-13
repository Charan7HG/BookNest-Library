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
            url.endsWith("/login") ||
            url.endsWith("/forgot-password") ||
            url.endsWith("/password-reset-requests");

        if (status === 401 && !isAuthRequest) {
            clearAuth();

            if (window.location.pathname !== "/login") {
                window.location.replace("/login");
            }
        }

        // The backend blocks every endpoint except /change-password while a
        // temporary password is active. Send the browser to the only page it
        // is allowed to use. Ordinary permission 403s are not redirected.
        if (
            status === 403 &&
            error.response?.data?.message
                ?.toLowerCase()
                .includes("password change required") &&
            window.location.pathname !== "/change-password"
        ) {
            window.location.replace("/change-password");
        }

        return Promise.reject(error);
    }
);
