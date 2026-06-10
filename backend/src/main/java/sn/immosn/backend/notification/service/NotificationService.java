package sn.immosn.backend.notification.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import sn.immosn.backend.notification.dto.NotificationResponseDto;
import sn.immosn.backend.notification.entity.Notification;

import java.util.List;

public interface NotificationService {

    Notification save(Long userId, String type, String title, String message, Long entityId, String entityType);

    void saveForAdmins(String type, String title, String message, Long entityId, String entityType);

    List<Notification> getUnreadSince(Long userId, Long lastSeenNotificationId);

    List<NotificationResponseDto> getUnreadNotifications(Long userId);

    Page<NotificationResponseDto> getHistory(Long userId, Pageable pageable);

    long countUnread(Long userId);

    NotificationResponseDto markAsRead(Long notificationId, Long userId);

    int markAllAsRead(Long userId);
}
