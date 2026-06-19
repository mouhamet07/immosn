package sn.immosn.backend.client.web.visite.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Demande de replanification d'une visite (Sprint 2) : nouvelle date proposée + motif.
 */
public record ReplanificationDto(

    @NotNull(message = "La nouvelle date proposée est obligatoire")
    @Future(message = "La nouvelle date doit être dans le futur")
    LocalDateTime nouvelleDate,

    String commentaire
) {}
