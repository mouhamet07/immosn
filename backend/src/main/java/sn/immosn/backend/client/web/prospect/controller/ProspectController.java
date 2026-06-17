package sn.immosn.backend.client.web.prospect.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.immosn.backend.client.web.prospect.dto.ConversionResultDto;
import sn.immosn.backend.prospect.service.ProspectConversionService;
import sn.immosn.backend.shared.response.RestResponse;

@RestController
@RequestMapping("/api/v1/prospects")
@RequiredArgsConstructor
@Tag(name = "PROSPECTS", description = "Conversion d'un prospect en compte client (Sprint 3)")
@SecurityRequirement(name = "bearerAuth")
public class ProspectController {

    private final ProspectConversionService conversionService;

    @Operation(
        summary = "Convertir un prospect en compte client",
        description = """
            Crée automatiquement un compte CLIENT depuis un prospect, génère un mot de passe
            temporaire (à changer à la première connexion), rattache ses discussions et visites,
            puis transmet les identifiants. Le prospect ne passe jamais par l'inscription classique.

            Idempotent : un prospect déjà converti renvoie le compte existant.

            **Accès : SUPER_ADMIN uniquement**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Prospect converti (ou déjà converti)",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Prospect non trouvé",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "409", description = "Un compte existe déjà pour cet email",
            content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/{id}/convertir")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<RestResponse<ConversionResultDto>> convertir(
            @Parameter(description = "Identifiant du prospect", required = true, example = "7")
            @PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant de prospect invalide", null));
        }
        return ResponseEntity.ok(
            RestResponse.success(conversionService.convertirProspect(id), HttpStatus.OK));
    }
}
