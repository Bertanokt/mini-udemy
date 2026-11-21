import type { Metadata } from "next";
import { ForgotPasswordForm } from "./ForgotPasswordForm";

export const metadata: Metadata = {
  title: "Parolamı unuttum · Mini Udemy",
};

export default function ForgotPasswordPage() {
  return (
    <section className="auth-page">
      <h1>Parolamı unuttum</h1>
      <ForgotPasswordForm />
    </section>
  );
}
