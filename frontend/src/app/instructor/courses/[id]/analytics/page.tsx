import type { Metadata } from "next";
import { Analytics } from "./Analytics";

export const metadata: Metadata = {
  title: "Kurs analitiği · Mini Udemy",
};

// Sayfa, oturum bilgisini bekleyen RequireRole'un altında; anlık gezinme doğrulamasından bilerek çıkarıldı.
export const instant = false;

/** Eğitmen panelindeki (#38) her kurs satırından bağlanır. */
export default function AnalyticsPage() {
  return <Analytics />;
}
