package sn.immosn.backend.notification.dto;

import java.time.LocalDateTime;

public record NotificationResponseDto(
    Long id,
    String type,
    String title,
    String message,
    Long entityId,
    String entityType,
    boolean isRead,
    LocalDateTime createdAt,
    LocalDateTime readAt
) {}
