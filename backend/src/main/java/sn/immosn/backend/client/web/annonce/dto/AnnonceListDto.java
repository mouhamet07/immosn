package sn.immosn.backend.client.web.annonce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AnnonceListDto (
    String id,
    String libelle,
    BigDecimal prix,
    String adresse,
    String departement,
    String quartier,
    Double latitude,
    Double longitude,
    TypeBienResponseDto typeBien,
    Integer nbrPieces,
    Double surface,
    String imagePrincipale,
    LocalDateTime createdAt,
    Boolean archived,
    Boolean isNew,
    Boolean isExclusivite
) {
}
