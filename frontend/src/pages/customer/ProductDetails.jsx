import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { toast } from "react-hot-toast";
import { addToCart } from "../../api/cartApi";
import { getProduct } from "../../api/productApi";
import { addToWishlist } from "../../api/wishlistApi";
import LoginRequiredModal from "../../components/LoginRequiredModal";
import StoreHeader from "../../components/StoreHeader";
import { useAuth } from "../../context/useAuth";

const money = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 });

export default function ProductDetails() {
  const { id } = useParams(); const { isAuthenticated, user } = useAuth();
  const [product, setProduct] = useState(null); const [quantity, setQuantity] = useState(1); const [error, setError] = useState(""); const [loginRequired, setLoginRequired] = useState(false);
  useEffect(() => { getProduct(id).then(setProduct).catch((e) => setError(e.response?.data?.message || "Product not found.")); }, [id]);
  const act = async (fn, success) => { if (!isAuthenticated) return setLoginRequired(true); if (user?.role === "ADMIN") return toast.error("Admin accounts cannot use customer shopping actions."); try { await fn(); toast.success(success); } catch (e) { toast.error(e.response?.data?.message || "Unable to complete this action."); } };
  if (error) return <><StoreHeader /><p className="p-12 text-center text-red-600">{error}</p></>;
  if (!product) return <><StoreHeader /><p className="p-12 text-center text-slate-500">Loading product…</p></>;
  return <><StoreHeader /><main className="mx-auto grid min-h-screen max-w-6xl gap-10 px-5 py-10 sm:px-8 md:grid-cols-2"><div className="overflow-hidden rounded-3xl bg-slate-100"><img src={product.imageUrl || "https://placehold.co/900"} alt={product.name} className="aspect-square h-full w-full object-cover" /></div><section><Link to="/products" className="text-sm font-bold text-emerald-600">← Back to products</Link><p className="mt-6 text-sm font-bold text-emerald-600">{product.brandName}</p><h1 className="mt-1 text-4xl font-black text-slate-950">{product.name}</h1><p className="mt-2 text-slate-500">{product.categoryName}</p><p className="mt-6 text-3xl font-black">{money.format(product.price)}</p><p className="mt-6 leading-7 text-slate-600">{product.description || "Product details will be available soon."}</p><p className={`mt-5 font-bold ${product.stock > 0 ? "text-emerald-600" : "text-red-600"}`}>{product.stock > 0 ? `${product.stock} available` : "Currently out of stock"}</p><div className="mt-6 flex items-center gap-3"><span className="text-sm font-bold">Qty</span><button onClick={() => setQuantity((value) => Math.max(1, value - 1))} className="h-9 w-9 rounded-lg border">−</button><span className="font-bold">{quantity}</span><button onClick={() => setQuantity((value) => Math.min(product.stock, value + 1))} className="h-9 w-9 rounded-lg border">+</button></div><div className="mt-6 flex flex-wrap gap-3"><button disabled={!product.stock || user?.role === "ADMIN"} onClick={() => act(() => addToCart(product.id, quantity), "Added to cart.")} className="rounded-xl bg-emerald-600 px-6 py-3 font-bold text-white disabled:bg-slate-300">Add to cart</button><button disabled={user?.role === "ADMIN"} onClick={() => act(() => addToWishlist(product.id), "Saved to wishlist.")} className="rounded-xl border border-slate-300 px-6 py-3 font-bold text-slate-700 disabled:text-slate-400">Save to wishlist</button></div></section></main><LoginRequiredModal open={loginRequired} onClose={() => setLoginRequired(false)} /></>;
}
