package com.miniudemy.notification.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.miniudemy.notification.domain.Notification;

/**
 * `Notification` kalıcılığı. Okuma sorguları hep alıcıya (`userId`) göre süzülür; kullanıcı yalnız kendi bildirimini
 * görür (#125). Liste sorguları `ix_notifications_user_created` index'ini kullanır.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserId(Long userId, Pageable pageable);

    Page<Notification> findByUserIdAndReadAtIsNull(Long userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(Long userId);

    /** Bildirim yoksa da başkasınınsa da boş döner; ikisi dışarıdan ayırt edilmez. */
    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    /** Kullanıcının okunmamış bildirimlerinin hepsini tek sorguda okundu yapar; okunmuşlara dokunmaz. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.readAt = :now where n.userId = :userId and n.readAt is null")
    int markAllRead(@Param("userId") Long userId, @Param("now") Instant now);
}
