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
    // Prospect non converti (visiteur sans compte) — null si la discussion appartient à un client authentifié
    Long prospectId,
    String prospectNom,
    String prospectPrenom,
    String prospectEmail,
    String prospectTelephone,
    String dernierMessage,
    SenderRoleDto dernierMessageRole,
    long unreadCount,
    LocalDateTime createdAt,
    LocalDateTime dernierMessageAt
) {
    public enum SenderRoleDto { CLIENT, ADMIN }
}
