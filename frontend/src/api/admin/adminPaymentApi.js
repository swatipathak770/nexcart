import api from "../../services/api";

const pageParams = (params = {}) => ({ page: 0, size: 20, sortBy: "id", direction: "desc", ...params });

export const getAdminPayments = (params) => api.get("/api/admin/payments", { params: pageParams(params) }).then((r) => r.data);
export const getAdminPayment = (id) => api.get(`/api/admin/payments/${id}`).then((r) => r.data);
export const getAdminPaymentByOrder = (orderId) => api.get(`/api/admin/payments/order/${orderId}`).then((r) => r.data);
export const getPaymentsByStatus = (status, params) => api.get(`/api/admin/payments/status/${status}`, { params: pageParams(params) }).then((r) => r.data);
