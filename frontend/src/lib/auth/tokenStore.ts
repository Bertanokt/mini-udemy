/**
 * Erişim token'ının tek kaynağı. Token `localStorage`'da saklanır; sayfa yenilense de oturum korunur
 * (ADR-002 › Token'ın saklanacağı yer). XSS'e karşı asıl koruma React'in kaçışlaması ve `dangerouslySetInnerHTML`
 * yasağı (ESLint `react/no-danger`); `next.config.ts`'teki CSP dış betiği ve dışarı veri gönderimini engeller, satır
 * içi betiği engelleyemez (ADR-007).
 *
 * `useSyncExternalStore` ile okunur; başka sekmede giriş/çıkış yapılınca `storage` olayıyla bu sekme de güncellenir.
 */

const KEY = "miniudemy.accessToken";

const listeners = new Set<() => void>();

function readStorage(): string | null {
  try {
    return window.localStorage.getItem(KEY);
  } catch {
    // Gizli pencere ya da engellenmiş depolama: oturum yalnız bu sayfa açıkken sürer.
    return null;
  }
}

let current: string | null = typeof window === "undefined" ? null : readStorage();

function emit(): void {
  listeners.forEach((listener) => listener());
}

export function getToken(): string | null {
  return current;
}

/** Sunucuda (SSR) token yoktur; oturum yalnız tarayıcıda bilinir. */
export function getServerToken(): string | null {
  return null;
}

export function setToken(token: string): void {
  current = token;
  try {
    window.localStorage.setItem(KEY, token);
  } catch {
    // Depolama kullanılamıyorsa token yalnız bellekte kalır.
  }
  emit();
}

export function clearToken(): void {
  if (current === null) return;
  current = null;
  try {
    window.localStorage.removeItem(KEY);
  } catch {
    // yok sayılır
  }
  emit();
}

export function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  const onStorage = (event: StorageEvent) => {
    if (event.key !== KEY) return;
    current = event.newValue;
    listener();
  };
  window.addEventListener("storage", onStorage);
  return () => {
    listeners.delete(listener);
    window.removeEventListener("storage", onStorage);
  };
}
