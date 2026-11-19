import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = {
  title: "Yönetici paneli · Mini Udemy",
};

/** Giriş sonrası yönetici ana sayfası (#18): kurs onayı (#62), kategori (#118), kullanıcı (#63) yönetimi, şikâyetler (#146) ve platform raporu (#147). */
export default function Page() {
  return (
    <section className="placeholder-page">
      <h1>Yönetici paneli</h1>
      <ul>
        <li>
          <Link href="/admin/courses">Kurs onayı</Link>
        </li>
        <li>
          <Link href="/admin/categories">Kategori yönetimi</Link>
        </li>
        <li>
          <Link href="/admin/users">Kullanıcı yönetimi</Link>
        </li>
        <li>
          <Link href="/admin/complaints">Şikâyetler</Link>
        </li>
        <li>
          <Link href="/admin/reports">Platform raporu</Link>
        </li>
      </ul>
      <p>Diğer panel bölümleri yakında.</p>
    </section>
  );
}
