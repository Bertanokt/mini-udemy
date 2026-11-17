"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { API_BASE_URL, ApiError } from "@/lib/api/client";
import { LEVEL_LABELS } from "@/lib/api/catalog";
import { getCourse, type CourseDetailResponse } from "@/lib/api/courses";
import { enrollCourse, listMyEnrollments } from "@/lib/api/enrollments";
import { useAuth } from "@/lib/auth/AuthProvider";
import { loginHref } from "@/lib/auth/routes";
import { formatDuration } from "@/components/course/CourseCard";
import { CourseReviews, type ReviewerState } from "@/components/review/CourseReviews";
import styles from "./course-detail.module.css";

type CourseResult =
  | { kind: "ok"; course: CourseDetailResponse }
  | { kind: "notFound" }
  | { kind: "error"; message: string };

type Loaded = { key: string; result: CourseResult };
/** Kayıt durumu: hangi kullanıcıya ait olduğu ve sonucu; `enrolled` null ise sorgu başarısız. */
type EnrollmentState = { userId: number; enrolled: boolean | null; key: number };

/**
 * Kurs detay sayfası (#36): kurs bilgisi, kazanımlar, bölüm/ders listesi ve kayıt düğmesi.
 * Kayıtlılık `GET /api/me/enrollments`'tan anlaşılır (yalnız giriş yapmış STUDENT için).
 * Ders izleme sayfası henüz yok; "Derse devam et" `/courses/{id}/learn` adresine bağlıdır (yer tutucu).
 */
export function CourseDetailView() {
  const { id: rawId } = useParams<{ id: string }>();
  const id = /^\d+$/.test(rawId) ? Number(rawId) : null;
  const { status, user } = useAuth();
  const [retry, setRetry] = useState(0);
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const key = `${rawId}#${retry}`;
  const result: CourseResult | null =
    id === null ? { kind: "notFound" } : loaded?.key === key ? loaded.result : null;

  const isStudent = status === "authenticated" && user?.role === "STUDENT";
  const [enrollment, setEnrollment] = useState<EnrollmentState | null>(null);
  const [enrollRetry, setEnrollRetry] = useState(0);
  const [enrolling, setEnrolling] = useState(false);
  const [enrollError, setEnrollError] = useState<string | null>(null);
  const [enrolledNow, setEnrolledNow] = useState<number | null>(null);

  useEffect(() => {
    if (id === null) return;
    let cancelled = false;
    getCourse(id)
      .then((course) => {
        if (!cancelled) setLoaded({ key, result: { kind: "ok", course } });
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 404) {
          setLoaded({ key, result: { kind: "notFound" } });
          return;
        }
        const message =
          err instanceof ApiError && err.body?.message
            ? err.body.message
            : "Kurs yüklenemedi. Bağlantınızı kontrol edip tekrar deneyin.";
        setLoaded({ key, result: { kind: "error", message } });
      });
    return () => {
      cancelled = true;
    };
  }, [id, key]);

  const userId = isStudent && user ? user.id : null;
  useEffect(() => {
    if (userId === null || id === null) return;
    let cancelled = false;
    listMyEnrollments()
      .then((list) => {
        if (!cancelled)
          setEnrollment({ userId, key: enrollRetry, enrolled: list.some((e) => e.course.id === id) });
      })
      .catch(() => {
        if (!cancelled) setEnrollment({ userId, key: enrollRetry, enrolled: null });
      });
    return () => {
      cancelled = true;
    };
  }, [userId, id, enrollRetry]);

  async function onEnroll() {
    if (id === null) return;
    setEnrolling(true);
    setEnrollError(null);
    try {
      await enrollCourse(id);
      setEnrolledNow(id);
    } catch (e) {
      if (e instanceof ApiError && e.status === 409) {
        // Zaten kayıtlı: başarılı sayılır.
        setEnrolledNow(id);
      } else if (e instanceof ApiError && e.status === 404) {
        setEnrollError("Bu kurs artık kayıt için uygun değil.");
      } else if (e instanceof ApiError && e.body?.message) {
        setEnrollError(e.body.message);
      } else {
        setEnrollError("Kayıt yapılamadı. Bağlantınızı kontrol edip tekrar deneyin.");
      }
    } finally {
      setEnrolling(false);
    }
  }

  if (result === null) {
    return <p className={styles.status}>Kurs yükleniyor…</p>;
  }
  if (result.kind === "notFound") {
    return (
      <section className="placeholder-page" role="alert">
        <h1>Kurs bulunamadı</h1>
        <p>Aradığınız kurs yok ya da artık yayında değil.</p>
        <p>
          <Link href="/" className="auth-switch">
            Kurslara dön
          </Link>
        </p>
      </section>
    );
  }
  if (result.kind === "error") {
    return (
      <section role="alert">
        <p className={styles.error}>{result.message}</p>
        <button type="button" className={styles.button} style={{ maxWidth: 200 }} onClick={() => setRetry((r) => r + 1)}>
          Tekrar dene
        </button>
      </section>
    );
  }

  const course = result.course;
  const cover = course.coverUrl ? `${API_BASE_URL}${course.coverUrl}` : null;
  const sections = [...course.sections].sort((a, b) => a.position - b.position);

  const enrollmentKnown = enrollment !== null && enrollment.userId === userId && enrollment.key === enrollRetry;
  const enrolled = enrolledNow === course.id || (enrollmentKnown && enrollment.enrolled === true);

  let reviewer: ReviewerState;
  if (status === "loading") reviewer = "loading";
  else if (status === "anonymous") reviewer = "anonymous";
  else if (!isStudent) reviewer = "not-student";
  else if (enrolled) reviewer = "enrolled";
  else if (!enrollmentKnown) reviewer = "loading";
  else reviewer = enrollment.enrolled === null ? "unknown" : "not-enrolled";

  let action: React.ReactNode;
  if (status === "anonymous") {
    action = (
      <Link href={loginHref(`/courses/${course.id}`)} className={styles.button}>
        Kayıt ol
      </Link>
    );
  } else if (status === "loading") {
    action = <p className={styles.status}>Oturum kontrol ediliyor…</p>;
  } else if (!isStudent) {
    action = null;
  } else if (enrolled) {
    action = (
      <Link href={`/courses/${course.id}/learn`} className={styles.button}>
        Derse devam et
      </Link>
    );
  } else if (!enrollmentKnown) {
    action = <p className={styles.status}>Kayıt durumu kontrol ediliyor…</p>;
  } else if (enrollment.enrolled === null) {
    action = (
      <>
        <p className={styles.error} role="alert">
          Kayıt durumunuz alınamadı.
        </p>
        <button type="button" className={styles.button} onClick={() => setEnrollRetry((r) => r + 1)}>
          Tekrar dene
        </button>
      </>
    );
  } else {
    action = (
      <>
        <button type="button" className={styles.button} disabled={enrolling} onClick={onEnroll}>
          {enrolling ? "Kaydediliyor…" : "Kursa kayıt ol"}
        </button>
        {enrollError && (
          <p className={styles.error} role="alert">
            {enrollError}
          </p>
        )}
      </>
    );
  }

  return (
    <article>
      <div className={styles.hero}>
        <div>
          <h1>{course.title}</h1>
          <p className={styles.instructor}>Eğitmen: {course.instructor.fullName}</p>
          <div className={styles.tags}>
            <span className="tag">{course.category.name}</span>
            <span className="tag">{LEVEL_LABELS[course.level]}</span>
          </div>
          <p className="course-meta">
            {course.lessonCount} ders · {formatDuration(course.totalDurationSeconds)}
            {course.averageRating != null && ` · ★ ${course.averageRating.toFixed(1)} (${course.reviewCount})`}
          </p>
          <p className={styles.description}>{course.description}</p>
        </div>
        <aside>
          <div
            className={styles.cover}
            role="img"
            aria-label={`${course.title} kapak görseli`}
            style={cover ? { backgroundImage: `url("${cover}")` } : undefined}
          >
            {!cover && course.title.charAt(0)}
          </div>
          <div className={styles.cta}>{action}</div>
        </aside>
      </div>

      {course.learningOutcomes.length > 0 && (
        <section aria-labelledby="outcomes-title">
          <h2 id="outcomes-title" className={styles["section-title"]}>
            Neler öğreneceksiniz
          </h2>
          <ul className={styles.outcomes}>
            {course.learningOutcomes.map((o, i) => (
              <li key={i}>{o}</li>
            ))}
          </ul>
        </section>
      )}

      <section aria-labelledby="content-title">
        <h2 id="content-title" className={styles["section-title"]}>
          Kurs içeriği
        </h2>
        {sections.length === 0 ? (
          <p className={styles.status}>Bu kursa henüz bölüm eklenmemiş.</p>
        ) : (
          <div className={styles.curriculum}>
            {sections.map((section) => (
              <div key={section.id} className={styles["curriculum-section"]}>
                <h3>{section.title}</h3>
                <ol className={styles.lessons}>
                  {[...section.lessons]
                    .sort((a, b) => a.position - b.position)
                    .map((lesson) => (
                      <li key={lesson.id}>
                        <span>{lesson.title}</span>
                        <span className={styles["lesson-duration"]}>{formatDuration(lesson.durationSeconds)}</span>
                      </li>
                    ))}
                </ol>
              </div>
            ))}
          </div>
        )}
      </section>

      <section aria-labelledby="reviews-title">
        <h2 id="reviews-title" className={styles["section-title"]}>
          Değerlendirmeler
        </h2>
        <CourseReviews courseId={course.id} reviewer={reviewer} userId={user?.id ?? null} />
      </section>
    </article>
  );
}
