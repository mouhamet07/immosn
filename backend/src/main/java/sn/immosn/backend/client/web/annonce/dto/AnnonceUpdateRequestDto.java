package sn.immosn.backend.client.web.annonce.dto;

import java.util.List;

public record AnnonceUpdateRequestDto(
    String libelle,
    String description,
    Integer nbrPieces,
    Double surface,
    Double prix,
    String adresse,
    Long typeBienId,
    List<Long> commoditeIds,
    List<String> images
) {}
