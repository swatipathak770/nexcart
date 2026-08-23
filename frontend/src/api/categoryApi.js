import api from "../services/api";

export async function getCategories() {
  const response = await api.get("/api/categories");
  return response.data.data;
}

export async function createCategory(category) {
  const response = await api.post("/api/categories", category);
  return response.data.data;
}

export async function updateCategory(id, category) {
  const response = await api.put(`/api/categories/${id}`, category);
  return response.data.data;
}

export async function deleteCategory(id) {
  return api.delete(`/api/categories/${id}`);
}
