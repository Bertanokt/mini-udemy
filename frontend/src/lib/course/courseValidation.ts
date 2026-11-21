import type { CourseLevel, CourseStatus } from "@/lib/api/catalog";
import type { CourseRequest } from "@/lib/api/instructor";

// Sınırlar docs/api/openapi.yaml › CreateCourseRequest ve uploadCourseCover ile aynıdır (FR-07, FR-11).
export const TITLE_MIN = 3;
export const TITLE_MAX = 200;
export const DESCRIPTION_MIN = 10;
export const DESCRIPTION_MAX = 5000;
export const OUTCOMES_MAX = 20;
export const OUTCOME_MAX = 300;
export const COVER_MAX_BYTES = 2 * 1024 * 1024;
export const COVER_TYPES = ["image/jpeg", "image/png", "image/webp"];
export const COVER_ACCEPT = ".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp";

/**
 * Formun kilitlenip kilitlenmeyeceği için arayüz ipucu: düzenleme izni sunucuda kursun durum nesnesindedir
 * (`CourseState.assertEditable`, ADR-010); form yalnız DRAFT ve UNPUBLISHED'da açık gösterilir, arada durum
 * değişirse sunucunun 409 mesajı gösterilir.
 */
export function isEditableStatus(status: CourseStatus): boolean {
  return status === "DRAFT" || status === "UNPUBLISHED";
}

/** Form alanları; kategori seçilmemişse boş metin. */
export interface CourseDraft {
  title: string;
  description: string;
  categoryId: string;
  level: CourseLevel | "";
  learningOutcomes: string[];
}

export interface CourseErrors {
  title?: string;
  description?: string;
  categoryId?: string;
  level?: string;
  /** Liste geneli (en fazla 20 madde). */
  learningOutcomes?: string;
  /** Madde bazlı hatalar, sıra listedeki sırayla aynı. */
  outcomeItems?: Record<number, string>;
}

export function emptyCourseDraft(): CourseDraft {
  return { title: "", description: "", categoryId: "", level: "", learningOutcomes: [] };
}

export function validateCourse(draft: CourseDraft): CourseErrors {
  const errors: CourseErrors = {};
  const title = draft.title.trim();
  if (title.length < TITLE_MIN || title.length > TITLE_MAX) {
    errors.title = `Başlık ${TITLE_MIN}–${TITLE_MAX} karakter olmalıdır.`;
  }
  const description = draft.description.trim();
  if (description.length < DESCRIPTION_MIN || description.length > DESCRIPTION_MAX) {
    errors.description = `Açıklama ${DESCRIPTION_MIN}–${DESCRIPTION_MAX} karakter olmalıdır.`;
  }
  if (!draft.categoryId) errors.categoryId = "Kategori zorunludur.";
  if (!draft.level) errors.level = "Seviye zorunludur.";
  if (draft.learningOutcomes.length > OUTCOMES_MAX) {
    errors.learningOutcomes = `En fazla ${OUTCOMES_MAX} öğrenme hedefi girilebilir.`;
  }
  const items: Record<number, string> = {};
  draft.learningOutcomes.forEach((outcome, index) => {
    const text = outcome.trim();
    if (text.length === 0) items[index] = "Öğrenme hedefi boş olamaz.";
    else if (text.length > OUTCOME_MAX) items[index] = `Öğrenme hedefi en fazla ${OUTCOME_MAX} karakter olabilir.`;
  });
  if (Object.keys(items).length > 0) errors.outcomeItems = items;
  return errors;
}

export function hasCourseErrors(errors: CourseErrors): boolean {
  return Boolean(
    errors.title ||
      errors.description ||
      errors.categoryId ||
      errors.level ||
      errors.learningOutcomes ||
      (errors.outcomeItems && Object.keys(errors.outcomeItems).length > 0),
  );
}

/** Doğrulanmış taslağı istek gövdesine çevirir (validateCourse geçtikten sonra çağrılır). */
export function toCourseRequest(draft: CourseDraft): CourseRequest {
  return {
    title: draft.title.trim(),
    description: draft.description.trim(),
    categoryId: Number(draft.categoryId),
    level: draft.level as CourseLevel,
    learningOutcomes: draft.learningOutcomes.map((o) => o.trim()),
  };
}

/** Sunucunun `errors` alanını (title, description, categoryId, level, learningOutcomes, learningOutcomes[i]) çevirir. */
export function fromCourseServerErrors(errors: Record<string, string> | undefined): CourseErrors {
  const result: CourseErrors = {};
  if (!errors) return result;
  for (const [key, message] of Object.entries(errors)) {
    const item = /^learningOutcomes\[(\d+)\]$/.exec(key);
    if (item) {
      result.outcomeItems = { ...result.outcomeItems, [Number(item[1])]: message };
    } else if (key === "title" || key === "description" || key === "categoryId" || key === "level") {
      result[key] = message;
    } else if (key.startsWith("learningOutcomes")) {
      result.learningOutcomes = message;
    }
  }
  return result;
}

/** Kapak dosyasını yüklemeden önce denetler; sunucu türü içerikten yeniden denetler (413/415). */
export function validateCover(file: File): string | null {
  if (!COVER_TYPES.includes(file.type)) return "Kapak görseli jpg, png ya da webp olmalıdır.";
  if (file.size > COVER_MAX_BYTES) return "Kapak görseli en fazla 2 MB olabilir.";
  return null;
}
