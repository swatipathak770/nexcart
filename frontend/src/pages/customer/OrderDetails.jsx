import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { toast } from "react-hot-toast";
import { getOrder } from "../../api/orderApi";
import { cancelPayment, getRetryCheckout, markPaymentFailed, verifyPayment } from "../../api/paymentApi";
import StoreHeader from "../../components/StoreHeader";

const money = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 });

const loadRazorpay = () => new Promise((resolve) => {
  if (window.Razorpay) return resolve(true);
  const script = document.createElement("script");
  script.src = "https://checkout.razorpay.com/v1/checkout.js";
  script.onload = () => resolve(true);
  script.onerror = () => resolve(false);
  document.body.appendChild(script);
});

export default function OrderDetails() {
  const { id } = useParams();
  const [order, setOrder] = useState(null);
  const [error, setError] = useState("");
  const [paying, setPaying] = useState(false);

  const refreshOrder = useCallback(() => getOrder(id).then(setOrder).catch((e) => setError(e.response?.data?.message || "Order not found.")), [id]);
  useEffect(() => { refreshOrder(); }, [refreshOrder]);

  const payAgain = async () => {
    setPaying(true);
    let paymentCompleted = false;
    let paymentFailed = false;
    try {
      if (!await loadRazorpay()) throw new Error("Unable to load Razorpay Checkout. Check your connection and try again.");
      const checkout = await getRetryCheckout(order.orderId);
      const razorpay = new window.Razorpay({
        key: checkout.keyId,
        amount: Math.round(Number(checkout.amount) * 100),
        currency: checkout.currency || "INR",
        name: "NexCart",
        description: `Order #${checkout.orderId}`,
        order_id: checkout.razorpayOrderId,
        method: { upi: true, card: true, netbanking: true, wallet: true },
        handler: async (response) => {
          paymentCompleted = true;
          try {
            await verifyPayment({ orderId: checkout.orderId, razorpayPaymentId: response.razorpay_payment_id, razorpaySignature: response.razorpay_signature });
            toast.success("Payment verified. Your order is confirmed.");
          } catch (verificationError) {
            toast.error(verificationError.response?.data?.message || "Payment could not be verified. Contact support before retrying.");
          } finally {
            await refreshOrder();
            setPaying(false);
          }
        },
        modal: { ondismiss: async () => {
          if (!paymentCompleted && !paymentFailed) {
            try { await cancelPayment(checkout.orderId, "Razorpay Checkout was closed by the customer."); } catch { /* Keep the order page usable if cancellation reporting fails. */ }
            toast("Payment cancelled. You can try again from this order.");
            await refreshOrder();
          }
          setPaying(false);
        } },
        theme: { color: "#059669" },
      });
      razorpay.on("payment.failed", async (event) => {
        paymentFailed = true;
        try { await markPaymentFailed(checkout.orderId, event.error?.description || "Razorpay Checkout reported a failed payment."); } catch { /* The order remains available for a later retry. */ }
        toast.error("Payment failed. You can try again from this order.");
        await refreshOrder();
        setPaying(false);
      });
      razorpay.open();
    } catch (checkoutError) {
      toast.error(checkoutError.response?.data?.message || checkoutError.message || "Unable to start payment.");
      setPaying(false);
    }
  };

  return <><StoreHeader /><main className="mx-auto min-h-screen max-w-4xl px-5 py-10 sm:px-8"><Link to="/orders" className="font-bold text-emerald-600">← Orders</Link>{error ? <p className="mt-8 text-red-600">{error}</p> : !order ? <p className="mt-8 text-slate-500">Loading order…</p> : <section className="mt-6 rounded-2xl border bg-white p-6"><div className="flex justify-between"><div><h1 className="text-3xl font-black">Order #{order.orderId}</h1><p className="mt-2 text-slate-500">Status: <span className="font-bold text-emerald-600">{order.status}</span></p></div><p className="text-xl font-black">{money.format(order.finalAmount ?? order.totalAmount)}</p></div>{order.deliveryAddress && <div className="mt-6 rounded-xl border bg-slate-50 p-4"><h2 className="font-black">Delivery address</h2><p className="mt-2 font-bold">{order.deliveryAddress.fullName}</p><p className="text-sm text-slate-600">{order.deliveryAddress.phoneNumber}</p><p className="mt-1 text-sm text-slate-600">{order.deliveryAddress.addressLine1}{order.deliveryAddress.addressLine2 ? `, ${order.deliveryAddress.addressLine2}` : ""}, {order.deliveryAddress.city}, {order.deliveryAddress.state} {order.deliveryAddress.postalCode}, {order.deliveryAddress.country}</p></div>}{order.retryAvailable && order.status === "PENDING" && <div className="mt-6 rounded-xl border border-emerald-200 bg-emerald-50 p-5"><p className="font-black text-emerald-900">Payment is still due</p><p className="mt-1 text-sm text-emerald-800">Your secure checkout is ready. You can safely retry if a previous attempt was cancelled or failed.</p><button type="button" disabled={paying} onClick={payAgain} className="mt-4 rounded-xl bg-emerald-600 px-4 py-2.5 font-bold text-white disabled:cursor-wait disabled:opacity-60">{paying ? "Opening secure checkout…" : "Pay again securely"}</button></div>}{order.recoveryPaymentLink && order.status !== "CANCELLED" && <div className="mt-6 rounded-xl border border-emerald-200 bg-emerald-50 p-5"><p className="font-black text-emerald-900">Payment recovery link available</p><p className="mt-1 text-sm text-emerald-800">Use this secure Razorpay link to finish this order.</p><a href={order.recoveryPaymentLink} target="_blank" rel="noreferrer" className="mt-4 inline-block rounded-xl bg-emerald-600 px-4 py-2.5 font-bold text-white">Complete payment securely</a></div>}<div className="mt-8 divide-y">{order.items?.map((item) => <div key={item.productId} className="flex items-center gap-4 py-4"><img src={item.imageUrl || "https://placehold.co/80"} alt="" className="h-16 w-16 rounded-lg object-cover" /><div className="flex-1"><p className="font-bold">{item.productName}</p><p className="text-sm text-slate-500">Qty {item.quantity}</p></div><p className="font-bold">{money.format(item.subtotal)}</p></div>)}</div></section>}</main></>;
}
