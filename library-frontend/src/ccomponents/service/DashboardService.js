import axios from "axios";
import { API_BASE_URL } from "./apiConfig";

export const getDashboard=()=>{

return axios.get(`${API_BASE_URL}/dashboard`);

}