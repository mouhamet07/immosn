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
    // Prospect non converti (visiteur sans compte) — null si la discussion appartient à un client authentifié
    Long prospectId,
    String prospectNom,
    String prospectPrenom,
    String prospectEmail,
    String prospectTelephone,
    List<MessageResponseDto> messages,
    long unreadCount,
    LocalDateTime createdAt
) {}
