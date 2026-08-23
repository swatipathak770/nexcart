import { useCallback, useEffect, useState } from "react";
import { loginUser, registerUser } from "../api/authApi";
import {
  getToken,
  getUser,
  removeToken,
  removeUser,
  saveToken,
  saveUser,
} from "../utils/localStorage";
import { AuthContext } from "./context";
import { getProfile } from "../api/userApi";

export function AuthProvider({ children }) {
  const [user, setUser] = useState(getUser);
  const [token, setToken] = useState(getToken);
  const [loading, setLoading] = useState(Boolean(getToken()));

  const register = async (userData) => {
    return registerUser(userData);
  };

  const login = async (email, password) => {
    const response = await loginUser({ email, password });
    // Spring wraps LoginResponse in ApiResponse: { success, message, data }.
    const data = response.data;
    const jwt = data?.accessToken;

    if (!jwt) {
      throw new Error(
        "JWT token was not returned by the server."
      );
    }

    saveToken(jwt);
    setToken(jwt);

    saveUser(data.user);
    setUser(data.user);

    return data;
  };

  const logout = () => {
    removeToken();
    removeUser();

    setToken(null);
    setUser(null);
  };

  const refreshUser = useCallback(async () => {
    const storedToken = getToken();
    const storedUser = getUser();
    if (!storedToken) {
      setUser(null);
      setToken(null);
      return null;
    }
    // The backend exposes /api/user/profile only to CUSTOMER accounts; the
    // authenticated login response remains the source of the admin session.
    if (storedUser?.role === "ADMIN") {
      setUser(storedUser);
      setToken(storedToken);
      return storedUser;
    }
    try {
      const profile = await getProfile();
      saveUser(profile);
      setUser(profile);
      setToken(storedToken);
      return profile;
    } catch (error) {
      removeToken();
      removeUser();
      setUser(null);
      setToken(null);
      throw error;
    }
  }, []);

  useEffect(() => {
    if (!getToken()) return undefined;
    const timer = setTimeout(() => {
      refreshUser().catch(() => {}).finally(() => setLoading(false));
    }, 0);
    return () => clearTimeout(timer);
  }, [refreshUser]);

  const updateUser = useCallback((nextUser) => {
    saveUser(nextUser);
    setUser(nextUser);
  }, []);

  const isAuthenticated = !!token;

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated,
        loading,
        register,
        login,
        logout,
        updateUser,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
