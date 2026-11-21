"use client";

import { useEffect, useState } from "react";
import { listMyQuizAttempts, listSectionQuizzes, type QuizSummaryResponse } from "@/lib/api/quizzes";

export type QuizStatus = "passed" | "failed" | "notAttempted";

export interface SectionQuiz {
  quiz: QuizSummaryResponse;
  status: QuizStatus;
}

type Loaded = { key: string; bySection: Map<number, SectionQuiz[]> };

/**
 * Oynatıcıdaki bölüm sınavları (#304): kursiyer için her bölümün sınavları ve kursiyerin durumu (geçti / denedi,
 * geçemedi / denemedi). Sınavlar `GET /api/sections/{id}/quizzes`'ten, durum kursiyerin denemelerinden gelir.
 * Kayıtsız kursiyerde uç 403 döner; o bölüm için sınav gösterilmez. `enabled` false ise istek atılmaz.
 */
export function useSectionQuizzes(sectionIds: number[], enabled: boolean): Map<number, SectionQuiz[]> {
  const idsKey = sectionIds.join(",");
  const key = `${enabled}#${idsKey}`;
  const [loaded, setLoaded] = useState<Loaded | null>(null);

  useEffect(() => {
    const ids = idsKey === "" ? [] : idsKey.split(",").map(Number);
    if (!enabled || ids.length === 0) return;
    let cancelled = false;
    Promise.all(
      ids.map(async (sectionId) => {
        try {
          const quizzes = await listSectionQuizzes(sectionId);
          const items = await Promise.all(
            quizzes.map(async (quiz): Promise<SectionQuiz> => {
              try {
                const attempts = await listMyQuizAttempts(quiz.id);
                const status: QuizStatus = attempts.some((a) => a.passed)
                  ? "passed"
                  : attempts.length > 0
                    ? "failed"
                    : "notAttempted";
                return { quiz, status };
              } catch {
                return { quiz, status: "notAttempted" };
              }
            }),
          );
          return [sectionId, items] as const;
        } catch {
          return [sectionId, [] as SectionQuiz[]] as const;
        }
      }),
    ).then((entries) => {
      if (!cancelled) setLoaded({ key, bySection: new Map(entries) });
    });
    return () => {
      cancelled = true;
    };
  }, [idsKey, key, enabled]);

  return loaded?.key === key ? loaded.bySection : new Map();
}
