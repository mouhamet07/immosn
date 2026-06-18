package sn.immosn.backend.client.web.lead.dto;

import sn.immosn.backend.annonce.data.entity.TypeTransaction;
import sn.immosn.backend.lead.data.entity.StatutLead;

import java.time.LocalDateTime;

public record LeadResponseDto(
    Long id,
    Long clientId,
    String clientNom,
    String clientEmail,
    Long prospectId,
    String prospectNom,
    Long annonceId,
    String annonceLibelle,
    String annonceAdresse,
    String imagePrincipale,
    TypeTransaction typeTransaction,
    Long visiteId,
    Long contratId,
    StatutLead statut,
    String noteAdmin,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime convertedAt
) {}
