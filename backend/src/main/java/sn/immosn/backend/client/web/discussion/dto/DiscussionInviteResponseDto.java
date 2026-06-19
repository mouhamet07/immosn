package sn.immosn.backend.client.web.discussion.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Réponse à un contact invité : la discussion ouverte + le {@code guestToken} de suivi
 * que le visiteur doit conserver pour poursuivre la conversation sans compte.
 */
public record DiscussionInviteResponseDto(
    Long id,
    String guestToken,
    Long annonceId,
    String annonceLibelle,
    String annonceAdresse,
    String imagePrincipale,
    String guestNom,
    String guestEmail,
    List<MessageResponseDto> messages,
    LocalDateTime createdAt
) {}
