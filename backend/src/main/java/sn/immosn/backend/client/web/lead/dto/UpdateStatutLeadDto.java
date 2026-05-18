package sn.immosn.backend.client.web.lead.dto;

import jakarta.validation.constraints.NotNull;
import sn.immosn.backend.lead.data.entity.StatutLead;

public record UpdateStatutLeadDto(
    @NotNull StatutLead statut,
    String noteAdmin
) {}
