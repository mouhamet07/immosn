package sn.immosn.backend.client.web.annonce.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AnnonceCreateRequestDto (
    @NotBlank 
    String libelle,
    String description,
    @NotNull @Positive 
    Integer nbrPieces,
    @NotNull @Positive 
    Double surface,
    @NotNull @Positive 
    BigDecimal prix,
    @NotBlank 
    String adresse,
    @NotNull Long typeBienId,
    List<@NotNull Long> commoditeIds,    
    List<@NotBlank String> images   
){
    
}
