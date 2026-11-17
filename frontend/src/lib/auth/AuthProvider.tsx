"use client";

import { createContext, use, useCallback, useEffect, useMemo, useState, useSyncExternalStore } from "react";
import * as authApi from "@/lib/api/auth";
import type { LoginRequest, UserResponse } from "@/lib/api/auth";
import { setTokenProvider, setUnauthorizedHandler } from "@/lib/api/client";
import { clearToken, getServerToken, getToken, setToken, subscribe } from "./tokenStore";

/**
 * Oturumun durumu:
 * - `loading`: token var, kullanıcı bilgisi (`GET /api/me`) henüz gelmedi;
 * - `authenticated`: token ve kullanıcı hazır;
 * - `anonymous`: token yok (giriş yapılmamış, çıkış yapılmış ya da token geçersiz olduğu için silinmiş).
 */
export type AuthStatus = "loading" | "authenticated" | "anonymous";

export interface AuthContextValue {
  status: AuthStatus;
  user: UserResponse | null;
  token: string | null;
  /** Giriş yapar; başarısızsa ApiError fırlatır (401 yanlış bilgi, 403 pasif hesap, 400 alan hatası). */
  login: (request: LoginRequest) => Promise<UserResponse>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

// API istemcisi token'ı her istekte buradan okur; token'la giden istek 401 alırsa token silinir (ADR-002).
setTokenProvider(getToken);
setUnauthorizedHandler(clearToken);

type LoadedUser = { token: string; user: UserResponse };

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const token = useSyncExternalStore(subscribe, getToken, getServerToken);
  const [loaded, setLoaded] = useState<LoadedUser | null>(null);

  // Kullanıcı bilgisi yalnız şimdiki token'a aitse geçerlidir; token değişince yeniden yüklenir.
  const user = token !== null && loaded?.token === token ? loaded.user : null;
  const status: AuthStatus = token === null ? "anonymous" : user === null ? "loading" : "authenticated";

  useEffect(() => {
    if (token === null || loaded?.token === token) return;
    let cancelled = false;
    authApi
      .me()
      .then((me) => {
        if (!cancelled) setLoaded({ token, user: me });
      })
      .catch(() => {
        // 401 ise API istemcisi token'ı zaten sildi. Ağ hatasında token korunur; kullanıcı bilgisi
        // bir sonraki token değişiminde ya da sayfa yenilenince tekrar istenir.
      });
    return () => {
      cancelled = true;
    };
  }, [token, loaded]);

  const login = useCallback(async (request: LoginRequest) => {
    const response = await authApi.login(request);
    setLoaded({ token: response.accessToken, user: response.user });
    setToken(response.accessToken);
    return response.user;
  }, []);

  const logout = useCallback(() => {
    clearToken();
    setLoaded(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ status, user, token, login, logout }),
    [status, user, token, login, logout],
  );

  return <AuthContext value={value}>{children}</AuthContext>;
}

/** Oturum bilgisi ve giriş/çıkış. Yalnız `AuthProvider` altında kullanılabilir. */
export function useAuth(): AuthContextValue {
  const value = use(AuthContext);
  if (value === null) {
    throw new Error("useAuth, AuthProvider içinde kullanılmalı.");
  }
  return value;
}
