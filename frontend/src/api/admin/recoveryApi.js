import api from "../axios";
export const recoveryMetrics = (mode) => api.get("/api/admin/recovery/metrics", { params: { mode, _ts: Date.now() } }).then(r => r.data);
export const recoveryCases = (mode) => api.get("/api/admin/recovery/cases", { params: { mode, _ts: Date.now() } }).then(r => r.data);
export const recoveryDetail = (id) => api.get(`/api/admin/recovery/cases/${id}`).then(r => r.data);
export const simulateRecovery = () => api.post("/api/admin/recovery/simulate").then(r => r.data);
export const executeRecovery = (id) => api.post(`/api/admin/recovery/cases/${id}/execute`).then(r => r.data);
