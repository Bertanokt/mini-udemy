"use client";

import { useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { register, type RegisterRequest, type RegisterRole } from "@/lib/api/auth";
import { ApiError } from "@/lib/api/client";
import { useAuth } from "@/lib/auth/AuthProvider";
import { validateRegister, type RegisterErrors, type RegisterField } from "@/lib/auth/validation";

const ROLE_OPTIONS: { value: RegisterRole; label: string }[] = [
  { value: "STUDENT", label: "Öğrenci" },
  { value: "INSTRUCTOR", label: "Eğitmen" },
];

const FIELDS: RegisterField[] = ["fullName", "email", "password", "role"];

/**
 * Kayıt formu (#17).
 * - Gönderilmeden önce istemci tarafında doğrulanır (validation.ts; kurallar backend'le aynı).
 * - Sunucunun 400 ve 409 hataları ilgili alanın altında, genel mesaj formun üstünde gösterilir.
 * - Başarılı kayıttan sonra giriş sayfasına gidilir (kayıt token döndürmez; sözleşme).
 * - Giriş yapmış kullanıcı ana sayfaya yönlendirilir.
 */
export function RegisterForm() {
  const router = useRouter();
  const { status } = useAuth();
  const [form, setForm] = useState<RegisterRequest>({ fullName: "", email: "", password: "", role: "STUDENT" });
  const [errors, setErrors] = useState<RegisterErrors>({});
  const [message, setMessage] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (status === "authenticated") {
      router.replace("/");
    }
  }, [status, router]);

  function update<K extends RegisterField>(field: K, value: RegisterRequest[K]) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage(null);
    const clientErrors = validateRegister(form);
    setErrors(clientErrors);
    if (Object.keys(clientErrors).length > 0) {
      setMessage("Girilen bilgilerde hata var.");
      return;
    }
    setSubmitting(true);
    try {
      await register(form);
      router.replace("/login?kayit=tamam");
    } catch (e) {
      setSubmitting(false);
      if (e instanceof ApiError) {
        const serverErrors: RegisterErrors = {};
        for (const field of FIELDS) {
          const text = e.body?.errors?.[field];
          if (text) serverErrors[field] = text;
        }
        setErrors(serverErrors);
        setMessage(e.body?.message ?? "Kayıt yapılamadı. Lütfen tekrar deneyin.");
      } else {
        setMessage("Sunucuya ulaşılamadı. Bağlantınızı kontrol edip tekrar deneyin.");
      }
    }
  }

  if (status !== "anonymous") {
    return <p className="auth-status">Oturum kontrol ediliyor…</p>;
  }

  return (
    <form className="auth-form" onSubmit={onSubmit} noValidate>
      {message && (
        <p className="auth-error" role="alert">
          {message}
        </p>
      )}
      <label>
        Ad soyad
        <input
          name="fullName"
          autoComplete="name"
          required
          maxLength={100}
          value={form.fullName}
          onChange={(e) => update("fullName", e.target.value)}
          aria-invalid={errors.fullName ? true : undefined}
        />
        {errors.fullName && <span className="field-error">{errors.fullName}</span>}
      </label>
      <label>
        E-posta
        <input
          type="email"
          name="email"
          autoComplete="email"
          required
          value={form.email}
          onChange={(e) => update("email", e.target.value)}
          aria-invalid={errors.email ? true : undefined}
        />
        {errors.email && <span className="field-error">{errors.email}</span>}
      </label>
      <label>
        Parola
        <input
          type="password"
          name="password"
          autoComplete="new-password"
          required
          value={form.password}
          onChange={(e) => update("password", e.target.value)}
          aria-invalid={errors.password ? true : undefined}
          aria-describedby="password-hint"
        />
        <span id="password-hint" className="field-hint">
          En az 8 karakter.
        </span>
        {errors.password && <span className="field-error">{errors.password}</span>}
      </label>
      <fieldset className="role-choice">
        <legend>Hesap türü</legend>
        {ROLE_OPTIONS.map((option) => (
          <label key={option.value}>
            <input
              type="radio"
              name="role"
              value={option.value}
              checked={form.role === option.value}
              onChange={() => update("role", option.value)}
            />
            {option.label}
          </label>
        ))}
        {errors.role && <span className="field-error">{errors.role}</span>}
      </fieldset>
      <button type="submit" disabled={submitting}>
        {submitting ? "Kaydediliyor…" : "Kayıt ol"}
      </button>
      <p className="auth-switch">
        Zaten hesabın var mı? <Link href="/login">Giriş yap</Link>
      </p>
    </form>
  );
}
