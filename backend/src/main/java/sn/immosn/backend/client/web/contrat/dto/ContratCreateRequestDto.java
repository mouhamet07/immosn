package sn.immosn.backend.client.web.contrat.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import sn.immosn.backend.contrat.data.entity.TypeContrat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContratCreateRequestDto(
    @NotNull Long clientId,
    @NotNull Long annonceId,
    Long leadId,
    TypeContrat typeContrat,
    @NotNull LocalDate dateDebut,
    LocalDate dateFin,
    @NotNull @Positive BigDecimal montant,
    String documentUrl,
    String notes
) {}
