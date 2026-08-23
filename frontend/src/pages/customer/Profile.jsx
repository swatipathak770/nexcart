import { useEffect, useState } from "react";
import { toast } from "react-hot-toast";
import { getProfile, updateProfile } from "../../api/userApi";
import { useAuth } from "../../context/useAuth";
import { Link } from "react-router-dom";
import StoreHeader from "../../components/StoreHeader";

function Profile() {
  const { user, updateUser } = useAuth();
  const [form, setForm] = useState({ fullName: user?.fullName || "", phoneNumber: user?.phoneNumber || "" });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    async function loadProfile() {
      try {
        const profile = await getProfile();
        updateUser(profile);
        setForm({ fullName: profile.fullName || "", phoneNumber: profile.phoneNumber || "" });
      } catch (error) {
        toast.error(error.response?.data?.message || "Unable to load your profile.");
      } finally {
        setLoading(false);
      }
    }

    loadProfile();
  }, [updateUser]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSaving(true);
    try {
      const profile = await updateProfile(form);
      updateUser(profile);
      toast.success("Profile updated successfully.");
    } catch (error) {
      toast.error(error.response?.data?.message || "Unable to update your profile.");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <main className="grid min-h-screen place-items-center bg-slate-50 text-slate-500">Loading your profile…</main>;
  }

  return (<><StoreHeader />
    <main className="min-h-screen bg-slate-50 px-5 py-12 sm:px-8">
      <section className="mx-auto max-w-2xl rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <p className="text-sm font-bold text-emerald-600">My account</p>
        <h1 className="mt-1 text-3xl font-black text-slate-950">Profile details</h1>
        <p className="mt-2 text-slate-500">Keep your account information up to date.</p>

        <form className="mt-8 space-y-5" onSubmit={handleSubmit}>
          <label className="block text-sm font-semibold text-slate-700">
            Full name
            <input className="mt-2 w-full rounded-xl border border-slate-300 px-4 py-3 outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100" value={form.fullName} onChange={(event) => setForm({ ...form, fullName: event.target.value })} required />
          </label>
          <label className="block text-sm font-semibold text-slate-700">
            Email address
            <input className="mt-2 w-full cursor-not-allowed rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-slate-500" value={user?.email || ""} disabled />
          </label>
          <label className="block text-sm font-semibold text-slate-700">
            Phone number
            <input className="mt-2 w-full rounded-xl border border-slate-300 px-4 py-3 outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100" value={form.phoneNumber} onChange={(event) => setForm({ ...form, phoneNumber: event.target.value })} pattern="[6-9][0-9]{9}" required />
          </label>
          <button disabled={saving} className="rounded-xl bg-emerald-600 px-5 py-3 text-sm font-bold text-white hover:bg-emerald-700 disabled:bg-emerald-300">{saving ? "Saving…" : "Save changes"}</button>
        </form>
        <section className="mt-10 border-t border-slate-200 pt-6">
          <h2 className="text-xl font-black text-slate-950">Shopping activity</h2>
          <div className="mt-4 grid gap-3 sm:grid-cols-2">
            <Link to="/addresses" className="rounded-xl border border-slate-200 p-4 font-bold hover:border-emerald-500 hover:text-emerald-700">Saved addresses <span className="mt-1 block text-sm font-normal text-slate-500">Add, edit, and choose delivery addresses.</span></Link>
            <Link to="/orders" className="rounded-xl border border-slate-200 p-4 font-bold hover:border-emerald-500 hover:text-emerald-700">My orders <span className="mt-1 block text-sm font-normal text-slate-500">Track past and current purchases.</span></Link>
            <Link to="/wishlist" className="rounded-xl border border-slate-200 p-4 font-bold hover:border-emerald-500 hover:text-emerald-700">Wishlist <span className="mt-1 block text-sm font-normal text-slate-500">Products saved for later.</span></Link>
            <Link to="/coupons" className="rounded-xl border border-slate-200 p-4 font-bold hover:border-emerald-500 hover:text-emerald-700">Available coupons <span className="mt-1 block text-sm font-normal text-slate-500">See current offers before checkout.</span></Link>
          </div>
        </section>
      </section>
    </main>
  </>);
}

export default Profile;
