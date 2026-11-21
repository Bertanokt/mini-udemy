import { apiFetch } from "./client";

// Tipler docs/api/openapi.yaml'daki NotificationResponse, NotificationPage ve UnreadCountResponse ile aynıdır.

export type NotificationType =
  | "ENROLLMENT_CREATED"
  | "LESSON_PUBLISHED"
  | "COURSE_APPROVED"
  | "COURSE_REJECTED"
  | "QUIZ_COMPLETED"
  | "CERTIFICATE_ISSUED";

export interface NotificationResponse {
  id: number;
  type: NotificationType;
  title: string;
  body: string;
  /** Ön yüzde açılacak yol, örn. `/courses/7`; yoksa null. */
  link?: string | null;
  read: boolean;
  createdAt: string;
}

export interface NotificationPage {
  content: NotificationResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface UnreadCountResponse {
  count: number;
}

/** Giriş yapmış kullanıcının bildirimleri; sayfalı, en yeni önce. `page` 0'dan başlar. */
export function listMyNotifications(
  { page = 0, size = 10, unreadOnly = false }: { page?: number; size?: number; unreadOnly?: boolean } = {},
): Promise<NotificationPage> {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (unreadOnly) params.set("unreadOnly", "true");
  return apiFetch<NotificationPage>(`/api/me/notifications?${params}`);
}

/** Okunmamış bildirim sayısı. */
export function getUnreadNotificationCount(): Promise<UnreadCountResponse> {
  return apiFetch<UnreadCountResponse>("/api/me/notifications/unread-count");
}

/** Bildirimi okundu işaretler (idempotent); başkasınınsa ya da yoksa 404 (ApiError). */
export function markNotificationRead(id: number): Promise<void> {
  return apiFetch<void>(`/api/me/notifications/${id}/read`, { method: "POST" });
}

/** Kullanıcının tüm bildirimlerini okundu işaretler. */
export function markAllNotificationsRead(): Promise<void> {
  return apiFetch<void>("/api/me/notifications/read-all", { method: "POST" });
}
