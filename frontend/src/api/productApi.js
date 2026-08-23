import api from "../services/api";

export async function getProducts(params) {
  const response = await api.get("/api/products", { params });
  return response.data.data;
}

export async function filterProducts(params) {
  const response = await api.get("/api/products/filter", { params });
  return response.data.data;
}

export async function searchProducts(keyword) {
  const response = await api.get("/api/products/search", { params: { keyword } });
  return response.data.data;
}

export async function getProduct(id) {
  const response = await api.get(`/api/products/${id}`);
  return response.data.data;
}

export async function createProduct(product) {
  const response = await api.post("/api/products", product);
  return response.data.data;
}

export async function updateProduct(id, product) {
  const response = await api.put(`/api/products/${id}`, product);
  return response.data.data;
}

export async function deleteProduct(id) {
  return api.delete(`/api/products/${id}`);
}
