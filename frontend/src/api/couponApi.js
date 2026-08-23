import api from "../services/api";

export const getCoupons = async () => (await api.get("/api/coupons")).data;
export const applyCoupon = async (couponCode, orderAmount) => (await api.post("/api/coupons/apply", { couponCode, orderAmount })).data;
export const createCoupon = async (coupon) => (await api.post("/api/coupons", coupon)).data;
export const updateCoupon = async (id, coupon) => (await api.put(`/api/coupons/${id}`, coupon)).data;
export const deleteCoupon = async (id) => api.delete(`/api/coupons/${id}`);
