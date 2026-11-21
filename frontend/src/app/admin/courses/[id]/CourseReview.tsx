"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { formatDuration } from "@/components/course/CourseCard";
import { LEVEL_LABELS } from "@/lib/api/catalog";
import { API_BASE_URL, ApiError } from "@/lib/api/client";
import { getCourse, type CourseDetailResponse, type LessonResponse } from "@/lib/api/courses";
import { STATUS_LABELS, type CourseResponse } from "@/lib/api/instructor";
import { learnHref } from "@/lib/learn/navigation";
import {
  CourseActionButtons,
  CourseActionDialog,
  actionDoneMessage,
  type CourseAction,
  type CourseActionTarget,
} from "../CourseActionDialog";
import { Loading } from "@/components/common/Loading";

type Load =
  | { key: string; kind: "ok"; course: CourseDetailResponse }
  | { key: string; kind: "notFound" }
  | { key: string; kind: "error"; message: string };

const TYPE_LABELS: Record<LessonResponse["contentType"], string> = {
  VIDEO: "Video",
  PDF: "PDF",
  DOCUMENT: "Doküman",
};

function parseId(raw: string | undefined): number | null {
  return raw !== undefined && /^\d+$/.test(raw) ? Number(raw) : null;
}

/**
 * Kurs inceleme (#62, UC-34): yönetici kursu yayımlamadan önce açıklamasını, hedeflerini, bölüm ve derslerini görür;
 * her dersin içeriğini mevcut ders oynatıcısında yeni sekmede açar (içerik ucu yöneticiye açıktır, ilerleme yazılmaz).
 * Detay `GET /api/courses/{id}`'den gelir; yayımlanmamış kurs yöneticiye görünür (CourseVisibility).
 */
export function CourseReview() {
  const params = useParams<{ id: string }>();
  const courseId = parseId(params.id);
  const [reload, setReload] = useState(0);
  const [loaded, setLoaded] = useState<Load | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [target, setTarget] = useState<CourseActionTarget | null>(null);
  const key = `${params.id}#${reload}`;
  const state: Load | null = courseId === null ? { key, kind: "notFound" } : loaded?.key === key ? loaded : null;

  useEffect(() => {
    if (courseId === null) return;
    let cancelled = false;
    getCourse(courseId)
      .then((course) => !cancelled && setLoaded({ key, kind: "ok", course }))
      .catch((err: unknown) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 404) setLoaded({ key, kind: "notFound" });
        else
          setLoaded({
            key,
            kind: "error",
            message:
              err instanceof ApiError && err.body?.message
                ? err.body.message
                : "Kurs yüklenemedi. Bağlantınızı kontrol edip tekrar deneyin.",
          });
      });
    return () => {
      cancelled = true;
    };
  }, [courseId, key]);

  // 409/404'te kurs bu arada değişmiştir; pencere hata mesajını gösterirken sayfa yeniden yüklenmesin (pencere de
  // kapanırdı), kapatılınca yüklensin.
  const [stale, setStale] = useState(false);
  function closeDialog() {
    setTarget(null);
    if (stale) {
      setStale(false);
      setReload((n) => n + 1);
    }
  }

  function onDone(updated: CourseResponse, action: CourseAction) {
    setTarget(null);
    setNotice(actionDoneMessage(action));
    setLoaded((cur) =>
      cur?.kind === "ok" ? { ...cur, course: { ...cur.course, status: updated.status, title: updated.title } } : cur,
    );
  }

  const back = (
    <Link href="/admin/courses" className="qb-btn">
      ← Kurs onayı listesi
    </Link>
  );

  if (state === null) return <Loading text="Kurs yükleniyor…" />;
  if (state.kind === "notFound") {
    return (
      <section className="qb" role="alert">
        <p className="auth-error">Kurs bulunamadı.</p>
        {back}
      </section>
    );
  }
  if (state.kind === "error") {
    return (
      <section className="qb" role="alert">
        <p className="auth-error">{state.message}</p>
        <div className="adm-actions">
          <button type="button" className="qb-btn" onClick={() => setReload((n) => n + 1)}>
            Tekrar dene
          </button>
          {back}
        </div>
      </section>
    );
  }

  const { course } = state;
  const lessonCount = course.sections.reduce((n, s) => n + s.lessons.length, 0);
  const missing = course.sections.reduce((n, s) => n + s.lessons.filter((l) => !l.hasContent).length, 0);
  const cover = course.coverUrl ? `${API_BASE_URL}${course.coverUrl}` : null;

  return (
    <section className="qb adm-review">
      <div className="qb-head">
        <h1>{course.title}</h1>
        {back}
      </div>

      {notice && (
        <p className="qb-notice" role="status">
          {notice}
        </p>
      )}

      <div className="adm-summary">
        <span className={`ic-badge ic-${course.status.toLowerCase()}`}>{STATUS_LABELS[course.status]}</span>
        <span>Eğitmen: {course.instructor.fullName}</span>
        <span className="tag">{course.category.name}</span>
        <span className="tag">{LEVEL_LABELS[course.level]}</span>
        <span>
          {course.sections.length} bölüm · {lessonCount} ders · {formatDuration(course.totalDurationSeconds)}
        </span>
      </div>

      <div className="adm-actions">
        <CourseActionButtons
          course={course}
          onSelect={(t) => {
            setNotice(null);
            setTarget(t);
          }}
        />
      </div>

      {cover && (
        <div
          className="course-cover adm-cover"
          role="img"
          aria-label={`${course.title} kapak görseli`}
          style={{ backgroundImage: `url("${cover}")` }}
        />
      )}

      <h2>Açıklama</h2>
      <p className="adm-text">{course.description}</p>

      <h2>Öğrenme hedefleri</h2>
      {course.learningOutcomes.length > 0 ? (
        <ul className="adm-outcomes">
          {course.learningOutcomes.map((o, i) => (
            <li key={i}>{o}</li>
          ))}
        </ul>
      ) : (
        <p className="qb-state">Hedef girilmemiş.</p>
      )}

      <h2>Bölümler ve dersler</h2>
      {missing > 0 && (
        <p className="adm-warn" role="note">
          {missing} dersin içeriği yüklenmemiş.
        </p>
      )}
      {course.sections.length === 0 && <p className="qb-state">Kursta bölüm yok.</p>}
      {course.sections.map((s) => (
        <div key={s.id} className="adm-section">
          <h3>
            {s.position + 1}. {s.title}
          </h3>
          {s.lessons.length === 0 ? (
            <p className="qb-state">Bu bölümde ders yok.</p>
          ) : (
            <ol className="adm-lessons">
              {s.lessons.map((l) => (
                <li key={l.id} className="adm-lesson">
                  <span className="adm-lesson-title">{l.title}</span>
                  <span className="adm-lesson-meta">
                    {TYPE_LABELS[l.contentType]}
                    {l.contentType === "VIDEO" && l.durationSeconds > 0 && ` · ${formatDuration(l.durationSeconds)}`}
                    {l.mandatory ? " · zorunlu" : " · isteğe bağlı"}
                  </span>
                  {l.hasContent ? (
                    <Link
                      href={learnHref(course.id, l.id)}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="qb-btn"
                      aria-label={`${l.title}: içeriği yeni sekmede aç`}
                    >
                      İçeriği aç
                    </Link>
                  ) : (
                    <span className="adm-warn">İçerik yok</span>
                  )}
                </li>
              ))}
            </ol>
          )}
        </div>
      ))}

      {target && (
        <CourseActionDialog
          key={`${target.course.id}-${target.action}`}
          target={target}
          onClose={closeDialog}
          onDone={onDone}
          onStale={() => setStale(true)}
        />
      )}
    </section>
  );
}
