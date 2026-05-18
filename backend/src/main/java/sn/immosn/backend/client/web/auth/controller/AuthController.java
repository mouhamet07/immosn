package sn.immosn.backend.client.web.auth.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.immosn.backend.client.web.auth.dto.AuthLoginRequestDto;
import sn.immosn.backend.client.web.auth.dto.AuthRegisterRequestDto;
import sn.immosn.backend.client.web.auth.dto.AuthResponseDto;
import sn.immosn.backend.client.web.auth.mapper.AuthMapper;
import sn.immosn.backend.shared.response.PagedResponse;
import sn.immosn.backend.shared.response.RestResponse;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.auth.service.AuthService;

@CrossOrigin(origins = "http://localhost:5173")
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

    @GetMapping("/profile")
    public ResponseEntity<RestResponse<AuthResponseDto>> profile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(RestResponse.error("Vous devez être connecté", HttpStatus.UNAUTHORIZED));
        }
        return ResponseEntity
        .status(HttpStatus.OK)
        .body(RestResponse.success(authMapper.toAuthResponseDto(user, null), HttpStatus.OK));
    }
}
