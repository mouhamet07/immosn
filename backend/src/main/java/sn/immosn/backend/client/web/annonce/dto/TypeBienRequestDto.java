package sn.immosn.backend.client.web.annonce.dto;

import jakarta.validation.constraints.NotBlank;

public record TypeBienRequestDto(
    @NotBlank
    String libelle
) {
}
