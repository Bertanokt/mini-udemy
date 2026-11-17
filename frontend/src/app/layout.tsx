import { Suspense } from "react";
import type { Metadata } from "next";
import { Header } from "@/components/layout/Header";
import { AuthProvider } from "@/lib/auth/AuthProvider";
import "./globals.css";

export const metadata: Metadata = {
  title: "Mini Udemy",
  description: "Online kurs platformu",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="tr">
      <body>
        <AuthProvider>
          {/* Header adresi okur (usePathname); `[id]` gibi dinamik yollarda bekleme sınırı gerekir. */}
          <Suspense fallback={<div className="site-header" style={{ height: 65 }} />}>
            <Header />
          </Suspense>
          <main className="container site-main">{children}</main>
        </AuthProvider>
      </body>
    </html>
  );
}
