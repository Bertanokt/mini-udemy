"use client";

import styles from "./status.module.css";

/**
 * Hata durumu (#71): kullanıcıya gösterilebilir mesaj (`role="alert"`) ve verilirse "Tekrar dene" düğmesi.
 * Mesaj genelde API'nin standart hata gövdesindeki `message`'tır.
 */
export function ErrorMessage({
  message,
  onRetry,
  retryLabel = "Tekrar dene",
}: {
  message: string;
  onRetry?: () => void;
  retryLabel?: string;
}) {
  return (
    <div className={styles.error} role="alert">
      <p>{message}</p>
      {onRetry && (
        <button type="button" className={styles.button} onClick={onRetry}>
          {retryLabel}
        </button>
      )}
    </div>
  );
}
