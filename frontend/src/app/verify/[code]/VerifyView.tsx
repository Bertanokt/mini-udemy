"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { ApiError } from "@/lib/api/client";
import { verifyCertificate, type CertificateVerificationResponse } from "@/lib/api/certificates";
import { CertificateNotice, formatCertificateDate } from "@/components/certificate/CertificateNotice";
import styles from "../../certificates/certificate.module.css";

type Loaded =
  | { key: string; kind: "ok"; result: CertificateVerificationResponse }
  | { key: string; kind: "notFound" }
  | { key: string; kind: "error"; message: string };

/**
 * Herkese açık sertifika doğrulama (#131): girişsiz çalışır. Geçerli kodda kursiyer adı, kurs ve tarih gösterilir
 * (API yalnız bunları döner, #130); geçersiz kodda açık bir "bulunamadı" mesajı.
 */
export function VerifyView() {
  const { code: rawCode } = useParams<{ code: string }>();
  const code = decodeURIComponent(rawCode ?? "");
  const [retry, setRetry] = useState(0);
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const key = `${code}#${retry}`;
  const state = loaded?.key === key ? loaded : null;

  useEffect(() => {
    let cancelled = false;
    verifyCertificate(code)
      .then((result) => !cancelled && setLoaded({ key, kind: "ok", result }))
      .catch((err: unknown) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 404) setLoaded({ key, kind: "notFound" });
        else
          setLoaded({
            key,
            kind: "error",
            message: "Doğrulama yapılamadı. Bağlantınızı kontrol edip tekrar deneyin.",
          });
      });
    return () => {
      cancelled = true;
    };
  }, [code, key]);

  return (
    <section>
      <h1>Sertifika doğrulama</h1>
      <p className={styles.meta}>
        Doğrulama kodu: <span className={styles.code}>{code}</span>
      </p>
      {state === null && <p className={styles.status}>Doğrulanıyor…</p>}
      {state?.kind === "notFound" && (
        <div role="alert">
          <p className={styles.error}>Bu kodla bir sertifika bulunamadı. Kodu sertifikadaki gibi yazdığınızdan emin olun.</p>
        </div>
      )}
      {state?.kind === "error" && (
        <div role="alert">
          <p className={styles.error}>{state.message}</p>
          <button type="button" className={styles.button} onClick={() => setRetry((r) => r + 1)}>
            Tekrar dene
          </button>
        </div>
      )}
      {state?.kind === "ok" && (
        <article className={styles.certificate} aria-label="Doğrulanan sertifika">
          <p className={styles.valid}>✓ Geçerli sertifika</p>
          <h2 className={styles.name}>{state.result.studentName}</h2>
          <p className={styles.course}>{state.result.courseTitle}</p>
          <p>Tarih: {formatCertificateDate(state.result.issuedAt)}</p>
          <CertificateNotice className={styles.notice} />
        </article>
      )}
      <p>
        <Link className="touch-link" href="/">Kurslara göz at</Link>
      </p>
    </section>
  );
}
