import { apiFetch } from "./client";

// Tipler docs/api/openapi.yaml'daki şemalarla aynıdır (Role, RegisterRole, RegisterRequest, LoginRequest,
// AuthResponse, UserResponse, UpdateProfileRequest, PasswordResetConfirmRequest).

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

export interface UpdateProfileRequest {
  fullName: string;
  /** Boş ya da null gönderilirse temizlenir. */
  bio: string | null;
}

/** PUT /api/me (#116): yalnız ad soyad ve biyografi; e-posta ve rol değiştirilemez. */
export function updateMyProfile(request: UpdateProfileRequest): Promise<UserResponse> {
  return apiFetch<UserResponse>("/api/me", {
    method: "PUT",
    body: JSON.stringify(request),
  });
}

/**
 * POST /api/auth/password-reset/request (#115): e-posta kayıtlı olsun olmasın aynı gövdesiz 202. Bağlantı e-postayla
 * gitmez; dev profilinde backend loguna yazılır.
 */
export function requestPasswordReset(email: string): Promise<void> {
  return apiFetch<void>("/api/auth/password-reset/request", {
    method: "POST",
    body: JSON.stringify({ email }),
  });
}

export interface PasswordResetConfirmRequest {
  token: string;
  newPassword: string;
}

/** POST /api/auth/password-reset/confirm (#115): 204; geçersiz, süresi dolmuş ya da kullanılmış token 400. */
export function confirmPasswordReset(request: PasswordResetConfirmRequest): Promise<void> {
  return apiFetch<void>("/api/auth/password-reset/confirm", {
    method: "POST",
    body: JSON.stringify(request),
  });
}
