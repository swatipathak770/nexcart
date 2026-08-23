import api from "../services/api";

export async function getBrands() {
  const response = await api.get("/api/brands");
  return response.data.data;
}

export async function createBrand(brand) {
  const response = await api.post("/api/brands", brand);
  return response.data.data;
}

export async function updateBrand(id, brand) { return (await api.put(`/api/brands/${id}`, brand)).data.data; }
export async function deleteBrand(id) { return api.delete(`/api/brands/${id}`); }
