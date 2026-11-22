"use client";

import { useCallback, useEffect, useId, useRef, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorMessage } from "@/components/common/ErrorMessage";
import { Loading } from "@/components/common/Loading";
import { ApiError } from "@/lib/api/client";
import {
  getUnreadNotificationCount,
  listMyNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  type NotificationResponse,
} from "@/lib/api/notifications";
import styles from "./notification.module.css";

/** Panelde bir seferde gösterilen bildirim sayısı; fazlası "Daha fazla göster" ile gelir. */
const PAGE_SIZE = 10;
/** Sekme görünürken okunmamış sayısının yenilenme aralığı. */
const POLL_MS = 60_000;

type ListState =
  | { state: "loading" }
  | { state: "error"; message: string }
  | { state: "ready"; items: NotificationResponse[]; page: number; totalPages: number };

function errorMessage(e: unknown, fallback: string): string {
  return e instanceof ApiError && e.body?.message ? e.body.message : fallback;
}

const relative = new Intl.RelativeTimeFormat("tr", { numeric: "auto" });
const absolute = new Intl.DateTimeFormat("tr-TR", { dateStyle: "medium", timeStyle: "short" });

/** "az önce", "5 dakika önce", "dün" …; bir haftadan eskiyse tarih. */
function formatTime(iso: string, now: number): string {
  const seconds = Math.round((new Date(iso).getTime() - now) / 1000);
  const abs = Math.abs(seconds);
  if (abs < 60) return "az önce";
  if (abs < 3600) return relative.format(Math.round(seconds / 60), "minute");
  if (abs < 86_400) return relative.format(Math.round(seconds / 3600), "hour");
  if (abs < 7 * 86_400) return relative.format(Math.round(seconds / 86_400), "day");
  return absolute.format(new Date(iso));
}

function BellIcon() {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true" focusable="false">
      <path
        d="M12 3a6 6 0 0 0-6 6v3.6l-1.6 2.7A1 1 0 0 0 5.3 17h13.4a1 1 0 0 0 .9-1.7L18 12.6V9a6 6 0 0 0-6-6Zm-2.5 15a2.5 2.5 0 0 0 5 0"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

/**
 * Üst menüdeki bildirim zili (#136, FR-44): okunmamış sayısı ve açılır bildirim listesi.
 *
 * - Sayı girişte, her sayfa geçişinde, sekmeye dönülünce ve sekme görünürken {@link POLL_MS}'de bir yenilenir;
 *   sekme gizliyken zamanlayıcı durur.
 * - Liste panel her açıldığında baştan çekilir (en yeni önce, {@link PAGE_SIZE}'lık sayfalar).
 * - Bildirime tıklamak onu okundu işaretler, bağlantısı varsa o sayfaya gider.
 *
 * Header bileşeni kullanıcı değişince `key` ile yeniden kurar; durum bir kullanıcıdan ötekine taşınmaz.
 * Header kök layout'ta olduğundan `cacheComponents` onu gizlemez; zamanlayıcı yine de efekt temizliğinde durur.
 */
export function NotificationBell({ onNavigate }: { onNavigate?: () => void }) {
  const router = useRouter();
  const pathname = usePathname();
  const panelId = useId();
  const rootRef = useRef<HTMLDivElement>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);
  // Panel art arda açılıp kapanınca eski isteğin yanıtı yenisinin üstüne yazılmasın.
  const listRequest = useRef(0);

  const [count, setCount] = useState(0);
  const [open, setOpen] = useState(false);
  const [list, setList] = useState<ListState>({ state: "loading" });
  const [loadingMore, setLoadingMore] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [now, setNow] = useState(() => Date.now());

  const refreshCount = useCallback(async () => {
    try {
      const { count: unread } = await getUnreadNotificationCount();
      setCount(unread);
    } catch {
      // Sayaç yardımcı bir bilgi; alınamazsa eski değer kalır, bir sonraki yenilemede yeniden denenir.
    }
  }, []);

  // Girişte ve her sayfa geçişinde.
  useEffect(() => {
    let cancelled = false;
    getUnreadNotificationCount()
      .then(({ count: unread }) => !cancelled && setCount(unread))
      .catch(() => {
        // refreshCount'taki gibi: alınamazsa eski değer kalır.
      });
    return () => {
      cancelled = true;
    };
  }, [pathname]);

  // Sekme görünürken düzenli aralıkla; sekmeye dönülünce hemen.
  useEffect(() => {
    let timer: number | undefined;
    const stop = () => {
      if (timer !== undefined) window.clearInterval(timer);
      timer = undefined;
    };
    const start = () => {
      stop();
      if (document.visibilityState === "visible") timer = window.setInterval(() => void refreshCount(), POLL_MS);
    };
    const onVisibility = () => {
      if (document.visibilityState === "visible") {
        void refreshCount();
        start();
      } else {
        stop();
      }
    };
    start();
    document.addEventListener("visibilitychange", onVisibility);
    return () => {
      stop();
      document.removeEventListener("visibilitychange", onVisibility);
    };
  }, [refreshCount]);

  const loadFirstPage = useCallback(async () => {
    const request = ++listRequest.current;
    setList({ state: "loading" });
    setActionError(null);
    setNow(Date.now());
    try {
      const page = await listMyNotifications({ page: 0, size: PAGE_SIZE });
      if (request !== listRequest.current) return;
      setList({ state: "ready", items: page.content, page: page.page, totalPages: page.totalPages });
    } catch (e) {
      if (request !== listRequest.current) return;
      setList({ state: "error", message: errorMessage(e, "Bildirimler yüklenemedi.") });
    }
    void refreshCount();
  }, [refreshCount]);

  // Açıkken: Esc ile ve panelin dışına tıklayınca kapanır.
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        setOpen(false);
        buttonRef.current?.focus();
      }
    };
    const onPointer = (e: PointerEvent) => {
      if (rootRef.current && !rootRef.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("keydown", onKey);
    document.addEventListener("pointerdown", onPointer);
    return () => {
      document.removeEventListener("keydown", onKey);
      document.removeEventListener("pointerdown", onPointer);
    };
  }, [open]);

  function toggle() {
    if (open) {
      setOpen(false);
      return;
    }
    setOpen(true);
    void loadFirstPage();
  }

  async function loadMore() {
    if (list.state !== "ready") return;
    const request = listRequest.current;
    setLoadingMore(true);
    setActionError(null);
    try {
      const next = await listMyNotifications({ page: list.page + 1, size: PAGE_SIZE });
      if (request !== listRequest.current) return;
      setList((current) => {
        if (current.state !== "ready") return current;
        // Bu arada yeni bildirim geldiyse sayfa kayar; aynı bildirim iki kez görünmesin.
        const seen = new Set(current.items.map((n) => n.id));
        return {
          state: "ready",
          items: [...current.items, ...next.content.filter((n) => !seen.has(n.id))],
          page: next.page,
          totalPages: next.totalPages,
        };
      });
    } catch (e) {
      setActionError(errorMessage(e, "Bildirimler yüklenemedi."));
    } finally {
      setLoadingMore(false);
    }
  }

  function markLocallyRead(predicate: (n: NotificationResponse) => boolean) {
    setList((current) =>
      current.state === "ready"
        ? { ...current, items: current.items.map((n) => (predicate(n) ? { ...n, read: true } : n)) }
        : current,
    );
  }

  async function openNotification(n: NotificationResponse) {
    setActionError(null);
    if (!n.read) {
      setBusy(true);
      try {
        await markNotificationRead(n.id);
      } catch (e) {
        // Okundu işaretlenemediyse panel açık kalır ve sebep gösterilir; sayfaya geçilmez.
        setActionError(errorMessage(e, "Bildirim okundu işaretlenemedi."));
        return;
      } finally {
        setBusy(false);
      }
      markLocallyRead((x) => x.id === n.id);
      setCount((c) => Math.max(0, c - 1));
    }
    if (n.link) {
      setOpen(false);
      onNavigate?.();
      router.push(n.link);
    }
  }

  async function readAll() {
    setActionError(null);
    setBusy(true);
    try {
      await markAllNotificationsRead();
      markLocallyRead(() => true);
      setCount(0);
    } catch (e) {
      setActionError(errorMessage(e, "Bildirimler okundu işaretlenemedi."));
    } finally {
      setBusy(false);
    }
  }

  const badge = count > 99 ? "99+" : String(count);
  const label = count > 0 ? `Bildirimler, ${count} okunmamış` : "Bildirimler";

  return (
    <div className={styles.bell} ref={rootRef}>
      <button
        type="button"
        ref={buttonRef}
        className={styles.trigger}
        aria-label={label}
        aria-expanded={open}
        aria-controls={panelId}
        onClick={toggle}
      >
        <BellIcon />
        <span className={styles.triggerText} aria-hidden="true">
          Bildirimler
        </span>
        {count > 0 && (
          <span className={styles.badge} aria-hidden="true">
            {badge}
          </span>
        )}
      </button>

      {open && (
        <div id={panelId} className={styles.panel} role="region" aria-label="Bildirimler">
          <div className={styles.panelHeader}>
            <h2 className={styles.panelTitle}>Bildirimler</h2>
            <button type="button" className={styles.readAll} onClick={readAll} disabled={busy || count === 0}>
              Hepsini okundu işaretle
            </button>
          </div>

          {actionError && <p className={styles.actionError} role="alert">{actionError}</p>}

          {list.state === "loading" && <Loading text="Bildirimler yükleniyor…" />}
          {list.state === "error" && <ErrorMessage message={list.message} onRetry={() => void loadFirstPage()} />}
          {list.state === "ready" && list.items.length === 0 && <EmptyState message="Henüz bildiriminiz yok." />}
          {list.state === "ready" && list.items.length > 0 && (
            <ul className={styles.list}>
              {list.items.map((n) => (
                <li key={n.id}>
                  <button
                    type="button"
                    className={n.read ? styles.item : `${styles.item} ${styles.unread}`}
                    onClick={() => void openNotification(n)}
                    disabled={busy}
                  >
                    <span className={styles.itemTitle}>
                      {!n.read && <span className={styles.dot} aria-hidden="true" />}
                      {n.title}
                      {!n.read && <span className={styles.srOnly}> (okunmadı)</span>}
                    </span>
                    {n.body && <span className={styles.itemBody}>{n.body}</span>}
                    <time className={styles.itemTime} dateTime={n.createdAt} title={absolute.format(new Date(n.createdAt))}>
                      {formatTime(n.createdAt, now)}
                    </time>
                  </button>
                </li>
              ))}
            </ul>
          )}
          {list.state === "ready" && list.page + 1 < list.totalPages && (
            <button type="button" className={styles.more} onClick={() => void loadMore()} disabled={loadingMore}>
              {loadingMore ? "Yükleniyor…" : "Daha fazla göster"}
            </button>
          )}
        </div>
      )}
    </div>
  );
}
