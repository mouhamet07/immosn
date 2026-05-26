package sn.immosn.backend.client.web.lead.dto;

import jakarta.validation.constraints.NotNull;

public record LeadCreateRequestDto(
    @NotNull Long clientId,
    @NotNull Long annonceId,
    Long visiteId,
    String noteAdmin
) {}
