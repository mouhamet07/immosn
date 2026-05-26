package sn.immosn.backend.client.web.visite.dto;

import sn.immosn.backend.visite.data.entity.StatutDemandeVisite;

import java.time.LocalDateTime;

public record DemandeVisiteResponseDto(
    Long id,
    Long clientId,
    String clientNom,
    Long annonceId,
    String annonceLibelle,
    String annonceAdresse,
    String imagePrincipale,
    LocalDateTime dateVisite,
    StatutDemandeVisite statut,
    String commentaire,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
