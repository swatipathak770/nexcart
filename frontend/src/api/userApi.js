import api from "../services/api";

export async function getProfile() {
  const response = await api.get("/api/user/profile");
  return response.data.data;
}

export async function updateProfile(profile) {
  const response = await api.put("/api/user/profile", profile);
  return response.data.data;
}

export async function changePassword(passwords) {
  return (await api.put("/api/user/change-password", passwords)).data;
}
