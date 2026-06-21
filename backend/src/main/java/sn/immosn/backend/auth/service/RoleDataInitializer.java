package sn.immosn.backend.auth.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
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

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
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
    public void run(String... args) {
        normaliserFlagsBooleensSiPossible();
        initialiserRolesEtSuperAdmin();
    }

    /**
     * Transaction dédiée et isolée : si cette requête échoue (colonne absente sur une base
     * fraîche, H2, etc.), PostgreSQL marque la transaction courante comme "aborted" jusqu'à son
     * rollback — un simple try/catch dans la transaction de run() ne suffit pas à l'empêcher de
     * contaminer les requêtes suivantes (création des rôles/SUPER_ADMIN). REQUIRES_NEW garantit
     * que l'échec reste cantonné à cette transaction et ne casse pas le reste du démarrage.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void normaliserFlagsBooleensSiPossible() {
        try {
            int normalises = userRepository.normaliserFlagsBooleens();
            if (normalises > 0) {
                log.info("Normalisation des indicateurs booléens utilisateurs : {} ligne(s) corrigée(s)", normalises);
            }
        } catch (Exception e) {
            log.debug("Normalisation des indicateurs booléens ignorée : {}", e.getMessage());
        }
    }

    @Transactional
    public void initialiserRolesEtSuperAdmin() {
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
