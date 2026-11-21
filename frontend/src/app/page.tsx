import { Suspense } from "react";
import { CatalogView } from "./CatalogView";

export default function Home() {
  // Katalog filtreleri URL'den okunduğu için istemcide çizilir; kabuk önceden hazırlanır.
  return (
    <Suspense fallback={<p className="catalog-status">Kurslar yükleniyor…</p>}>
      <CatalogView />
    </Suspense>
  );
}
