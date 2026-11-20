"use client";

import { useCallback, useEffect, useRef, useState, type FormEvent } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import type { CourseStatus } from "@/lib/api/catalog";
import { getCourse, listSections, type LessonResponse, type SectionResponse } from "@/lib/api/courses";
import {
  STATUS_LABELS,
  createSection,
  deleteLesson,
  deleteSection,
  moveLesson,
  moveSection,
  updateSection,
} from "@/lib/api/instructor";
import { isEditableStatus } from "@/lib/course/courseValidation";
import { SECTION_TITLE_MAX, validateSectionTitle } from "@/lib/course/lessonValidation";
import { LessonForm, LessonRow } from "./LessonEditor";
import { apiMessage, fieldErrors, isConflict, type Permissions } from "./shared";

interface CourseInfo {
  title: string;
  status: CourseStatus;
}

type Load = { state: "loading" } | { state: "error"; message: string } | { state: "ready" };

type DeleteTarget = { kind: "section"; section: SectionResponse } | { kind: "lesson"; lesson: LessonResponse };

/**
 * Bölüm ve ders yönetimi (#34): bölüm ekle/adlandır/sil/sırala, ders ekle/düzenle/sil/sırala, ders içeriği yükle
 * ve önizle. Hangi düğmenin görüneceği kurs durumuna göre bir arayüz ipucudur; asıl izin kararı sunucuda kursun
 * durum nesnesindedir (`CourseState`, ADR-010) ve izin yoksa sunucunun 409 mesajı gösterilip liste yenilenir.
 */
export function SectionManager() {
  const params = useParams<{ id: string }>();
  const courseId = Number(params.id);
  const validId = Number.isInteger(courseId) && courseId > 0;

  const [load, setLoad] = useState<Load>({ state: "loading" });
  const [reload, setReload] = useState(0);
  const [course, setCourse] = useState<CourseInfo | null>(null);
  const [sections, setSections] = useState<SectionResponse[]>([]);
  const [message, setMessage] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  // Yeni bölüm formu
  const [newTitle, setNewTitle] = useState("");
  const [newTitleError, setNewTitleError] = useState<string | null>(null);
  // Adı düzenlenen bölüm
  const [renaming, setRenaming] = useState<{ id: number; title: string; error: string | null } | null>(null);
  // Ders formu açık olan bölüm (yeni ders) ya da düzenlenen ders
  const [addingTo, setAddingTo] = useState<number | null>(null);
  const [editingLesson, setEditingLesson] = useState<number | null>(null);

  useEffect(() => {
    if (!validId) return;
    let cancelled = false;
    Promise.all([getCourse(courseId), listSections(courseId)])
      .then(([detail, list]) => {
        if (cancelled) return;
        setCourse({ title: detail.title, status: detail.status });
        setSections(list);
        setLoad({ state: "ready" });
      })
      .catch((e: unknown) => {
        if (!cancelled) setLoad({ state: "error", message: apiMessage(e, "Kursun bölümleri yüklenemedi.") });
      });
    return () => {
      cancelled = true;
    };
  }, [courseId, validId, reload]);

  /** Bir işlem hata verince mesajı gösterir; 409'da kurs bu arada başka duruma geçmiş olabilir, güncel hali yüklenir. */
  const fail = useCallback((e: unknown, fallback: string) => {
    setMessage(apiMessage(e, fallback));
    if (isConflict(e)) setReload((n) => n + 1);
  }, []);

  /** Sunucu işlemini çalıştırır; aynı anda tek işlem. */
  async function run(action: () => Promise<void>, fallback: string) {
    setBusy(true);
    setMessage(null);
    setNotice(null);
    try {
      await action();
    } catch (e) {
      fail(e, fallback);
    } finally {
      setBusy(false);
    }
  }

  const replaceLesson = useCallback((lesson: LessonResponse) => {
    setSections((list) =>
      list.map((s) =>
        s.id === lesson.sectionId ? { ...s, lessons: s.lessons.map((l) => (l.id === lesson.id ? lesson : l)) } : s,
      ),
    );
  }, []);

  const addLesson = useCallback((lesson: LessonResponse) => {
    setSections((list) => list.map((s) => (s.id === lesson.sectionId ? { ...s, lessons: [...s.lessons, lesson] } : s)));
  }, []);

  async function onAddSection(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const problem = validateSectionTitle(newTitle);
    setNewTitleError(problem);
    if (problem) return;
    await run(async () => {
      try {
        const created = await createSection(courseId, { title: newTitle.trim() });
        setSections((list) => [...list, created]);
        setNewTitle("");
        setNotice(`“${created.title}” bölümü eklendi.`);
      } catch (e) {
        const titleError = fieldErrors(e)?.title;
        if (titleError) setNewTitleError(titleError);
        throw e;
      }
    }, "Bölüm eklenemedi. Lütfen tekrar deneyin.");
  }

  async function onRename(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!renaming) return;
    const problem = validateSectionTitle(renaming.title);
    if (problem) {
      setRenaming({ ...renaming, error: problem });
      return;
    }
    await run(async () => {
      const updated = await updateSection(renaming.id, { title: renaming.title.trim() });
      setSections((list) => list.map((s) => (s.id === updated.id ? { ...s, title: updated.title } : s)));
      setRenaming(null);
      setNotice("Bölüm adı güncellendi.");
    }, "Bölüm adı güncellenemedi. Lütfen tekrar deneyin.");
  }

  function onMoveSection(section: SectionResponse, delta: -1 | 1) {
    void run(async () => {
      setSections(await moveSection(section.id, section.position + delta));
    }, "Bölümün sırası değiştirilemedi.");
  }

  function onMoveLesson(lesson: LessonResponse, delta: -1 | 1) {
    void run(async () => {
      const lessons = await moveLesson(lesson.id, lesson.position + delta);
      setSections((list) => list.map((s) => (s.id === lesson.sectionId ? { ...s, lessons } : s)));
    }, "Dersin sırası değiştirilemedi.");
  }

  // Silme onayı
  const [toDelete, setToDelete] = useState<DeleteTarget | null>(null);
  const dialogRef = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (toDelete && !dialog.open) dialog.showModal();
    if (!toDelete && dialog.open) dialog.close();
  }, [toDelete]);

  async function confirmDelete() {
    if (!toDelete) return;
    const target = toDelete;
    await run(async () => {
      if (target.kind === "section") {
        await deleteSection(target.section.id);
        setNotice(`“${target.section.title}” bölümü silindi.`);
      } else {
        await deleteLesson(target.lesson.id);
        setNotice(`“${target.lesson.title}” dersi silindi.`);
      }
      // Kalan sıralar sunucuda boşluksuz yeniden hesaplanır; güncel listeyi al.
      setSections(await listSections(courseId));
    }, "Silinemedi. Lütfen tekrar deneyin.");
    setToDelete(null);
  }

  if (!validId) {
    return (
      <p className="auth-error" role="alert">
        Kurs bulunamadı.
      </p>
    );
  }
  if (load.state === "loading") return <p className="auth-status">Bölümler yükleniyor…</p>;
  if (load.state === "error" || !course) {
    return (
      <section className="qb">
        <h1>Bölüm ve dersler</h1>
        <div className="qb-state" role="alert">
          <p className="auth-error">{load.state === "error" ? load.message : "Kurs bulunamadı."}</p>
          <div className="qb-actions">
            <button type="button" className="qb-btn" onClick={() => setReload((n) => n + 1)}>
              Tekrar dene
            </button>
            <Link className="qb-btn" href="/instructor">
              Kurslarıma dön
            </Link>
          </div>
        </div>
      </section>
    );
  }

  const editable = isEditableStatus(course.status);
  // Sözleşme: yayımlanmış kursa ders eklenebilir ve içeriği olmayan derse dosya yüklenebilir; gerisi kapalı.
  const permissions: Permissions = {
    editable,
    canAddLesson: editable || course.status === "PUBLISHED",
    canUploadNew: editable || course.status === "PUBLISHED",
  };
  const lessonCount = sections.reduce((n, s) => n + s.lessons.length, 0);
  const withContent = sections.reduce((n, s) => n + s.lessons.filter((l) => l.hasContent).length, 0);

  return (
    <section className="qb cf sl">
      <div className="qb-head">
        <div>
          <h1>Bölüm ve dersler</h1>
          <p className="qz-muted">
            {course.title} · {sections.length} bölüm, {lessonCount} ders ({withContent} içerikli)
          </p>
        </div>
        <span className={`ic-badge ic-${course.status.toLowerCase()}`}>{STATUS_LABELS[course.status]}</span>
      </div>

      {course.status === "IN_REVIEW" && (
        <p className="qb-notice cf-locked" role="status">
          Kurs incelemede; bölüm ve dersler değiştirilemez. İnceleme sonuçlanınca düzenleyebilirsin.
        </p>
      )}
      {course.status === "PUBLISHED" && (
        <p className="qb-notice cf-locked" role="status">
          Kurs yayımlandı: yalnız yeni ders ekleyebilir ve içeriği olmayan derslere dosya yükleyebilirsin. Diğer
          değişiklikler için kursun yayından kaldırılmış olması gerekir.
        </p>
      )}
      {notice && (
        <p className="qb-notice" role="status">
          {notice}
        </p>
      )}
      {message && (
        <p className="auth-error" role="alert">
          {message}
        </p>
      )}

      {sections.length === 0 && (
        <div className="qb-state">
          <p>Henüz bölüm yok. Kursu onaya göndermek için en az bir bölüm, bir ders ve içeriği yüklenmiş bir ders gerekir.</p>
        </div>
      )}

      <ol className="qb-list">
        {sections.map((section, index) => (
          <li key={section.id} className="qb-item">
            <div className="qb-item-head">
              {renaming?.id === section.id ? (
                <form className="sl-inline" onSubmit={onRename} noValidate>
                  <label>
                    Bölüm adı
                    <input
                      value={renaming.title}
                      maxLength={SECTION_TITLE_MAX}
                      onChange={(e) => setRenaming({ ...renaming, title: e.target.value, error: null })}
                      aria-invalid={renaming.error ? true : undefined}
                      autoFocus
                    />
                    {renaming.error && <span className="field-error">{renaming.error}</span>}
                  </label>
                  <div className="qb-actions">
                    <button type="submit" className="qb-btn qb-primary" disabled={busy}>
                      Kaydet
                    </button>
                    <button type="button" className="qb-btn" onClick={() => setRenaming(null)} disabled={busy}>
                      Vazgeç
                    </button>
                  </div>
                </form>
              ) : (
                <h2 className="qz-section-title">
                  {index + 1}. {section.title}
                </h2>
              )}
              {editable && renaming?.id !== section.id && (
                <div className="qb-actions">
                  <button
                    type="button"
                    className="qb-btn"
                    onClick={() => onMoveSection(section, -1)}
                    disabled={busy || index === 0}
                    aria-label={`${section.title} bölümünü yukarı taşı`}
                  >
                    ↑
                  </button>
                  <button
                    type="button"
                    className="qb-btn"
                    onClick={() => onMoveSection(section, 1)}
                    disabled={busy || index === sections.length - 1}
                    aria-label={`${section.title} bölümünü aşağı taşı`}
                  >
                    ↓
                  </button>
                  <button
                    type="button"
                    className="qb-btn"
                    onClick={() => setRenaming({ id: section.id, title: section.title, error: null })}
                    disabled={busy}
                  >
                    Yeniden adlandır
                  </button>
                  <button
                    type="button"
                    className="qb-btn qb-danger"
                    onClick={() => setToDelete({ kind: "section", section })}
                    disabled={busy}
                    aria-label={`${section.title} bölümünü sil`}
                  >
                    Sil
                  </button>
                </div>
              )}
            </div>

            {section.lessons.length === 0 && <p className="qz-muted">Bu bölümde henüz ders yok.</p>}
            <ol className="qz-selected">
              {section.lessons.map((lesson, lessonIndex) =>
                editingLesson === lesson.id ? (
                  <li key={lesson.id}>
                    <LessonForm
                      sectionId={section.id}
                      lesson={lesson}
                      onSaved={(saved) => {
                        replaceLesson(saved);
                        setEditingLesson(null);
                        setMessage(null);
                        setNotice(`“${saved.title}” dersi güncellendi.`);
                      }}
                      onCancel={() => setEditingLesson(null)}
                      onConflict={(e) => fail(e, "Ders kaydedilemedi.")}
                    />
                  </li>
                ) : (
                  <LessonRow
                    key={lesson.id}
                    lesson={lesson}
                    first={lessonIndex === 0}
                    last={lessonIndex === section.lessons.length - 1}
                    permissions={permissions}
                    busy={busy}
                    onMove={(delta) => onMoveLesson(lesson, delta)}
                    onEdit={() => {
                      setAddingTo(null);
                      setEditingLesson(lesson.id);
                    }}
                    onDelete={() => setToDelete({ kind: "lesson", lesson })}
                    onUploaded={(updated) => {
                      replaceLesson(updated);
                      setMessage(null);
                      setNotice(`“${updated.title}” dersinin içeriği yüklendi.`);
                    }}
                    onConflict={(e) => fail(e, "İçerik yüklenemedi.")}
                  />
                ),
              )}
            </ol>

            {permissions.canAddLesson &&
              (addingTo === section.id ? (
                <LessonForm
                  sectionId={section.id}
                  lesson={null}
                  onSaved={(created) => {
                    addLesson(created);
                    setAddingTo(null);
                    setMessage(null);
                    setNotice(`“${created.title}” dersi eklendi. Şimdi içeriğini yükleyebilirsin.`);
                  }}
                  onCancel={() => setAddingTo(null)}
                  onConflict={(e) => fail(e, "Ders eklenemedi.")}
                />
              ) : (
                <div className="qb-actions">
                  <button
                    type="button"
                    className="qb-btn"
                    onClick={() => {
                      setEditingLesson(null);
                      setAddingTo(section.id);
                    }}
                    disabled={busy}
                  >
                    Ders ekle
                  </button>
                </div>
              ))}
          </li>
        ))}
      </ol>

      {editable && (
        <form className="qb-form sl-new" onSubmit={onAddSection} noValidate aria-label="Bölüm ekle">
          <label>
            Yeni bölüm başlığı
            <input
              name="sectionTitle"
              value={newTitle}
              maxLength={SECTION_TITLE_MAX}
              onChange={(e) => {
                setNewTitle(e.target.value);
                setNewTitleError(null);
              }}
              aria-invalid={newTitleError ? true : undefined}
            />
            {newTitleError && <span className="field-error">{newTitleError}</span>}
          </label>
          <div className="qb-actions">
            <button type="submit" className="qb-btn qb-primary" disabled={busy}>
              Bölüm ekle
            </button>
          </div>
        </form>
      )}

      <div className="qb-actions">
        <Link className="qb-btn" href={`/instructor/courses/${courseId}/edit`}>
          Kurs bilgileri
        </Link>
        <Link className="qb-btn" href="/instructor">
          Kurslarıma dön
        </Link>
      </div>

      <dialog
        ref={dialogRef}
        className="qb-dialog"
        onClose={() => {
          if (!busy) setToDelete(null);
        }}
        onCancel={(e) => {
          if (busy) e.preventDefault();
        }}
      >
        <h2>{toDelete?.kind === "section" ? "Bölümü sil" : "Dersi sil"}</h2>
        {toDelete && (
          <p className="qb-dialog-text">
            “{toDelete.kind === "section" ? toDelete.section.title : toDelete.lesson.title}”
          </p>
        )}
        <p>
          {toDelete?.kind === "section"
            ? toDelete.section.lessons.length > 0
              ? `Bölüm ${toDelete.section.lessons.length} dersi ve içerik dosyalarıyla birlikte silinecek.`
              : "Boş bölüm silinecek."
            :"Ders içerik dosyasıyla birlikte silinecek; kursiyerlerin ilerleme yüzdesi yeniden hesaplanır."}{" "}
          Bu işlem geri alınamaz.
        </p>
        <div className="qb-actions">
          <button type="button" className="qb-btn qb-danger" onClick={() => void confirmDelete()} disabled={busy}>
            {busy ? "Siliniyor…" : "Evet, sil"}
          </button>
          <button type="button" className="qb-btn" onClick={() => setToDelete(null)} disabled={busy}>
            Vazgeç
          </button>
        </div>
      </dialog>
    </section>
  );
}
