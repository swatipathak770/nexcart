import api from "../services/api";

export const getOrders = async (params = {}) => (await api.get("/api/orders", {
  params: { page: 0, size: 20, sortBy: "id", direction: "desc", ...params },
})).data;
export const getOrder = async (id) => (await api.get(`/api/orders/${id}`)).data;
export const placeOrder = async (couponCode) => (await api.post("/api/orders", null, { params: couponCode ? { couponCode } : {} })).data;
export const cancelOrder = async (id) => api.put(`/api/orders/${id}/cancel`);
