package sn.immosn.backend.client.web.favoris.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FavorisResponseDto(
    Long annonceId,
    String libelle,
    BigDecimal prix,
    String adresse,
    String typeBien,
    Integer nbrPieces,
    Integer nbrSallesBain,
    Double surface,
    String imagePrincipale,
    LocalDateTime addedAt
) {}
