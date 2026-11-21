"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import Link from "next/link";
import { createCategory, deleteCategory, updateCategory } from "@/lib/api/admin";
import { listCategories, type CategoryResponse } from "@/lib/api/catalog";
import { ApiError } from "@/lib/api/client";
import { NAME_MAX, errorMessage, nameFieldError, sortByName, validateName } from "./validation";
import { Loading } from "@/components/common/Loading";
import { ErrorMessage } from "@/components/common/ErrorMessage";
import { EmptyState } from "@/components/common/EmptyState";

type Load =
  | { state: "loading" }
  | { state: "error"; message: string }
  | { state: "ready"; categories: CategoryResponse[] };

/** Yönetici kategori ekranı (#118): liste, ekleme, satır içi yeniden adlandırma, onaylı silme. */
export function CategoryManager() {
  const [load, setLoad] = useState<Load>({ state: "loading" });
  const [attempt, setAttempt] = useState(0);
  const [notice, setNotice] = useState<string | null>(null);

  const [newName, setNewName] = useState("");
  const [newError, setNewError] = useState<string | null>(null);
  const [adding, setAdding] = useState(false);

  const [editingId, setEditingId] = useState<number | null>(null);
  const [editName, setEditName] = useState("");
  const [editError, setEditError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const [toDelete, setToDelete] = useState<CategoryResponse | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listCategories()
      .then((categories) => {
        if (!cancelled) setLoad({ state: "ready", categories: sortByName(categories) });
      })
      .catch((e: unknown) => {
        if (!cancelled) setLoad({ state: "error", message: errorMessage(e, "Kategoriler yüklenemedi.") });
      });
    return () => {
      cancelled = true;
    };
  }, [attempt]);

  function retry() {
    setLoad({ state: "loading" });
    setAttempt((n) => n + 1);
  }

  const dialogRef = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (toDelete && !dialog.open) dialog.showModal();
    if (!toDelete && dialog.open) dialog.close();
  }, [toDelete]);

  function change(update: (list: CategoryResponse[]) => CategoryResponse[]) {
    setLoad((current) =>
      current.state === "ready" ? { state: "ready", categories: sortByName(update(current.categories)) } : current,
    );
  }

  async function onAdd(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setNotice(null);
    const invalid = validateName(newName);
    setNewError(invalid);
    if (invalid) return;
    setAdding(true);
    try {
      const created = await createCategory({ name: newName.trim() });
      change((list) => [...list, created]);
      setNewName("");
      setNotice("Kategori eklendi.");
    } catch (e) {
      setNewError(nameFieldError(e) ?? errorMessage(e, "Kategori eklenemedi. Lütfen tekrar deneyin."));
    } finally {
      setAdding(false);
    }
  }

  function startEdit(category: CategoryResponse) {
    setNotice(null);
    setEditingId(category.id);
    setEditName(category.name);
    setEditError(null);
  }

  function cancelEdit() {
    if (saving) return;
    setEditingId(null);
    setEditError(null);
  }

  async function onSaveEdit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (editingId === null) return;
    const invalid = validateName(editName);
    setEditError(invalid);
    if (invalid) return;
    setSaving(true);
    try {
      const updated = await updateCategory(editingId, { name: editName.trim() });
      change((list) => list.map((c) => (c.id === updated.id ? updated : c)));
      setEditingId(null);
      setNotice("Kategori güncellendi.");
    } catch (e) {
      if (e instanceof ApiError && e.status === 404) {
        // Başka yerde silinmiş: listeden düşür.
        const id = editingId;
        change((list) => list.filter((c) => c.id !== id));
        setEditingId(null);
        setNotice(errorMessage(e, "Kategori bulunamadı."));
      } else {
        setEditError(nameFieldError(e) ?? errorMessage(e, "Kategori güncellenemedi. Lütfen tekrar deneyin."));
      }
    } finally {
      setSaving(false);
    }
  }

  async function confirmDelete() {
    if (!toDelete) return;
    setDeleting(true);
    setDeleteError(null);
    const id = toDelete.id;
    try {
      await deleteCategory(id);
      change((list) => list.filter((c) => c.id !== id));
      setToDelete(null);
      setNotice("Kategori silindi.");
    } catch (e) {
      if (e instanceof ApiError && e.status === 404) change((list) => list.filter((c) => c.id !== id));
      setDeleteError(errorMessage(e, "Kategori silinemedi. Lütfen tekrar deneyin."));
    } finally {
      setDeleting(false);
    }
  }

  function closeDelete() {
    if (deleting) return;
    setToDelete(null);
    setDeleteError(null);
  }

  return (
    <section className="qb cat">
      <div className="qb-head">
        <h1>Kategori yönetimi</h1>
        <Link href="/admin">Yönetici paneli</Link>
      </div>

      {notice && (
        <p className="qb-notice" role="status">
          {notice}
        </p>
      )}

      {load.state === "loading" && <Loading text="Kategoriler yükleniyor…" />}

      {load.state === "error" && <ErrorMessage message={load.message} onRetry={retry} />}

      {load.state === "ready" && (
        <>
          <form className="qb-form" onSubmit={onAdd} noValidate aria-label="Yeni kategori">
            <label>
              Yeni kategori adı
              <span className="cat-add-row">
                <input
                  name="name"
                  value={newName}
                  maxLength={NAME_MAX}
                  autoComplete="off"
                  onChange={(e) => setNewName(e.target.value)}
                  aria-invalid={newError ? true : undefined}
                  aria-describedby={newError ? "new-category-error" : undefined}
                />
                <button type="submit" className="qb-btn qb-primary" disabled={adding}>
                  {adding ? "Ekleniyor…" : "Ekle"}
                </button>
              </span>
              {newError && (
                <span id="new-category-error" className="field-error" role="alert">
                  {newError}
                </span>
              )}
            </label>
          </form>

          {load.categories.length === 0 && (
            <EmptyState message="Henüz kategori yok. Yukarıdan ilk kategoriyi ekleyin." />
          )}

          <ul className="qb-list">
            {load.categories.map((c) => (
              <li key={c.id} className="qb-item cat-item">
                {editingId === c.id ? (
                  <form
                    className="cat-edit"
                    onSubmit={onSaveEdit}
                    noValidate
                    aria-label={`${c.name} kategorisini yeniden adlandır`}
                  >
                    <label>
                      <span className="visually-hidden">Yeni ad</span>
                      <input
                        value={editName}
                        maxLength={NAME_MAX}
                        autoComplete="off"
                        autoFocus
                        onChange={(e) => setEditName(e.target.value)}
                        aria-invalid={editError ? true : undefined}
                        aria-describedby={editError ? "edit-category-error" : undefined}
                      />
                    </label>
                    {editError && (
                      <span id="edit-category-error" className="field-error" role="alert">
                        {editError}
                      </span>
                    )}
                    <div className="qb-actions">
                      <button type="submit" className="qb-btn qb-primary" disabled={saving}>
                        {saving ? "Kaydediliyor…" : "Kaydet"}
                      </button>
                      <button type="button" className="qb-btn" onClick={cancelEdit} disabled={saving}>
                        Vazgeç
                      </button>
                    </div>
                  </form>
                ) : (
                  <>
                    <p className="qb-text cat-name">{c.name}</p>
                    <div className="qb-actions">
                      <button
                        type="button"
                        className="qb-btn"
                        onClick={() => startEdit(c)}
                        aria-label={`${c.name} kategorisini yeniden adlandır`}
                      >
                        Yeniden adlandır
                      </button>
                      <button
                        type="button"
                        className="qb-btn qb-danger"
                        onClick={() => {
                          setNotice(null);
                          setDeleteError(null);
                          setToDelete(c);
                        }}
                        aria-label={`${c.name} kategorisini sil`}
                      >
                        Sil
                      </button>
                    </div>
                  </>
                )}
              </li>
            ))}
          </ul>
        </>
      )}

      <dialog
        ref={dialogRef}
        className="qb-dialog"
        onClose={closeDelete}
        onCancel={(e) => {
          if (deleting) e.preventDefault();
        }}
      >
        <h2>Kategoriyi sil</h2>
        {toDelete && <p className="qb-dialog-text">“{toDelete.name}”</p>}
        <p>Bu kategori silinecek. Bu işlem geri alınamaz.</p>
        {deleteError && (
          <p className="auth-error" role="alert">
            {deleteError}
          </p>
        )}
        <div className="qb-actions">
          <button type="button" className="qb-btn qb-danger" onClick={() => void confirmDelete()} disabled={deleting}>
            {deleting ? "Siliniyor…" : "Evet, sil"}
          </button>
          <button type="button" className="qb-btn" onClick={closeDelete} disabled={deleting}>
            {deleteError ? "Kapat" : "Vazgeç"}
          </button>
        </div>
      </dialog>
    </section>
  );
}
