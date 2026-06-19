package sn.immosn.backend.client.web.visite.dto;

import sn.immosn.backend.annonce.data.entity.TypeTransaction;
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
    TypeTransaction typeTransaction,
    LocalDateTime dateVisite,
    StatutDemandeVisite statut,
    String commentaire,
    // Sprint 2 — affectation / replanification
    Long adminResponsableId,
    String adminResponsableNom,
    LocalDateTime dateReplanificationProposee,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
