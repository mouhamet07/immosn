package sn.immosn.backend.client.web.discussion.dto;

import jakarta.validation.constraints.NotBlank;

public record MessageCreateRequestDto(

    @NotBlank(message = "Le contenu du message ne peut pas être vide")
    String contenu
) {}
