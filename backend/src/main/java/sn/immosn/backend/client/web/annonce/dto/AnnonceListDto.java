package sn.immosn.backend.client.web.annonce.dto;

import sn.immosn.backend.annonce.data.entity.TypeTransaction;
import sn.immosn.backend.client.web.proprietaire.dto.ProprietaireSummaryDto;

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
    TypeTransaction typeTransaction,
    Integer nbrPieces,
    Integer nbrSallesBain,
    Double surface,
    String description,
    String imagePrincipale,
    LocalDateTime createdAt,
    Boolean archived,
    Boolean isNew,
    Boolean isExclusivite,
    ProprietaireSummaryDto owner
) {
}
