package sn.immosn.backend.client.web.annonce.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AnnonceResponseDto(
    Long id,
    String libelle,
    String description,
    Integer nbrPieces,
    Double surface,
    Double prix,
    String adresse,
    TypeBienResponseDto typeBien,
    List<CommoditeResponseDto> commodites,
    List<String> images,
    Boolean archived,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    
}
