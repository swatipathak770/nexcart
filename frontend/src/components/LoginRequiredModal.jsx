import { Link } from "react-router-dom";

export default function LoginRequiredModal({ open, onClose }) {
  if (!open) return null;
  return <div role="dialog" aria-modal="true" aria-labelledby="login-required-title" className="fixed inset-0 z-[100] grid place-items-center bg-slate-950/55 p-5" onMouseDown={onClose}>
    <section className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl" onMouseDown={(event) => event.stopPropagation()}>
      <p className="text-sm font-bold text-emerald-600">NexCart</p><h2 id="login-required-title" className="mt-1 text-2xl font-black text-slate-950">Login required</h2>
      <p className="mt-3 text-slate-600">Please sign in or create an account to continue shopping.</p>
      <div className="mt-6 flex flex-wrap gap-3"><Link to="/login" className="rounded-xl bg-emerald-600 px-4 py-2.5 text-sm font-bold text-white" onClick={onClose}>Login</Link><Link to="/register" className="rounded-xl border border-slate-300 px-4 py-2.5 text-sm font-bold text-slate-700" onClick={onClose}>Create account</Link><button onClick={onClose} className="px-2 py-2.5 text-sm font-bold text-slate-500">Continue browsing</button></div>
    </section>
  </div>;
}
