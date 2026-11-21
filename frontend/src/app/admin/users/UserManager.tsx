"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { listUsers, setUserStatus, type AdminUserPage, type AdminUserResponse } from "@/lib/api/admin";
import type { Role } from "@/lib/api/auth";
import { ApiError } from "@/lib/api/client";
import { useAuth } from "@/lib/auth/AuthProvider";
import { Loading } from "@/components/common/Loading";
import { ErrorMessage } from "@/components/common/ErrorMessage";
import { EmptyState } from "@/components/common/EmptyState";

const PAGE_SIZE = 20;

const ROLE_LABELS: Record<Role, string> = {
  STUDENT: "Öğrenci",
  INSTRUCTOR: "Eğitmen",
  ADMIN: "Yönetici",
};

type Result = { kind: "error"; message: string } | { kind: "ok"; data: AdminUserPage };
type Loaded = { key: string; result: Result };

function errorMessage(e: unknown, fallback: string): string {
  return e instanceof ApiError && e.body?.message ? e.body.message : fallback;
}

// Filtrelerin tek kaynağı URL'dir (katalogla aynı kalıp).
function readQuery(params: URLSearchParams) {
  const role = params.get("role");
  const active = params.get("active");
  const page = Number(params.get("page") ?? "0");
  return {
    role: role && role in ROLE_LABELS ? (role as Role) : undefined,
    active: active === "true" ? true : active === "false" ? false : undefined,
    q: params.get("q")?.trim() || undefined,
    page: Number.isInteger(page) && page > 0 ? page : 0,
  };
}

function formatDate(iso: string): string {
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleDateString("tr-TR");
}

/** Yönetici kullanıcı ekranı (#63): filtreli sayfalı liste, onaylı pasifleştirme/aktifleştirme. */
export function UserManager() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const { user: me } = useAuth();
  const queryKey = searchParams.toString();
  const query = readQuery(new URLSearchParams(queryKey));

  const [retry, setRetry] = useState(0);
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  // Son yanıt şimdiki filtrelere ait değilse yükleniyor sayılır.
  const requestKey = `${queryKey}#${retry}`;
  const result: Result | null = loaded?.key === requestKey ? loaded.result : null;

  useEffect(() => {
    let cancelled = false;
    const key = `${queryKey}#${retry}`;
    const q = readQuery(new URLSearchParams(queryKey));
    listUsers({ ...q, size: PAGE_SIZE })
      .then((data) => {
        if (!cancelled) setLoaded({ key, result: { kind: "ok", data } });
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        const message = errorMessage(e, "Kullanıcılar yüklenemedi. Bağlantınızı kontrol edip tekrar deneyin.");
        setLoaded({ key, result: { kind: "error", message } });
      });
    return () => {
      cancelled = true;
    };
  }, [queryKey, retry]);

  function go(changes: Record<string, string | null>) {
    const next = new URLSearchParams(searchParams);
    for (const [key, value] of Object.entries(changes)) {
      if (value === null || value === "") next.delete(key);
      else next.set(key, value);
    }
    if (!("page" in changes)) next.delete("page");
    const qs = next.toString();
    setNotice(null);
    router.push(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
  }

  function onSearch(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const q = new FormData(e.currentTarget).get("q");
    go({ q: typeof q === "string" ? q.trim() : null });
  }

  const [target, setTarget] = useState<AdminUserResponse | null>(null);
  const [working, setWorking] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const dialogRef = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (target && !dialog.open) dialog.showModal();
    if (!target && dialog.open) dialog.close();
  }, [target]);

  function closeDialog() {
    if (working) return;
    setTarget(null);
    setActionError(null);
  }

  async function confirm() {
    if (!target) return;
    setWorking(true);
    setActionError(null);
    try {
      const updated = await setUserStatus(target.id, !target.active);
      setLoaded((cur) =>
        cur && cur.result.kind === "ok"
          ? {
              ...cur,
              result: {
                kind: "ok",
                data: { ...cur.result.data, content: cur.result.data.content.map((u) => (u.id === updated.id ? updated : u)) },
              },
            }
          : cur,
      );
      setTarget(null);
      setNotice(updated.active ? "Hesap aktifleştirildi." : "Hesap pasifleştirildi.");
    } catch (e) {
      setActionError(errorMessage(e, "İşlem tamamlanamadı. Lütfen tekrar deneyin."));
    } finally {
      setWorking(false);
    }
  }

  return (
    <section className="qb usr">
      <div className="qb-head">
        <h1>Kullanıcı yönetimi</h1>
        <Link href="/admin">Yönetici paneli</Link>
      </div>

      {notice && (
        <p className="qb-notice" role="status">
          {notice}
        </p>
      )}

      <form className="catalog-search" role="search" onSubmit={onSearch} key={query.q ?? ""}>
        <label htmlFor="user-q" className="visually-hidden">
          Kullanıcı ara
        </label>
        <input id="user-q" name="q" type="search" placeholder="Ad ya da e-posta ara" defaultValue={query.q ?? ""} maxLength={100} />
        <button type="submit" className="qb-btn qb-primary">
          Ara
        </button>
      </form>

      <div className="usr-filters">
        <label>
          Rol
          <select value={query.role ?? ""} onChange={(e) => go({ role: e.target.value || null })}>
            <option value="">Tümü</option>
            {(Object.keys(ROLE_LABELS) as Role[]).map((r) => (
              <option key={r} value={r}>
                {ROLE_LABELS[r]}
              </option>
            ))}
          </select>
        </label>
        <label>
          Durum
          <select
            value={query.active === undefined ? "" : String(query.active)}
            onChange={(e) => go({ active: e.target.value || null })}
          >
            <option value="">Tümü</option>
            <option value="true">Aktif</option>
            <option value="false">Pasif</option>
          </select>
        </label>
      </div>

      {result === null && <Loading text="Kullanıcılar yükleniyor…" />}

      {result?.kind === "error" && <ErrorMessage message={result.message} onRetry={() => setRetry((n) => n + 1)} />}

      {result?.kind === "ok" && result.data.content.length === 0 && (
        <EmptyState
          message="Aramanıza uyan kullanıcı bulunamadı."
          action={queryKey !== "" ? { label: "Filtreleri temizle", onClick: () => router.push(pathname) } : undefined}
        />
      )}

      {result?.kind === "ok" && result.data.content.length > 0 && (
        <>
          <p className="qb-state">{result.data.totalElements} kullanıcı</p>
          <div className="usr-wrap">
            <table className="usr-table">
              <thead>
                <tr>
                  <th scope="col">Ad</th>
                  <th scope="col">E-posta</th>
                  <th scope="col">Rol</th>
                  <th scope="col">Durum</th>
                  <th scope="col">Kayıt tarihi</th>
                  <th scope="col">
                    <span className="visually-hidden">İşlem</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {result.data.content.map((u) => {
                  const self = me?.id === u.id;
                  return (
                    <tr key={u.id}>
                      <td data-label="Ad" className="usr-name">
                        {u.fullName}
                        {self && " (siz)"}
                      </td>
                      <td data-label="E-posta" className="usr-email">
                        {u.email}
                      </td>
                      <td data-label="Rol">{ROLE_LABELS[u.role]}</td>
                      <td data-label="Durum" className={u.active ? "usr-on" : "usr-off"}>
                        {u.active ? "Aktif" : "Pasif"}
                      </td>
                      <td data-label="Kayıt tarihi">{formatDate(u.createdAt)}</td>
                      <td className="usr-act">
                        <button
                          type="button"
                          className={u.active ? "qb-btn qb-danger" : "qb-btn"}
                          disabled={self && u.active}
                          title={self && u.active ? "Kendi hesabınızı pasifleştiremezsiniz." : undefined}
                          aria-label={`${u.fullName} hesabını ${u.active ? "pasifleştir" : "aktifleştir"}`}
                          onClick={() => {
                            setNotice(null);
                            setActionError(null);
                            setTarget(u);
                          }}
                        >
                          {u.active ? "Pasifleştir" : "Aktifleştir"}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          {result.data.totalPages > 1 && (
            <nav className="pagination" aria-label="Sayfalar">
              <button
                type="button"
                className="qb-btn"
                disabled={result.data.page <= 0}
                onClick={() => go({ page: String(result.data.page - 1) })}
              >
                ‹ Önceki
              </button>
              <span>
                Sayfa {result.data.page + 1} / {result.data.totalPages}
              </span>
              <button
                type="button"
                className="qb-btn"
                disabled={result.data.page + 1 >= result.data.totalPages}
                onClick={() => go({ page: String(result.data.page + 1) })}
              >
                Sonraki ›
              </button>
            </nav>
          )}
        </>
      )}

      <dialog
        ref={dialogRef}
        className="qb-dialog"
        onClose={closeDialog}
        onCancel={(e) => {
          if (working) e.preventDefault();
        }}
      >
        <h2>{target?.active ? "Hesabı pasifleştir" : "Hesabı aktifleştir"}</h2>
        {target && (
          <p className="qb-dialog-text">
            {target.fullName} ({target.email})
          </p>
        )}
        <p>
          {target?.active
            ? "Pasif kullanıcı giriş yapamaz. Hesabı daha sonra yeniden aktifleştirebilirsiniz."
            : "Kullanıcı yeniden giriş yapabilecek."}
        </p>
        {actionError && (
          <p className="auth-error" role="alert">
            {actionError}
          </p>
        )}
        <div className="qb-actions">
          <button
            type="button"
            className={target?.active ? "qb-btn qb-danger" : "qb-btn qb-primary"}
            onClick={() => void confirm()}
            disabled={working}
          >
            {working ? "Kaydediliyor…" : target?.active ? "Evet, pasifleştir" : "Evet, aktifleştir"}
          </button>
          <button type="button" className="qb-btn" onClick={closeDialog} disabled={working}>
            {actionError ? "Kapat" : "Vazgeç"}
          </button>
        </div>
      </dialog>
    </section>
  );
}
