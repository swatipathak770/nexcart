import api from "../services/api";

export const createPaymentOrder = async (orderId) => (await api.post("/api/payments/create-order", { orderId })).data;
export const verifyPayment = async (payment) => (await api.post("/api/payments/verify", payment)).data;
export const getPaymentConfig = async () => (await api.get("/api/payments/config")).data;
export const markPaymentFailed = async (orderId, reason) => (await api.post("/api/payments/failed", { orderId, reason })).data;
export const cancelPayment = async (orderId, reason) => (await api.post("/api/payments/cancelled", { orderId, reason })).data;
