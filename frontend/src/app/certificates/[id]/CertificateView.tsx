"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { ApiError } from "@/lib/api/client";
import { getMyCertificate, type CertificateResponse } from "@/lib/api/certificates";
import { CertificateNotice, formatCertificateDate } from "@/components/certificate/CertificateNotice";
import styles from "../certificate.module.css";
import { Loading } from "@/components/common/Loading";

type Loaded =
  | { key: string; kind: "ok"; certificate: CertificateResponse }
  | { key: string; kind: "notFound" }
  | { key: string; kind: "error"; message: string };

/** Doğrulama sayfasının tam adresi; sertifikaya yazılır ki basılı belgeden de açılabilsin. */
function verifyUrl(code: string): string {
  return `${window.location.origin}/verify/${encodeURIComponent(code)}`;
}

/**
 * Yazdırılabilir sertifika (#131): kursiyer adı, kurs, tarih, doğrulama kodu ve bağlantısı, akreditasyon dışı ibaresi
 * (FR-29). "Yazdır" tarayıcının yazdırma penceresini açar; site başlığı ve düğmeler basılmaz (globals.css › print).
 */
export function CertificateView() {
  const { id: rawId } = useParams<{ id: string }>();
  const id = /^\d+$/.test(rawId) ? Number(rawId) : null;
  const [retry, setRetry] = useState(0);
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const key = `${rawId}#${retry}`;
  const state: Loaded | null = id === null ? { key, kind: "notFound" } : loaded?.key === key ? loaded : null;

  useEffect(() => {
    if (id === null) return;
    let cancelled = false;
    getMyCertificate(id)
      .then((certificate) => !cancelled && setLoaded({ key, kind: "ok", certificate }))
      .catch((err: unknown) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 404) setLoaded({ key, kind: "notFound" });
        else
          setLoaded({
            key,
            kind: "error",
            message:
              err instanceof ApiError && err.body?.message
                ? err.body.message
                : "Sertifika yüklenemedi. Bağlantınızı kontrol edip tekrar deneyin.",
          });
      });
    return () => {
      cancelled = true;
    };
  }, [id, key]);

  if (state === null) return <Loading text="Sertifika yükleniyor…" />;
  if (state.kind === "notFound") {
    return (
      <div role="alert">
        <p className={styles.error}>Sertifika bulunamadı.</p>
        <Link className="touch-link" href="/certificates">Sertifikalarım</Link>
      </div>
    );
  }
  if (state.kind === "error") {
    return (
      <div role="alert">
        <p className={styles.error}>{state.message}</p>
        <button type="button" className={styles.button} onClick={() => setRetry((r) => r + 1)}>
          Tekrar dene
        </button>
      </div>
    );
  }

  const c = state.certificate;
  const url = verifyUrl(c.verificationCode);
  return (
    <section>
      <div className={styles.actions}>
        <button type="button" className={`${styles.button} ${styles.primary}`} onClick={() => window.print()}>
          Yazdır
        </button>
        <Link href="/certificates" className={styles.button}>
          ← Sertifikalarım
        </Link>
      </div>
      <article className={styles.certificate} aria-label="Sertifika">
        <p className={styles.kicker}>Mini Udemy · Katılım Sertifikası</p>
        <p>Bu belge,</p>
        <h1 className={styles.name}>{c.studentName}</h1>
        <p>adlı kursiyerin aşağıdaki kursu tamamladığını gösterir:</p>
        <p className={styles.course}>{c.courseTitle}</p>
        {c.instructorName && <p>Eğitmen: {c.instructorName}</p>}
        <div className={styles.details}>
          <p>Tarih: {formatCertificateDate(c.issuedAt)}</p>
          <p>
            Doğrulama kodu: <span className={styles.code}>{c.verificationCode}</span>
          </p>
        </div>
        <p className={styles.link}>
          Doğrulama: <a href={url}>{url}</a>
        </p>
        <CertificateNotice className={styles.notice} />
      </article>
    </section>
  );
}
