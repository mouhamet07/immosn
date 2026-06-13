package sn.immosn.backend.client.web.annonce.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record AnnonceCreateRequestDto(

    @NotBlank(message = "Le libellé est obligatoire")
    @Size(min = 3, max = 200, message = "Le libellé doit contenir entre 3 et 200 caractères")
    String libelle,

    @Size(max = 5000, message = "La description ne peut pas dépasser 5000 caractères")
    String description,

    @NotNull(message = "Le nombre de pièces est obligatoire")
    @Positive(message = "Le nombre de pièces doit être positif")
    Integer nbrPieces,

    @NotNull(message = "Le nombre de salles de bains est obligatoire")
    @Min(value = 1, message = "Le nombre de salles de bains doit être au minimum 1")
    Integer nbrSallesBain,

    @NotNull(message = "La surface est obligatoire")
    @Positive(message = "La surface doit être positive")
    Double surface,

    @NotNull(message = "Le prix est obligatoire")
    @Positive(message = "Le prix doit être positif")
    BigDecimal prix,

    @JsonAlias("adresse")
    @Size(max = 500, message = "L'adresse ne peut pas dépasser 500 caractères")
    String adresseExacte,

    String region,

    @NotBlank(message = "Le département est obligatoire")
    String departement,

    @NotBlank(message = "Le quartier est obligatoire")
    String quartier,

    @NotNull(message = "Le type de bien est obligatoire")
    Long typeBienId,

    List<@NotNull Long> commoditeIds,

    // Toutes les URLs d'images DOIVENT provenir de Cloudinary — le backend ne stocke pas de fichiers
    List<@Pattern(
        regexp = "^https://res\\.cloudinary\\.com/.+",
        message = "Les images doivent être des URLs Cloudinary valides (https://res.cloudinary.com/...)"
    ) String> images,

    Boolean isExclusivite
) {}
