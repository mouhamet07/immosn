package sn.immosn.backend.client.web.visite.dto;

import jakarta.validation.constraints.NotNull;
import sn.immosn.backend.visite.data.entity.StatutDemandeVisite;

public record UpdateStatutVisiteDto(
    @NotNull StatutDemandeVisite statut,
    String commentaire
) {}
