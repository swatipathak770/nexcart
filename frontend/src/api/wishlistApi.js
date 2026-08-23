import api from "../services/api";

export async function getWishlist() {
  const response = await api.get("/api/wishlist");
  return response.data;
}

export async function addToWishlist(productId) {
  const response = await api.post(`/api/wishlist/${productId}`);
  return response.data;
}

export async function removeFromWishlist(wishlistId) {
  await api.delete(`/api/wishlist/${wishlistId}`);
}
