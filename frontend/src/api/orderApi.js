import api from "../services/api";

export const getOrders = async () => (await api.get("/api/orders")).data;
export const getOrder = async (id) => (await api.get(`/api/orders/${id}`)).data;
export const placeOrder = async (couponCode) => (await api.post("/api/orders", null, { params: couponCode ? { couponCode } : {} })).data;
export const cancelOrder = async (id) => api.put(`/api/orders/${id}/cancel`);
