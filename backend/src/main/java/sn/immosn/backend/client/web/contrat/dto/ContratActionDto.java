package sn.immosn.backend.client.web.contrat.dto;

import java.time.LocalDate;

public record ContratActionDto(
    LocalDate nouvelleDate,
    String motif
) {}
