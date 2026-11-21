import type { Metadata } from "next";
import { ProfileForm } from "./ProfileForm";

export const metadata: Metadata = {
  title: "Profilim · Mini Udemy",
};

// Sayfa, oturum bilgisini bekleyen RequireRole'un altında; anlık gezinme doğrulamasından bilerek çıkarıldı.
export const instant = false;

export default function ProfilePage() {
  return (
    <section className="auth-page">
      <h1>Profilim</h1>
      <ProfileForm />
    </section>
  );
}
