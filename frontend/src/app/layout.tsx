import type { Metadata } from "next";
import { Footer } from "@/components/layout/Footer";
import { Header } from "@/components/layout/Header";
import "./globals.css";

export const metadata: Metadata = {
  title: "Mini Udemy",
  description: "Online kurs platformu",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="tr">
      <body>
        <Header />
        <main className="container site-main">{children}</main>
        <Footer />
      </body>
    </html>
  );
}
