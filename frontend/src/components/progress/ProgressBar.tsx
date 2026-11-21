import styles from "./progress.module.css";

/** Yüzde, ekranda tam sayı: 66.67 → "%67". */
export function formatPercent(value: number): string {
  return `%${Math.round(Math.max(0, Math.min(100, value)))}`;
}

/** Erişilebilir ilerleme çubuğu (role=progressbar); `label` ekran okuyucuya ne ölçüldüğünü söyler. */
export function ProgressBar({ value, label, small = false }: { value: number; label: string; small?: boolean }) {
  const percent = Math.round(Math.max(0, Math.min(100, value)));
  return (
    <div
      className={`${styles.bar} ${small ? styles.small : ""}`}
      role="progressbar"
      aria-label={label}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={percent}
      aria-valuetext={`Yüzde ${percent} tamamlandı`}
    >
      <div className={styles.fill} style={{ width: `${percent}%` }} />
    </div>
  );
}
