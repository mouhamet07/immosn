package sn.immosn.backend.client.web.lead.dto;

import jakarta.validation.constraints.Size;

public record UpdateNoteLeadDto(
    @Size(max = 2000, message = "La note ne peut pas dépasser 2 000 caractères.") String note
) {}
