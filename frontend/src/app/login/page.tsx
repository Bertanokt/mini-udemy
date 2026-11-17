import type { Metadata } from "next";
import { LoginForm } from "./LoginForm";

export const metadata: Metadata = {
  title: "Giriş yap · Mini Udemy",
};

export default function LoginPage() {
  return (
    <section className="auth-page">
      <h1>Giriş yap</h1>
      <LoginForm />
    </section>
  );
}
