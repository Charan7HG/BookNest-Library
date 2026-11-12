import axios from "axios";

import { API_BASE_URL } from "./apiConfig";

const BASE_URL = API_BASE_URL;
export const getAllAuthors = () => {
    return axios.get(`${BASE_URL}/authors`);
};

export const getAuthorById = (id) => {
    return axios.get(`${BASE_URL}/author/${id}`);
};

export const addAuthor = (author) => {
    return axios.post(`${BASE_URL}/author`, author);
};

export const updateAuthor = (id, author) => {
    return axios.put(`${BASE_URL}/author/${id}`, author);
};

export const deleteAuthor = (id) => {
    return axios.delete(`${BASE_URL}/author/${id}`);
};
export const searchAuthorByName = (name) => {
    return axios.get(`${BASE_URL}/author/search`, {
        params: { name }
    });
};


