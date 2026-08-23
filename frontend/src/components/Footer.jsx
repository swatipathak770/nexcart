import { Link } from "react-router-dom";
import { useAuth } from "../context/useAuth";

export default function Footer() {
  const { isAuthenticated, user } = useAuth();
  const admin = user?.role === "ADMIN";
  const links = admin ? [["Dashboard", "/admin"], ["Products", "/products"], ["Categories", "/categories"]] : isAuthenticated ? [["Products", "/products"], ["Wishlist", "/wishlist"], ["Cart", "/cart"], ["Profile", "/profile"]] : [["Products", "/products"], ["Categories", "/categories"], ["Login", "/login"], ["Register", "/register"]];
  return <footer className="border-t border-slate-200 bg-white"><div className="mx-auto flex max-w-7xl flex-col gap-5 px-5 py-8 sm:flex-row sm:items-center sm:justify-between sm:px-8"><Link to="/" className="text-xl font-black">Nex<span className="text-emerald-600">Cart</span></Link><nav className="flex flex-wrap gap-x-5 gap-y-2 text-sm font-semibold text-slate-600">{links.map(([label, to]) => <Link key={to} to={to} className="hover:text-emerald-600">{label}</Link>)}</nav><p className="text-xs text-slate-400">© {new Date().getFullYear()} NexCart</p></div></footer>;
}
