package sn.immosn.backend.client.web.visite.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record DemandeVisiteCreateRequestDto(

    @NotNull(message = "L'identifiant de l'annonce est obligatoire")
    Long annonceId,

    @NotNull(message = "La date de visite souhaitée est obligatoire")
    @Future(message = "La date de visite doit être dans le futur")
    LocalDateTime dateVisite,

    String commentaire
) {}
