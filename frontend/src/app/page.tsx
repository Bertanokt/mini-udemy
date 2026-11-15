import { Suspense } from "react";
import { CatalogView } from "./CatalogView";
import { HealthStatus } from "./HealthStatus";

export default function Home() {
  return (
    <>
      {/* Katalog filtreleri URL'den okunduğu için istemcide çizilir; kabuk önceden hazırlanır. */}
      <Suspense fallback={<p className="catalog-status">Kurslar yükleniyor…</p>}>
        <CatalogView />
      </Suspense>
      <HealthStatus />
    </>
  );
}
