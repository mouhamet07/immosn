package sn.immosn.backend.client.web.annonce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AnnonceListDto (
    String id,
    String libelle,
    BigDecimal prix,
    String adresse,
    TypeBienResponseDto typeBien,
    Integer nbrPieces,
    Double surface,
    String imagePrincipale,          
    LocalDateTime createdAt,
    Boolean archived
){
    
}
