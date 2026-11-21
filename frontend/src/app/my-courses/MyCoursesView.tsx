"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { API_BASE_URL, ApiError } from "@/lib/api/client";
import { listMyEnrollments, type EnrollmentResponse } from "@/lib/api/enrollments";
import { formatDuration } from "@/components/course/CourseCard";
import { learnHref } from "@/lib/learn/navigation";
import { ResumeLink } from "@/components/progress/ResumeLink";
import styles from "./my-courses.module.css";
import { Loading } from "@/components/common/Loading";
import { ErrorMessage } from "@/components/common/ErrorMessage";
import { EmptyState } from "@/components/common/EmptyState";

type Result =
  | { kind: "ok"; items: EnrollmentResponse[] }
  | { kind: "error"; message: string };

type Loaded = { key: number; result: Result };

/** Kaldığı yere gidilir: son izlenen ders varsa oynatıcıda o ders (#45), yoksa kurs detayı. */
export function resumeHref(e: EnrollmentResponse): string {
  return e.lastLessonId != null
    ? learnHref(e.course.id, e.lastLessonId)
    : `/courses/${e.course.id}`;
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("tr-TR", { day: "numeric", month: "long", year: "numeric" });
}

/**
 * Kayıtlı kurslar paneli (#37). Sayfa RequireRole ile yalnız STUDENT'a açılır (layout.tsx).
 * Her kurs için kapak, başlık, ilerleme çubuğu ve "Kaldığın yerden devam et" (son dersin adı ve yüzdesiyle, #120)
 * gösterilir. Devam noktası her kurs için ayrı istekle gelir (`ResumeLink`).
 */
export function MyCoursesView() {
  const [retry, setRetry] = useState(0);
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const result = loaded?.key === retry ? loaded.result : null;

  useEffect(() => {
    let cancelled = false;
    listMyEnrollments()
      .then((items) => {
        if (!cancelled) setLoaded({ key: retry, result: { kind: "ok", items } });
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        const message =
          err instanceof ApiError && err.body?.message
            ? err.body.message
            : "Kurslarınız yüklenemedi. Bağlantınızı kontrol edip tekrar deneyin.";
        setLoaded({ key: retry, result: { kind: "error", message } });
      });
    return () => {
      cancelled = true;
    };
  }, [retry]);

  return (
    <section>
      <h1>Kayıtlı kurslarım</h1>
      <p>
        <Link href="/certificates">Sertifikalarım</Link>
      </p>
      {result === null && <Loading text="Kurslarınız yükleniyor…" />}
      {result?.kind === "error" && <ErrorMessage message={result.message} onRetry={() => setRetry((r) => r + 1)} />}
      {result?.kind === "ok" && result.items.length === 0 && (
        <EmptyState message="Henüz bir kursa kayıt olmadınız." action={{ label: "Kurslara göz at", href: "/" }} />
      )}
      {result?.kind === "ok" && result.items.length > 0 && (
        <ul className={styles.list}>
          {result.items.map((e) => (
            <EnrollmentItem key={e.id} enrollment={e} />
          ))}
        </ul>
      )}
    </section>
  );
}

function EnrollmentItem({ enrollment: e }: { enrollment: EnrollmentResponse }) {
  const course = e.course;
  const cover = course.coverUrl ? `${API_BASE_URL}${course.coverUrl}` : null;
  const percent = Math.max(0, Math.min(100, Math.round(e.progressPercent)));
  const started = e.lastLessonId != null;
  return (
    <li className={styles.item}>
      <div
        className={styles.cover}
        role="img"
        aria-label={`${course.title} kapak görseli`}
        style={cover ? { backgroundImage: `url("${cover}")` } : undefined}
      >
        {!cover && course.title.charAt(0)}
      </div>
      <div className={styles.body}>
        <h2 className={styles.title}>
          <Link href={resumeHref(e)}>{course.title}</Link>
        </h2>
        <p className={styles.meta}>
          {course.instructor.fullName} · {course.lessonCount} ders · {formatDuration(course.totalDurationSeconds)}
        </p>
        <div
          className={styles.bar}
          role="progressbar"
          aria-label={`${course.title} ilerlemesi`}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-valuenow={percent}
          aria-valuetext={`Yüzde ${percent} tamamlandı`}
        >
          <div className={styles.fill} style={{ width: `${percent}%` }} />
        </div>
        <p className={styles.meta}>
          %{percent} tamamlandı
          {started && e.lastAccessedAt && ` · Son izleme: ${formatDate(e.lastAccessedAt)}`}
        </p>
        <ResumeLink courseId={course.id} buttonClassName={styles.button} />
      </div>
    </li>
  );
}
