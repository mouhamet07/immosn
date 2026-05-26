package sn.immosn.backend.client.web.discussion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DiscussionCreateRequestDto(

    @NotNull(message = "L'identifiant de l'annonce est obligatoire")
    Long annonceId,

    @NotBlank(message = "Le premier message ne peut pas être vide")
    String premierMessage
) {}
