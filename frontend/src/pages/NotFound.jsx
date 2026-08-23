import { Link } from "react-router-dom";

function NotFound() {
  return (
    <main className="grid min-h-screen place-items-center bg-slate-50 px-6 text-center">
      <div>
        <p className="text-sm font-bold uppercase tracking-[0.2em] text-emerald-600">404</p>
        <h1 className="mt-3 text-4xl font-black tracking-tight text-slate-950">Page not found</h1>
        <p className="mt-3 text-slate-500">The page you are looking for does not exist.</p>
        <Link to="/" className="mt-7 inline-flex rounded-xl bg-emerald-600 px-5 py-3 text-sm font-bold text-white hover:bg-emerald-700">
          Return home
        </Link>
      </div>
    </main>
  );
}

export default NotFound;
