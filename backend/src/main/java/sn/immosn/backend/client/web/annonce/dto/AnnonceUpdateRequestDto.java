package sn.immosn.backend.client.web.annonce.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import sn.immosn.backend.annonce.data.entity.TypeTransaction;

import java.math.BigDecimal;
import java.util.List;

public record AnnonceUpdateRequestDto(

    @Size(min = 3, max = 200, message = "Le libellé doit contenir entre 3 et 200 caractères")
    String libelle,

    @Size(max = 5000)
    String description,

    // Autorisé à 0 : un terrain / une parcelle n'a pas de pièces
    @PositiveOrZero(message = "Le nombre de pièces ne peut pas être négatif")
    Integer nbrPieces,

    // Autorisé à 0 : un terrain / une parcelle n'a pas de salle de bain
    @Min(value = 0, message = "Le nombre de salles de bains ne peut pas être négatif")
    Integer nbrSallesBain,

    @Positive(message = "La surface doit être positive")
    Double surface,

    @Positive(message = "Le prix doit être positif")
    BigDecimal prix,

    @JsonAlias("adresse")
    @Size(max = 500)
    String adresseExacte,

    @Size(min = 1, message = "La région ne peut pas être vide")
    String region,

    @Size(min = 1, message = "Le département ne peut pas être vide")
    String departement,

    @Size(min = 1, message = "Le quartier ne peut pas être vide")
    String quartier,

    Long typeBienId,

    TypeTransaction typeTransaction,

    List<Long> commoditeIds,

    // Toutes les URLs d'images DOIVENT provenir de Cloudinary
    List<@Pattern(
        regexp = "^https://res\\.cloudinary\\.com/.+",
        message = "Les images doivent être des URLs Cloudinary valides"
    ) String> images,

    Boolean isExclusivite,

    // Optionnel : null = pas de changement. Pour retirer le propriétaire, voir AnnonceService.
    Long proprietaireId
) {}
