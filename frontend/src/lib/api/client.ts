/**
 * Backend'e giden tüm istekler bu katmandan geçer.
 * Sayfalar/bileşenler fetch'i doğrudan çağırmaz; endpoint modülleri (health.ts, ileride auth.ts, courses.ts ...)
 * bu dosyadaki apiFetch'i kullanır.
 */

export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

/** Backend'in döndüğü hata gövdesi. Sözleşme docs/api/openapi.yaml'da netleşecek (bkz. global exception handler issue'su). */
export interface ApiErrorBody {
  status: number;
  message: string;
  errors?: Record<string, string>;
}

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly body?: ApiErrorBody,
  ) {
    super(body?.message ?? `İstek başarısız: ${status}`);
    this.name = "ApiError";
  }
}

type TokenProvider = () => string | null;

let tokenProvider: TokenProvider = () => null;

/** Auth katmanı (auth context) giriş yapıldığında token kaynağını buraya bağlar. */
export function setTokenProvider(provider: TokenProvider): void {
  tokenProvider = provider;
}

export async function apiFetch<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body !== undefined && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const token = tokenProvider();
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, { ...init, headers });

  if (!response.ok) {
    let body: ApiErrorBody | undefined;
    try {
      body = (await response.json()) as ApiErrorBody;
    } catch {
      body = undefined;
    }
    throw new ApiError(response.status, body);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}
