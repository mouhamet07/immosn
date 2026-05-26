package sn.immosn.backend.client.web.visite.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record UpdateDateVisiteDto(
    @NotNull @Future LocalDateTime dateVisite,
    String commentaire
) {}
