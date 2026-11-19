package com.miniudemy.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.miniudemy.notification.domain.Notification;

/** `Notification` kalıcılığı. Listeleme ve okundu işaretleme sorguları #125'te eklenir. */
public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
