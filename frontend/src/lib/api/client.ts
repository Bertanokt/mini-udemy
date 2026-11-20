/**
 * Backend'e giden tüm istekler bu katmandan geçer.
 * Sayfalar/bileşenler fetch'i doğrudan çağırmaz; endpoint modülleri (health.ts, auth.ts, catalog.ts ...)
 * bu dosyadaki apiFetch'i kullanır.
 */

export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

/** Backend'in döndüğü hata gövdesi (docs/api/openapi.yaml › ErrorResponse). */
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
let unauthorizedHandler: () => void = () => {};

/** Auth katmanı (AuthProvider) token kaynağını buraya bağlar. */
export function setTokenProvider(provider: TokenProvider): void {
  tokenProvider = provider;
}

/** Token'la giden bir istek 401 alınca çağrılır: auth katmanı token'ı siler ve oturumu kapatır. */
export function setUnauthorizedHandler(handler: () => void): void {
  unauthorizedHandler = handler;
}

async function send(path: string, init: RequestInit, token: string | null): Promise<Response> {
  const headers = new Headers(init.headers);
  // FormData (dosya yükleme) için Content-Type'ı tarayıcı multipart sınırıyla (boundary) kendisi yazar.
  if (init.body !== undefined && !(init.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  } else {
    headers.delete("Authorization");
  }
  return fetch(`${API_BASE_URL}${path}`, { ...init, headers });
}

/**
 * Backend'e istek atar; token varsa `Authorization: Bearer` ekler.
 *
 * Token'la giden istek 401 alırsa (token geçersiz ya da süresi dolmuş) oturum kapatılır ve istek **bir kez**
 * token'sız tekrarlanır. Herkese açık uçlar (katalog vb.) geçersiz token'la da 401 döndüğü için kullanıcı bu
 * sayede sayfayı token'sız görmeye devam eder; korumalı bir uçta tekrar da 401 döner ve hata çağırana iletilir
 * (ADR-002, sözleşme › Kimlik doğrulama).
 */
export async function apiFetch<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const token = tokenProvider();
  let response = await send(path, init, token);

  if (response.status === 401 && token) {
    unauthorizedHandler();
    response = await send(path, init, null);
  }

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

/** Yüklemenin gönderilen kısmı, 0–100 arası tam sayı. */
export type UploadProgress = (percent: number) => void;

function sendUpload(path: string, body: FormData, token: string | null, onProgress?: UploadProgress) {
  return new Promise<{ status: number; text: string }>((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open("POST", `${API_BASE_URL}${path}`);
    if (token) xhr.setRequestHeader("Authorization", `Bearer ${token}`);
    if (onProgress) {
      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable) onProgress(Math.min(100, Math.round((e.loaded / e.total) * 100)));
      };
    }
    xhr.onload = () => resolve({ status: xhr.status, text: xhr.responseText });
    xhr.onerror = () => reject(new TypeError("Ağ hatası"));
    xhr.send(body);
  });
}

/**
 * Çok parçalı (multipart) dosya yükleme; `apiFetch`'in ilerleme bildiren karşılığıdır. `fetch` gönderilen bayt
 * sayısını bildiremediği için `XMLHttpRequest` kullanır. Token, 401'de oturumu kapatma ve `ApiError` davranışı
 * `apiFetch` ile aynıdır; 401'de istek tekrarlanmaz (yükleme uçları herkese açık değildir).
 */
export async function apiUpload<T>(path: string, body: FormData, onProgress?: UploadProgress): Promise<T> {
  const token = tokenProvider();
  const response = await sendUpload(path, body, token, onProgress);
  if (response.status === 401 && token) unauthorizedHandler();

  if (response.status < 200 || response.status >= 300) {
    let errorBody: ApiErrorBody | undefined;
    try {
      errorBody = JSON.parse(response.text) as ApiErrorBody;
    } catch {
      errorBody = undefined;
    }
    throw new ApiError(response.status, errorBody);
  }
  return (response.text ? JSON.parse(response.text) : undefined) as T;
}
