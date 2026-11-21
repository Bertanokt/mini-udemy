import type { Metadata } from "next";
import { CertificatesView } from "./CertificatesView";

export const metadata: Metadata = {
  title: "Sertifikalarım · Mini Udemy",
};

// Sayfa, oturum bilgisini bekleyen RequireRole'un altında; anlık gezinme doğrulamasından bilerek çıkarıldı.
export const instant = false;

export default function CertificatesPage() {
  return <CertificatesView />;
}
