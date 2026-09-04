import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useEffect, useState } from "react";
import { FaBell, FaHeart, FaShoppingBag, FaUser } from "react-icons/fa";
import { useAuth } from "../context/useAuth";
import { getOrders } from "../api/orderApi";

function StoreHeader() {
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [query, setQuery] = useState(searchParams.get("keyword") || "");
  const [recoveryOrders, setRecoveryOrders] = useState([]);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  useEffect(() => {
    if (!isAuthenticated || user?.role === "ADMIN") { setRecoveryOrders([]); return; }
    let mounted = true;
    const loadRecoveryNotifications = () => getOrders().then(orders => {
      if (mounted) setRecoveryOrders(orders.filter(order => order.status !== "CANCELLED" && order.recoveryPaymentLink));
    }).catch(() => { if (mounted) setRecoveryOrders([]); });
    loadRecoveryNotifications();
    const interval = setInterval(loadRecoveryNotifications, 30000);
    return () => { mounted = false; clearInterval(interval); };
  }, [isAuthenticated, user?.role]);
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
          {user?.role !== "ADMIN" && <div className="relative"><button onClick={() => setNotificationsOpen(open => !open)} className="relative grid h-10 w-10 place-items-center rounded-xl text-slate-600 hover:bg-slate-100 hover:text-emerald-600" aria-label="Payment recovery notifications"><FaBell />{recoveryOrders.length > 0 && <span className="absolute right-0 top-0 grid h-5 min-w-5 place-items-center rounded-full bg-emerald-600 px-1 text-[10px] text-white">{recoveryOrders.length}</span>}</button>{notificationsOpen && <div className="absolute right-0 mt-2 w-80 rounded-xl border bg-white p-3 shadow-xl"><p className="px-2 pb-2 text-sm font-black">Notifications</p>{recoveryOrders.length ? recoveryOrders.map(order => <div key={order.orderId} className="border-t px-2 py-3 text-sm"><p className="font-semibold">Payment recovery available for Order #{order.orderId}</p><a href={order.recoveryPaymentLink} target="_blank" rel="noreferrer" className="mt-2 inline-block font-bold text-emerald-600 hover:text-emerald-700">Complete Payment</a></div>) : <p className="border-t px-2 py-3 text-sm text-slate-500">No payment recovery notifications.</p>}</div>}</div>}
          <Link to={user?.role === "ADMIN" ? "/admin" : "/profile"} className="hidden items-center gap-2 rounded-xl px-3 py-2 text-slate-700 hover:bg-slate-100 sm:flex"><FaUser />{user?.role === "ADMIN" ? "Admin" : "Profile"}</Link>
          <button onClick={signOut} className="rounded-xl bg-slate-900 px-4 py-2 text-white hover:bg-slate-800">Logout</button>
        </> : <><Link to="/login" className="px-3 py-2 text-slate-700">Login</Link><Link to="/register" className="rounded-xl bg-emerald-600 px-4 py-2 text-white hover:bg-emerald-700">Register</Link></>}
      </div>
    </div>
  </header>;
}

export default StoreHeader;
