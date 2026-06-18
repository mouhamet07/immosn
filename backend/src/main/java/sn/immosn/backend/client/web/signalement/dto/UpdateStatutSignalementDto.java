package sn.immosn.backend.client.web.signalement.dto;

import jakarta.validation.constraints.NotNull;
import sn.immosn.backend.signalement.data.entity.StatutSignalement;

public record UpdateStatutSignalementDto(
    @NotNull StatutSignalement statut,
    String reponseAdmin,
    /** Motif obligatoire lorsque le statut cible est REJETE (Sprint 4). */
    String motifRejet
) {}
