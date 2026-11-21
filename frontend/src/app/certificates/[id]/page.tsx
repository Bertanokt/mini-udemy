import type { Metadata } from "next";
import { Suspense } from "react";
import { CertificateView } from "./CertificateView";

export const metadata: Metadata = {
  title: "Sertifika · Mini Udemy",
};

// Sayfa, oturum bilgisini bekleyen RequireRole'un altında; anlık gezinme doğrulamasından bilerek çıkarıldı.
export const instant = false;

export default function CertificatePage() {
  return (
    <Suspense fallback={<p className="auth-status">Sertifika yükleniyor…</p>}>
      <CertificateView />
    </Suspense>
  );
}
