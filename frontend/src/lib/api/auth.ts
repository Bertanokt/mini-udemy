import { apiFetch } from "./client";

// Tipler docs/api/openapi.yaml'daki şemalarla aynıdır (Role, RegisterRole, RegisterRequest, LoginRequest,
// AuthResponse, UserResponse).

export type Role = "STUDENT" | "INSTRUCTOR" | "ADMIN";

/** Kayıtta seçilebilen roller; ADMIN kayıtla oluşturulamaz. */
export type RegisterRole = Exclude<Role, "ADMIN">;

export interface UserResponse {
  id: number;
  email: string;
  fullName: string;
  role: Role;
  bio: string | null;
  createdAt: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  role: RegisterRole;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: "Bearer";
  /** Token'ın geçerlilik süresi (saniye). */
  expiresIn: number;
  user: UserResponse;
}

/** POST /api/auth/register: 201 + kullanıcı; token dönmez, ardından login yapılır. */
export function register(request: RegisterRequest): Promise<UserResponse> {
  return apiFetch<UserResponse>("/api/auth/register", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

/** POST /api/auth/login: yanlış bilgide 401, pasif hesapta 403 (ApiError). */
export function login(request: LoginRequest): Promise<AuthResponse> {
  return apiFetch<AuthResponse>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

/** GET /api/me: token sahibinin profili. */
export function me(): Promise<UserResponse> {
  return apiFetch<UserResponse>("/api/me");
}
