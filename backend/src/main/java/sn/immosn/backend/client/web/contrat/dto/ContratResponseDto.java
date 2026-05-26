package sn.immosn.backend.client.web.contrat.dto;

import sn.immosn.backend.contrat.data.entity.StatutContrat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ContratResponseDto(
    Long id,
    Long clientId,
    String clientNom,
    Long annonceId,
    String annonceLibelle,
    String annonceAdresse,
    String imagePrincipale,
    Long leadId,
    LocalDate dateDebut,
    LocalDate dateFin,
    BigDecimal montant,
    StatutContrat statut,
    String documentUrl,
    String notes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
