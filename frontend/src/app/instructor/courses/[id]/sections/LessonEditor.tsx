"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import { ApiError, isAbortError } from "@/lib/api/client";
import {
  contentTokenRefreshDelayMs,
  createContentToken,
  lessonContentUrl,
  uploadLessonContent,
  type ContentTokenResponse,
} from "@/lib/api/content";
import type { ContentType, LessonResponse } from "@/lib/api/courses";
import { createLesson, updateLesson } from "@/lib/api/instructor";
import {
  CONTENT_RULES,
  CONTENT_TYPES,
  CONTENT_TYPE_LABELS,
  LESSON_TITLE_MAX,
  emptyLessonDraft,
  formatDuration,
  formatSize,
  fromLessonServerErrors,
  hasLessonErrors,
  lessonDraftOf,
  toLessonRequest,
  validateContentFile,
  validateLesson,
  type LessonDraft,
  type LessonErrors,
} from "@/lib/course/lessonValidation";
import { apiMessage, isConflict, type Permissions } from "./shared";

/**
 * Ders ekleme (`lesson` null) ve düzenleme formu: başlık, içerik türü, zorunlu işareti, süre (#34).
 * 409 (kurs bu arada düzenlenemez duruma geçti) yöneticiye iletilir; diğer hatalar formda gösterilir.
 */
export function LessonForm({
  sectionId,
  lesson,
  onSaved,
  onCancel,
  onConflict,
}: {
  sectionId: number;
  lesson: LessonResponse | null;
  onSaved: (lesson: LessonResponse) => void;
  onCancel: () => void;
  onConflict: (e: unknown) => void;
}) {
  const [draft, setDraft] = useState<LessonDraft>(() => (lesson ? lessonDraftOf(lesson) : emptyLessonDraft()));
  const [errors, setErrors] = useState<LessonErrors>({});
  const [message, setMessage] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const heading = lesson ? "Dersi düzenle" : "Yeni ders";
  const detachWarning = lesson !== null && lesson.hasContent && draft.contentType !== lesson.contentType;

  function setField<K extends keyof LessonDraft>(key: K, value: LessonDraft[K]) {
    setDraft((d) => ({ ...d, [key]: value }));
    const errorKey = key === "minutes" || key === "seconds" ? "durationSeconds" : key;
    setErrors((e) => ({ ...e, [errorKey]: undefined }));
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage(null);
    const clientErrors = validateLesson(draft);
    setErrors(clientErrors);
    if (hasLessonErrors(clientErrors)) {
      setMessage("Girilen bilgilerde hata var.");
      return;
    }
    setSaving(true);
    try {
      const request = toLessonRequest(draft);
      const saved = lesson ? await updateLesson(lesson.id, request) : await createLesson(sectionId, request);
      onSaved(saved);
    } catch (e) {
      if (isConflict(e)) {
        onConflict(e);
        onCancel();
        return;
      }
      if (e instanceof ApiError) setErrors(fromLessonServerErrors(e.body?.errors));
      setMessage(apiMessage(e, lesson ? "Ders kaydedilemedi." : "Ders eklenemedi."));
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="qb-form sl-lesson-form" onSubmit={onSubmit} noValidate aria-label={heading}>
      <h3 className="sl-form-title">{heading}</h3>
      {message && (
        <p className="auth-error" role="alert">
          {message}
        </p>
      )}
      <fieldset className="qb-options cf-fields" disabled={saving}>
        <label>
          Başlık
          <input
            name="title"
            value={draft.title}
            maxLength={LESSON_TITLE_MAX}
            onChange={(e) => setField("title", e.target.value)}
            aria-invalid={errors.title ? true : undefined}
            autoFocus
          />
          {errors.title && <span className="field-error">{errors.title}</span>}
        </label>
        <div className="cf-row">
          <label>
            İçerik türü
            <select
              name="contentType"
              value={draft.contentType}
              onChange={(e) => setField("contentType", e.target.value as ContentType)}
              aria-invalid={errors.contentType ? true : undefined}
            >
              {CONTENT_TYPES.map((t) => (
                <option key={t} value={t}>
                  {CONTENT_TYPE_LABELS[t]} ({CONTENT_RULES[t].hint})
                </option>
              ))}
            </select>
            {errors.contentType && <span className="field-error">{errors.contentType}</span>}
          </label>
          <fieldset className="sl-duration">
            <legend>Süre</legend>
            <label>
              <input
                name="minutes"
                inputMode="numeric"
                value={draft.minutes}
                onChange={(e) => setField("minutes", e.target.value)}
                aria-invalid={errors.durationSeconds ? true : undefined}
                aria-label="Süre, dakika"
              />
              dk
            </label>
            <label>
              <input
                name="seconds"
                inputMode="numeric"
                value={draft.seconds}
                onChange={(e) => setField("seconds", e.target.value)}
                aria-invalid={errors.durationSeconds ? true : undefined}
                aria-label="Süre, saniye"
              />
              sn
            </label>
            {errors.durationSeconds && <span className="field-error">{errors.durationSeconds}</span>}
          </fieldset>
        </div>
        {detachWarning && (
          <p className="sl-warn" role="status">
            İçerik türü değişince bu derse yüklenmiş dosya dersten ayrılır; yeni türe uygun dosyayı yeniden yüklemen gerekir.
          </p>
        )}
        <label className="sl-check">
          <input
            type="checkbox"
            name="mandatory"
            checked={draft.mandatory}
            onChange={(e) => setField("mandatory", e.target.checked)}
          />
          Zorunlu ders (sertifika için tamamlanması gerekir)
        </label>
        {errors.mandatory && <span className="field-error">{errors.mandatory}</span>}
      </fieldset>
      <div className="qb-actions">
        <button type="submit" className="qb-btn qb-primary" disabled={saving}>
          {saving ? "Kaydediliyor…" : lesson ? "Kaydet" : "Dersi ekle"}
        </button>
        <button type="button" className="qb-btn" onClick={onCancel} disabled={saving}>
          Vazgeç
        </button>
      </div>
    </form>
  );
}

/** Ders satırı: bilgiler, sıralama/düzenle/sil, içerik yükleme (ilerleme göstergeli) ve önizleme. */
export function LessonRow({
  lesson,
  first,
  last,
  permissions,
  busy,
  onMove,
  onEdit,
  onDelete,
  onUploaded,
  onConflict,
}: {
  lesson: LessonResponse;
  first: boolean;
  last: boolean;
  permissions: Permissions;
  busy: boolean;
  onMove: (delta: -1 | 1) => void;
  onEdit: () => void;
  onDelete: () => void;
  onUploaded: (lesson: LessonResponse) => void;
  onConflict: (e: unknown) => void;
}) {
  const [progress, setProgress] = useState<number | null>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [uploading, setUploading] = useState<File | null>(null);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [version, setVersion] = useState(0);
  const fileInput = useRef<HTMLInputElement>(null);
  const uploadAbort = useRef<AbortController | null>(null);

  // Sayfadan çıkınca ya da ders/bölüm silinince süren yükleme kesilir (#317). Next önceki sayfayı kaldırmadan gizli
  // tutsa da (cacheComponents) efekt temizliği çalışır.
  useEffect(() => () => uploadAbort.current?.abort(), []);

  const rule = CONTENT_RULES[lesson.contentType];
  const canUpload = permissions.editable || (permissions.canUploadNew && !lesson.hasContent);
  const locked = busy || uploading !== null;

  async function upload(file: File | null) {
    if (fileInput.current) fileInput.current.value = "";
    if (!file) return;
    const problem = validateContentFile(lesson.contentType, file);
    setUploadError(problem);
    if (problem) return;
    setUploading(file);
    setProgress(0);
    setPreviewOpen(false);
    const controller = new AbortController();
    uploadAbort.current = controller;
    try {
      const updated = await uploadLessonContent(lesson.id, file, setProgress, controller.signal);
      setVersion((v) => v + 1);
      onUploaded(updated);
    } catch (e) {
      // İptal yalnız sayfadan çıkınca olur, hata değildir. Gizli tutulan sayfanın durumu korunduğu için burada mesaj
      // yazılsaydı kullanıcı geri dönünce görürdü.
      if (isAbortError(e)) return;
      if (isConflict(e)) onConflict(e);
      else setUploadError(apiMessage(e, "İçerik yüklenemedi. Lütfen tekrar deneyin."));
    } finally {
      uploadAbort.current = null;
      setUploading(null);
      setProgress(null);
    }
  }

  return (
    <li className="qz-row sl-lesson">
      <div className="sl-lesson-main">
        <span className="qz-pos" aria-hidden="true">
          {lesson.position + 1}.
        </span>
        <div className="qz-row-text">
          <strong>{lesson.title}</strong>
          <div className="course-tags">
            <span className="tag">{CONTENT_TYPE_LABELS[lesson.contentType]}</span>
            <span className="tag">{lesson.mandatory ? "Zorunlu" : "İsteğe bağlı"}</span>
            {lesson.durationSeconds > 0 && <span className="tag">{formatDuration(lesson.durationSeconds)}</span>}
            <span className={lesson.hasContent ? "tag sl-ok" : "tag sl-missing"}>
              {lesson.hasContent ? "✓ İçerik yüklü" : "⚠ İçerik yok"}
            </span>
          </div>
        </div>
        {permissions.editable && (
          <span className="qb-actions">
            <button
              type="button"
              className="qb-btn"
              onClick={() => onMove(-1)}
              disabled={locked || first}
              aria-label={`${lesson.title} dersini yukarı taşı`}
            >
              ↑
            </button>
            <button
              type="button"
              className="qb-btn"
              onClick={() => onMove(1)}
              disabled={locked || last}
              aria-label={`${lesson.title} dersini aşağı taşı`}
            >
              ↓
            </button>
            <button type="button" className="qb-btn" onClick={onEdit} disabled={locked}>
              Düzenle
            </button>
            <button
              type="button"
              className="qb-btn qb-danger"
              onClick={onDelete}
              disabled={locked}
              aria-label={`${lesson.title} dersini sil`}
            >
              Sil
            </button>
          </span>
        )}
      </div>

      <div className="sl-content">
        {canUpload && (
          <label className="sl-file">
            {lesson.hasContent ? "Dosyayı değiştir" : "İçerik yükle"} ({rule.hint})
            <input
              ref={fileInput}
              type="file"
              accept={rule.accept}
              onChange={(e) => void upload(e.target.files?.[0] ?? null)}
              disabled={locked}
              aria-invalid={uploadError ? true : undefined}
            />
          </label>
        )}
        {uploading && progress !== null && (
          <div className="sl-progress" role="status">
            <progress max={100} value={progress} aria-label={`${uploading.name} yükleniyor`} />
            <span>
              {uploading.name} ({formatSize(uploading.size)}) — %{progress}
              {progress === 100 && ", sunucu dosyayı denetliyor…"}
            </span>
          </div>
        )}
        {uploadError && (
          <span className="field-error" role="alert">
            {uploadError}
          </span>
        )}
        {lesson.hasContent && !uploading && (
          <button type="button" className="qb-btn sl-preview-toggle" onClick={() => setPreviewOpen((o) => !o)}>
            {previewOpen ? "Önizlemeyi kapat" : "Önizle"}
          </button>
        )}
        {previewOpen && lesson.hasContent && <ContentPreview key={`${lesson.id}-${version}`} lesson={lesson} />}
      </div>
    </li>
  );
}

type Preview =
  | { kind: "loading" }
  | { kind: "ok"; token: ContentTokenResponse; url: string }
  | { kind: "error"; message: string };

/**
 * Yüklenen içeriğin önizlemesi: içerik token'ı alınır (eğitmen kendi kursunun içeriğini açabilir, ADR-012).
 * Video sayfada oynatılır. PDF yeni sekmede açılır: backend'in güvenlik başlığı (X-Frame-Options) başka adresteki
 * sayfanın içine gömülmesine izin vermez; kursiyer oynatıcısı da PDF'i aynı yolla açar. Doküman indirilir.
 */
function ContentPreview({ lesson }: { lesson: LessonResponse }) {
  const [state, setState] = useState<Preview>({ kind: "loading" });
  const [attempt, setAttempt] = useState(0);
  const [videoFailed, setVideoFailed] = useState(false);

  // Yenilemede eski bağlantı yeni token gelene kadar görünür kalır ("hazırlanıyor"a dönülmez).
  useEffect(() => {
    let cancelled = false;
    createContentToken(lesson.id)
      .then((token) => !cancelled && setState({ kind: "ok", token, url: lessonContentUrl(lesson.id, token.token) }))
      .catch((e: unknown) => !cancelled && setState({ kind: "error", message: apiMessage(e, "Önizleme açılamadı.") }));
    return () => {
      cancelled = true;
    };
  }, [lesson.id, attempt]);

  // PDF ve doküman bağlantısı tıklandığında geçerli olsun diye token süresi dolmadan yenilenir; kural oynatıcıyla
  // aynıdır (#317). Video yenilenmez: adres değişince video baştan yüklenir.
  const token = state.kind === "ok" ? state.token : null;
  useEffect(() => {
    if (token === null || lesson.contentType === "VIDEO") return;
    const timer = setTimeout(() => setAttempt((a) => a + 1), contentTokenRefreshDelayMs(token));
    return () => clearTimeout(timer);
  }, [token, lesson.contentType]);

  if (state.kind === "loading") return <p className="qz-muted">Önizleme hazırlanıyor…</p>;
  if (state.kind === "error") {
    return (
      <p className="auth-error" role="alert">
        {state.message}
      </p>
    );
  }
  if (lesson.contentType === "VIDEO") {
    if (videoFailed) {
      return (
        <p className="auth-error" role="alert">
          Video oynatılamadı. Önizlemeyi kapatıp yeniden açın.
        </p>
      );
    }
    return (
      <video
        className="sl-video"
        src={state.url}
        controls
        preload="metadata"
        aria-label={`${lesson.title} önizlemesi`}
        onError={() => setVideoFailed(true)}
      />
    );
  }
  if (lesson.contentType === "PDF") {
    return (
      <a className="qb-btn qb-primary sl-open" href={state.url} target="_blank" rel="noopener noreferrer">
        PDF&apos;i yeni sekmede aç
      </a>
    );
  }
  return (
    <a className="qb-btn qb-primary sl-open" href={state.url}>
      Dokümanı indir
    </a>
  );
}
