import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { toast } from "react-hot-toast";
import { cancelOrder, getOrders } from "../../api/orderApi";
import StoreHeader from "../../components/StoreHeader";
const money = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 });
const PAGE_SIZE = 10;

export default function Orders() {
  const [page, setPage] = useState(0);
  const [ordersPage, setOrdersPage] = useState({ content: [], totalPages: 0, currentPage: 0 });
  const [loading, setLoading] = useState(true);
  const [cancellingId, setCancellingId] = useState(null);
  const orders = ordersPage.content || [];

  const load = useCallback((pageNumber = page) => {
    setLoading(true);
    getOrders({ page: pageNumber, size: PAGE_SIZE })
      .then(setOrdersPage)
      .catch(() => toast.error("Unable to load orders."))
      .finally(() => setLoading(false));
  }, [page]);

  const handleCancel = async (orderId) => {
    if (cancellingId !== null) return;
    setCancellingId(orderId);
    try {
      await cancelOrder(orderId);
      toast.success("Order cancelled.");
      await load();
    } catch (e) {
      toast.error(e.response?.data?.message || "Unable to cancel this order.");
    } finally {
      setCancellingId(null);
    }
  };

  useEffect(() => { const timer = setTimeout(() => load(page), 0); return () => clearTimeout(timer); }, [load, page]);

  return <><StoreHeader /><main className="mx-auto min-h-screen max-w-6xl px-5 py-10 sm:px-8"><h1 className="text-4xl font-black">Your orders</h1>
    {loading ? <p className="py-12 text-slate-500">Loading orders…</p> : orders.length ? <>
      <div className="mt-8 space-y-4">{orders.map((order) => <article key={order.orderId} className="rounded-2xl border bg-white p-5"><div className="flex flex-wrap justify-between gap-4"><div><p className="font-black">Order #{order.orderId}</p><p className="mt-1 text-sm text-slate-500">{order.createdAt ? new Date(order.createdAt).toLocaleString() : ""}</p></div><div className="text-right"><p className="font-black">{money.format(order.finalAmount ?? order.totalAmount)}</p><p className="text-sm font-bold text-emerald-600">{order.status}</p></div></div><p className="mt-4 text-sm text-slate-600">{order.items?.map((item) => `${item.productName} × ${item.quantity}`).join(", ")}</p><div className="mt-4 flex gap-4"><Link to={`/orders/${order.orderId}`} className="text-sm font-bold text-emerald-600">View details</Link>{order.status === "PENDING" && <button type="button" disabled={cancellingId !== null} onClick={() => handleCancel(order.orderId)} className="text-sm font-bold text-red-600 disabled:cursor-wait disabled:opacity-50">{cancellingId === order.orderId ? "Cancelling…" : "Cancel order"}</button>}</div></article>)}</div>
      {ordersPage.totalPages > 1 && <nav aria-label="Order pages" className="mt-8 flex justify-center gap-3"><button disabled={page === 0} onClick={() => setPage((current) => current - 1)} className="rounded-lg border px-4 py-2 disabled:opacity-40">Previous</button><span className="px-3 py-2 text-sm text-slate-600">Page {(ordersPage.currentPage ?? page) + 1} of {ordersPage.totalPages}</span><button disabled={page + 1 >= ordersPage.totalPages} onClick={() => setPage((current) => current + 1)} className="rounded-lg border px-4 py-2 disabled:opacity-40">Next</button></nav>}
    </> : <div className="py-16 text-center text-slate-500">You haven’t placed any orders yet. <Link to="/products" className="font-bold text-emerald-600">Start shopping</Link></div>}</main></>;
}
