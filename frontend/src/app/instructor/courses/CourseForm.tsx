"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { API_BASE_URL, ApiError } from "@/lib/api/client";
import { LEVEL_LABELS, listCategories, type CategoryResponse, type CourseLevel, type CourseStatus } from "@/lib/api/catalog";
import { getCourse } from "@/lib/api/courses";
import { STATUS_LABELS, createCourse, updateCourse, uploadCourseCover } from "@/lib/api/instructor";
import {
  COVER_ACCEPT,
  DESCRIPTION_MAX,
  OUTCOMES_MAX,
  OUTCOME_MAX,
  TITLE_MAX,
  emptyCourseDraft,
  fromCourseServerErrors,
  hasCourseErrors,
  isEditableStatus,
  toCourseRequest,
  validateCourse,
  validateCover,
  type CourseDraft,
  type CourseErrors,
} from "@/lib/course/courseValidation";
import { Loading } from "@/components/common/Loading";

const LEVELS = Object.keys(LEVEL_LABELS) as CourseLevel[];

/** Yeni kurs oluşturulduktan sonra düzenleme sayfasına taşınan kapak sonucu (adreste kısa kod). */
const COVER_FAILURE: Record<string, string> = {
  "413": "Kapak görseli en fazla 2 MB olabilir.",
  "415": "Kapak görseli jpg, png ya da webp olmalıdır.",
  failed: "Kapak görseli yüklenemedi. Lütfen tekrar deneyin.",
};

function errorMessage(e: unknown, fallback: string): string {
  if (e instanceof ApiError) {
    if (e.status === 403) return e.body?.message ?? "Bu kursu düzenleme yetkiniz yok. Yalnız kursun sahibi eğitmen düzenleyebilir.";
    if (e.status === 404) return e.body?.message ?? "Kurs bulunamadı.";
    return e.body?.message ?? fallback;
  }
  return "Sunucuya ulaşılamadı. Bağlantınızı kontrol edip tekrar deneyin.";
}

function coverFailureCode(e: unknown): string {
  return e instanceof ApiError && (e.status === 413 || e.status === 415) ? String(e.status) : "failed";
}

interface LoadedCourse {
  status: CourseStatus;
  coverUrl: string | null;
  updatedAt: string;
}

type Load = { state: "loading" } | { state: "error"; message: string } | { state: "ready" };

/**
 * Eğitmen kurs formu (#33): `courseId` yoksa yeni kurs (POST), varsa düzenleme (PUT). Kapak ayrı uçla yüklenir.
 * Kurs düzenlenemez durumdaysa (İncelemede, Yayımlandı) alanlar salt okunur gösterilir; asıl karar sunucudadır.
 */
export function CourseForm({ courseId }: { courseId: number | null }) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  // Oluşturma sonrası yönlendirmeden gelen sonuç ilk durum olarak bir kez okunur; adres aşağıda temizlenir.
  const [arrival] = useState(() => {
    if (searchParams.get("created") !== "1") return null;
    const cover = searchParams.get("cover");
    return cover
      ? {
          message: "Kurs taslak olarak oluşturuldu, ancak kapak görseli yüklenemedi.",
          coverError: COVER_FAILURE[cover] ?? COVER_FAILURE.failed,
        }
      : { notice: "Kurs taslak olarak oluşturuldu. Bölüm ve dersleri ekleyip onaya gönderebilirsin." };
  });

  const [load, setLoad] = useState<Load>({ state: "loading" });
  const [reload, setReload] = useState(0);
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [course, setCourse] = useState<LoadedCourse | null>(null);
  const [draft, setDraft] = useState<CourseDraft>(emptyCourseDraft);
  const [errors, setErrors] = useState<CourseErrors>({});
  const [message, setMessage] = useState<string | null>(arrival?.message ?? null);
  const [notice, setNotice] = useState<string | null>(arrival?.notice ?? null);
  const [submitting, setSubmitting] = useState(false);

  const [coverFile, setCoverFile] = useState<File | null>(null);
  const [coverPreview, setCoverPreview] = useState<string | null>(null);
  const [coverError, setCoverError] = useState<string | null>(arrival?.coverError ?? null);
  const coverInput = useRef<HTMLInputElement>(null);
  const previewUrl = useRef<string | null>(null);

  useEffect(() => {
    if (arrival) router.replace(pathname, { scroll: false });
  }, [arrival, router, pathname]);

  // Önizleme adresi bileşen kapanınca serbest bırakılır.
  useEffect(
    () => () => {
      if (previewUrl.current) URL.revokeObjectURL(previewUrl.current);
    },
    [],
  );

  useEffect(() => {
    let cancelled = false;
    Promise.all([listCategories(), courseId === null ? Promise.resolve(null) : getCourse(courseId)])
      .then(([cats, detail]) => {
        if (cancelled) return;
        setCategories(cats);
        if (detail) {
          setCourse({ status: detail.status, coverUrl: detail.coverUrl, updatedAt: String(Date.now()) });
          setDraft({
            title: detail.title,
            description: detail.description,
            categoryId: String(detail.category.id),
            level: detail.level,
            learningOutcomes: detail.learningOutcomes,
          });
        }
        setLoad({ state: "ready" });
      })
      .catch((e: unknown) => {
        if (!cancelled) setLoad({ state: "error", message: errorMessage(e, "Kurs bilgileri yüklenemedi.") });
      });
    return () => {
      cancelled = true;
    };
  }, [courseId, reload]);

  const readOnly = course !== null && !isEditableStatus(course.status);
  const savedCover = course?.coverUrl ? `${API_BASE_URL}${course.coverUrl}?v=${course.updatedAt}` : null;
  const shownCover = coverPreview ?? savedCover;

  function setField<K extends keyof CourseDraft>(key: K, value: CourseDraft[K]) {
    setDraft((d) => ({ ...d, [key]: value }));
    setErrors((e) => ({ ...e, [key]: undefined }));
  }

  function setOutcomes(change: (outcomes: string[]) => string[]) {
    setDraft((d) => ({ ...d, learningOutcomes: change(d.learningOutcomes) }));
    setErrors((e) => ({ ...e, learningOutcomes: undefined, outcomeItems: undefined }));
  }

  function moveOutcome(index: number, delta: -1 | 1) {
    setOutcomes((list) => {
      const target = index + delta;
      if (target < 0 || target >= list.length) return list;
      const next = [...list];
      [next[index], next[target]] = [next[target], next[index]];
      return next;
    });
  }

  /** Seçili dosyayı ve önizlemesini değiştirir; önceki önizleme adresi serbest bırakılır. */
  function setCover(file: File | null) {
    if (previewUrl.current) URL.revokeObjectURL(previewUrl.current);
    previewUrl.current = file ? URL.createObjectURL(file) : null;
    setCoverFile(file);
    setCoverPreview(previewUrl.current);
  }

  function chooseCover(file: File | null) {
    setNotice(null);
    const problem = file ? validateCover(file) : null;
    setCoverError(problem);
    setCover(problem ? null : file);
    if (problem && coverInput.current) coverInput.current.value = "";
  }

  function clearCover() {
    setCover(null);
    setCoverError(null);
    if (coverInput.current) coverInput.current.value = "";
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (readOnly) return;
    setMessage(null);
    setNotice(null);
    const clientErrors = validateCourse(draft);
    setErrors(clientErrors);
    if (hasCourseErrors(clientErrors) || coverError) {
      setMessage("Girilen bilgilerde hata var.");
      return;
    }
    setSubmitting(true);
    const request = toCourseRequest(draft);

    if (courseId === null) {
      let created;
      try {
        created = await createCourse(request);
      } catch (e) {
        setSubmitting(false);
        if (e instanceof ApiError) setErrors(fromCourseServerErrors(e.body?.errors));
        setMessage(errorMessage(e, "Kurs oluşturulamadı. Lütfen tekrar deneyin."));
        return;
      }
      let cover = "";
      if (coverFile) {
        try {
          await uploadCourseCover(created.id, coverFile);
        } catch (e) {
          cover = `&cover=${coverFailureCode(e)}`;
        }
      }
      router.replace(`/instructor/courses/${created.id}/edit?created=1${cover}`);
      return;
    }

    try {
      let saved = await updateCourse(courseId, request);
      if (coverFile) {
        try {
          saved = await uploadCourseCover(courseId, coverFile);
          clearCover();
        } catch (e) {
          setCourse({ status: saved.status, coverUrl: saved.coverUrl ?? null, updatedAt: saved.updatedAt });
          // Reddedilen dosya seçili kalmaz; sonraki kaydetme yalnız bilgileri gönderir.
          clearCover();
          setCoverError(errorMessage(e, COVER_FAILURE.failed));
          setMessage("Kurs bilgileri kaydedildi, ancak kapak görseli yüklenemedi.");
          return;
        }
      }
      setCourse({ status: saved.status, coverUrl: saved.coverUrl ?? null, updatedAt: saved.updatedAt });
      setNotice("Değişiklikler kaydedildi.");
    } catch (e) {
      if (e instanceof ApiError) setErrors(fromCourseServerErrors(e.body?.errors));
      setMessage(errorMessage(e, "Kurs kaydedilemedi. Lütfen tekrar deneyin."));
      // Kurs bu arada düzenlenemez bir duruma geçtiyse güncel durumu yükle; form kilitlenir.
      if (e instanceof ApiError && e.status === 409) setReload((n) => n + 1);
    } finally {
      setSubmitting(false);
    }
  }

  const heading = courseId === null ? "Yeni kurs" : readOnly ? "Kurs bilgileri" : "Kursu düzenle";

  if (load.state === "loading") return <Loading text="Kurs bilgileri yükleniyor…" />;
  if (load.state === "error") {
    return (
      <section className="qb">
        <h1>{heading}</h1>
        <div className="qb-state" role="alert">
          <p className="auth-error">{load.message}</p>
          <div className="qb-actions">
            <button type="button" className="qb-btn" onClick={() => setReload((n) => n + 1)}>
              Tekrar dene
            </button>
            <Link className="qb-btn" href="/instructor">
              Kurslarıma dön
            </Link>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="qb cf">
      <div className="qb-head">
        <h1>{heading}</h1>
        {course && (
          <span className={`ic-badge ic-${course.status.toLowerCase()}`}>{STATUS_LABELS[course.status]}</span>
        )}
      </div>

      {readOnly && course && (
        <p className="qb-notice cf-locked" role="status">
          Kurs “{STATUS_LABELS[course.status]}” durumunda ve düzenlenemez. Kurs yalnız Taslak ve Yayından kaldırıldı
          durumlarında düzenlenebilir.
        </p>
      )}
      {notice && (
        <p className="qb-notice" role="status">
          {notice}
        </p>
      )}

      <form className="qb-form" onSubmit={onSubmit} noValidate aria-label={heading}>
        {message && (
          <p className="auth-error" role="alert">
            {message}
          </p>
        )}

        <fieldset className="qb-options cf-fields" disabled={readOnly || submitting}>
          <label>
            Başlık
            <input
              name="title"
              maxLength={TITLE_MAX}
              value={draft.title}
              onChange={(e) => setField("title", e.target.value)}
              aria-invalid={errors.title ? true : undefined}
            />
            {errors.title && <span className="field-error">{errors.title}</span>}
          </label>

          <label>
            Açıklama
            <textarea
              name="description"
              rows={6}
              maxLength={DESCRIPTION_MAX}
              value={draft.description}
              onChange={(e) => setField("description", e.target.value)}
              aria-invalid={errors.description ? true : undefined}
            />
            {errors.description && <span className="field-error">{errors.description}</span>}
          </label>

          <div className="cf-row">
            <label>
              Kategori
              <select
                name="categoryId"
                value={draft.categoryId}
                onChange={(e) => setField("categoryId", e.target.value)}
                aria-invalid={errors.categoryId ? true : undefined}
              >
                <option value="">Seçin</option>
                {categories.map((c) => (
                  <option key={c.id} value={String(c.id)}>
                    {c.name}
                  </option>
                ))}
              </select>
              {errors.categoryId && <span className="field-error">{errors.categoryId}</span>}
            </label>

            <label>
              Seviye
              <select
                name="level"
                value={draft.level}
                onChange={(e) => setField("level", e.target.value as CourseLevel | "")}
                aria-invalid={errors.level ? true : undefined}
              >
                <option value="">Seçin</option>
                {LEVELS.map((l) => (
                  <option key={l} value={l}>
                    {LEVEL_LABELS[l]}
                  </option>
                ))}
              </select>
              {errors.level && <span className="field-error">{errors.level}</span>}
            </label>
          </div>

          <fieldset className="qb-options">
            <legend>
              Öğrenme hedefleri ({draft.learningOutcomes.length}/{OUTCOMES_MAX}) — kurs sayfasında bu sırayla görünür
            </legend>
            {draft.learningOutcomes.length === 0 && <p className="qz-muted">Henüz hedef eklenmedi.</p>}
            <ol className="qz-selected">
              {draft.learningOutcomes.map((outcome, index) => {
                const itemError = errors.outcomeItems?.[index];
                return (
                  <li key={index} className={itemError ? "qz-row qz-row-warn" : "qz-row"}>
                    <span className="qz-pos" aria-hidden="true">
                      {index + 1}.
                    </span>
                    <span className="qz-row-text">
                      <input
                        className="cf-outcome"
                        value={outcome}
                        maxLength={OUTCOME_MAX}
                        onChange={(e) => setOutcomes((list) => list.map((o, i) => (i === index ? e.target.value : o)))}
                        aria-label={`${index + 1}. öğrenme hedefi`}
                        aria-invalid={itemError ? true : undefined}
                      />
                      {itemError && <span className="field-error">{itemError}</span>}
                    </span>
                    <span className="qb-actions">
                      <button
                        type="button"
                        className="qb-btn"
                        onClick={() => moveOutcome(index, -1)}
                        disabled={index === 0}
                        aria-label={`${index + 1}. hedefi yukarı taşı`}
                      >
                        ↑
                      </button>
                      <button
                        type="button"
                        className="qb-btn"
                        onClick={() => moveOutcome(index, 1)}
                        disabled={index === draft.learningOutcomes.length - 1}
                        aria-label={`${index + 1}. hedefi aşağı taşı`}
                      >
                        ↓
                      </button>
                      <button
                        type="button"
                        className="qb-btn"
                        onClick={() => setOutcomes((list) => list.filter((_, i) => i !== index))}
                        aria-label={`${index + 1}. hedefi sil`}
                      >
                        Sil
                      </button>
                    </span>
                  </li>
                );
              })}
            </ol>
            {errors.learningOutcomes && <span className="field-error">{errors.learningOutcomes}</span>}
            <button
              type="button"
              className="qb-btn"
              onClick={() => setOutcomes((list) => [...list, ""])}
              disabled={draft.learningOutcomes.length >= OUTCOMES_MAX}
            >
              Hedef ekle
            </button>
          </fieldset>

          <fieldset className="qb-options">
            <legend>Kapak görseli (jpg, png ya da webp; en fazla 2 MB)</legend>
            <div className="cf-cover-row">
              <div
                className="course-cover cf-cover"
                role="img"
                aria-label={
                  coverPreview ? "Seçilen kapak görselinin önizlemesi" : savedCover ? "Kayıtlı kapak görseli" : "Kapak görseli yok"
                }
                style={shownCover ? { backgroundImage: `url("${shownCover}")` } : undefined}
              >
                {!shownCover && <span className="course-cover-placeholder">{draft.title.trim().charAt(0) || "?"}</span>}
              </div>
              <div className="cf-cover-actions">
                <label>
                  {savedCover ? "Yeni kapak seç" : "Kapak seç"}
                  <input
                    ref={coverInput}
                    type="file"
                    name="cover"
                    accept={COVER_ACCEPT}
                    onChange={(e) => chooseCover(e.target.files?.[0] ?? null)}
                    aria-invalid={coverError ? true : undefined}
                  />
                </label>
                {coverFile && (
                  <p className="qz-muted">
                    Seçilen: {coverFile.name}. Kaydedince yüklenir.{" "}
                    <button type="button" className="qb-btn" onClick={clearCover}>
                      Seçimi kaldır
                    </button>
                  </p>
                )}
                {coverError && <span className="field-error">{coverError}</span>}
              </div>
            </div>
          </fieldset>
        </fieldset>

        <div className="qb-actions">
          {!readOnly && (
            <button type="submit" className="qb-btn qb-primary" disabled={submitting}>
              {submitting ? "Kaydediliyor…" : courseId === null ? "Kursu oluştur" : "Kaydet"}
            </button>
          )}
          {courseId !== null && (
            <Link className="qb-btn" href={`/instructor/courses/${courseId}/sections`}>
              Bölüm ve dersler
            </Link>
          )}
          <Link className="qb-btn" href="/instructor">
            {readOnly ? "Kurslarıma dön" : "Vazgeç"}
          </Link>
        </div>
      </form>
    </section>
  );
}
