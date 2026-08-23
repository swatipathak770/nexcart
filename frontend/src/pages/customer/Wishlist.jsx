import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { toast } from "react-hot-toast";
import { addToCart } from "../../api/cartApi";
import { getWishlist, removeFromWishlist } from "../../api/wishlistApi";
import StoreHeader from "../../components/StoreHeader";

const money = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 });
function Wishlist() {
  const [items, setItems] = useState([]); const [loading, setLoading] = useState(true);
  useEffect(() => { getWishlist().then(setItems).catch(() => toast.error("Unable to load wishlist.")).finally(() => setLoading(false)); }, []);
  const remove = async (id) => { await removeFromWishlist(id); setItems((all) => all.filter((item) => item.wishlistId !== id)); toast.success("Removed from wishlist."); };
  const moveToCart = async (item) => { try { await addToCart(item.productId); setItems((all) => all.filter((saved) => saved.wishlistId !== item.wishlistId)); toast.success("Added to cart and removed from wishlist."); } catch { toast.error("Unable to add item to cart."); } };
  return <><StoreHeader /><main className="mx-auto min-h-screen max-w-6xl px-5 py-10 sm:px-8"><h1 className="text-4xl font-black text-slate-950">Wishlist</h1>{loading ? <p className="py-16 text-slate-500">Loading wishlist…</p> : !items.length ? <div className="py-16 text-center text-slate-500">No saved products yet. <Link to="/products" className="font-bold text-emerald-600">Explore products</Link></div> : <section className="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">{items.map((item) => <article key={item.wishlistId} className="overflow-hidden rounded-2xl border border-slate-200 bg-white"><img src={item.imageUrl || "https://placehold.co/600"} alt="" className="aspect-square w-full object-cover" /><div className="p-4"><p className="text-xs font-semibold text-emerald-700">{item.brandName}</p><h2 className="mt-1 font-bold">{item.productName}</h2><p className="mt-2 font-black">{money.format(item.price)}</p><div className="mt-4 flex gap-2"><button onClick={() => moveToCart(item)} className="flex-1 rounded-xl bg-emerald-600 py-2.5 text-sm font-bold text-white">Add to cart</button><button onClick={() => remove(item.wishlistId)} className="rounded-xl border border-slate-300 px-3 text-sm font-bold text-red-600">Remove</button></div></div></article>)}</section>}</main></>;
}
export default Wishlist;
