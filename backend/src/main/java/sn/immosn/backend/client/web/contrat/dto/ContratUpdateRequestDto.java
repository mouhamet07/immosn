package sn.immosn.backend.client.web.contrat.dto;

import sn.immosn.backend.contrat.data.entity.StatutContrat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContratUpdateRequestDto(
    LocalDate dateDebut,
    LocalDate dateFin,
    BigDecimal montant,
    StatutContrat statut,
    String documentUrl,
    String notes
) {}
