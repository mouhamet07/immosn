package sn.immosn.backend.client.web.visite.dto;

import jakarta.validation.constraints.NotNull;
import sn.immosn.backend.contrat.data.entity.TypeContrat;

public record CloturerVisiteDto(
    @NotNull CloturageType type,
    TypeContrat typeContrat,
    Integer dureeLocationMois
) {
    public enum CloturageType {
        SANS_SUITE,
        AVEC_CONTRAT
    }
}
