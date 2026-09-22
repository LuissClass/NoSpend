import axios from "axios";
export const api=axios.create({baseURL:import.meta.env.VITE_API_URL||"http://localhost:8080/api"});
export const getAccounts=async()=> (await api.get("/accounts")).data;
export const getAccount=async(id)=> (await api.get(`/accounts/${id}`)).data;
export const getCategories=async(id)=> (await api.get(`/accounts/${id}/categories`)).data;
export const getMovements=async(id,limit=50,categoryId)=> (await api.get(`/accounts/${id}/movements`,{params:{limit,categoryId}})).data;
export const getExpenses=async(id)=> (await api.get(`/accounts/${id}/reports/expenses`)).data;
