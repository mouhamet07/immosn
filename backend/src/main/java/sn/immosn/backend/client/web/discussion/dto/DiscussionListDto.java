package sn.immosn.backend.client.web.discussion.dto;

import java.time.LocalDateTime;

public record DiscussionListDto(
    Long id,
    Long annonceId,
    String annonceLibelle,
    String annonceAdresse,
    String imagePrincipale,
    Long clientId,
    String clientNom,
    String dernierMessage,
    SenderRoleDto dernierMessageRole,
    long unreadCount,
    LocalDateTime createdAt,
    LocalDateTime dernierMessageAt
) {
    public enum SenderRoleDto { CLIENT, ADMIN }
}
