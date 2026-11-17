import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Eğitmen paneli · Mini Udemy",
};

/** Yer tutucu (#18): giriş sonrası yönlendirme adresi. Asıl panel #38'de bu dosyanın üstüne yazılacak. */
export default function Page() {
  return (
    <section className="placeholder-page">
      <h1>Eğitmen paneli</h1>
      <p>Panel yakında.</p>
    </section>
  );
}
