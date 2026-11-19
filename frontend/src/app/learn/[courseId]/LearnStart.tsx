"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { ApiError } from "@/lib/api/client";
import { listSections } from "@/lib/api/courses";
import { learnHref, orderedLessons } from "@/lib/learn/navigation";
import styles from "./player.module.css";

type Outcome = { key: string; kind: "empty" | "notFound" | "error" };

/** `/learn/{kurs}`: kursun ilk dersine yönlendirir (ders sırası bölüm ve ders `position`'ına göre). */
export function LearnStart() {
  const { courseId: raw } = useParams<{ courseId: string }>();
  const courseId = /^\d+$/.test(raw) ? Number(raw) : null;
  const router = useRouter();
  const [retry, setRetry] = useState(0);
  const [outcome, setOutcome] = useState<Outcome | null>(null);
  const key = `${raw}#${retry}`;
  const shown: Outcome | null = courseId === null ? { key, kind: "notFound" } : outcome?.key === key ? outcome : null;

  useEffect(() => {
    if (courseId === null) return;
    let cancelled = false;
    listSections(courseId)
      .then((sections) => {
        if (cancelled) return;
        const first = orderedLessons(sections)[0];
        if (first) router.replace(learnHref(courseId, first.id));
        else setOutcome({ key, kind: "empty" });
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setOutcome({ key, kind: err instanceof ApiError && err.status === 404 ? "notFound" : "error" });
      });
    return () => {
      cancelled = true;
    };
  }, [courseId, key, router]);

  if (shown === null) return <p className={styles.status}>Ders yükleniyor…</p>;
  if (shown.kind === "error") {
    return (
      <div role="alert">
        <p className={styles.error}>Ders listesi yüklenemedi. Bağlantınızı kontrol edip tekrar deneyin.</p>
        <button type="button" className={styles.button} onClick={() => setRetry((r) => r + 1)}>
          Tekrar dene
        </button>
      </div>
    );
  }
  return (
    <div role="alert">
      <p className={styles.error}>{shown.kind === "empty" ? "Bu kursta henüz ders yok." : "Kurs bulunamadı."}</p>
      <Link href={courseId !== null && shown.kind === "empty" ? `/courses/${courseId}` : "/"}>Geri dön</Link>
    </div>
  );
}
