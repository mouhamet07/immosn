package sn.immosn.backend.auth.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.immosn.backend.auth.data.entity.BlacklistedToken;
import sn.immosn.backend.auth.data.entity.Role;
import sn.immosn.backend.auth.data.entity.RoleType;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.auth.data.jwt.JwtTokenProvider;
import sn.immosn.backend.auth.data.repository.BlacklistedTokenRepository;
import sn.immosn.backend.auth.data.repository.RoleRepository;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.client.web.auth.dto.AuthLoginRequestDto;
import sn.immosn.backend.client.web.auth.dto.AuthRegisterRequestDto;
import sn.immosn.backend.client.web.auth.dto.AuthResponseDto;
import sn.immosn.backend.client.web.auth.mapper.AuthMapper;
import sn.immosn.backend.shared.exception.EntityExistException;
import sn.immosn.backend.shared.exception.EntityNotFoundException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthUserDetailService        authUserDetailService;
    private final RoleRepository               roleRepository;
    private final UserRepository               userRepository;
    private final AuthenticationManager        authenticationManager;
    private final PasswordEncoder              passwordEncoder;
    private final AuthMapper                   authMapper;
    private final JwtTokenProvider             jwtTokenProvider;
    private final BlacklistedTokenRepository   blacklistedTokenRepository;

    // ── Déconnexion ────────────────────────────────────────

    public void logout(String token) {
        try {
            var expiry = jwtTokenProvider.getExpirationDateFromToken(token);
            blacklistedTokenRepository.save(new BlacklistedToken(token, expiry));
            log.debug("Token blacklisté lors du logout");
        } catch (Exception e) {
            // Token invalide → on considère la déconnexion comme réussie quand même
            log.warn("Tentative de logout avec token non parseable : {}", e.getMessage());
        }
    }

    // ── Inscription CLIENT ──────────────────────────────────

    @Transactional
    public AuthResponseDto register(AuthRegisterRequestDto request) {
        if (authUserDetailService.existsByEmail(request.getEmail())) {
            throw new EntityExistException("Un utilisateur avec cet email existe déjà : " + request.getEmail());
        }

        User user = authMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getMotDePasse()));

        Role clientRole = loadRole(RoleType.CLIENT);
        user.getRoles().add(clientRole);

        User saved = authUserDetailService.save(user);
        log.info("Nouveau CLIENT inscrit : email={}", saved.getEmail());

        return authMapper.toAuthResponseDto(saved, jwtTokenProvider.generateToken(saved));
    }

    // ── Connexion ───────────────────────────────────────────

    public AuthResponseDto login(AuthLoginRequestDto request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getMotDePasse())
        );
        User user  = (User) authentication.getPrincipal();
        String jwt = jwtTokenProvider.generateToken(user);
        log.info("Connexion réussie : email={}", user.getEmail());
        return authMapper.toAuthResponseDto(user, jwt);
    }

    // ── Création ADMIN (réservé SUPER_ADMIN, contrôlé par SecurityConfig) ─

    @Transactional
    public AuthResponseDto registerAdmin(AuthRegisterRequestDto request) {
        if (authUserDetailService.existsByEmail(request.getEmail())) {
            throw new EntityExistException("Un utilisateur avec cet email existe déjà : " + request.getEmail());
        }

        User user = authMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getMotDePasse()));
        user.getRoles().add(loadRole(RoleType.ADMIN));

        User saved = authUserDetailService.save(user);
        log.info("Nouvel ADMIN créé par SUPER_ADMIN : email={}", saved.getEmail());

        return authMapper.toAuthResponseDto(saved, jwtTokenProvider.generateToken(saved));
    }

    // ── Liste des admins (SUPER_ADMIN) ──────────────────────

    @Transactional(readOnly = true)
    public Page<AuthResponseDto> listAdmins(Pageable pageable) {
        return userRepository
            .findByRoles_Role(RoleType.ADMIN, pageable)
            .map(user -> authMapper.toAuthResponseDto(user, null));
    }

    // ── Archivage utilisateur (SUPER_ADMIN) ─────────────────

    @Transactional
    public void archiveUser(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable : " + id));
        user.setArchived(true);
        userRepository.save(user);
        log.info("Utilisateur archivé par SUPER_ADMIN : id={}", id);
    }

    // ── Helpers ─────────────────────────────────────────────

    private Role loadRole(RoleType type) {
        return roleRepository.findByRole(type)
            .orElseThrow(() -> new EntityNotFoundException("Rôle introuvable : " + type));
    }
}
