package sn.immosn.backend.auth.service;

import sn.immosn.backend.auth.data.entity.BlacklistedToken;
import sn.immosn.backend.auth.data.entity.Role;
import sn.immosn.backend.auth.data.entity.RoleType;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.auth.data.repository.BlacklistedTokenRepository;
import sn.immosn.backend.auth.data.repository.RoleRepository;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.client.web.auth.dto.AuthLoginRequestDto;
import sn.immosn.backend.client.web.auth.dto.AuthRegisterRequestDto;
import sn.immosn.backend.client.web.auth.dto.AuthResponseDto;
import sn.immosn.backend.auth.data.jwt.JwtTokenProvider;
import sn.immosn.backend.client.web.auth.mapper.AuthMapper;
import sn.immosn.backend.shared.exception.EntityExistException;
import sn.immosn.backend.shared.exception.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthUserDetailService authUserDetailService;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final AuthMapper authMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final BlacklistedTokenRepository blacklistedTokenRepository;

    public AuthService(AuthUserDetailService authUserDetailService,
    RoleRepository roleRepository,
    UserRepository userRepository,
    AuthenticationManager authenticationManager,
    PasswordEncoder passwordEncoder,
    AuthMapper authMapper,
    JwtTokenProvider jwtTokenProvider,
    BlacklistedTokenRepository blacklistedTokenRepository) {
        this.authUserDetailService = authUserDetailService;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.authMapper = authMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.blacklistedTokenRepository = blacklistedTokenRepository;
    }

    public void logout(String token) {
        try {
            var expiry = jwtTokenProvider.getExpirationDateFromToken(token);
            BlacklistedToken blacklistedToken = new BlacklistedToken(token, expiry);
            blacklistedTokenRepository.save(blacklistedToken);
        } catch (Exception ignored) {
            // If token can't be parsed, we still return OK on logout
        }
    }

    public AuthResponseDto register(AuthRegisterRequestDto request) {
        if (authUserDetailService.existsByEmail(request.getEmail())) {
            throw new EntityExistException("Un utilisateur avec cet email existe déjà.");
        }

        User user = authMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getMotDePasse()));

        Role clientRole = roleRepository.findByRole(RoleType.CLIENT)
                .orElseThrow(() -> new EntityNotFoundException("Role CLIENT introuvable."));
        user.getRoles().add(clientRole);
        User savedUser = authUserDetailService.save(user);

        String token = jwtTokenProvider.generateToken(savedUser);
        return authMapper.toAuthResponseDto(savedUser, token);
    }

    public AuthResponseDto login(AuthLoginRequestDto request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getMotDePasse())
        );

        User user = (User) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(user);
        return authMapper.toAuthResponseDto(user, token);
    }

    public AuthResponseDto registerAdmin(AuthRegisterRequestDto request) {
        if (authUserDetailService.existsByEmail(request.getEmail())) {
            throw new EntityExistException("Un utilisateur avec cet email existe déjà.");
        }

        User user = authMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getMotDePasse()));

        Role adminRole = roleRepository.findByRole(RoleType.ADMIN)
                .orElseThrow(() -> new EntityNotFoundException("Role ADMIN introuvable."));
        user.getRoles().add(adminRole);
        User savedUser = authUserDetailService.save(user);

        String token = jwtTokenProvider.generateToken(savedUser);
        return authMapper.toAuthResponseDto(savedUser, token);
    }

    public Page<AuthResponseDto> listAdmins(Pageable pageable) {
        return userRepository.findByRoles_Role(RoleType.ADMIN, pageable)
                .map(user -> authMapper.toAuthResponseDto(user, null));
    }
}
