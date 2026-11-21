"use client";

import { useCallback, useEffect, useState } from "react";
import { ApiError } from "@/lib/api/client";
import { completeLesson, getMyCourseProgress, type CourseProgressResponse } from "@/lib/api/progress";

export type ProgressState =
  | { kind: "off" }
  | { kind: "loading" }
  | { kind: "ok"; progress: CourseProgressResponse }
  | { kind: "notEnrolled" }
  | { kind: "error" };

type Loaded = { key: string; state: ProgressState };

/**
 * Oynatıcıdaki kurs ilerlemesi (#46). Yalnız kursiyer için yüklenir (`enabled`); kayıtsızsa (403) "notEnrolled".
 * `complete` dersi tamamlar ve sunucunun döndürdüğü güncel ilerlemeyi state'e yazar: çubuklar ve tikler aynı
 * yanıttan güncellenir, ayrıca bir istek atılmaz.
 */
export function useCourseProgress(courseId: number | null, enabled: boolean) {
  const key = `${courseId}#${enabled}`;
  const [loaded, setLoaded] = useState<Loaded | null>(null);

  useEffect(() => {
    if (!enabled || courseId === null) return;
    let cancelled = false;
    getMyCourseProgress(courseId)
      .then((progress) => !cancelled && setLoaded({ key, state: { kind: "ok", progress } }))
      .catch((err: unknown) => {
        if (cancelled) return;
        setLoaded({ key, state: err instanceof ApiError && err.status === 403 ? { kind: "notEnrolled" } : { kind: "error" } });
      });
    return () => {
      cancelled = true;
    };
  }, [courseId, enabled, key]);

  const complete = useCallback(
    async (lessonId: number) => {
      const progress = await completeLesson(lessonId);
      setLoaded({ key, state: { kind: "ok", progress } });
    },
    [key],
  );

  let state: ProgressState;
  if (!enabled || courseId === null) state = { kind: "off" };
  else state = loaded?.key === key ? loaded.state : { kind: "loading" };
  return { state, complete };
}
