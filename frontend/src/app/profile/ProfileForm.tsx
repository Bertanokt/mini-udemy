"use client";

import { useState, type FormEvent } from "react";
import { ApiError } from "@/lib/api/client";
import { updateMyProfile, type UserResponse } from "@/lib/api/auth";
import { useAuth } from "@/lib/auth/AuthProvider";
import { validateProfile, type ProfileErrors } from "@/lib/auth/validation";
import { Loading } from "@/components/common/Loading";

const ROLE_LABEL: Record<UserResponse["role"], string> = {
  STUDENT: "Kursiyer",
  INSTRUCTOR: "Eğitmen",
  ADMIN: "Yönetici",
};

const BIO_MAX = 500;

/**
 * Profil formu (#117, `PUT /api/me` #116): ad soyad ve kısa biyografi değişir; e-posta ve rol yalnız gösterilir.
 * Kaydedince başlıktaki ad da güncellenir (`updateUser`).
 */
export function ProfileForm() {
  const { user } = useAuth();
  if (!user) return <Loading text="Profil yükleniyor…" />;
  // Kullanıcı değişirse (başka hesapla giriş) form baştan kurulur.
  return <Form key={user.id} user={user} />;
}

function Form({ user }: { user: UserResponse }) {
  const { updateUser } = useAuth();
  const [fullName, setFullName] = useState(user.fullName);
  const [bio, setBio] = useState(user.bio ?? "");
  const [errors, setErrors] = useState<ProfileErrors>({});
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [saving, setSaving] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaved(false);
    setError(null);
    const found = validateProfile(fullName, bio);
    setErrors(found);
    if (Object.keys(found).length > 0) return;
    setSaving(true);
    try {
      const updated = await updateMyProfile({ fullName: fullName.trim(), bio: bio.trim() || null });
      updateUser(updated);
      setFullName(updated.fullName);
      setBio(updated.bio ?? "");
      setSaved(true);
    } catch (e) {
      if (e instanceof ApiError) {
        if (e.status === 400 && e.body?.errors) setErrors({ fullName: e.body.errors.fullName, bio: e.body.errors.bio });
        setError(e.body?.message ?? "Profil kaydedilemedi. Lütfen tekrar deneyin.");
      } else {
        setError("Sunucuya ulaşılamadı. Bağlantınızı kontrol edip tekrar deneyin.");
      }
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="auth-form" onSubmit={onSubmit} noValidate>
      {saved && !error && (
        <p className="auth-notice" role="status">
          Profiliniz güncellendi.
        </p>
      )}
      {error && (
        <p className="auth-error" role="alert">
          {error}
        </p>
      )}
      <dl className="profile-fixed">
        <dt>E-posta</dt>
        <dd>{user.email}</dd>
        <dt>Hesap türü</dt>
        <dd>{ROLE_LABEL[user.role]}</dd>
      </dl>
      <label>
        Ad soyad
        <input
          name="fullName"
          autoComplete="name"
          required
          maxLength={100}
          value={fullName}
          onChange={(e) => setFullName(e.target.value)}
          aria-invalid={errors.fullName ? true : undefined}
        />
        {errors.fullName && <span className="field-error">{errors.fullName}</span>}
      </label>
      <label>
        Kısa biyografi
        <textarea
          name="bio"
          rows={4}
          value={bio}
          onChange={(e) => setBio(e.target.value)}
          aria-invalid={errors.bio ? true : undefined}
          aria-describedby="bio-count"
        />
        <span id="bio-count" className="field-hint">
          {bio.length} / {BIO_MAX}
        </span>
        {errors.bio && <span className="field-error">{errors.bio}</span>}
      </label>
      <button type="submit" disabled={saving}>
        {saving ? "Kaydediliyor…" : "Kaydet"}
      </button>
    </form>
  );
}
