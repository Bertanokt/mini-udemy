import type { Metadata } from "next";
import { QuestionBank } from "./QuestionBank";

export const metadata: Metadata = {
  title: "Soru bankası · Mini Udemy",
};

// Sayfa, oturum bilgisini bekleyen RequireRole'un altında; anlık gezinme doğrulamasından bilerek çıkarıldı.
export const instant = false;

/** Eğitmen paneli (#38) bu sayfaya henüz bağlanmıyor; yalnız doğrudan adresle ulaşılır. */
export default function QuestionBankPage() {
  return <QuestionBank />;
}
