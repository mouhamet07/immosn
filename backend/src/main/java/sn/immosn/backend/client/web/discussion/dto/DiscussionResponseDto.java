package sn.immosn.backend.client.web.discussion.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DiscussionResponseDto(
    Long id,
    Long annonceId,
    String annonceLibelle,
    String annonceAdresse,
    String imagePrincipale,
    Long clientId,
    String clientNom,
    List<MessageResponseDto> messages,
    long unreadCount,
    LocalDateTime createdAt
) {}
