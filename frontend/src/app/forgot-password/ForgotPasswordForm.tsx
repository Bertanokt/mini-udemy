"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { ApiError } from "@/lib/api/client";
import { requestPasswordReset } from "@/lib/api/auth";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * Parola yenileme isteği (#117, `POST /api/auth/password-reset/request` #115). Sunucu e-posta kayıtlı olsun olmasın
 * aynı yanıtı verir; ekran da aynı mesajı gösterir, böylece hangi adresin kayıtlı olduğu anlaşılmaz.
 */
export function ForgotPasswordForm() {
  const [email, setEmail] = useState("");
  const [fieldError, setFieldError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [sent, setSent] = useState(false);
  const [sending, setSending] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    const value = email.trim();
    if (!value) return setFieldError("E-posta zorunludur.");
    if (!EMAIL_PATTERN.test(value)) return setFieldError("Geçerli bir e-posta adresi girin.");
    setFieldError(null);
    setSending(true);
    try {
      await requestPasswordReset(value);
      setSent(true);
    } catch (e) {
      if (e instanceof ApiError) {
        setFieldError(e.body?.errors?.email ?? null);
        setError(e.body?.message ?? "İstek gönderilemedi. Lütfen tekrar deneyin.");
      } else {
        setError("Sunucuya ulaşılamadı. Bağlantınızı kontrol edip tekrar deneyin.");
      }
    } finally {
      setSending(false);
    }
  }

  if (sent) {
    return (
      <div className="auth-form">
        <p className="auth-notice" role="status">
          İsteğiniz alındı. Bu e-posta adresi kayıtlıysa parola yenileme bağlantısı oluşturuldu; bağlantı 30 dakika
          geçerli ve tek kullanımlıktır.
        </p>
        <p className="auth-switch">
          <Link className="touch-link" href="/login">Girişe dön</Link>
        </p>
      </div>
    );
  }

  return (
    <form className="auth-form" onSubmit={onSubmit} noValidate>
      <p className="field-hint">Hesabınızın e-posta adresini yazın; parolanızı yenilemek için bir bağlantı oluşturulur.</p>
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
          aria-invalid={fieldError ? true : undefined}
        />
        {fieldError && <span className="field-error">{fieldError}</span>}
      </label>
      <button type="submit" disabled={sending}>
        {sending ? "Gönderiliyor…" : "Bağlantı iste"}
      </button>
      <p className="auth-switch">
        <Link className="touch-link" href="/login">Girişe dön</Link>
      </p>
    </form>
  );
}
