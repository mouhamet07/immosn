package sn.immosn.backend.client.web.visite.dto;

import jakarta.validation.constraints.NotNull;

public record CloturerVisiteDto(
    @NotNull CloturageType type,
    Integer dureeLocationMois
) {
    public enum CloturageType {
        SANS_SUITE,
        AVEC_CONTRAT
    }
}
