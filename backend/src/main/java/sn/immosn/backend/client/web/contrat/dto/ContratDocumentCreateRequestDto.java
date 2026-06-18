package sn.immosn.backend.client.web.contrat.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import sn.immosn.backend.contrat.data.entity.TypeDocumentContrat;

import java.util.List;

public record ContratDocumentCreateRequestDto(
    @NotEmpty(message = "Au moins un document doit être fourni")
    @Valid
    List<Item> documents
) {
    public record Item(
        @NotNull(message = "Le type de document est obligatoire") TypeDocumentContrat type,
        @NotNull(message = "L'URL du document est obligatoire") String url
    ) {}
}
