package sn.immosn.backend.client.web.visite.dto;

import sn.immosn.backend.visite.data.entity.StatutDemandeVisite;

import java.time.LocalDateTime;

/**
 * Réponse à une demande de visite invité.
 * Renvoie le détail de la demande + le {@code prospectToken} de suivi, à conserver par le visiteur.
 */
public record VisiteInviteResponseDto(
    Long id,
    String prospectToken,
    String nom,
    String prenom,
    String email,
    String telephone,
    Long annonceId,
    String annonceLibelle,
    String annonceAdresse,
    LocalDateTime dateVisite,
    String heureVisite,
    StatutDemandeVisite statut,
    String commentaire,
    LocalDateTime createdAt
) {}
