"use client";

import { useEffect, useState, type FormEvent } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { CourseCard } from "@/components/course/CourseCard";
import { ApiError } from "@/lib/api/client";
import {
  LEVEL_LABELS,
  SORT_LABELS,
  listCategories,
  searchCatalog,
  type CatalogQuery,
  type CatalogSort,
  type CategoryResponse,
  type CourseLevel,
  type CourseSummaryPage,
} from "@/lib/api/catalog";

const PAGE_SIZE = 12;

type Result =
  | { kind: "loading" }
  | { kind: "error"; message: string }
  | { kind: "ok"; data: CourseSummaryPage };

type Loaded = { key: string; result: Exclude<Result, { kind: "loading" }> };

function toPositiveInt(value: string | null): number | undefined {
  if (!value) return undefined;
  const n = Number(value);
  return Number.isInteger(n) && n > 0 ? n : undefined;
}

// Filtrelerin tek kaynağı URL'dir: geri tuşu ve paylaşılan bağlantı aynı sonucu gösterir.
function readQuery(params: URLSearchParams): CatalogQuery {
  const level = params.get("level");
  const sort = params.get("sort");
  const page = Number(params.get("page") ?? "0");
  return {
    q: params.get("q")?.trim() || undefined,
    categoryId: toPositiveInt(params.get("categoryId")),
    level: level && level in LEVEL_LABELS ? (level as CourseLevel) : undefined,
    instructorId: toPositiveInt(params.get("instructorId")),
    sort: sort && sort in SORT_LABELS ? (sort as CatalogSort) : undefined,
    page: Number.isInteger(page) && page > 0 ? page : 0,
    size: PAGE_SIZE,
  };
}

function buildHref(pathname: string, current: URLSearchParams, changes: Record<string, string | null>): string {
  const next = new URLSearchParams(current);
  for (const [key, value] of Object.entries(changes)) {
    if (value === null || value === "") next.delete(key);
    else next.set(key, value);
  }
  if (!("page" in changes)) next.delete("page");
  const qs = next.toString();
  return qs ? `${pathname}?${qs}` : pathname;
}

export function CatalogView() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const queryKey = searchParams.toString();
  const query = readQuery(new URLSearchParams(queryKey));

  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [retry, setRetry] = useState(0);
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  // Son yanıt şimdiki filtrelere ait değilse yükleniyor sayılır (effect içinde setState gerekmez).
  const requestKey = `${queryKey}#${retry}`;
  const result: Result = loaded?.key === requestKey ? loaded.result : { kind: "loading" };

  useEffect(() => {
    listCategories()
      .then(setCategories)
      .catch(() => setCategories([]));
  }, []);

  useEffect(() => {
    let cancelled = false;
    const key = `${queryKey}#${retry}`;
    searchCatalog(readQuery(new URLSearchParams(queryKey)))
      .then((data) => {
        if (!cancelled) setLoaded({ key, result: { kind: "ok", data } });
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        const message =
          err instanceof ApiError && err.body?.message
            ? err.body.message
            : "Kurslar yüklenemedi. Bağlantınızı kontrol edip tekrar deneyin.";
        setLoaded({ key, result: { kind: "error", message } });
      });
    return () => {
      cancelled = true;
    };
  }, [queryKey, retry]);

  const go = (changes: Record<string, string | null>) =>
    router.push(buildHref(pathname, searchParams, changes), { scroll: false });

  const onSearch = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const q = new FormData(e.currentTarget).get("q");
    go({ q: typeof q === "string" ? q.trim() : null });
  };

  const instructorLabel =
    result.kind === "ok"
      ? result.data.content.find((c) => c.instructor.id === query.instructorId)?.instructor.fullName
      : undefined;

  return (
    <section className="catalog">
      <h1>Kurs kataloğu</h1>

      <form className="catalog-search" role="search" onSubmit={onSearch} key={query.q ?? ""}>
        <label htmlFor="catalog-q" className="visually-hidden">
          Kurs ara
        </label>
        <input
          id="catalog-q"
          name="q"
          type="search"
          placeholder="Kurs adı ya da konu ara"
          defaultValue={query.q ?? ""}
          maxLength={100}
        />
        <button type="submit">Ara</button>
      </form>

      <div className="catalog-filters">
        <label>
          Kategori
          <select
            value={query.categoryId ?? ""}
            onChange={(e) => go({ categoryId: e.target.value || null })}
          >
            <option value="">Tümü</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          Seviye
          <select value={query.level ?? ""} onChange={(e) => go({ level: e.target.value || null })}>
            <option value="">Tümü</option>
            {(Object.keys(LEVEL_LABELS) as CourseLevel[]).map((l) => (
              <option key={l} value={l}>
                {LEVEL_LABELS[l]}
              </option>
            ))}
          </select>
        </label>
        <label>
          Sıralama
          <select value={query.sort ?? "newest"} onChange={(e) => go({ sort: e.target.value === "newest" ? null : e.target.value })}>
            {(Object.keys(SORT_LABELS) as CatalogSort[]).map((s) => (
              <option key={s} value={s}>
                {SORT_LABELS[s]}
              </option>
            ))}
          </select>
        </label>
        {query.instructorId && (
          <button type="button" className="chip" onClick={() => go({ instructorId: null })}>
            Eğitmen: {instructorLabel || `#${query.instructorId}`} ✕
          </button>
        )}
      </div>

      {result.kind === "loading" && <p className="catalog-status">Kurslar yükleniyor…</p>}

      {result.kind === "error" && (
        <div className="catalog-status status-down" role="alert">
          <p>{result.message}</p>
          <button type="button" onClick={() => setRetry((n) => n + 1)}>
            Tekrar dene
          </button>
        </div>
      )}

      {result.kind === "ok" && result.data.content.length === 0 && (
        <div className="catalog-status">
          <p>Aramanıza uyan kurs bulunamadı.</p>
          {queryKey && (
            <button type="button" onClick={() => router.push(pathname)}>
              Filtreleri temizle
            </button>
          )}
        </div>
      )}

      {result.kind === "ok" && result.data.content.length > 0 && (
        <>
          <p className="catalog-count">{result.data.totalElements} kurs</p>
          <div className="course-grid">
            {result.data.content.map((course) => (
              <CourseCard
                key={course.id}
                course={course}
                instructorHref={buildHref(pathname, searchParams, { instructorId: String(course.instructor.id) })}
              />
            ))}
          </div>
          {result.data.totalPages > 1 && (
            <nav className="pagination" aria-label="Sayfalar">
              <button type="button" disabled={result.data.page <= 0} onClick={() => go({ page: String(result.data.page - 1) })}>
                ‹ Önceki
              </button>
              <span>
                Sayfa {result.data.page + 1} / {result.data.totalPages}
              </span>
              <button
                type="button"
                disabled={result.data.page + 1 >= result.data.totalPages}
                onClick={() => go({ page: String(result.data.page + 1) })}
              >
                Sonraki ›
              </button>
            </nav>
          )}
        </>
      )}
    </section>
  );
}
