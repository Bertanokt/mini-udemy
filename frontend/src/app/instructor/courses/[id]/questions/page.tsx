import type { Metadata } from "next";
import { QuestionBank } from "./QuestionBank";

export const metadata: Metadata = {
  title: "Soru bankası · Mini Udemy",
};

// Sayfa, oturum bilgisini bekleyen RequireRole'un altında; anlık gezinme doğrulamasından bilerek çıkarıldı.
export const instant = false;

/** Eğitmen panelindeki (#38) her kurs satırından bağlanır. */
export default function QuestionBankPage() {
  return <QuestionBank />;
}
