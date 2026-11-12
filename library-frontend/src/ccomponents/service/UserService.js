import axios from "axios";

import { API_BASE_URL } from "./apiConfig";

const BASE_URL = API_BASE_URL;

export const getAllUsers = () => {
    return axios.get(`${BASE_URL}/users`);
};

export const getUserById = (id) => {
    return axios.get(`${BASE_URL}/user/${id}`);
};

export const addUser = (user) => {
    return axios.post(`${BASE_URL}/user`, user);
};

export const updateUser = (id, user) => {
    return axios.put(`${BASE_URL}/user/${id}`, user);
};

export const deleteUser = (id) => {
    return axios.delete(`${BASE_URL}/user/${id}`);
};

export const searchUserByName = (name) => {
    return axios.get(`${BASE_URL}/user/search`, {
        params: { name }
    });
};