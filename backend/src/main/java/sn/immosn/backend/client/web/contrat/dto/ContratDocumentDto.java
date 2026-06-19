package sn.immosn.backend.client.web.contrat.dto;

import sn.immosn.backend.contrat.data.entity.TypeDocumentContrat;

import java.time.LocalDateTime;

public record ContratDocumentDto(
    Long id,
    TypeDocumentContrat type,
    String url,
    String uploadedByNom,
    LocalDateTime createdAt
) {}
