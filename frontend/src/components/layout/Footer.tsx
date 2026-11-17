"use client";

import { usePathname } from "next/navigation";

/** Footer'ın gösterilmediği sayfalar. */
const HIDDEN_ON = new Set(["/login"]);

export function Footer() {
  const pathname = usePathname();
  if (HIDDEN_ON.has(pathname)) {
    return null;
  }
  return (
    <footer className="site-footer">
      <div className="container">Mini Udemy · Yazılım Mimarisi dersi projesi</div>
    </footer>
  );
}
