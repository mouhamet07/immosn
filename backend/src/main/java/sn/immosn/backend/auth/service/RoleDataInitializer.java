package sn.immosn.backend.auth.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.immosn.backend.auth.data.entity.Role;
import sn.immosn.backend.auth.data.entity.RoleType;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.auth.data.repository.RoleRepository;
import sn.immosn.backend.auth.data.repository.UserRepository;

/**
 * Initialise les rôles et crée le SUPER_ADMIN de bootstrap si absent.
 * Le SUPER_ADMIN est le seul compte pouvant créer ou gérer des admins.
 */
@Component
@RequiredArgsConstructor
public class RoleDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RoleDataInitializer.class);

    private final RoleRepository  roleRepository;
    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.super-admin.email:superadmin@immosn.sn}")
    private String superAdminEmail;

    @Value("${app.super-admin.password:SuperAdmin@2024!}")
    private String superAdminPassword;

    @Value("${app.super-admin.nom:Super Administrateur}")
    private String superAdminNom;

    @Value("${app.super-admin.telephone:+221770000000}")
    private String superAdminTelephone;

    @Override
    @Transactional
    public void run(String... args) {
        // Créer tous les rôles manquants
        for (RoleType type : RoleType.values()) {
            roleRepository.findByRole(type).orElseGet(() -> {
                Role role = new Role();
                role.setRole(type);
                log.info("Rôle créé : {}", type);
                return roleRepository.save(role);
            });
        }

        // Créer le SUPER_ADMIN de bootstrap s'il n'existe pas
        if (!userRepository.existsByEmail(superAdminEmail)) {
            Role superAdminRole = roleRepository.findByRole(RoleType.SUPER_ADMIN)
                .orElseThrow(() -> new IllegalStateException("Rôle SUPER_ADMIN introuvable après initialisation"));

            User superAdmin = new User();
            superAdmin.setNomComplet(superAdminNom);
            superAdmin.setEmail(superAdminEmail);
            superAdmin.setTelephone(superAdminTelephone);
            superAdmin.setPassword(passwordEncoder.encode(superAdminPassword));
            superAdmin.getRoles().add(superAdminRole);

            userRepository.save(superAdmin);
            log.warn("SUPER_ADMIN bootstrap créé : {} — Changez le mot de passe via /auth/profile en production !",
                superAdminEmail);
        }
    }
}
