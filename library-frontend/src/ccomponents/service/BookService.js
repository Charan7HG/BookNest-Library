import axios from 'axios';
import { API_BASE_URL } from "./apiConfig";

const BASE_URL = API_BASE_URL;

export const getAllBooks = () => {
    return axios.get(`${BASE_URL}/books`);
};

export const getBookById = (id) => {
    return axios.get(`${BASE_URL}/book/${id}`);
};

export const addBook = (book) => {
    return axios.post(`${BASE_URL}/book`, book);
};

export const deleteBook = (id) => {
    return axios.delete(`${BASE_URL}/book/${id}`);
};

export const updateBook = (id, book) => {
    return axios.put(`${BASE_URL}/book/${id}`, book);
};


export const searchBookByName = (name) => {
    return axios.get(`${BASE_URL}/book/search`, {
        params: { name }
    });
}