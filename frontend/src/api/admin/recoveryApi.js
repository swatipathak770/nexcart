import api from "../axios";
export const recoveryMetrics = (mode) => api.get("/api/admin/recovery/metrics", { params: { mode, _ts: Date.now() } }).then(r => r.data);
export const recoveryCases = (mode, page = 0) => api.get("/api/admin/recovery/cases", { params: { mode, page, size: 20, sortBy: "createdAt", direction: "desc", _ts: Date.now() } }).then(r => r.data);
export const recoveryDetail = (id) => api.get(`/api/admin/recovery/cases/${id}`).then(r => r.data);
export const recoveryAudit = (id, page = 0) => api.get(`/api/admin/recovery/cases/${id}/audit`, { params: { page, size: 20, _ts: Date.now() } }).then(r => r.data);
export const simulateRecovery = () => api.post("/api/admin/recovery/simulate").then(r => r.data);
export const executeRecovery = (id) => api.post(`/api/admin/recovery/cases/${id}/execute`).then(r => r.data);
