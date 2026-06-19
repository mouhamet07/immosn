package sn.immosn.backend.auth.data.repository;

import sn.immosn.backend.auth.data.entity.RoleType;
import sn.immosn.backend.auth.data.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Page<User> findByRoles_Role(RoleType role, Pageable pageable);
    long countByRoles_Role(RoleType role);

    /**
     * Normalise les colonnes booléennes Sprint 3 ajoutées via ddl-auto=update.
     * Requête native : les lignes existantes peuvent contenir NULL (colonne ajoutée sans backfill),
     * ce qui casse le mapping vers un boolean primitif et provoque un 401 au login.
     * Idempotent : ne touche que les lignes NULL. Exécutée au démarrage.
     */
    @Modifying
    @Query(value = "UPDATE users SET compte_genere_auto = COALESCE(compte_genere_auto, false), "
        + "mot_de_passe_a_changer = COALESCE(mot_de_passe_a_changer, false) "
        + "WHERE compte_genere_auto IS NULL OR mot_de_passe_a_changer IS NULL", nativeQuery = true)
    int normaliserFlagsBooleens();
}