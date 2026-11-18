import type { Metadata } from "next";
import { Suspense } from "react";
import { QuizTakeView } from "./QuizTakeView";

export const metadata: Metadata = {
  title: "Sınav · Mini Udemy",
};

export default function QuizTakePage() {
  return (
    <Suspense fallback={<p className="auth-status">Sınav yükleniyor…</p>}>
      <QuizTakeView />
    </Suspense>
  );
}
