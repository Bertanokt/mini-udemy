import type { Metadata } from "next";
import { Suspense } from "react";
import { LoginForm } from "./LoginForm";

export const metadata: Metadata = {
  title: "Giriş yap · Mini Udemy",
};

export default function LoginPage() {
  return (
    <section className="auth-page">
      <h1>Giriş yap</h1>
      {/* LoginForm `?next=` parametresini okur (useSearchParams); bu kısım istemcide çizilir. */}
      <Suspense fallback={<p className="auth-status">Yükleniyor…</p>}>
        <LoginForm />
      </Suspense>
    </section>
  );
}
