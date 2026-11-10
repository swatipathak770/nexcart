import api from "../services/api";

export const getReviews = (productId, params = {}) => api.get(`/api/reviews/products/${productId}`, {
  params: { page: 0, size: 10, sortBy: "id", direction: "desc", ...params },
}).then((r) => r.data.data);
export const addReview = (productId, review) => api.post(`/api/reviews/products/${productId}`, review).then((r) => r.data.data);
export const updateReview = (reviewId, review) => api.put(`/api/reviews/${reviewId}`, review).then((r) => r.data.data);
export const deleteReview = (reviewId) => api.delete(`/api/reviews/${reviewId}`).then((r) => r.data.data);
