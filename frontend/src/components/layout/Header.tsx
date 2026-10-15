import Link from "next/link";

export function Header() {
  return (
    <header className="site-header">
      <div className="container header-inner">
        <Link href="/" className="brand">
          Mini Udemy
        </Link>
        <nav className="nav">
          <Link href="/">Kurslar</Link>
          {/* Giriş / kayıt / rol bazlı menü: auth modülü issue'larında eklenecek */}
        </nav>
      </div>
    </header>
  );
}
