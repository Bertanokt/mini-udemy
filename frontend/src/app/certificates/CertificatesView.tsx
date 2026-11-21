"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { ApiError } from "@/lib/api/client";
import { listMyCertificates, type CertificateResponse } from "@/lib/api/certificates";
import { CertificateNotice, formatCertificateDate } from "@/components/certificate/CertificateNotice";
import styles from "./certificate.module.css";

type Loaded = { key: number; kind: "ok"; items: CertificateResponse[] } | { key: number; kind: "error"; message: string };

/** Sertifikalarım (#131): kursiyerin sertifikaları, en yeni önce; her biri yazdırılabilir sertifika sayfasına gider. */
export function CertificatesView() {
  const [retry, setRetry] = useState(0);
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const state = loaded?.key === retry ? loaded : null;

  useEffect(() => {
    let cancelled = false;
    listMyCertificates()
      .then((items) => !cancelled && setLoaded({ key: retry, kind: "ok", items }))
      .catch((err: unknown) => {
        if (cancelled) return;
        const message =
          err instanceof ApiError && err.body?.message
            ? err.body.message
            : "Sertifikalarınız yüklenemedi. Bağlantınızı kontrol edip tekrar deneyin.";
        setLoaded({ key: retry, kind: "error", message });
      });
    return () => {
      cancelled = true;
    };
  }, [retry]);

  return (
    <section>
      <h1>Sertifikalarım</h1>
      {state === null && <p className={styles.status}>Sertifikalarınız yükleniyor…</p>}
      {state?.kind === "error" && (
        <div role="alert">
          <p className={styles.error}>{state.message}</p>
          <button type="button" className={styles.button} onClick={() => setRetry((r) => r + 1)}>
            Tekrar dene
          </button>
        </div>
      )}
      {state?.kind === "ok" && state.items.length === 0 && (
        <div>
          <p>Henüz sertifikanız yok. Bir kursun zorunlu derslerini ve sınavlarını tamamlayınca sertifika otomatik oluşur.</p>
          <Link href="/my-courses" className={styles.button}>
            Kayıtlı kurslarım
          </Link>
        </div>
      )}
      {state?.kind === "ok" && state.items.length > 0 && (
        <>
          <ul className={styles.list}>
            {state.items.map((c) => (
              <li key={c.id} className={styles.item}>
                <div>
                  <h2>{c.courseTitle}</h2>
                  <p className={styles.meta}>
                    {formatCertificateDate(c.issuedAt)} · Doğrulama kodu: <span className={styles.code}>{c.verificationCode}</span>
                  </p>
                </div>
                <Link href={`/certificates/${c.id}`} className={`${styles.button} ${styles.primary}`}>
                  Sertifikayı görüntüle
                </Link>
              </li>
            ))}
          </ul>
          <CertificateNotice className={styles.notice} />
        </>
      )}
    </section>
  );
}
