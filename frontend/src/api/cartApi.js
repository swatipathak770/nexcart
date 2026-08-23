import api from "../services/api";

export async function getCart() {
  const response = await api.get("/api/cart");
  return response.data;
}

export async function addToCart(productId, quantity = 1) {
  const response = await api.post("/api/cart", { productId, quantity });
  return response.data;
}

export async function updateCartQuantity(cartId, quantity) {
  const response = await api.put(`/api/cart/${cartId}`, null, { params: { quantity } });
  return response.data;
}

export async function removeCartItem(cartId) {
  await api.delete(`/api/cart/${cartId}`);
}

export async function clearCart() {
  await api.delete("/api/cart");
}
