package sn.immosn.backend.client.web.annonce.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

public record AnnonceResponseDto(
    Long id,
    String libelle,
    String description,
    Integer nbrPieces,
    Integer nbrSallesBain,
    Double surface,
    BigDecimal prix,
    String adresse,
    String region,
    String departement,
    String quartier,
    Double latitude,
    Double longitude,
    TypeBienResponseDto typeBien,
    List<CommoditeResponseDto> commodites,
    List<String> images,
    Boolean archived,
    Boolean isNew,
    Boolean isExclusivite,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
