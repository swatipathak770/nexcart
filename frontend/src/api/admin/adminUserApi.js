import api from "../../services/api";

export const getAdminUsers = (params = {}) => api.get("/api/user/users", {
  params: { page: 0, size: 20, sortBy: "id", direction: "desc", ...params },
}).then((r) => r.data.data);
export const getAdminUser = (id) => api.get(`/api/user/users/${id}`).then((r) => r.data.data);
export const deleteAdminUser = (id) => api.delete(`/api/user/users/${id}`).then((r) => r.data.data);
