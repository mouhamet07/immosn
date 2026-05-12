package sn.immosn.backend.client.web.annonce.dto;

import java.time.LocalDateTime;

public record AnnonceListDto (
    String libelle,
    Double prix,
    String adresse,
    TypeBienResponseDto typeBien,
    Integer nbrPieces,
    Double surface,
    String imagePrincipale,          
    LocalDateTime createdAt,
    Boolean archived
){
    
}
