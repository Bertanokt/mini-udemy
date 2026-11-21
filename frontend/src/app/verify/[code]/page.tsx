import type { Metadata } from "next";
import { Suspense } from "react";
import { VerifyView } from "./VerifyView";

export const metadata: Metadata = {
  title: "Sertifika doğrulama · Mini Udemy",
};

/** Girişsiz sayfa (#131). `params` çalışma zamanında bilinir; kabuk önceden çizilsin diye Suspense içinde çözülür. */
export default function VerifyPage() {
  return (
    <Suspense fallback={<p className="auth-status">Doğrulanıyor…</p>}>
      <VerifyView />
    </Suspense>
  );
}
