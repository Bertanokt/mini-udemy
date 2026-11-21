"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { ApiError } from "@/lib/api/client";
import { useAuth } from "@/lib/auth/AuthProvider";
import { homeForRole, safeNextPath } from "@/lib/auth/routes";
import { Loading } from "@/components/common/Loading";

type FieldErrors = Partial<Record<"email" | "password", string>>;

/**
 * Giriş formu (#18).
 * - Başarılı girişte token AuthProvider'da saklanır. `?next=` geçerli bir site içi adresse oraya (#19), değilse
 *   kullanıcının rolünün sayfasına gidilir (routes.ts).
 * - 401 (yanlış e-posta/parola) ve 403 (pasif hesap) sunucunun mesajıyla, 400 alan hatası ilgili alanın altında gösterilir.
 * - Zaten giriş yapmış kullanıcı `?next=` adresine ya da ana sayfaya yönlendirilir.
 */
export function LoginForm() {
  const router = useRouter();
  const { status, login } = useAuth();
  const searchParams = useSearchParams();
  const next = safeNextPath(searchParams.get("next"));
  // Kayıt sayfasından gelindiyse (#17) bilgi mesajı.
  const justRegistered = searchParams.get("kayit") === "tamam";
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  // Bu formdan yapılan girişte yönlendirmeyi submit belirler (role göre); effect ana sayfaya atmasın.
  const loggingIn = useRef(false);

  useEffect(() => {
    if (status === "authenticated" && !loggingIn.current) {
      router.replace(next ?? "/");
    }
  }, [status, router, next]);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    setFieldErrors({});
    loggingIn.current = true;
    try {
      const user = await login({ email, password });
      // Parola state'te kalmasın: Next.js önceki sayfayı gizli tutar, alanın değeri DOM'da kalırdı (#306).
      setPassword("");
      router.replace(next ?? homeForRole(user.role));
    } catch (e) {
      loggingIn.current = false;
      setSubmitting(false);
      if (e instanceof ApiError) {
        if (e.status === 400 && e.body?.errors) {
          setFieldErrors({ email: e.body.errors.email, password: e.body.errors.password });
        }
        setError(e.body?.message ?? "Giriş yapılamadı. Lütfen tekrar deneyin.");
      } else {
        setError("Sunucuya ulaşılamadı. Bağlantınızı kontrol edip tekrar deneyin.");
      }
    }
  }

  // Giriş sürerken (submitting) form ekranda kalır; aksi hâlde oturum biliniyorsa form gösterilmez.
  if (status !== "anonymous" && !submitting) {
    return <Loading text="Oturum kontrol ediliyor…" />;
  }

  return (
    <form className="auth-form" onSubmit={onSubmit} noValidate>
      {justRegistered && !error && (
        <p className="auth-notice" role="status">
          Kaydınız oluşturuldu. Şimdi giriş yapabilirsiniz.
        </p>
      )}
      {error && (
        <p className="auth-error" role="alert">
          {error}
        </p>
      )}
      <label>
        E-posta
        <input
          type="email"
          name="email"
          autoComplete="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          aria-invalid={fieldErrors.email ? true : undefined}
        />
        {fieldErrors.email && <span className="field-error">{fieldErrors.email}</span>}
      </label>
      <label>
        Parola
        <input
          type="password"
          name="password"
          autoComplete="current-password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          aria-invalid={fieldErrors.password ? true : undefined}
        />
        {fieldErrors.password && <span className="field-error">{fieldErrors.password}</span>}
      </label>
      <button type="submit" disabled={submitting}>
        {submitting ? "Giriş yapılıyor…" : "Giriş yap"}
      </button>
      <p className="auth-switch">
        Hesabın yok mu? <Link href="/register">Kayıt ol</Link>
      </p>
    </form>
  );
}
