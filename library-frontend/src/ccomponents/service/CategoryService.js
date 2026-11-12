import axios from "axios";

import { API_BASE_URL } from "./apiConfig";

const BASE_URL = API_BASE_URL;

export const getAllCategories = () =>
    axios.get(`${BASE_URL}/categories`);

export const getCategoryById = (id) =>
    axios.get(`${BASE_URL}/category/${id}`);

export const addCategory = (category) =>
    axios.post(`${BASE_URL}/category`, category);

export const updateCategory = (id, category) =>
    axios.put(`${BASE_URL}/category/${id}`, category);

export const deleteCategory = (id) =>
    axios.delete(`${BASE_URL}/category/${id}`);

export const searchCategoryByName = (name) =>
    axios.get(`${BASE_URL}/category/search`, {
        params: { name }
    });