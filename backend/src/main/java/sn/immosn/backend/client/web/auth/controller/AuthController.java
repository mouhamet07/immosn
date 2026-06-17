package sn.immosn.backend.client.web.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.auth.service.AuthService;
import sn.immosn.backend.client.web.auth.dto.AdminUpdateRequestDto;
import sn.immosn.backend.client.web.auth.dto.AuthLoginRequestDto;
import sn.immosn.backend.client.web.auth.dto.AuthRegisterRequestDto;
import sn.immosn.backend.client.web.auth.dto.AuthResponseDto;
import sn.immosn.backend.client.web.auth.dto.UpdateProfileRequestDto;
import sn.immosn.backend.client.web.auth.mapper.AuthMapper;
import sn.immosn.backend.shared.response.PagedResponse;
import sn.immosn.backend.shared.response.RestResponse;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "AUTHENTIFICATION", description = "Inscription, connexion, déconnexion, gestion du profil et administration des comptes administrateurs")
public class AuthController {

    private final AuthService authService;
    private final AuthMapper authMapper;

    public AuthController(AuthService authService, AuthMapper authMapper) {
        this.authService = authService;
        this.authMapper = authMapper;
    }

    @Operation(
        summary = "Inscription d'un nouveau client",
        description = """
            Crée un nouveau compte utilisateur avec le rôle **CLIENT** attribué automatiquement.

            - L'email doit être unique dans le système
            - Le mot de passe est haché avant stockage (BCrypt)
            - Un token JWT est retourné immédiatement, le client est connecté dès l'inscription

            **Accès : PUBLIC — aucun token requis**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Compte créé avec succès — token JWT inclus dans la réponse",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(name = "Inscription réussie", value = """
                    {
                      "success": true,
                      "status": 201,
                      "message": "Opération réalisée avec succès",
                      "data": {
                        "id": 42,
                        "nomComplet": "Aminata Diallo",
                        "email": "aminata@immosn.sn",
                        "telephone": "+221 77 123 45 67",
                        "adresse": null,
                        "photo": null,
                        "archived": false,
                        "roles": ["CLIENT"],
                        "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbWluYXRhQGltbW9zbi5zbiJ9...",
                        "tokenType": "Bearer"
                      },
                      "timestamp": "2024-01-15T10:30:00"
                    }"""))),
        @ApiResponse(responseCode = "400", description = "Données invalides — champs manquants ou format incorrect",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {"success":false,"status":400,"message":"Le champ email est obligatoire","data":null}"""))),
        @ApiResponse(responseCode = "409", description = "Email déjà utilisé",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {"success":false,"status":409,"message":"Un compte avec cet email existe déjà","data":null}""")))
    })
    @PostMapping("/register")
    public ResponseEntity<RestResponse<AuthResponseDto>> register(@Valid @RequestBody AuthRegisterRequestDto request) {
        return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(RestResponse.success(authService.register(request), HttpStatus.CREATED));
    }

    @Operation(
        summary = "Connexion",
        description = """
            Authentifie un utilisateur (CLIENT, ADMIN ou SUPER_ADMIN) et retourne un token JWT.

            Le token doit être inclus dans les requêtes suivantes via l'en-tête :
            ```
            Authorization: Bearer <token>
            ```

            **Accès : PUBLIC — aucun token requis**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Connexion réussie — token JWT retourné",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(name = "Connexion client", value = """
                    {
                      "success": true,
                      "status": 200,
                      "message": "Opération réalisée avec succès",
                      "data": {
                        "id": 1,
                        "nomComplet": "Moussa Ndiaye",
                        "email": "client@immosn.sn",
                        "telephone": "+221 76 456 78 90",
                        "roles": ["CLIENT"],
                        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                        "tokenType": "Bearer"
                      },
                      "timestamp": "2024-01-15T10:30:00"
                    }"""))),
        @ApiResponse(responseCode = "400", description = "Identifiants manquants ou format email invalide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Email ou mot de passe incorrect",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {"success":false,"status":401,"message":"Email ou mot de passe incorrect","data":null}""")))
    })
    @PostMapping("/login")
    public ResponseEntity<RestResponse<AuthResponseDto>> login(@Valid @RequestBody AuthLoginRequestDto request) {
        return ResponseEntity
        .status(HttpStatus.OK)
        .body(RestResponse.success(authService.login(request), HttpStatus.OK));
    }

    @Operation(
        summary = "Créer un compte administrateur",
        description = """
            Crée un nouveau compte avec le rôle **ADMIN**.

            Seul le SUPER_ADMIN peut créer des comptes administrateurs.
            Le compte admin créé peut ensuite se connecter via `POST /api/v1/auth/login`.

            **Accès : SUPER_ADMIN uniquement**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Compte admin créé avec succès",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true, "status": 201,
                      "data": {
                        "id": 10,
                        "nomComplet": "Ibrahima Sow",
                        "email": "admin@immosn.sn",
                        "roles": ["ADMIN"],
                        "archived": false
                      }
                    }"""))),
        @ApiResponse(responseCode = "400", description = "Données invalides",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "409", description = "Email déjà utilisé",
            content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/admin")
    public ResponseEntity<RestResponse<AuthResponseDto>> addAdmin(@Valid @RequestBody AuthRegisterRequestDto request) {
        return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(RestResponse.success(authService.registerAdmin(request), HttpStatus.CREATED));
    }

    @Operation(
        summary = "Déconnexion",
        description = """
            Invalide le token JWT en le plaçant dans une liste noire (blacklist).

            Le token ne sera plus accepté après cet appel, même s'il n'est pas encore expiré.

            **Accès : PUBLIC — token optionnel (si fourni, il est invalidé)**
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Déconnexion réussie — aucun contenu retourné"),
        @ApiResponse(responseCode = "400", description = "En-tête Authorization manquant ou format invalide (doit commencer par 'Bearer ')",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {"success":false,"status":400,"message":"Token d'authentification invalide","data":null}""")))
    })
    @PostMapping("/logout")
    public ResponseEntity<RestResponse<Void>> logout(
            @Parameter(description = "Token JWT au format 'Bearer <token>'", required = true)
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(RestResponse.badRequest("Token d'authentification invalide", null));
        }
        String token = authorization.substring(7);
        authService.logout(token);
        return ResponseEntity
        .status(HttpStatus.NO_CONTENT)
        .build();
    }

    @Operation(
        summary = "Lister les administrateurs",
        description = """
            Retourne la liste paginée de tous les comptes ADMIN (actifs et archivés).

            Permet au SUPER_ADMIN de superviser l'ensemble des gestionnaires de la plateforme.

            **Accès : SUPER_ADMIN uniquement**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste des admins retournée avec succès",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/admins")
    public ResponseEntity<PagedResponse<AuthResponseDto>> listAdmins(Pageable pageable) {
        return ResponseEntity
        .status(HttpStatus.OK)
        .body(PagedResponse.fromPage(authService.listAdmins(pageable)));
    }

    @Operation(
        summary = "Archiver un administrateur",
        description = """
            Archive (désactive) un compte administrateur. Le compte reste en base de données
            mais l'admin ne peut plus se connecter.

            Un SUPER_ADMIN ne peut pas s'archiver lui-même.

            **Accès : SUPER_ADMIN uniquement**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Admin archivé avec succès — aucun contenu retourné"),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide (null ou ≤ 0)",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle SUPER_ADMIN requis, ou tentative d'auto-archivage",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Administrateur non trouvé",
            content = @Content(mediaType = "application/json"))
    })
    @PatchMapping("/admins/{id}/archive")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<RestResponse<Void>> archiveAdmin(
            @Parameter(description = "Identifiant de l'administrateur à archiver", required = true, example = "10")
            @PathVariable Long id, Authentication authentication) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant d'admin invalide", null));
        }
        User operator = (User) authentication.getPrincipal();
        authService.archiveUser(id, operator);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(
        summary = "Restaurer un administrateur archivé",
        description = """
            Réactive un compte administrateur précédemment archivé.
            L'admin peut de nouveau se connecter après cette opération.

            **Accès : SUPER_ADMIN uniquement**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Admin restauré avec succès",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Administrateur non trouvé",
            content = @Content(mediaType = "application/json"))
    })
    @PatchMapping("/admins/{id}/restore")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<RestResponse<AuthResponseDto>> restoreAdmin(
            @Parameter(description = "Identifiant de l'administrateur à restaurer", required = true, example = "10")
            @PathVariable Long id, Authentication authentication) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant d'admin invalide", null));
        }
        User operator = (User) authentication.getPrincipal();
        return ResponseEntity.ok(
            RestResponse.success(authService.restoreAdmin(id, operator), HttpStatus.OK));
    }

    @Operation(
        summary = "Révoquer le rôle admin",
        description = """
            Rétrograde un compte ADMIN en compte CLIENT. L'utilisateur conserve son compte
            mais perd tous les privilèges administrateurs.

            Utile pour retirer les droits sans supprimer le compte.

            **Accès : SUPER_ADMIN uniquement**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rôle admin révoqué — compte rétrogradé en CLIENT",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Identifiant invalide",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Administrateur non trouvé",
            content = @Content(mediaType = "application/json"))
    })
    @PatchMapping("/admins/{id}/revoke")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<RestResponse<AuthResponseDto>> revokeAdmin(
            @Parameter(description = "Identifiant de l'administrateur dont le rôle est révoqué", required = true, example = "10")
            @PathVariable Long id, Authentication authentication) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant d'admin invalide", null));
        }
        User operator = (User) authentication.getPrincipal();
        return ResponseEntity.ok(
            RestResponse.success(authService.revokeAdmin(id, operator), HttpStatus.OK));
    }

    @Operation(
        summary = "Modifier un administrateur",
        description = """
            Modifie les informations d'un compte administrateur (nom, email, téléphone)
            et permet, en option, de réinitialiser son mot de passe.

            - Seuls les champs fournis sont mis à jour
            - L'email doit rester unique
            - Pour réinitialiser le mot de passe : fournir `nouveauMotDePasse`
              (soumis aux mêmes règles de complexité que l'inscription)

            **Accès : SUPER_ADMIN uniquement**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Administrateur modifié avec succès",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Données invalides",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Accès interdit — rôle SUPER_ADMIN requis",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Administrateur non trouvé",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "409", description = "Email déjà utilisé par un autre compte",
            content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/admins/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<RestResponse<AuthResponseDto>> updateAdmin(
            @Parameter(description = "Identifiant de l'administrateur à modifier", required = true, example = "10")
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateRequestDto request,
            Authentication authentication) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant d'admin invalide", null));
        }
        User operator = (User) authentication.getPrincipal();
        return ResponseEntity.ok(
            RestResponse.success(authService.updateAdmin(id, request, operator), HttpStatus.OK));
    }

    @Operation(
        summary = "Consulter mon profil",
        description = """
            Retourne les informations du compte de l'utilisateur actuellement connecté
            (nom, email, téléphone, adresse, photo, rôles).

            **Accès : Tout utilisateur authentifié (CLIENT, ADMIN, SUPER_ADMIN)**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profil retourné avec succès",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "success": true, "status": 200,
                      "data": {
                        "id": 42,
                        "nomComplet": "Aminata Diallo",
                        "email": "aminata@immosn.sn",
                        "telephone": "+221 77 123 45 67",
                        "adresse": "Almadies, Dakar",
                        "photo": "https://storage.immosn.sn/photos/42.jpg",
                        "archived": false,
                        "roles": ["CLIENT"]
                      }
                    }"""))),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant, expiré ou invalidé (après logout)",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/profile")
    public ResponseEntity<RestResponse<AuthResponseDto>> profile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof User user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(RestResponse.error("Vous devez être connecté", HttpStatus.UNAUTHORIZED));
        }
        long sessions = authService.countActiveSessions(user.getId());
        return ResponseEntity.ok(
            RestResponse.success(authMapper.toAuthResponseDto(user, null, sessions), HttpStatus.OK));
    }

    @Operation(
        summary = "Mettre à jour mon profil",
        description = """
            Modifie les informations du compte de l'utilisateur connecté.

            - Tous les champs sont optionnels (seuls ceux fournis sont mis à jour)
            - Pour changer le mot de passe : fournir `motDePasseActuel` + `nouveauMotDePasse`
            - L'email peut être modifié (doit rester unique)

            **Accès : Tout utilisateur authentifié (CLIENT, ADMIN, SUPER_ADMIN)**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profil mis à jour avec succès",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Données invalides ou mot de passe actuel incorrect",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "409", description = "Nouvel email déjà utilisé par un autre compte",
            content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/profile")
    public ResponseEntity<RestResponse<AuthResponseDto>> updateProfile(
            @Valid @RequestBody UpdateProfileRequestDto request,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof User user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(RestResponse.error("Vous devez être connecté", HttpStatus.UNAUTHORIZED));
        }
        return ResponseEntity.ok(
            RestResponse.success(authService.updateProfile(user.getId(), request), HttpStatus.OK));
    }

    @Operation(
        summary = "Changer mon mot de passe",
        description = """
            Définit un nouveau mot de passe pour l'utilisateur connecté.

            - Pour un compte à mot de passe temporaire (1re connexion après conversion de prospect),
              le mot de passe actuel n'est pas exigé et l'indicateur `motDePasseAChanger` est levé.
            - Pour un changement volontaire, `motDePasseActuel` est obligatoire.

            **Accès : Tout utilisateur authentifié**
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Mot de passe changé avec succès",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Données invalides ou mot de passe actuel incorrect",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou expiré",
            content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/change-password")
    public ResponseEntity<RestResponse<AuthResponseDto>> changePassword(
            @Valid @RequestBody sn.immosn.backend.client.web.auth.dto.ChangePasswordRequestDto request,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof User user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(RestResponse.error("Vous devez être connecté", HttpStatus.UNAUTHORIZED));
        }
        return ResponseEntity.ok(
            RestResponse.success(authService.changePassword(user.getId(), request), HttpStatus.OK));
    }
}
