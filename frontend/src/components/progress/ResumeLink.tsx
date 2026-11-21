"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { getMyResumePoint, type ResumeResponse } from "@/lib/api/progress";
import { learnHref } from "@/lib/learn/navigation";
import { formatPercent } from "./ProgressBar";
import styles from "./progress.module.css";

type Loaded = { courseId: number; result: ResumeResponse | "error" };

/**
 * "Kaldığın yerden devam et" (#120, Senaryo 3): kursun devam noktasını (`GET /api/me/courses/{id}/resume`) alır ve
 * oynatıcıda doğrudan o dersi açar; yanında son dersin adı ve kurs yüzdesi yazar. Hiç ders açılmamışsa ilk
 * tamamlanmamış derse "Derse başla" der ("Sıradaki ders"). Yüklenirken ya da hata olursa kursun ilk dersine giden bağlantı kalır.
 */
export function ResumeLink({ courseId, buttonClassName }: { courseId: number; buttonClassName?: string }) {
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const resume = loaded?.courseId === courseId ? loaded.result : null;

  useEffect(() => {
    let cancelled = false;
    getMyResumePoint(courseId)
      .then((result) => !cancelled && setLoaded({ courseId, result }))
      .catch(() => !cancelled && setLoaded({ courseId, result: "error" }));
    return () => {
      cancelled = true;
    };
  }, [courseId]);

  if (resume === null || resume === "error") {
    return (
      <Link href={learnHref(courseId)} className={buttonClassName}>
        Derse git
      </Link>
    );
  }
  if (resume.lessonId == null) {
    return <p className={styles.resumeMeta}>Bu kursta henüz ders yok.</p>;
  }
  const started = resume.lastAccessedAt != null;
  return (
    <div className={styles.resume}>
      <Link href={learnHref(courseId, resume.lessonId)} className={buttonClassName}>
        {started ? "Kaldığın yerden devam et" : "Derse başla"}
      </Link>
      <p className={styles.resumeMeta}>
        {started ? "Son ders" : "Sıradaki ders"}: <strong>{resume.lessonTitle}</strong> · {formatPercent(resume.progressPercent)}{" "}
        tamamlandı
      </p>
    </div>
  );
}
