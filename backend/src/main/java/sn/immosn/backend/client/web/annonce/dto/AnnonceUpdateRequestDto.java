package sn.immosn.backend.client.web.annonce.dto;

import java.util.List;
import java.math.BigDecimal;

public record AnnonceUpdateRequestDto(
    String id,
    String libelle,
    String description,
    Integer nbrPieces,
    Double surface,
    BigDecimal prix,
    String adresse,
    Long typeBienId,
    List<Long> commoditeIds,
    List<String> images
) {}
