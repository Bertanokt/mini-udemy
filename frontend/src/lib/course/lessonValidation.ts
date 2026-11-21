import type { ContentType, LessonResponse } from "@/lib/api/courses";
import type { LessonRequest } from "@/lib/api/instructor";

// Sınırlar docs/api/openapi.yaml › SectionRequest, LessonRequest ve uploadLessonContent ile aynıdır (FR-08, FR-09).
export const SECTION_TITLE_MAX = 200;
export const LESSON_TITLE_MAX = 200;

export const CONTENT_TYPE_LABELS: Record<ContentType, string> = {
  VIDEO: "Video",
  PDF: "PDF",
  DOCUMENT: "Doküman",
};

export const CONTENT_TYPES = Object.keys(CONTENT_TYPE_LABELS) as ContentType[];

interface ContentRule {
  extensions: string[];
  maxBytes: number;
  /** Seçim penceresinin süzgeci. */
  accept: string;
  /** Kullanıcıya gösterilen kural, örn. "mp4 ya da webm, en fazla 200 MB". */
  hint: string;
  tooLarge: string;
  wrongType: string;
}

const MB = 1024 * 1024;

/**
 * Dosya kuralının arayüz kopyası: kullanıcı büyük dosyayı boşuna göndermesin diye yüklemeden önce denetlenir. Asıl
 * kural backend'de türün fabrikasındadır (Factory Method, ADR-011) ve tür dosyanın içeriğinden yeniden denetlenir;
 * mesajlar oradaki 413/415 mesajlarıyla aynıdır.
 */
export const CONTENT_RULES: Record<ContentType, ContentRule> = {
  VIDEO: {
    extensions: ["mp4", "webm"],
    maxBytes: 200 * MB,
    accept: ".mp4,.webm,video/mp4,video/webm",
    hint: "mp4 ya da webm, en fazla 200 MB",
    tooLarge: "Video en fazla 200 MB olabilir.",
    wrongType: "Yalnız mp4 ve webm videolar yüklenebilir.",
  },
  PDF: {
    extensions: ["pdf"],
    maxBytes: 20 * MB,
    accept: ".pdf,application/pdf",
    hint: "pdf, en fazla 20 MB",
    tooLarge: "PDF en fazla 20 MB olabilir.",
    wrongType: "Yalnız pdf dosyaları yüklenebilir.",
  },
  DOCUMENT: {
    extensions: ["pdf", "docx", "pptx"],
    maxBytes: 20 * MB,
    accept: ".pdf,.docx,.pptx",
    hint: "pdf, docx ya da pptx, en fazla 20 MB",
    tooLarge: "Doküman en fazla 20 MB olabilir.",
    wrongType: "Yalnız pdf, docx ve pptx dokümanlar yüklenebilir.",
  },
};

/** Dosyayı türün kuralına göre denetler; sorun yoksa null. Tür uzantıdan bakılır (docx/pptx'in MIME'ı tarayıcıya göre değişir). */
export function validateContentFile(type: ContentType, file: File): string | null {
  const rule = CONTENT_RULES[type];
  const extension = file.name.includes(".") ? file.name.split(".").pop()!.toLowerCase() : "";
  if (!rule.extensions.includes(extension)) return rule.wrongType;
  if (file.size > rule.maxBytes) return rule.tooLarge;
  return null;
}

export function validateSectionTitle(title: string): string | null {
  const text = title.trim();
  if (text.length === 0) return "Bölüm başlığı zorunludur.";
  if (text.length > SECTION_TITLE_MAX) return `Bölüm başlığı en fazla ${SECTION_TITLE_MAX} karakter olabilir.`;
  return null;
}

/** Ders formu; süre dakika ve saniye olarak girilir, boş alan 0 sayılır. */
export interface LessonDraft {
  title: string;
  contentType: ContentType;
  mandatory: boolean;
  minutes: string;
  seconds: string;
}

export interface LessonErrors {
  title?: string;
  contentType?: string;
  mandatory?: string;
  durationSeconds?: string;
}

export function emptyLessonDraft(): LessonDraft {
  return { title: "", contentType: "VIDEO", mandatory: true, minutes: "", seconds: "" };
}

export function lessonDraftOf(lesson: LessonResponse): LessonDraft {
  return {
    title: lesson.title,
    contentType: lesson.contentType,
    mandatory: lesson.mandatory,
    minutes: String(Math.floor(lesson.durationSeconds / 60)),
    seconds: String(lesson.durationSeconds % 60),
  };
}

function wholeNumber(value: string): number | null {
  const text = value.trim();
  if (text === "") return 0;
  return /^\d+$/.test(text) ? Number(text) : null;
}

export function validateLesson(draft: LessonDraft): LessonErrors {
  const errors: LessonErrors = {};
  const title = draft.title.trim();
  if (title.length === 0) errors.title = "Başlık zorunludur.";
  else if (title.length > LESSON_TITLE_MAX) errors.title = `Başlık en fazla ${LESSON_TITLE_MAX} karakter olabilir.`;
  const minutes = wholeNumber(draft.minutes);
  const seconds = wholeNumber(draft.seconds);
  if (minutes === null || seconds === null) errors.durationSeconds = "Süre için sıfır ya da pozitif tam sayı girin.";
  else if (seconds > 59) errors.durationSeconds = "Saniye 0–59 arasında olmalıdır.";
  return errors;
}

export function hasLessonErrors(errors: LessonErrors): boolean {
  return Boolean(errors.title || errors.contentType || errors.mandatory || errors.durationSeconds);
}

/** Doğrulanmış taslağı istek gövdesine çevirir (validateLesson geçtikten sonra çağrılır). */
export function toLessonRequest(draft: LessonDraft): LessonRequest {
  return {
    title: draft.title.trim(),
    contentType: draft.contentType,
    mandatory: draft.mandatory,
    durationSeconds: (wholeNumber(draft.minutes) ?? 0) * 60 + (wholeNumber(draft.seconds) ?? 0),
  };
}

/** Sunucunun `errors` alanını (title, contentType, mandatory, durationSeconds) ders formunun alanlarına çevirir. */
export function fromLessonServerErrors(errors: Record<string, string> | undefined): LessonErrors {
  const result: LessonErrors = {};
  if (!errors) return result;
  for (const key of ["title", "contentType", "mandatory", "durationSeconds"] as const) {
    if (errors[key]) result[key] = errors[key];
  }
  return result;
}

/** "9 dk 5 sn" gibi kısa süre metni. */
export function formatDuration(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  if (minutes === 0) return `${seconds} sn`;
  return seconds === 0 ? `${minutes} dk` : `${minutes} dk ${seconds} sn`;
}

/** Bayt → "12,4 MB". */
export function formatSize(bytes: number): string {
  if (bytes < MB) return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  return `${(bytes / MB).toLocaleString("tr-TR", { maximumFractionDigits: 1 })} MB`;
}
