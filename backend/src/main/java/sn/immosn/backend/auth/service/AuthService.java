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
import sn.immosn.backend.client.web.auth.dto.UpdateProfileRequestDto;
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

    // Déconnexion

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

    // Inscription CLIENT

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

    // Connexion

    public AuthResponseDto login(AuthLoginRequestDto request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getMotDePasse())
        );
        User user  = (User) authentication.getPrincipal();
        String jwt = jwtTokenProvider.generateToken(user);
        log.info("Connexion réussie : email={}", user.getEmail());
        return authMapper.toAuthResponseDto(user, jwt);
    }

    // Création ADMIN (réservé SUPER_ADMIN)

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

    // Liste des admins (SUPER_ADMIN)

    @Transactional(readOnly = true)
    public Page<AuthResponseDto> listAdmins(Pageable pageable) {
        return userRepository
            .findByRoles_Role(RoleType.ADMIN, pageable)
            .map(user -> authMapper.toAuthResponseDto(user, null));
    }

    // Archivage admin (SUPER_ADMIN)

    @Transactional
    public void archiveUser(Long id, User operator) {
        User target = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Administrateur introuvable : id=" + id));
        if (target.isArchived()) {
            throw new IllegalStateException("Cet administrateur est déjà archivé");
        }
        target.setArchived(true);
        userRepository.save(target);
        log.info("[{}] USER:{} {} ARCHIVE_ADMIN TARGET:{}",
            java.time.LocalDateTime.now(), operator.getId(),
            operator.getRoles().stream().map(r -> r.getRole().name()).findFirst().orElse("?"), id);
    }

    // Restauration admin (SUPER_ADMIN)

    @Transactional
    public AuthResponseDto restoreAdmin(Long id, User operator) {
        User target = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Administrateur introuvable : id=" + id));
        boolean isAdmin = target.getRoles().stream()
            .anyMatch(r -> r.getRole() == RoleType.ADMIN);
        if (!isAdmin) {
            throw new IllegalStateException("Cet utilisateur n'est pas un administrateur");
        }
        if (!target.isArchived()) {
            throw new IllegalStateException("Cet administrateur n'est pas archivé");
        }
        target.setArchived(false);
        User saved = userRepository.save(target);
        log.info("[{}] USER:{} {} RESTORE_ADMIN TARGET:{}",
            java.time.LocalDateTime.now(), operator.getId(),
            operator.getRoles().stream().map(r -> r.getRole().name()).findFirst().orElse("?"), id);
        return authMapper.toAuthResponseDto(saved, null);
    }

    // Révocation admin → CLIENT (SUPER_ADMIN)

    @Transactional
    public AuthResponseDto revokeAdmin(Long id, User operator) {
        User target = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Administrateur introuvable : id=" + id));
        boolean isAdmin = target.getRoles().stream()
            .anyMatch(r -> r.getRole() == RoleType.ADMIN);
        if (!isAdmin) {
            throw new IllegalStateException("Cet utilisateur n'est pas un administrateur");
        }
        target.getRoles().remove(loadRole(RoleType.ADMIN));
        target.getRoles().add(loadRole(RoleType.CLIENT));
        User saved = userRepository.save(target);
        log.info("[{}] USER:{} {} REVOKE_ADMIN TARGET:{}",
            java.time.LocalDateTime.now(), operator.getId(),
            operator.getRoles().stream().map(r -> r.getRole().name()).findFirst().orElse("?"), id);
        return authMapper.toAuthResponseDto(saved, null);
    }

    // Modification profil (utilisateur connecté)

    @Transactional
    public AuthResponseDto updateProfile(Long userId, UpdateProfileRequestDto request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));

        if (request.getNomComplet() != null && !request.getNomComplet().isBlank()) {
            user.setNomComplet(request.getNomComplet());
        }
        if (request.getTelephone() != null && !request.getTelephone().isBlank()) {
            user.setTelephone(request.getTelephone());
        }
        if (request.getAdresse() != null) {
            user.setAdresse(request.getAdresse());
        }
        if (request.getPhoto() != null) {
            user.setPhoto(request.getPhoto());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && !request.getEmail().equals(user.getEmail())) {
            if (authUserDetailService.existsByEmail(request.getEmail())) {
                throw new EntityExistException("Email déjà utilisé : " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }
        if (request.getNouveauMotDePasse() != null && !request.getNouveauMotDePasse().isBlank()) {
            if (request.getMotDePasseActuel() == null
                    || !passwordEncoder.matches(request.getMotDePasseActuel(), user.getPassword())) {
                throw new IllegalStateException("Mot de passe actuel incorrect");
            }
            if (!request.getNouveauMotDePasse().equals(request.getConfirmationMotDePasse())) {
                throw new IllegalStateException("La confirmation du mot de passe ne correspond pas");
            }
            user.setPassword(passwordEncoder.encode(request.getNouveauMotDePasse()));
        }

        User saved = userRepository.save(user);
        log.info("[{}] USER:{} {} MODIFY_PROFILE",
            java.time.LocalDateTime.now(), user.getId(),
            user.getRoles().stream().map(r -> r.getRole().name()).findFirst().orElse("?"));
        return authMapper.toAuthResponseDto(saved, null);
    }

    // Helpers

    private Role loadRole(RoleType type) {
        return roleRepository.findByRole(type)
            .orElseThrow(() -> new EntityNotFoundException("Rôle introuvable : " + type));
    }
}
