package sn.immosn.backend.notification.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.immosn.backend.auth.data.entity.RoleType;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.notification.dto.NotificationResponseDto;
import sn.immosn.backend.notification.entity.Notification;
import sn.immosn.backend.notification.repository.NotificationRepository;
import sn.immosn.backend.notification.service.NotificationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final int REPLAY_WINDOW_DAYS = 7;

    private final NotificationRepository notificationRepository;
    private final UserRepository         userRepository;

    @Override
    @Transactional
    public Notification save(Long userId, String type, String title, String message,
                             Long entityId, String entityType) {
        Notification n = Notification.builder()
            .userId(userId)
            .type(type)
            .title(title)
            .message(message)
            .entityId(entityId)
            .entityType(entityType)
            .isRead(false)
            .build();
        return notificationRepository.save(n);
    }

    @Override
    @Transactional
    public void saveForAdmins(String type, String title, String message,
                              Long entityId, String entityType) {
        userRepository.findAdmins(Set.of(RoleType.ADMIN, RoleType.SUPER_ADMIN))
            .forEach(admin -> save(admin.getId(), type, title, message, entityId, entityType));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUnreadSince(Long userId, Long lastSeenNotificationId) {
        if (lastSeenNotificationId != null && lastSeenNotificationId > 0) {
            return notificationRepository.findUnreadAfter(userId, lastSeenNotificationId);
        }
        LocalDateTime since = LocalDateTime.now().minusDays(REPLAY_WINDOW_DAYS);
        return notificationRepository.findUnreadSince(userId, since);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByIdAsc(userId)
            .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getHistory(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public NotificationResponseDto markAsRead(Long notificationId, Long userId) {
        Notification n = notificationRepository.findById(notificationId)
            .filter(notif -> notif.getUserId().equals(userId))
            .orElseThrow(() -> new EntityNotFoundException("Notification introuvable : id=" + notificationId));
        n.setRead(true);
        n.setReadAt(LocalDateTime.now());
        return toDto(notificationRepository.save(n));
    }

    @Override
    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsRead(userId, LocalDateTime.now());
    }

    private NotificationResponseDto toDto(Notification n) {
        return new NotificationResponseDto(
            n.getId(), n.getType(), n.getTitle(), n.getMessage(),
            n.getEntityId(), n.getEntityType(),
            n.isRead(), n.getCreatedAt(), n.getReadAt()
        );
    }
}
