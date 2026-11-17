"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import type { Role } from "@/lib/api/auth";
import { useAuth } from "@/lib/auth/AuthProvider";
import { HOME_BY_ROLE } from "@/lib/auth/routes";

/** Rolün kendi sayfası: eğitmen/yönetici paneli, öğrencinin kayıtlı kursları (#37). */
const PANEL_LINKS: Partial<Record<Role, { href: string; label: string }>> = {
  STUDENT: { href: "/my-courses", label: "Kayıtlı kurslarım" },
  INSTRUCTOR: { href: HOME_BY_ROLE.INSTRUCTOR, label: "Eğitmen paneli" },
  ADMIN: { href: HOME_BY_ROLE.ADMIN, label: "Yönetici paneli" },
};

/**
 * Üst menü (#19). Girişsizken Giriş/Kayıt; girişliyken rolün paneli, bildirim alanı yeri, kullanıcı adı ve Çıkış.
 * 600 px altında menü düğmesiyle açılıp kapanır.
 */
export function Header() {
  const { status, user, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const [open, setOpen] = useState(false);
  const close = () => setOpen(false);
  // Çıkış korumalı bir sayfadayken (/admin, /instructor) yapılırsa önce ana sayfaya gidilir, oturum ana sayfaya
  // varınca kapatılır. Önce kapatılsaydı sayfanın koruması (RequireRole) kullanıcıyı /login'e gönderirdi.
  const logoutPending = useRef(false);

  useEffect(() => {
    if (logoutPending.current && pathname === "/") {
      logoutPending.current = false;
      logout();
    }
  }, [pathname, logout]);

  function onLogout() {
    close();
    if (pathname === "/") {
      logout();
      return;
    }
    logoutPending.current = true;
    router.replace("/");
  }

  const panel = user ? PANEL_LINKS[user.role] : undefined;

  return (
    <header className="site-header">
      <div className="container header-inner">
        <Link href="/" className="brand" onClick={close}>
          Mini Udemy
        </Link>
        <button
          type="button"
          className="nav-toggle"
          aria-expanded={open}
          aria-controls="site-nav"
          onClick={() => setOpen((v) => !v)}
        >
          {open ? "Kapat" : "Menü"}
        </button>
        <nav id="site-nav" className={open ? "nav open" : "nav"} aria-label="Ana menü">
          <Link href="/" onClick={close}>
            Kurslar
          </Link>
          {status === "anonymous" && (
            <>
              <Link href="/login" onClick={close}>
                Giriş
              </Link>
              <Link href="/register" onClick={close}>
                Kayıt
              </Link>
            </>
          )}
          {status === "authenticated" && user && (
            <>
              {panel && (
                <Link href={panel.href} onClick={close}>
                  {panel.label}
                </Link>
              )}
              {/* Bildirim zili ve okunmamış sayısı #136'da buraya gelecek. */}
              <span className="nav-notifications" data-slot="notifications" />
              <span className="nav-user" title={user.email}>
                {user.fullName}
              </span>
              <button type="button" className="nav-logout" onClick={onLogout}>
                Çıkış
              </button>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}
