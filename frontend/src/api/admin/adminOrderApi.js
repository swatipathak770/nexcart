import api from "../../services/api";

const pageParams = (params = {}) => ({ page: 0, size: 20, sortBy: "id", direction: "desc", ...params });

export const getAdminOrders = (params) => api.get("/api/admin/orders", { params: pageParams(params) }).then((r) => r.data);
export const getAdminOrder = (id) => api.get(`/api/admin/orders/${id}`).then((r) => r.data);
export const getOrdersByStatus = (status, params) => api.get(`/api/admin/orders/status/${status}`, { params: pageParams(params) }).then((r) => r.data);
export const updateAdminOrderStatus = (id, status) => api.patch(`/api/admin/orders/${id}/status`, null, { params: { status } }).then((r) => r.data);
