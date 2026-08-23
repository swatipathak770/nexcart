import { useEffect, useState } from "react";
import { toast } from "react-hot-toast";
import { getCoupons } from "../../api/couponApi";
import StoreHeader from "../../components/StoreHeader";

export default function Coupons() {
  const [coupons, setCoupons] = useState([]);
  useEffect(() => { getCoupons().then((data) => setCoupons(data.filter((coupon) => coupon.active && new Date(coupon.expiryDate) > new Date()))).catch(() => toast.error("Unable to load available coupons.")); }, []);
  return <><StoreHeader /><main className="mx-auto min-h-screen max-w-5xl px-5 py-10 sm:px-8"><p className="font-bold text-emerald-600">Offers for you</p><h1 className="mt-1 text-4xl font-black">Available coupons</h1><p className="mt-2 text-slate-500">Each coupon can be used once per account.</p><div className="mt-8 grid gap-4 sm:grid-cols-2">{coupons.length ? coupons.map((coupon) => <article key={coupon.couponId} className="rounded-2xl border border-emerald-200 bg-white p-5"><p className="text-xl font-black text-emerald-700">{coupon.code}</p><p className="mt-2 text-slate-700">{coupon.description}</p><p className="mt-3 text-sm text-slate-500">Min. order ₹{coupon.minimumOrderAmount} · Valid until {new Date(coupon.expiryDate).toLocaleDateString()}</p></article>) : <p className="text-slate-500">There are no active coupons right now.</p>}</div></main></>;
}
