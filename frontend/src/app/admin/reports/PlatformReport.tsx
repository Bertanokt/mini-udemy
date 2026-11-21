"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { getPlatformReport, type PlatformReportResponse } from "@/lib/api/admin";
import { ApiError } from "@/lib/api/client";
import { Loading } from "@/components/common/Loading";
import { ErrorMessage } from "@/components/common/ErrorMessage";
import { EmptyState } from "@/components/common/EmptyState";

const ROLE_LABELS: Record<string, string> = {
  STUDENT: "Öğrenci",
  INSTRUCTOR: "Eğitmen",
  ADMIN: "Yönetici",
};

const STATUS_LABELS: Record<string, string> = {
  DRAFT: "Taslak",
  IN_REVIEW: "İncelemede",
  PUBLISHED: "Yayımlandı",
  UNPUBLISHED: "Yayından kaldırıldı",
};

type Result = { kind: "error"; message: string } | { kind: "ok"; data: PlatformReportResponse };
type Loaded = { key: number; result: Result };

function errorMessage(e: unknown, fallback: string): string {
  return e instanceof ApiError && e.body?.message ? e.body.message : fallback;
}

function total(counts: Record<string, number>): number {
  return Object.values(counts).reduce((sum, n) => sum + n, 0);
}

function formatCount(n: number): string {
  return n.toLocaleString("tr-TR");
}

/**
 * Platform raporu (#147, İnce kapsam 17.11.2026): özet kartlar, rol ve durum dağılımı, en çok kayıt alan kurslar ve
 * kategori dağılımı. Sayılar tüm zamanlarındır; tarih aralığı ve grafik yok (İnce kapsamda kesildi).
 */
export function PlatformReport() {
  const [retry, setRetry] = useState(0);
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const result: Result | null = loaded?.key === retry ? loaded.result : null;

  useEffect(() => {
    let cancelled = false;
    const key = retry;
    getPlatformReport()
      .then((data) => {
        if (!cancelled) setLoaded({ key, result: { kind: "ok", data } });
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        const message = errorMessage(e, "Rapor yüklenemedi. Bağlantınızı kontrol edip tekrar deneyin.");
        setLoaded({ key, result: { kind: "error", message } });
      });
    return () => {
      cancelled = true;
    };
  }, [retry]);

  return (
    <section className="placeholder-page">
      <p>
        <Link className="touch-link" href="/admin">← Yönetici paneli</Link>
      </p>
      <h1>Platform raporu</h1>
      <p className="an-note">Sayılar tüm zamanlarındır. Tamamlanma: kursu %100 bitirmiş kayıt.</p>

      {!result && <Loading text="Rapor yükleniyor…" />}

      {result?.kind === "error" && <ErrorMessage message={result.message} onRetry={() => setRetry((n) => n + 1)} />}

      {result?.kind === "ok" && <ReportBody report={result.data} />}
    </section>
  );
}

function ReportBody({ report }: { report: PlatformReportResponse }) {
  const users = total(report.usersByRole);
  const courses = total(report.coursesByStatus);
  const published = report.coursesByStatus.PUBLISHED ?? 0;

  return (
    <>
      <section aria-labelledby="rp-summary-h">
        <h2 id="rp-summary-h">Özet</h2>
        <dl className="an-cards">
          <div className="an-card">
            <dt>Kullanıcı</dt>
            <dd>
              {formatCount(users)}
              <span className="an-sub">
                {formatCount(report.usersByRole.STUDENT ?? 0)} öğrenci, {formatCount(report.usersByRole.INSTRUCTOR ?? 0)}{" "}
                eğitmen
              </span>
            </dd>
          </div>
          <div className="an-card">
            <dt>Kurs</dt>
            <dd>
              {formatCount(courses)}
              <span className="an-sub">{formatCount(published)} yayımlanmış</span>
            </dd>
          </div>
          <div className="an-card">
            <dt>Kayıt</dt>
            <dd>
              {formatCount(report.enrollmentCount)}
              <span className="an-sub">{formatCount(report.completionCount)} tamamlanmış</span>
            </dd>
          </div>
          <div className="an-card">
            <dt>Sertifika</dt>
            <dd>{formatCount(report.certificateCount)}</dd>
          </div>
        </dl>
      </section>

      <section aria-labelledby="rp-top-h">
        <h2 id="rp-top-h">En çok kayıt alan kurslar</h2>
        {report.topCourses.length === 0 ? (
          <EmptyState message="Henüz kayıt yok." />
        ) : (
          <div className="an-scroll">
            <table className="an-table">
              <thead>
                <tr>
                  <th scope="col">#</th>
                  <th scope="col">Kurs</th>
                  <th scope="col">Kayıt</th>
                </tr>
              </thead>
              <tbody>
                {report.topCourses.map((c, i) => (
                  <tr key={c.courseId}>
                    <td>{i + 1}</td>
                    <td>
                      <Link className="touch-link" href={`/courses/${c.courseId}`}>{c.title}</Link>
                    </td>
                    <td>{formatCount(c.enrollmentCount)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <CountTable
        id="rp-roles-h"
        title="Rol bazında kullanıcılar"
        column="Rol"
        rows={Object.entries(report.usersByRole).map(([role, n]) => [ROLE_LABELS[role] ?? role, n])}
      />
      <CountTable
        id="rp-status-h"
        title="Durum bazında kurslar"
        column="Durum"
        rows={Object.entries(report.coursesByStatus).map(([status, n]) => [STATUS_LABELS[status] ?? status, n])}
      />
      <CountTable
        id="rp-category-h"
        title="Kategori bazında kurslar"
        column="Kategori"
        rows={report.categoryDistribution.map((c) => [c.name, c.courseCount])}
      />
    </>
  );
}

function CountTable({
  id,
  title,
  column,
  rows,
}: {
  id: string;
  title: string;
  column: string;
  rows: [string, number][];
}) {
  return (
    <section aria-labelledby={id}>
      <h2 id={id}>{title}</h2>
      <div className="an-scroll">
        <table className="an-table">
          <thead>
            <tr>
              <th scope="col">{column}</th>
              <th scope="col">Sayı</th>
            </tr>
          </thead>
          <tbody>
            {rows.map(([label, n]) => (
              <tr key={label}>
                <td>{label}</td>
                <td>{formatCount(n)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
