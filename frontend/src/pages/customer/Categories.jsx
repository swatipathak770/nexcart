import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getCategories } from "../../api/categoryApi";
import StoreHeader from "../../components/StoreHeader";

const PAGE_SIZE = 12;

function Categories() {
  const [page, setPage] = useState(0);
  const [categoryPage, setCategoryPage] = useState({ content: [], totalPages: 0, currentPage: 0 });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    getCategories({ page, size: PAGE_SIZE })
      .then((data) => {
        if (active) {
          setCategoryPage(data);
          setError("");
        }
      })
      .catch(() => {
        if (active) setError("Unable to load categories.");
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, [page]);

  const categories = categoryPage.content || [];
  const totalPages = categoryPage.totalPages || 0;

  return <>
    <StoreHeader />
    <main className="mx-auto min-h-screen max-w-6xl px-5 py-10 sm:px-8">
      <p className="text-sm font-bold text-emerald-600">Browse by category</p>
      <h1 className="mt-1 text-4xl font-black text-slate-950">Shop every interest</h1>
      {error ? <p className="mt-10 text-red-600">{error}</p> : loading ? <p className="mt-10 text-slate-500">Loading categories…</p> : <>
        <section className="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {categories.map((category) => <Link key={category.id} to={`/products?category=${encodeURIComponent(category.name)}`} className="overflow-hidden rounded-2xl border border-slate-200 bg-white transition hover:-translate-y-1 hover:shadow-lg">
            {category.imageUrl ? <img src={category.imageUrl} alt={category.name} className="h-44 w-full object-cover" /> : <div className="grid h-44 place-items-center bg-emerald-50 text-3xl text-emerald-600">✦</div>}
            <div className="p-6"><h2 className="text-xl font-black">{category.name}</h2><p className="mt-2 text-sm leading-6 text-slate-500">{category.description || "Explore products in this category."}</p><span className="mt-5 block text-sm font-bold text-emerald-600">Explore products →</span></div>
          </Link>)}
        </section>
        {totalPages > 1 && <nav aria-label="Category pages" className="mt-8 flex items-center justify-center gap-3">
          <button type="button" disabled={page === 0} onClick={() => { setLoading(true); setPage((current) => current - 1); }} className="rounded-lg border px-4 py-2 disabled:opacity-40">Previous</button>
          <span className="px-3 py-2 text-sm text-slate-600">Page {(categoryPage.currentPage ?? page) + 1} of {totalPages}</span>
          <button type="button" disabled={page + 1 >= totalPages} onClick={() => { setLoading(true); setPage((current) => current + 1); }} className="rounded-lg border px-4 py-2 disabled:opacity-40">Next</button>
        </nav>}
      </>}
    </main>
  </>;
}

export default Categories;
