import type { Metadata } from "next";
import { QuizList } from "./QuizList";

export const metadata: Metadata = {
  title: "Bölüm sınavları · Mini Udemy",
};

// Sayfa, oturum bilgisini bekleyen RequireRole'un altında; anlık gezinme doğrulamasından bilerek çıkarıldı.
export const instant = false;

/** Eğitmen panelindeki (#38) her kurs satırından bağlanır (#50). */
export default function QuizzesPage() {
  return <QuizList />;
}
