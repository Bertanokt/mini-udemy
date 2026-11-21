import styles from "./status.module.css";

/**
 * Yükleniyor durumu (#71). Ekran okuyucuya `role="status"` ile kibarca duyurulur. Kanca kullanmadığı için sunucu
 * bileşenlerinde de (`Suspense` fallback'i) kullanılabilir.
 */
export function Loading({ text = "Yükleniyor…" }: { text?: string }) {
  return (
    <p className={styles.loading} role="status" aria-live="polite">
      {text}
    </p>
  );
}
