"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { ApiError } from "@/lib/api/client";
import {
  contentTokenRefreshDelayMs,
  createContentToken,
  lessonContentUrl,
  type ContentTokenResponse,
} from "@/lib/api/content";
import { listSections, type LessonResponse, type SectionResponse } from "@/lib/api/courses";
import { formatDuration } from "@/components/course/CourseCard";
import { learnHref, locateLesson } from "@/lib/learn/navigation";
import { useSectionQuizzes, type SectionQuiz } from "@/lib/learn/useSectionQuizzes";
import { useCourseProgress, type ProgressState } from "@/lib/learn/useCourseProgress";
import { useAuth } from "@/lib/auth/AuthProvider";
import { ProgressBar, formatPercent } from "@/components/progress/ProgressBar";
import styles from "../player.module.css";
import { Loading } from "@/components/common/Loading";

type Sections =
  | { key: string; kind: "ok"; sections: SectionResponse[] }
  | { key: string; kind: "notFound" }
  | { key: string; kind: "error"; message: string };

function parseId(raw: string | undefined): number | null {
  return raw !== undefined && /^\d+$/.test(raw) ? Number(raw) : null;
}

function errorMessage(err: unknown, fallback: string): string {
  return err instanceof ApiError && err.body?.message ? err.body.message : fallback;
}

/**
 * Ders oynatıcı (#45): solda bölüm/ders listesi, ortada içerik, altta önceki/sonraki ders.
 * Liste `GET /api/courses/{id}/sections`'tan gelir; içerik token'lı adresten açılır (ADR-012).
 * Kursiyer için (#46): kurs ve bölüm ilerleme çubukları, tamamlanan derslerde tik ve "Tamamlandı" düğmesi; ikisi de
 * `GET /api/me/courses/{id}/progress` ve tamamlama yanıtından gelir. Eğitmen ve yönetici önizlerken görünmez.
 * Her bölümün altında o bölümün sınav bağlantıları ve kursiyerin durumu (#304, `SectionQuizLinks`).
 */
export function LessonPlayerView() {
  const params = useParams<{ courseId: string; lessonId: string }>();
  const courseId = parseId(params.courseId);
  const lessonId = parseId(params.lessonId);
  const [retry, setRetry] = useState(0);
  const [loaded, setLoaded] = useState<Sections | null>(null);
  const [listOpen, setListOpen] = useState(false);
  const { user } = useAuth();
  const sectionQuizzes = useSectionQuizzes(
    loaded?.kind === "ok" ? loaded.sections.map((s) => s.id) : [],
    user?.role === "STUDENT",
  );
  const { state: progress, complete } = useCourseProgress(courseId, user?.role === "STUDENT");
  const done = new Set(progress.kind === "ok" ? progress.progress.completedLessonIds : []);
  const sectionPercent = new Map(
    progress.kind === "ok" ? progress.progress.sections.map((s) => [s.sectionId, s.progressPercent] as const) : [],
  );
  const key = `${params.courseId}#${retry}`;
  const state: Sections | null =
    courseId === null ? { key, kind: "notFound" } : loaded?.key === key ? loaded : null;

  useEffect(() => {
    if (courseId === null) return;
    let cancelled = false;
    listSections(courseId)
      .then((sections) => !cancelled && setLoaded({ key, kind: "ok", sections }))
      .catch((err: unknown) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 404) setLoaded({ key, kind: "notFound" });
        else
          setLoaded({
            key,
            kind: "error",
            message: errorMessage(err, "Ders listesi yüklenemedi. Bağlantınızı kontrol edip tekrar deneyin."),
          });
      });
    return () => {
      cancelled = true;
    };
  }, [courseId, key]);

  if (state === null) return <Loading text="Ders yükleniyor…" />;
  if (state.kind === "notFound") return <NotFound text="Kurs bulunamadı." />;
  if (state.kind === "error") {
    return (
      <div role="alert">
        <p className={styles.error}>{state.message}</p>
        <button type="button" className={styles.button} onClick={() => setRetry((r) => r + 1)}>
          Tekrar dene
        </button>
      </div>
    );
  }

  const position = lessonId === null ? null : locateLesson(state.sections, lessonId);
  if (courseId === null || position === null) return <NotFound text="Ders bulunamadı." />;
  const { lesson, previous, next } = position;

  return (
    <div className={styles.layout}>
      <aside className={styles.sidebar}>
        <button
          type="button"
          className={styles.listToggle}
          aria-expanded={listOpen}
          aria-controls="lesson-list"
          onClick={() => setListOpen((o) => !o)}
        >
          {listOpen ? "Ders listesini kapat" : "Ders listesi"}
        </button>
        {progress.kind === "ok" && (
          <div className={styles.courseProgress}>
            <p>
              Kurs ilerlemesi: <strong>{formatPercent(progress.progress.progressPercent)}</strong>
            </p>
            <ProgressBar value={progress.progress.progressPercent} label="Kurs ilerlemesi" />
          </div>
        )}
        <nav
          id="lesson-list"
          aria-label="Bölümler ve dersler"
          className={`${styles.list} ${listOpen ? styles.listOpen : ""}`}
        >
          {state.sections.map((s) => (
            <div key={s.id} className={styles.section}>
              <h2 className={styles.sectionTitle}>
                {s.title}
                {sectionPercent.has(s.id) && (
                  <span className={styles.sectionPercent}> {formatPercent(sectionPercent.get(s.id) ?? 0)}</span>
                )}
              </h2>
              {sectionPercent.has(s.id) && (
                <div className={styles.sectionBar}>
                  <ProgressBar value={sectionPercent.get(s.id) ?? 0} label={`${s.title} bölüm ilerlemesi`} small />
                </div>
              )}
              <ol className={styles.lessons}>
                {s.lessons.map((l) => (
                  <li key={l.id}>
                    <Link
                      href={learnHref(courseId, l.id)}
                      aria-current={l.id === lesson.id ? "page" : undefined}
                      className={l.id === lesson.id ? styles.current : undefined}
                      onClick={() => setListOpen(false)}
                    >
                      <span>
                        {done.has(l.id) && (
                          <span className={styles.tick} aria-label="tamamlandı" role="img">
                            ✓{" "}
                          </span>
                        )}
                        {l.title}
                      </span>
                      <span className={styles.lessonMeta}>
                        {TYPE_LABEL[l.contentType]}
                        {l.contentType === "VIDEO" && l.durationSeconds > 0 && ` · ${formatDuration(l.durationSeconds)}`}
                      </span>
                    </Link>
                  </li>
                ))}
              </ol>
              <SectionQuizLinks quizzes={sectionQuizzes.get(s.id)} courseId={courseId} />
            </div>
          ))}
        </nav>
      </aside>
      <section className={styles.main}>
        <h1>{lesson.title}</h1>
        <LessonContent key={lesson.id} courseId={courseId} lesson={lesson} />
        <CompleteButton key={`complete-${lesson.id}`} lessonId={lesson.id} progress={progress} onComplete={complete} />
        <nav aria-label="Ders gezinmesi" className={styles.pager}>
          {previous ? (
            <Link href={learnHref(courseId, previous.id)} className={styles.button} rel="prev">
              ← Önceki: {previous.title}
            </Link>
          ) : (
            <span />
          )}
          {next && (
            <Link href={learnHref(courseId, next.id)} className={styles.button} rel="next">
              Sonraki: {next.title} →
            </Link>
          )}
        </nav>
        <p>
          <Link className="touch-link" href={`/courses/${courseId}`}>Kurs sayfasına dön</Link>
        </p>
      </section>
    </div>
  );
}

const QUIZ_STATUS: Record<SectionQuiz["status"], string> = {
  passed: "✓ Geçildi",
  failed: "Denendi, geçilmedi",
  notAttempted: "",
};

/**
 * Bölümün sınav bağlantıları (#304): yalnız kursa kayıtlı kursiyerde (`useSectionQuizzes`). Sınav sayfasına kurs id'si
 * de gönderilir ki sonuç ekranındaki "Kursa dön" buraya geri getirsin (#58).
 */
function SectionQuizLinks({ quizzes, courseId }: { quizzes: SectionQuiz[] | undefined; courseId: number }) {
  if (!quizzes || quizzes.length === 0) return null;
  return (
    <ul className={styles.quizzes} aria-label="Bölüm sınavları">
      {quizzes.map(({ quiz, status }) => (
        <li key={quiz.id}>
          <Link href={`/quizzes/${quiz.id}?course=${courseId}`}>
            <span>📝 {quiz.title}</span>
            <span className={styles.lessonMeta}>
              Sınav{quiz.required ? " · Zorunlu" : ""}
              {QUIZ_STATUS[status] && (
                <span className={status === "passed" ? styles.quizPassed : undefined}> · {QUIZ_STATUS[status]}</span>
              )}
            </span>
          </Link>
        </li>
      ))}
    </ul>
  );
}

/**
 * "Tamamlandı" düğmesi (#46): yalnız kursa kayıtlı kursiyerde. Tamamlanmış derste pasif; basınca sunucunun döndürdüğü
 * ilerlemeyle çubuklar ve tikler güncellenir.
 */
function CompleteButton({
  lessonId,
  progress,
  onComplete,
}: {
  lessonId: number;
  progress: ProgressState;
  onComplete: (lessonId: number) => Promise<void>;
}) {
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  if (progress.kind !== "ok") return null;
  const completed = progress.progress.completedLessonIds.includes(lessonId);

  async function mark() {
    setPending(true);
    setError(null);
    try {
      await onComplete(lessonId);
    } catch (err) {
      setError(errorMessage(err, "Ders tamamlanamadı. Bağlantınızı kontrol edip tekrar deneyin."));
    } finally {
      setPending(false);
    }
  }

  return (
    <div className={styles.complete}>
      <button
        type="button"
        className={`${styles.button} ${completed ? styles.completed : styles.primary}`}
        disabled={completed || pending}
        onClick={() => void mark()}
      >
        {completed ? "✓ Tamamlandı" : pending ? "İşaretleniyor…" : "Tamamlandı olarak işaretle"}
      </button>
      {error && (
        <p role="alert" className={styles.error}>
          {error}
        </p>
      )}
    </div>
  );
}

const TYPE_LABEL: Record<LessonResponse["contentType"], string> = {
  VIDEO: "Video",
  PDF: "PDF",
  DOCUMENT: "Doküman",
};

function NotFound({ text }: { text: string }) {
  return (
    <div role="alert">
      <p className={styles.error}>{text}</p>
      <Link href="/">Kurslara dön</Link>
    </div>
  );
}

type TokenState =
  | { kind: "ok"; token: ContentTokenResponse }
  | { kind: "forbidden" }
  | { kind: "notFound" }
  | { kind: "error"; message: string };

/**
 * Dersin içeriği. Önce içerik token'ı alınır (403 → kayıt ekranı, 404 → içerik yok); sonra türüne göre video
 * oynatıcı, yeni sekmede açılan PDF ya da indirme bağlantısı gösterilir.
 */
function LessonContent({ courseId, lesson }: { courseId: number; lesson: LessonResponse }) {
  const [state, setState] = useState<TokenState | null>(null);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    if (!lesson.hasContent) return;
    let cancelled = false;
    createContentToken(lesson.id)
      .then((token) => !cancelled && setState({ kind: "ok", token }))
      .catch((err: unknown) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 403) setState({ kind: "forbidden" });
        else if (err instanceof ApiError && err.status === 404) setState({ kind: "notFound" });
        else
          setState({
            kind: "error",
            message: errorMessage(err, "Ders içeriği açılamadı. Bağlantınızı kontrol edip tekrar deneyin."),
          });
      });
    return () => {
      cancelled = true;
    };
  }, [lesson.id, lesson.hasContent, attempt]);

  // PDF ve doküman bağlantısı tıklandığında geçerli olsun diye token süresi dolmadan yenilenir. Videoda yenileme
  // yalnız oynatma hata verince yapılır; adres değişince video baştan yüklenir (VideoPlayer).
  const token = state?.kind === "ok" ? state.token : null;
  useEffect(() => {
    if (token === null || lesson.contentType === "VIDEO") return;
    const timer = setTimeout(() => setAttempt((a) => a + 1), contentTokenRefreshDelayMs(token));
    return () => clearTimeout(timer);
  }, [token, lesson.contentType]);

  if (!lesson.hasContent || state?.kind === "notFound") {
    return <p className={styles.status}>Bu dersin içeriği henüz yüklenmemiş.</p>;
  }
  if (state === null) return <p className={styles.status}>İçerik hazırlanıyor…</p>;
  if (state.kind === "forbidden") {
    return (
      <div role="alert" className={styles.locked}>
        <p>Bu derse erişmek için kursa kaydolun.</p>
        <Link href={`/courses/${courseId}`} className={`${styles.button} ${styles.primary}`}>
          Kurs sayfasına git
        </Link>
      </div>
    );
  }
  if (state.kind === "error") {
    return (
      <div role="alert">
        <p className={styles.error}>{state.message}</p>
        <button type="button" className={styles.button} onClick={() => setAttempt((a) => a + 1)}>
          Tekrar dene
        </button>
      </div>
    );
  }

  const url = lessonContentUrl(lesson.id, state.token.token);
  if (lesson.contentType === "VIDEO") {
    return <VideoPlayer url={url} title={lesson.title} onExpired={() => refreshToken(lesson.id, setState)} />;
  }
  if (lesson.contentType === "PDF") {
    return (
      <div className={styles.file}>
        <p>Bu ders bir PDF. Tarayıcınızın PDF görüntüleyicisinde yeni sekmede açılır.</p>
        <a href={url} target="_blank" rel="noopener noreferrer" className={`${styles.button} ${styles.primary}`}>
          PDF&apos;i aç
        </a>
      </div>
    );
  }
  return (
    <div className={styles.file}>
      <p>Bu ders bir doküman. Bilgisayarınıza indirilir.</p>
      <a href={url} className={`${styles.button} ${styles.primary}`}>
        Dokümanı indir
      </a>
    </div>
  );
}

/** Yeni token alır; ok değilse durumu hata ekranına çevirir. */
function refreshToken(lessonId: number, setState: (s: TokenState) => void): Promise<boolean> {
  return createContentToken(lessonId)
    .then((token) => {
      setState({ kind: "ok", token });
      return true;
    })
    .catch((err: unknown) => {
      if (err instanceof ApiError && err.status === 403) setState({ kind: "forbidden" });
      else setState({ kind: "error", message: errorMessage(err, "Video oynatılamadı. Tekrar deneyin.") });
      return false;
    });
}

/**
 * HTML5 video; tarayıcı Range isteğiyle ileri sarar. Token'ın süresi dolduktan sonraki istek 401 alır ve video
 * hata verir: o zaman yeni token alınır, video kaldığı saniyeden (ve oynuyorsa oynayarak) devam eder.
 * Yeni adres de yüklenemezse ya da hata çözme (decode) hatasıysa gerçek bir oynatma hatasıdır; yeniden denenmez.
 */
function VideoPlayer({ url, title, onExpired }: { url: string; title: string; onExpired: () => Promise<boolean> }) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const resume = useRef<{ time: number; playing: boolean } | null>(null);
  const retrying = useRef(false);
  const [failed, setFailed] = useState(false);

  function onError() {
    const video = videoRef.current;
    if (video === null) return;
    if (retrying.current || video.error?.code === MediaError.MEDIA_ERR_DECODE) {
      setFailed(true);
      return;
    }
    retrying.current = true;
    resume.current = { time: video.currentTime, playing: !video.paused };
    void onExpired();
  }

  function onLoadedMetadata() {
    retrying.current = false;
    const video = videoRef.current;
    const at = resume.current;
    if (video === null || at === null) return;
    resume.current = null;
    video.currentTime = at.time;
    if (at.playing) void video.play().catch(() => {});
  }

  if (failed) {
    return (
      <p role="alert" className={styles.error}>
        Video oynatılamadı. Sayfayı yenileyip tekrar deneyin.
      </p>
    );
  }
  return (
    <video
      ref={videoRef}
      className={styles.video}
      src={url}
      controls
      preload="metadata"
      aria-label={title}
      onError={onError}
      onLoadedMetadata={onLoadedMetadata}
    />
  );
}
