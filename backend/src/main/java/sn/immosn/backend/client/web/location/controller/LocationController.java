package sn.immosn.backend.client.web.location.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import sn.immosn.backend.client.web.location.dto.GeocodeResponseDto;
import sn.immosn.backend.location.service.GeoCodingService;
import sn.immosn.backend.location.service.LocationService;
import sn.immosn.backend.shared.response.RestResponse;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
@Tag(name = "LOCALISATION", description = "Données géographiques du Sénégal — départements, quartiers et géocodage pour les annonces et la recherche")
public class LocationController {

    private final LocationService locationService;
    private final GeoCodingService geoCodingService;

    @Operation(
        summary = "Lister les départements",
        description = """
            Retourne la liste de tous les départements du Sénégal disponibles sur la plateforme.

            Utilisé pour alimenter les sélecteurs de localisation dans :
            - Le formulaire de création/modification d'annonce
            - Les filtres de recherche multicritère

            **Accès : PUBLIC — aucun token requis**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste des départements",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                        "success": true, "status": 200,
                        "data": ["Dakar", "Pikine", "Guédiawaye", "Rufisque", "Thiès", "Mbour", "Tivaouane", "Saint-Louis"]
                    }""")))
    })
    @GetMapping("/departements")
    public ResponseEntity<RestResponse<List<String>>> getDepartements() {
        return ResponseEntity.ok(
            RestResponse.success(locationService.getDepartements(), HttpStatus.OK)
        );
    }

    @Operation(
        summary = "Lister les quartiers d'un département",
        description = """
            Retourne la liste des quartiers disponibles pour un département donné.

            Utilisé en cascade après la sélection du département dans les formulaires.

            **Accès : PUBLIC — aucun token requis**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste des quartiers du département",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true, "status": 200,
                      "data": ["Almadies", "Ngor", "Ouakam", "Mermoz", "Sacré-Cœur", "Point E", "Fann", "Liberté"]
                    }"""))),
        @ApiResponse(responseCode = "400", description = "Paramètre 'departement' manquant",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Département non trouvé dans le référentiel",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/quartiers")
    public ResponseEntity<RestResponse<List<String>>> getQuartiers(
            @Parameter(description = "Nom du département", required = true, example = "Dakar")
            @RequestParam String departement) {
        return ResponseEntity.ok(
            RestResponse.success(locationService.getQuartiersByDepartement(departement), HttpStatus.OK)
        );
    }

    @Operation(
        summary = "Géocoder une adresse",
        description = """
            Convertit une adresse (département + quartier + adresse optionnelle) en coordonnées GPS
            (latitude, longitude).

            Utilisé lors de la création ou modification d'une annonce pour calculer
            automatiquement la position sur la carte.

            Si les coordonnées ne peuvent pas être trouvées, retourne `{"latitude": null, "longitude": null}`.

            **Accès : PUBLIC — aucun token requis**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Coordonnées GPS calculées (null si non trouvées)",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(name = "Succès géocodage", value = """
                    {
                      "success": true, "status": 200,
                      "data": {"latitude": 14.7462, "longitude": -17.5185}
                    }"""))),
        @ApiResponse(responseCode = "400", description = "Paramètres manquants (departement ou quartier obligatoires)",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/geocode")
    public ResponseEntity<RestResponse<GeocodeResponseDto>> geocode(
            @Parameter(description = "Nom du département", required = true, example = "Dakar")
            @RequestParam String departement,
            @Parameter(description = "Nom du quartier", required = true, example = "Almadies")
            @RequestParam String quartier,
            @Parameter(description = "Adresse complémentaire (optionnel)", example = "Route des Almadies, Villa 12")
            @RequestParam(required = false) String adresse) {
        double[] coords = geoCodingService.geocode(quartier, departement, adresse).orElse(null);
        GeocodeResponseDto result = coords != null
            ? new GeocodeResponseDto(coords[0], coords[1])
            : new GeocodeResponseDto(null, null);
        return ResponseEntity.ok(RestResponse.success(result, HttpStatus.OK));
    }
}
