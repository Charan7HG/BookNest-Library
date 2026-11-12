import axios from "axios";
import { API_BASE_URL } from "./apiConfig";

const BASE_URL = API_BASE_URL;

// MEMBER: submit a request for a book. The backend resolves the member
// identity from the JWT, so no member id is sent from the frontend.
export const createBorrowRequest = (bookId) => {
    return axios.post(`${BASE_URL}/borrow-requests`, { bookId });
};

// MEMBER: request history for the authenticated member.
export const getMyBorrowRequests = () => {
    return axios.get(`${BASE_URL}/borrow-requests/my`);
};

// ADMIN / LIBRARIAN: pending request queue.
export const getPendingBorrowRequests = () => {
    return axios.get(`${BASE_URL}/borrow-requests/pending`);
};

// ADMIN / LIBRARIAN: review a pending request.
export const approveBorrowRequest = (requestId) => {
    return axios.post(`${BASE_URL}/borrow-requests/${requestId}/approve`);
};

export const rejectBorrowRequest = (requestId) => {
    return axios.post(`${BASE_URL}/borrow-requests/${requestId}/reject`);
};

// ADMIN / LIBRARIAN: active loans and returns.
export const getActiveLoans = () => {
    return axios.get(`${BASE_URL}/loans/active`);
};

export const returnLoan = (loanId) => {
    return axios.post(`${BASE_URL}/loans/${loanId}/return`);
};
