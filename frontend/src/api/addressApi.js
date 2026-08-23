import api from "../services/api";

export const getAddresses = async () => (await api.get("/api/addresses")).data;
export const createAddress = async (address) => (await api.post("/api/addresses", address)).data;
export const updateAddress = async (id, address) => (await api.put(`/api/addresses/${id}`, address)).data;
export const deleteAddress = async (id) => api.delete(`/api/addresses/${id}`);
export const setDefaultAddress = async (id) => api.put(`/api/addresses/${id}/default`);
