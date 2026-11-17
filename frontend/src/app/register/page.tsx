import type { Metadata } from "next";
import { RegisterForm } from "./RegisterForm";

export const metadata: Metadata = {
  title: "Kayıt ol · Mini Udemy",
};

export default function RegisterPage() {
  return (
    <section className="auth-page">
      <h1>Kayıt ol</h1>
      <RegisterForm />
    </section>
  );
}
