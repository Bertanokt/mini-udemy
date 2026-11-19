import type { Metadata } from "next";
import { PlatformReport } from "./PlatformReport";

export const metadata: Metadata = {
  title: "Platform raporu · Mini Udemy",
};

// Sayfa, oturum bilgisini bekleyen RequireRole'un (admin/layout.tsx) altında; anlık gezinme doğrulamasından bilerek çıkarıldı.
export const instant = false;

export default function ReportsPage() {
  return <PlatformReport />;
}
