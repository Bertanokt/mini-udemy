import type { NextConfig } from "next";

const isDev = process.env.NODE_ENV === "development";
const apiOrigin = new URL(process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080").origin;

/**
 * Content-Security-Policy, nonce'suz biçim (ADR-007).
 *
 * - Next.js App Router sayfaya kendi satır içi betiklerini gömdüğü için `'unsafe-inline'` gerekli. Nonce, projede
 *   açık olan cacheComponents / kısmi ön-render ile uyumsuz; `script-src 'self'` + SRI ise uygulamayı çalıştırmadı
 *   (17.11.2026'da ölçüldü).
 * - **Kalan risk:** enjekte edilen satır içi betik çalışabilir. Engellenenler: dış kaynaktan betik (script-src),
 *   başka sitelere fetch/XHR (connect-src), eklentiler (object-src), sitenin başka sayfaya gömülmesi
 *   (frame-ancestors).
 * - XSS'e karşı asıl koruma React'in kaçışlaması ve `react/no-danger` yasağı (eslint.config.mjs). Token
 *   localStorage'da durduğu için kullanıcı içeriği her zaman metin olarak basılır (ADR-002).
 * - Geliştirmede React hata ayıklaması için 'unsafe-eval' gerekir (Next.js CSP rehberi).
 * - API (`NEXT_PUBLIC_API_URL`) ayrı bir origin'de: istekler (connect-src), kapak görselleri (img-src) ve ders
 *   videoları (media-src) oradan gelir. `upgrade-insecure-requests` yok: demo HTTP ile çalışıyor.
 */
const csp = [
  "default-src 'self'",
  `script-src 'self' 'unsafe-inline'${isDev ? " 'unsafe-eval'" : ""}`,
  "style-src 'self' 'unsafe-inline'",
  `img-src 'self' blob: data: ${apiOrigin}`,
  `media-src 'self' ${apiOrigin}`,
  `connect-src 'self' ${apiOrigin}`,
  "font-src 'self'",
  "object-src 'none'",
  "base-uri 'self'",
  "form-action 'self'",
  "frame-ancestors 'none'",
].join("; ");

const nextConfig: NextConfig = {
  // Docker imajı için yalnız çalışma zamanı dosyalarını üretir (.next/standalone).
  output: "standalone",
  cacheComponents: true,
  partialPrefetching: true,
  async headers() {
    return [
      {
        source: "/(.*)",
        headers: [{ key: "Content-Security-Policy", value: csp }],
      },
    ];
  },
};

export default nextConfig;
