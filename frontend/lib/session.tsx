"use client";

import { apiFetch, AuthResponse } from "@/lib/api";
import type { ReactNode } from "react";
import { createContext, useContext, useEffect, useState } from "react";

type AuthContextValue = {
  token: string | null;
  loading: boolean;
  user: { username: string } | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshSession: () => Promise<void>;
  setToken: (token: string | null) => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<{ username: string } | null>(null);
  const [loading, setLoading] = useState(true);

  const refreshSession = async () => {
    try {
      const response = await apiFetch<AuthResponse>("/api/v1/auth/refresh", { method: "POST" });
      setToken(response.accessToken);
    } catch {
      setToken(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void refreshSession();
  }, []);

  useEffect(() => {
    if (!token) {
      setUser(null);
      return;
    }
    let active = true;
    apiFetch<{ username: string }>("/api/v1/users/me", {}, token)
      .then((data) => {
        if (active) setUser(data);
      })
      .catch(() => {
        if (active) setUser(null);
      });
    return () => {
      active = false;
    };
  }, [token]);

  const login = async (username: string, password: string) => {
    const response = await apiFetch<AuthResponse>("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify({ username, password })
    });
    setToken(response.accessToken);
  };

  const logout = async () => {
    try {
      await apiFetch<void>("/api/v1/auth/logout", { method: "POST" }, token);
    } finally {
      setToken(null);
    }
  };

  const value: AuthContextValue = {
    token,
    loading,
    user,
    login,
    logout,
    refreshSession,
    setToken
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}