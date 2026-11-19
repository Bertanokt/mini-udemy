import type { LessonResponse, SectionResponse } from "@/lib/api/courses";

/** Oynatıcı adresi (#45): `/learn/{kurs}/{ders}`; ders verilmezse kursun ilk dersi açılır. */
export function learnHref(courseId: number, lessonId?: number | null): string {
  return lessonId != null ? `/learn/${courseId}/${lessonId}` : `/learn/${courseId}`;
}

/** Bölümler ve dersler `position` sırasıyla tek listeye dizilir; önceki/sonraki bu sıradadır. */
export function orderedLessons(sections: SectionResponse[]): LessonResponse[] {
  return [...sections]
    .sort((a, b) => a.position - b.position)
    .flatMap((s) => [...s.lessons].sort((a, b) => a.position - b.position));
}

export interface LessonPosition {
  lesson: LessonResponse;
  previous: LessonResponse | null;
  next: LessonResponse | null;
}

/** Dersin sıradaki yeri; ders kursta yoksa null. */
export function locateLesson(sections: SectionResponse[], lessonId: number): LessonPosition | null {
  const lessons = orderedLessons(sections);
  const i = lessons.findIndex((l) => l.id === lessonId);
  if (i < 0) return null;
  return {
    lesson: lessons[i],
    previous: i > 0 ? lessons[i - 1] : null,
    next: i < lessons.length - 1 ? lessons[i + 1] : null,
  };
}
