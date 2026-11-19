import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = {
  title: "Yönetici paneli · Mini Udemy",
};

/** Yer tutucu (#18): giriş sonrası yönlendirme adresi. Asıl panel #62'de; şimdilik kategori (#118), kullanıcı (#63) yönetimi ve platform raporu (#147) bağlantıları var. */
export default function Page() {
  return (
    <section className="placeholder-page">
      <h1>Yönetici paneli</h1>
      <ul>
        <li>
          <Link href="/admin/categories">Kategori yönetimi</Link>
        </li>
        <li>
          <Link href="/admin/users">Kullanıcı yönetimi</Link>
        </li>
        <li>
          <Link href="/admin/reports">Platform raporu</Link>
        </li>
      </ul>
      <p>Diğer panel bölümleri yakında.</p>
    </section>
  );
}
