import Link from "next/link";
import styles from "./status.module.css";

/** Boş durumdaki eylem: bir sayfaya bağlantı ya da aynı sayfada bir işlem (ör. süzgeci temizle). */
export type EmptyAction = { label: string; href: string } | { label: string; onClick: () => void };

/**
 * Boş liste durumu (#71): ne olmadığını söyleyen kısa metin ve verilirse kullanıcıyı bir sonraki adıma götüren
 * eylem (ör. "Kurslara göz at" bağlantısı ya da "Filtreleri temizle" düğmesi). `onClick`'li eylem yalnız istemci
 * bileşeninin içinden verilebilir.
 */
export function EmptyState({ message, action }: { message: string; action?: EmptyAction }) {
  return (
    <div className={styles.empty}>
      <p>{message}</p>
      {action &&
        ("href" in action ? (
          <Link href={action.href} className={`${styles.button} ${styles.primary}`}>
            {action.label}
          </Link>
        ) : (
          <button type="button" className={styles.button} onClick={action.onClick}>
            {action.label}
          </button>
        ))}
    </div>
  );
}
