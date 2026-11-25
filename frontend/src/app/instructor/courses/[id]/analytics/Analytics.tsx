"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { ApiError } from "@/lib/api/client";
import {
  getCourseStats,
  listCourseStudents,
  type CourseStats,
  type CourseStudent,
  type CourseStudentPage,
  type ProgressBucket,
} from "@/lib/api/instructor";
import { Loading } from "@/components/common/Loading";
import { ErrorMessage } from "@/components/common/ErrorMessage";
import { EmptyState } from "@/components/common/EmptyState";

const PAGE_SIZE = 20;

type Failure = { message: string; kind: "forbidden" | "notfound" | "other" };

function failure(e: unknown, fallback: string): Failure {
  if (e instanceof ApiError) {
    if (e.status === 403) return { kind: "forbidden", message: e.body?.message ?? "Bu kursun analitiğini görme yetkin yok." };
    if (e.status === 404) return { kind: "notfound", message: e.body?.message ?? "Kurs bulunamadı." };
    return { kind: "other", message: e.body?.message ?? fallback };
  }
  return { kind: "other", message: "Sunucuya ulaşılamadı. Bağlantınızı kontrol edip tekrar deneyin." };
}

const pct = (n: number) => `%${Math.round(n * 10) / 10}`;
const dateFmt = new Intl.DateTimeFormat("tr-TR", { dateStyle: "medium" });
const fmtDate = (iso?: string | null) => (iso ? dateFmt.format(new Date(iso)) : "—");

type SortKey = "name" | "enrolledAt" | "progress" | "lastAccessedAt";
type SortDir = "asc" | "desc";

const bucketLabel = (b: ProgressBucket) => (b.minPercent === b.maxPercent ? `%${b.minPercent}` : `%${b.minPercent}–${b.maxPercent}`);

/** Yatay çubuk grafik: görsel çubuk aria-hidden; anlam, satırdaki metin değerinde ve alt tablodadır. */
function BarRow({ label, value, text, max = 100 }: { label: string; value: number; text: string; max?: number }) {
  const width = max > 0 ? Math.min(100, (value / max) * 100) : 0;
  return (
    <li className="an-bar-row">
      <span className="an-bar-label">{label}</span>
      <span className="an-bar-track" aria-hidden="true">
        <span className="an-bar-fill" style={{ width: `${width}%` }} />
      </span>
      <span className="an-bar-value">{text}</span>
    </li>
  );
}

/** Kurs analitiği (#135): istatistik kartları, grafikler ve kursiyer ilerleme tablosu. */
export function Analytics() {
  const params = useParams<{ id: string }>();
  const courseId = Number(params.id);
  const validId = Number.isInteger(courseId) && courseId > 0;

  const [attempt, setAttempt] = useState(0);
  const [stats, setStats] = useState<{ key: string; data: CourseStats | Failure } | null>(null);
  const statsKey = `${courseId}#${attempt}`;
  const statsResult = stats?.key === statsKey ? stats.data : null;

  useEffect(() => {
    if (!validId) return;
    let cancelled = false;
    const key = `${courseId}#${attempt}`;
    getCourseStats(courseId)
      .then((data) => !cancelled && setStats({ key, data }))
      .catch((e: unknown) => !cancelled && setStats({ key, data: failure(e, "İstatistikler yüklenemedi.") }));
    return () => {
      cancelled = true;
    };
  }, [validId, courseId, attempt]);

  const [page, setPage] = useState(0);
  const [students, setStudents] = useState<{ key: string; data: CourseStudentPage | Failure } | null>(null);
  const studentsKey = `${courseId}#${page}#${attempt}`;
  const studentsResult = students?.key === studentsKey ? students.data : null;

  useEffect(() => {
    if (!validId) return;
    let cancelled = false;
    const key = `${courseId}#${page}#${attempt}`;
    listCourseStudents(courseId, page, PAGE_SIZE)
      .then((data) => !cancelled && setStudents({ key, data }))
      .catch((e: unknown) => !cancelled && setStudents({ key, data: failure(e, "Kursiyerler yüklenemedi.") }));
    return () => {
      cancelled = true;
    };
  }, [validId, courseId, page, attempt]);

  const [sort, setSort] = useState<{ key: SortKey; dir: SortDir }>({ key: "enrolledAt", dir: "desc" });

  const pageData = studentsResult && "content" in studentsResult ? studentsResult : null;
  const sorted = useMemo(() => {
    if (!pageData) return [];
    const val = (s: CourseStudent): string | number => {
      switch (sort.key) {
        case "name":
          return s.student.fullName.toLocaleLowerCase("tr");
        case "enrolledAt":
          return Date.parse(s.enrolledAt);
        case "progress":
          return s.progressPercent;
        case "lastAccessedAt":
          return s.lastAccessedAt ? Date.parse(s.lastAccessedAt) : -1;
      }
    };
    const rows = [...pageData.content];
    rows.sort((a, b) => {
      const x = val(a);
      const y = val(b);
      const c = typeof x === "string" && typeof y === "string" ? x.localeCompare(y, "tr") : (x as number) - (y as number);
      return sort.dir === "asc" ? c : -c;
    });
    return rows;
  }, [pageData, sort]);

  function toggleSort(key: SortKey) {
    setSort((s) => (s.key === key ? { key, dir: s.dir === "asc" ? "desc" : "asc" } : { key, dir: key === "name" ? "asc" : "desc" }));
  }

  function retry() {
    setStats(null);
    setStudents(null);
    setAttempt((n) => n + 1);
  }

  function sortHeader(k: SortKey, children: string) {
    const active = sort.key === k;
    return (
      <th key={k} scope="col" aria-sort={active ? (sort.dir === "asc" ? "ascending" : "descending") : "none"}>
        <button type="button" className="an-sort" onClick={() => toggleSort(k)}>
          {children} <span aria-hidden="true">{active ? (sort.dir === "asc" ? "▲" : "▼") : "↕"}</span>
        </button>
      </th>
    );
  }

  const back = (
    <Link className="qb-btn" href="/instructor">
      ← Kurslarım
    </Link>
  );

  if (!validId) {
    return (
      <section className="qb an">
        <div className="qb-head">
          <h1>Kurs analitiği</h1>
          {back}
        </div>
        <p className="auth-error" role="alert">
          Geçersiz kurs adresi.
        </p>
      </section>
    );
  }

  const statsFail = statsResult && "message" in statsResult ? statsResult : null;
  const statsOk = statsResult && "enrollmentCount" in statsResult ? statsResult : null;
  const studentsFail = studentsResult && "message" in studentsResult ? studentsResult : null;
  // 403/404 tüm sayfayı kapsar (kurs ya da yetki sorunu); başka hata bölüm içinde gösterilir.
  const fatal = [statsFail, studentsFail].find((f) => f && f.kind !== "other") ?? null;

  const quizTitles = new Map(statsOk?.quizzes.map((q) => [q.quizId, q.title]));

  return (
    <section className="qb an">
      <div className="qb-head">
        <h1>Kurs analitiği</h1>
        {back}
      </div>

      {fatal && <ErrorMessage message={fatal.message} />}

      {!fatal && (
        <>
          <section aria-labelledby="an-stats-h">
            <h2 id="an-stats-h">Özet</h2>
            {!statsResult && <Loading text="İstatistikler yükleniyor…" />}
            {statsFail && <ErrorMessage message={statsFail.message} onRetry={retry} />}
            {statsOk && (
              <>
                <dl className="an-cards">
                  <div className="an-card">
                    <dt>Kayıtlı kursiyer</dt>
                    <dd>{statsOk.enrollmentCount}</dd>
                  </div>
                  <div className="an-card">
                    <dt>Ortalama ilerleme</dt>
                    <dd>{pct(statsOk.averageProgressPercent)}</dd>
                  </div>
                  <div className="an-card">
                    <dt>Tamamlama oranı</dt>
                    <dd>{pct(statsOk.completionRate)}</dd>
                  </div>
                  <div className="an-card">
                    <dt>Ortalama puan</dt>
                    <dd>
                      {statsOk.rating.averageRating != null ? `${statsOk.rating.averageRating.toFixed(1)} / 5` : "—"}
                      <span className="an-sub">{statsOk.rating.reviewCount} değerlendirme</span>
                    </dd>
                  </div>
                </dl>

                {statsOk.enrollmentCount === 0 && <EmptyState message="Bu kursa henüz kayıt olan yok." />}

                <div className="an-charts">
                  <figure className="an-chart">
                    <figcaption>Sınav başına başarı oranı</figcaption>
                    {statsOk.quizzes.length === 0 ? (
                      <p>Bu kursta sınav yok.</p>
                    ) : (
                      <>
                        <ul className="an-bars" aria-label="Sınav başına geçme oranı">
                          {statsOk.quizzes.map((q) => (
                            <BarRow
                              key={q.quizId}
                              label={q.title}
                              value={q.passRate}
                              text={`${pct(q.passRate)} (${q.attemptCount} deneme)`}
                            />
                          ))}
                        </ul>
                        <table className="an-table an-mini">
                          <caption className="an-sr">Sınav başarı oranları</caption>
                          <thead>
                            <tr>
                              <th scope="col">Sınav</th>
                              <th scope="col">Deneme</th>
                              <th scope="col">Geçme oranı</th>
                            </tr>
                          </thead>
                          <tbody>
                            {statsOk.quizzes.map((q) => (
                              <tr key={q.quizId}>
                                <th scope="row">{q.title}</th>
                                <td>{q.attemptCount}</td>
                                <td>{pct(q.passRate)}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </>
                    )}
                  </figure>

                  <figure className="an-chart">
                    <figcaption>Puan dağılımı</figcaption>
                    {statsOk.rating.reviewCount === 0 ? (
                      <p>Henüz değerlendirme yok.</p>
                    ) : (
                      <ul className="an-bars" aria-label="Puan başına değerlendirme sayısı">
                        {[5, 4, 3, 2, 1].map((n) => {
                          const c = statsOk.rating.distribution[String(n)] ?? 0;
                          return (
                            <BarRow
                              key={n}
                              label={`${n} yıldız`}
                              value={c}
                              max={statsOk.rating.reviewCount}
                              text={`${c} değerlendirme`}
                            />
                          );
                        })}
                      </ul>
                    )}
                  </figure>

                  <figure className="an-chart">
                    <figcaption>İlerleme dağılımı</figcaption>
                    {statsOk.enrollmentCount === 0 ? (
                      <p>Gösterilecek kursiyer yok.</p>
                    ) : (
                      <ul className="an-bars" aria-label="İlerleme aralığına göre kursiyer sayısı">
                        {statsOk.progressDistribution.map((b) => (
                          <BarRow
                            key={b.minPercent}
                            label={bucketLabel(b)}
                            value={b.count}
                            max={statsOk.enrollmentCount}
                            text={`${b.count} kursiyer`}
                          />
                        ))}
                      </ul>
                    )}
                  </figure>
                </div>
              </>
            )}
          </section>

          <section aria-labelledby="an-students-h">
            <h2 id="an-students-h">Kursiyerler</h2>
            {!studentsResult && <Loading text="Kursiyerler yükleniyor…" />}
            {studentsFail && <ErrorMessage message={studentsFail.message} onRetry={retry} />}
            {pageData && pageData.content.length === 0 && <EmptyState message="Henüz kayıtlı kursiyer yok." />}
            {pageData && pageData.content.length > 0 && (
              <>
                <div className="an-scroll">
                  <table className="an-table">
                    <caption className="an-sr">
                      Kursiyer ilerleme tablosu; sütun başlıklarındaki düğmelerle sıralanır (bu sayfadaki kayıtlar).
                    </caption>
                    <thead>
                      <tr>
                        {sortHeader("name", "Kursiyer")}
                        {sortHeader("enrolledAt", "Kayıt tarihi")}
                        {sortHeader("progress", "İlerleme")}
                        {sortHeader("lastAccessedAt", "Son erişim")}
                        <th scope="col">Sınavlar (en iyi sonuç)</th>
                      </tr>
                    </thead>
                    <tbody>
                      {sorted.map((s) => (
                        <tr key={s.student.id}>
                          <th scope="row">{s.student.fullName}</th>
                          <td>{fmtDate(s.enrolledAt)}</td>
                          <td>
                            <span className="an-progress">
                              <span className="an-bar-track" aria-hidden="true">
                                <span className="an-bar-fill" style={{ width: `${Math.min(100, s.progressPercent)}%` }} />
                              </span>
                              {pct(s.progressPercent)}
                            </span>
                          </td>
                          <td>{fmtDate(s.lastAccessedAt)}</td>
                          <td>
                            {s.quizResults.length === 0 ? (
                              "—"
                            ) : (
                              <ul className="an-results">
                                {s.quizResults.map((r) => (
                                  <li key={r.quizId}>
                                    {quizTitles.get(r.quizId) ?? `Sınav ${r.quizId}`}: {pct(r.bestPercent)}{" "}
                                    <span aria-hidden="true">{r.passed ? "✓" : "✗"}</span>
                                    <span className="an-sr">{r.passed ? " geçti" : " kaldı"}</span>
                                  </li>
                                ))}
                              </ul>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <nav className="an-pager" aria-label="Kursiyer sayfaları">
                  <button type="button" className="qb-btn" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                    Önceki
                  </button>
                  <span role="status">
                    Sayfa {pageData.page + 1} / {Math.max(1, pageData.totalPages)} · {pageData.totalElements} kursiyer
                  </span>
                  <button
                    type="button"
                    className="qb-btn"
                    disabled={page + 1 >= pageData.totalPages}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    Sonraki
                  </button>
                </nav>
              </>
            )}
          </section>
        </>
      )}
    </section>
  );
}
