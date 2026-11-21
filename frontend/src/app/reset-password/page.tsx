import type { Metadata } from "next";
import { Suspense } from "react";
import { ResetPasswordForm } from "./ResetPasswordForm";
import { Loading } from "@/components/common/Loading";

export const metadata: Metadata = {
  title: "Yeni parola · Mini Udemy",
};

export default function ResetPasswordPage() {
  return (
    <section className="auth-page">
      <h1>Yeni parola</h1>
      {/* Form `?token=` parametresini okur (useSearchParams); bu kısım istemcide çizilir. */}
      <Suspense fallback={<Loading />}>
        <ResetPasswordForm />
      </Suspense>
    </section>
  );
}
