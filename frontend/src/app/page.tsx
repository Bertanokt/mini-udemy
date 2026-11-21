import { Suspense } from "react";
import { CatalogView } from "./CatalogView";
import { Loading } from "@/components/common/Loading";

export default function Home() {
  // Katalog filtreleri URL'den okunduğu için istemcide çizilir; kabuk önceden hazırlanır.
  return (
    <Suspense fallback={<Loading text="Kurslar yükleniyor…" />}>
      <CatalogView />
    </Suspense>
  );
}
