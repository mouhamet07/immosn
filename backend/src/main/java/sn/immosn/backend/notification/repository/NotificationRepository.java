package sn.immosn.backend.notification.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.immosn.backend.notification.entity.Notification;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdAndIsReadFalseOrderByIdAsc(Long userId);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.isRead = false AND n.id > :lastSeenId ORDER BY n.id ASC")
    List<Notification> findUnreadAfter(@Param("userId") Long userId, @Param("lastSeenId") Long lastSeenId);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.isRead = false AND n.createdAt >= :since ORDER BY n.id ASC")
    List<Notification> findUnreadSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
