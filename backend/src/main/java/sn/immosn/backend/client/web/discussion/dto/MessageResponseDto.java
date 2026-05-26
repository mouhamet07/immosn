package sn.immosn.backend.client.web.discussion.dto;

import sn.immosn.backend.discussion.data.entity.SenderRole;

import java.time.LocalDateTime;

public record MessageResponseDto(
    Long id,
    String contenu,
    SenderRole senderRole,
    String senderName,
    boolean isRead,
    LocalDateTime createdAt
) {}
