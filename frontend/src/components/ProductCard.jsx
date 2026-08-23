import { Link } from "react-router-dom";
import { FaHeart, FaShoppingBag } from "react-icons/fa";
import { toast } from "react-hot-toast";
import { addToCart } from "../api/cartApi";
import { addToWishlist } from "../api/wishlistApi";
import { useAuth } from "../context/useAuth";
import LoginRequiredModal from "./LoginRequiredModal";
import { useState } from "react";

const money = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 });

function ProductCard({ product }) {
  const { isAuthenticated, user } = useAuth();
  const [loginRequired, setLoginRequired] = useState(false);
  const requireLogin = () => { setLoginRequired(true); return false; };
  const addCart = async () => { if (!isAuthenticated) return requireLogin(); try { await addToCart(product.id); toast.success("Added to cart."); } catch (error) { toast.error(error.response?.data?.message || "Unable to add this item to cart."); } };
  const addWish = async () => { if (!isAuthenticated) return requireLogin(); try { await addToWishlist(product.id); toast.success("Saved to wishlist."); } catch (error) { toast.error(error.response?.data?.message || "Unable to save this item."); } };
  return <><article className="group overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm transition hover:-translate-y-1 hover:shadow-lg">
    <Link to={`/products/${product.id}`} className="block aspect-square overflow-hidden bg-slate-100"><img src={product.imageUrl || "https://placehold.co/600x600/e2e8f0/475569?text=NexCart"} alt={product.name} className="h-full w-full object-cover transition duration-500 group-hover:scale-105" /></Link>
    <div className="p-4"><p className="text-xs font-semibold text-emerald-700">{product.brandName || "NexCart"}</p><Link to={`/products/${product.id}`} className="mt-1 block truncate font-bold text-slate-900">{product.name}</Link><p className="mt-1 text-xs text-slate-500">{product.categoryName || "Uncategorised"}</p><div className="mt-3 flex items-center justify-between"><span className="text-lg font-black text-slate-950">{money.format(product.price || 0)}</span><span className={`text-xs font-bold ${product.stock > 0 ? "text-emerald-600" : "text-red-600"}`}>{product.stock > 0 ? "In stock" : "Out of stock"}</span></div>{user?.role !== "ADMIN" && <div className="mt-4 grid grid-cols-[auto_1fr] gap-2"><button onClick={addWish} aria-label="Add to wishlist" className="grid h-10 w-10 place-items-center rounded-xl border border-slate-200 text-slate-600 hover:border-rose-200 hover:bg-rose-50 hover:text-rose-500"><FaHeart /></button><button onClick={addCart} disabled={!product.stock} className="inline-flex items-center justify-center gap-2 rounded-xl bg-emerald-600 px-3 text-sm font-bold text-white hover:bg-emerald-700 disabled:cursor-not-allowed disabled:bg-slate-300"><FaShoppingBag /> Add to cart</button></div>}</div>
  </article><LoginRequiredModal open={loginRequired} onClose={() => setLoginRequired(false)} /></>;
}

export default ProductCard;
