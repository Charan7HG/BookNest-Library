import axios from "axios";
import { API_BASE_URL } from "./apiConfig";

const BASE_URL = API_BASE_URL;

// Public: a member asks for a password reset. The backend always returns the
// same generic response, so the UI never learns whether the email exists.
export const submitPasswordResetRequest = (email) => {
    return axios.post(`${BASE_URL}/password-reset-requests`, { email });
};

// ADMIN / LIBRARIAN: request queue and history. Responses never contain a
// temporary password or any password hash.
export const getPendingPasswordResetRequests = () => {
    return axios.get(`${BASE_URL}/password-reset-requests/pending`);
};

export const getPasswordResetRequestHistory = () => {
    return axios.get(`${BASE_URL}/password-reset-requests`);
};

// ADMIN / LIBRARIAN: process a pending request. Only approval returns the
// one-time temporary password in the response body.
export const approvePasswordResetRequest = (requestId) => {
    return axios.post(`${BASE_URL}/password-reset-requests/${requestId}/approve`);
};

export const rejectPasswordResetRequest = (requestId) => {
    return axios.post(`${BASE_URL}/password-reset-requests/${requestId}/reject`);
};
