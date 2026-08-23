import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { FaEye, FaEyeSlash, FaEnvelope, FaLock } from "react-icons/fa";
import { toast } from "react-hot-toast";

import { useAuth } from "../../context/useAuth";

function Login() {
  const navigate = useNavigate();

  const { login } = useAuth();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!email || !password) {
      toast.error("Please enter email and password");
      return;
    }

    try {
      setLoading(true);

      const data = await login(email, password);

      toast.success("Login successful!");

      if (data.user?.role === "ADMIN") {
        navigate("/admin", { replace: true });
      } else {
        navigate("/profile", { replace: true });
      }
    } catch (error) {
      console.error(error);

      const message =
        error.response?.data?.message ||
        "Invalid email or password";

      toast.error(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex bg-gray-50">

      {/* Left Section */}

      <div className="hidden lg:flex lg:w-1/2 bg-emerald-600 text-white items-center justify-center px-16">

        <div className="max-w-lg">

          <h1 className="text-6xl font-bold mb-6">
            NexCart
          </h1>

          <h2 className="text-3xl font-semibold mb-4">
            Everything you need,
            <br />
            in one place.
          </h2>

          <p className="text-emerald-50 text-lg leading-8">
            Discover products you love, manage your
            cart effortlessly and enjoy a smooth
            shopping experience.
          </p>

        </div>

      </div>

      {/* Login Section */}

      <div className="flex-1 flex items-center justify-center px-6 py-12">

        <div className="w-full max-w-md">

          {/* Mobile Logo */}

          <div className="lg:hidden text-center mb-8">

            <h1 className="text-4xl font-bold text-emerald-600">
              NexCart
            </h1>

          </div>

          <div className="bg-white rounded-2xl shadow-lg border border-gray-100 p-8">

            <div className="mb-8">

              <h2 className="text-3xl font-bold text-gray-900">
                Welcome back
              </h2>

              <p className="text-gray-500 mt-2">
                Login to continue shopping with NexCart.
              </p>

            </div>

            <form
              onSubmit={handleSubmit}
              className="space-y-5"
            >

              {/* Email */}

              <div>

                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Email address
                </label>

                <div className="relative">

                  <FaEnvelope className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" />

                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="you@example.com"
                    className="w-full border border-gray-300 rounded-xl py-3 pl-11 pr-4 outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 transition"
                  />

                </div>

              </div>

              {/* Password */}

              <div>

                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Password
                </label>

                <div className="relative">

                  <FaLock className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" />

                  <input
                    type={showPassword ? "text" : "password"}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Enter your password"
                    className="w-full border border-gray-300 rounded-xl py-3 pl-11 pr-12 outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 transition"
                  />

                  <button
                    type="button"
                    onClick={() =>
                      setShowPassword(!showPassword)
                    }
                    className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                  >
                    {showPassword ? (
                      <FaEyeSlash />
                    ) : (
                      <FaEye />
                    )}
                  </button>

                </div>

              </div>

              {/* Login Button */}

              <button
                type="submit"
                disabled={loading}
                className="w-full bg-emerald-600 hover:bg-emerald-700 disabled:bg-emerald-300 text-white font-semibold py-3 rounded-xl transition"
              >
                {loading ? "Signing in..." : "Sign in"}
              </button>

            </form>

            <div className="text-center mt-6">

              <p className="text-gray-500">

                Don't have an account?

                <Link
                  to="/register"
                  className="text-emerald-600 font-semibold ml-1 hover:text-emerald-700"
                >
                  Create account
                </Link>

              </p>

            </div>

          </div>

        </div>

      </div>

    </div>
  );
}

export default Login;
