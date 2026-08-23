import { BrowserRouter, Navigate, Routes, Route } from "react-router-dom";

import Home from "../pages/customer/Home";
import Login from "../pages/auth/Login";
import Register from "../pages/auth/Register";
import Profile from "../pages/customer/Profile";
import Products from "../pages/customer/Products";
import ProductDetails from "../pages/customer/ProductDetails";
import Categories from "../pages/customer/Categories";
import Cart from "../pages/customer/Cart";
import Wishlist from "../pages/customer/Wishlist";
import Addresses from "../pages/customer/Addresses";
import Orders from "../pages/customer/Orders";
import OrderDetails from "../pages/customer/OrderDetails";
import Checkout from "../pages/customer/Checkout";
import Coupons from "../pages/customer/Coupons";
import Dashboard from "../pages/admin/Dashboard";
import AdminRoute from "./AdminRoute";
import ProtectedRoute from "./ProtectedRoute";
import NotFound from "../pages/NotFound";

function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>

        {/* Customer */}
        <Route path="/" element={<Home />} />

        {/* Authentication */}
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/profile" element={<ProtectedRoute><Profile /></ProtectedRoute>} />
        <Route path="/products" element={<Products />} />
        <Route path="/products/:id" element={<ProductDetails />} />
        <Route path="/categories" element={<Categories />} />
        <Route path="/cart" element={<ProtectedRoute><Cart /></ProtectedRoute>} />
        <Route path="/wishlist" element={<ProtectedRoute><Wishlist /></ProtectedRoute>} />
        <Route path="/addresses" element={<ProtectedRoute><Addresses /></ProtectedRoute>} />
        <Route path="/orders" element={<ProtectedRoute><Orders /></ProtectedRoute>} />
        <Route path="/orders/:id" element={<ProtectedRoute><OrderDetails /></ProtectedRoute>} />
        <Route path="/checkout" element={<ProtectedRoute><Checkout /></ProtectedRoute>} />
        <Route path="/coupons" element={<ProtectedRoute><Coupons /></ProtectedRoute>} />

        <Route
          path="/admin"
          element={(
            <AdminRoute>
              <Dashboard />
            </AdminRoute>
          )}
        />
        <Route path="/admin/dashboard" element={<Navigate to="/admin" replace />} />
        <Route path="*" element={<NotFound />} />

      </Routes>
    </BrowserRouter>
  );
}

export default AppRoutes;
