package sn.immosn.backend.client.web.auth.controller;

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
import sn.immosn.backend.client.web.auth.dto.AuthLoginRequestDto;
import sn.immosn.backend.client.web.auth.dto.AuthRegisterRequestDto;
import sn.immosn.backend.client.web.auth.dto.AuthResponseDto;
import sn.immosn.backend.client.web.auth.dto.UpdateProfileRequestDto;
import sn.immosn.backend.client.web.auth.mapper.AuthMapper;
import sn.immosn.backend.shared.response.PagedResponse;
import sn.immosn.backend.shared.response.RestResponse;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthMapper authMapper;

    public AuthController(AuthService authService, AuthMapper authMapper) {
        this.authService = authService;
        this.authMapper = authMapper;
    }

    @PostMapping("/register")
    public ResponseEntity<RestResponse<AuthResponseDto>> register(@Valid @RequestBody AuthRegisterRequestDto request) {
        return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(RestResponse.success(authService.register(request), HttpStatus.CREATED));
    }

    @PostMapping("/login")
    public ResponseEntity<RestResponse<AuthResponseDto>> login(@Valid @RequestBody AuthLoginRequestDto request) {
        return ResponseEntity
        .status(HttpStatus.OK)
        .body(RestResponse.success(authService.login(request), HttpStatus.OK));
    }

    @PostMapping("/admin")
    public ResponseEntity<RestResponse<AuthResponseDto>> addAdmin(@Valid @RequestBody AuthRegisterRequestDto request) {
        return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(RestResponse.success(authService.registerAdmin(request), HttpStatus.CREATED));
    }

    @PostMapping("/logout")
    public ResponseEntity<RestResponse<Void>> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
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

    @GetMapping("/admins")
    public ResponseEntity<PagedResponse<AuthResponseDto>> listAdmins(Pageable pageable) {
        return ResponseEntity
        .status(HttpStatus.OK)
        .body(PagedResponse.fromPage(authService.listAdmins(pageable)));
    }

    /**
     * PATCH /api/v1/auth/admins/{id}/archive
     * Archive un admin — réservé SUPER_ADMIN.
     */
    @PatchMapping("/admins/{id}/archive")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<RestResponse<Void>> archiveAdmin(
            @PathVariable Long id, Authentication authentication) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant d'admin invalide", null));
        }
        User operator = (User) authentication.getPrincipal();
        authService.archiveUser(id, operator);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * PATCH /api/v1/auth/admins/{id}/restore
     * Restaure un admin archivé — réservé SUPER_ADMIN.
     */
    @PatchMapping("/admins/{id}/restore")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<RestResponse<AuthResponseDto>> restoreAdmin(
            @PathVariable Long id, Authentication authentication) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant d'admin invalide", null));
        }
        User operator = (User) authentication.getPrincipal();
        return ResponseEntity.ok(
            RestResponse.success(authService.restoreAdmin(id, operator), HttpStatus.OK));
    }

    /**
     * PATCH /api/v1/auth/admins/{id}/revoke
     * Révoque le rôle ADMIN (repasse CLIENT) — réservé SUPER_ADMIN.
     */
    @PatchMapping("/admins/{id}/revoke")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<RestResponse<AuthResponseDto>> revokeAdmin(
            @PathVariable Long id, Authentication authentication) {
        if (id == null || id <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.badRequest("Identifiant d'admin invalide", null));
        }
        User operator = (User) authentication.getPrincipal();
        return ResponseEntity.ok(
            RestResponse.success(authService.revokeAdmin(id, operator), HttpStatus.OK));
    }

    /**
     * GET /api/v1/auth/profile
     * Retourne le profil de l'utilisateur connecté.
     */
    @GetMapping("/profile")
    public ResponseEntity<RestResponse<AuthResponseDto>> profile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof User user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(RestResponse.error("Vous devez être connecté", HttpStatus.UNAUTHORIZED));
        }
        return ResponseEntity.ok(
            RestResponse.success(authMapper.toAuthResponseDto(user, null), HttpStatus.OK));
    }

    /**
     * PUT /api/v1/auth/profile
     * Modifie le profil de l'utilisateur connecté.
     */
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
}
