package sn.immosn.backend.client.web.annonce.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import sn.immosn.backend.annonce.data.entity.TypeTransaction;

import java.math.BigDecimal;
import java.util.List;

public record SearchAnnonceRequestDto(

    Long typeBienId,

    TypeTransaction typeTransaction,   // filtre VENTE / LOCATION (Sprint 4)

    @Positive BigDecimal prixMin,
    @Positive BigDecimal prixMax,

    String adresse,

    @Min(1) Integer nbrPieces,      // égalité exacte (conservé pour compatibilité)
    @Min(1) Integer piecesMin,      // nbrPieces >= piecesMin
    @Min(1) Integer piecesMax,      // nbrPieces <= piecesMax

    List<Long> commoditeIds,

    @Min(0) Integer page,
    @Min(1) Integer size,

    String sortBy,      // createdAt | prix
    String sortDir      // ASC | DESC
) {}
