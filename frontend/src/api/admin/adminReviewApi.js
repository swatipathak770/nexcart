import api from "../../services/api";

export const getAdminReviews = (params = {}) => api.get("/api/admin/reviews", {
  params: { page: 0, size: 20, sortBy: "id", direction: "desc", ...params },
}).then((r) => r.data);
export const getAdminReview = (id) => api.get(`/api/admin/reviews/${id}`).then((r) => r.data);
export const deleteAdminReview = (id) => api.delete(`/api/admin/reviews/${id}`).then((r) => r.data);
