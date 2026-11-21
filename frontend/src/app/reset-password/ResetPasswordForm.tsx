"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { ApiError } from "@/lib/api/client";
import { confirmPasswordReset } from "@/lib/api/auth";
import { validatePassword } from "@/lib/auth/validation";

type FieldErrors = Partial<Record<"newPassword" | "repeat", string>>;

/**
 * Yeni parola (#117, `POST /api/auth/password-reset/confirm` #115). Token adresten (`?token=`) gelir. Geçersiz,
 * süresi dolmuş ya da kullanılmış bağlantıda sunucunun mesajı ve yeni bağlantı isteme yolu gösterilir.
 */
export function ResetPasswordForm() {
  const token = useSearchParams().get("token") ?? "";
  const [newPassword, setNewPassword] = useState("");
  const [repeat, setRepeat] = useState("");
  const [errors, setErrors] = useState<FieldErrors>({});
  const [error, setError] = useState<string | null>(null);
  const [linkInvalid, setLinkInvalid] = useState(false);
  const [done, setDone] = useState(false);
  const [saving, setSaving] = useState(false);

  if (!token) {
    return <InvalidLink message="Bağlantıda token yok. Parola yenileme bağlantısını eksiksiz açın ya da yeni bir bağlantı isteyin." />;
  }
  if (linkInvalid) return <InvalidLink message={error ?? "Bağlantı geçersiz ya da süresi dolmuş."} />;
  if (done) {
    return (
      <div className="auth-form">
        <p className="auth-notice" role="status">
          Parolanız değiştirildi. Yeni parolanızla giriş yapabilirsiniz.
        </p>
        <p className="auth-switch">
          <Link className="touch-link" href="/login">Giriş yap</Link>
        </p>
      </div>
    );
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    const found: FieldErrors = {};
    const passwordError = validatePassword(newPassword);
    if (passwordError) found.newPassword = passwordError;
    if (repeat !== newPassword) found.repeat = "Parolalar aynı değil.";
    setErrors(found);
    if (Object.keys(found).length > 0) return;
    setSaving(true);
    try {
      await confirmPasswordReset({ token, newPassword });
      // Parola state'te kalmasın (#306).
      setNewPassword("");
      setRepeat("");
      setDone(true);
    } catch (e) {
      if (e instanceof ApiError) {
        setError(e.body?.message ?? "Parola değiştirilemedi. Lütfen tekrar deneyin.");
        if (e.body?.errors?.token) setLinkInvalid(true);
        else if (e.body?.errors?.newPassword) setErrors({ newPassword: e.body.errors.newPassword });
      } else {
        setError("Sunucuya ulaşılamadı. Bağlantınızı kontrol edip tekrar deneyin.");
      }
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="auth-form" onSubmit={onSubmit} noValidate>
      {error && (
        <p className="auth-error" role="alert">
          {error}
        </p>
      )}
      <label>
        Yeni parola
        <input
          type="password"
          name="newPassword"
          autoComplete="new-password"
          required
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          aria-invalid={errors.newPassword ? true : undefined}
        />
        <span className="field-hint">En az 8 karakter.</span>
        {errors.newPassword && <span className="field-error">{errors.newPassword}</span>}
      </label>
      <label>
        Yeni parola (tekrar)
        <input
          type="password"
          name="repeat"
          autoComplete="new-password"
          required
          value={repeat}
          onChange={(e) => setRepeat(e.target.value)}
          aria-invalid={errors.repeat ? true : undefined}
        />
        {errors.repeat && <span className="field-error">{errors.repeat}</span>}
      </label>
      <button type="submit" disabled={saving}>
        {saving ? "Kaydediliyor…" : "Parolayı değiştir"}
      </button>
    </form>
  );
}

function InvalidLink({ message }: { message: string }) {
  return (
    <div className="auth-form">
      <p className="auth-error" role="alert">
        {message}
      </p>
      <p className="auth-switch">
        <Link className="touch-link" href="/forgot-password">Yeni bağlantı iste</Link>
      </p>
    </div>
  );
}
