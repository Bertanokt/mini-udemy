package com.miniudemy.notification.domain;

/**
 * Bir bildirimin türü ve kullanıcıya gösterilen metni. Alıcıdan bağımsızdır: aynı içerik birden fazla alıcıya
 * yazılabilir (yeni ders → kayıtlı kursiyerlerin hepsi). Metinler yalnız `NotificationMessages`'ta üretilir.
 *
 * @param link ön yüzde açılacak yol (örn. `/courses/7`); yoksa null
 */
public record NotificationContent(NotificationType type, String title, String body, String link) {
}
