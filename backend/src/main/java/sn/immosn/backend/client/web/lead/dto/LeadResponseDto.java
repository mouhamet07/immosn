package sn.immosn.backend.client.web.lead.dto;

import sn.immosn.backend.lead.data.entity.StatutLead;

import java.time.LocalDateTime;

public record LeadResponseDto(
    Long id,
    Long clientId,
    String clientNom,
    String clientEmail,
    Long annonceId,
    String annonceLibelle,
    String annonceAdresse,
    String imagePrincipale,
    Long visiteId,
    StatutLead statut,
    String noteAdmin,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
