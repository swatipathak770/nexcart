import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useState } from "react";
import { FaHeart, FaShoppingBag, FaUser } from "react-icons/fa";
import { useAuth } from "../context/useAuth";

function StoreHeader() {
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [query, setQuery] = useState(searchParams.get("keyword") || "");
  const signOut = () => { logout(); navigate("/", { replace: true }); };

  const search = (event) => { event.preventDefault(); navigate(`/products${query.trim() ? `?keyword=${encodeURIComponent(query.trim())}` : ""}`); };
  return <header className="sticky top-0 z-50 border-b border-slate-200 bg-white/95 backdrop-blur">
    <div className="mx-auto flex h-[72px] max-w-7xl items-center gap-4 px-5 sm:px-8">
      <Link to="/" className="flex shrink-0 items-center gap-2"><span className="grid h-9 w-9 place-items-center rounded-xl bg-emerald-600 font-black text-white">N</span><span className="text-xl font-black text-slate-950">Nex<span className="text-emerald-600">Cart</span></span></Link>
      <nav className="hidden items-center gap-5 text-sm font-semibold text-slate-600 md:flex">{user?.role === "ADMIN" ? <><Link to="/admin" className="hover:text-emerald-600">Dashboard</Link><Link to="/products" className="hover:text-emerald-600">Products</Link></> : <><Link to="/products" className="hover:text-emerald-600">Products</Link><Link to="/categories" className="hover:text-emerald-600">Categories</Link></>}</nav>
      <form onSubmit={search} className="hidden max-w-sm flex-1 md:block"><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search products" className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-2 text-sm outline-none focus:border-emerald-500" /></form>
      <div className="ml-auto flex items-center gap-2 text-sm font-bold">
        {isAuthenticated ? <>
          {user?.role !== "ADMIN" && <><Link to="/wishlist" className="grid h-10 w-10 place-items-center rounded-xl text-slate-600 hover:bg-slate-100 hover:text-emerald-600" aria-label="Wishlist"><FaHeart /></Link>
          <Link to="/cart" className="grid h-10 w-10 place-items-center rounded-xl text-slate-600 hover:bg-slate-100 hover:text-emerald-600" aria-label="Cart"><FaShoppingBag /></Link></>}
          <Link to={user?.role === "ADMIN" ? "/admin" : "/profile"} className="hidden items-center gap-2 rounded-xl px-3 py-2 text-slate-700 hover:bg-slate-100 sm:flex"><FaUser />{user?.role === "ADMIN" ? "Admin" : "Profile"}</Link>
          <button onClick={signOut} className="rounded-xl bg-slate-900 px-4 py-2 text-white hover:bg-slate-800">Logout</button>
        </> : <><Link to="/login" className="px-3 py-2 text-slate-700">Login</Link><Link to="/register" className="rounded-xl bg-emerald-600 px-4 py-2 text-white hover:bg-emerald-700">Register</Link></>}
      </div>
    </div>
  </header>;
}

export default StoreHeader;
