package sn.immosn.backend.annonce.data.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sn.immosn.backend.annonce.data.entity.Annonce;

@Repository
public interface AnnonceRepository extends JpaRepository<Annonce, Long>, JpaSpecificationExecutor<Annonce> {

    // --- Liste publique : élimine le N+1 sur typeBien ---
    // images chargées en batch via @BatchSize(size=20) sur Annonce.images
    @EntityGraph(attributePaths = {"typeBien"})
    Page<Annonce> findByIsArchivedFalse(Pageable pageable);

    // --- Vue admin (toutes annonces) : même optimisation ---
    @Override
    @EntityGraph(attributePaths = {"typeBien"})
    Page<Annonce> findAll(Pageable pageable);

    // --- Recherche avancée (Specification) : élimine le N+1 sur typeBien ---
    // @EntityGraph appliqué ici — jamais dans la Specification (créerait un double join)
    @Override
    @EntityGraph(attributePaths = {"typeBien"})
    Page<Annonce> findAll(Specification<Annonce> spec, Pageable pageable);

    // Recherche simple par libelle ou adresse (insensible à la casse)
    Page<Annonce> findByIsArchivedFalseAndLibelleContainingIgnoreCase(
    String libelle, Pageable pageable);

    // Filtres combinés pour la recherche avancée (Sprint 2)
    @Query("""
        SELECT a FROM Annonce a
        WHERE a.isArchived = false
            AND (:typeBienId IS NULL OR a.typeBien.id = :typeBienId)
            AND (:prixMin IS NULL    OR a.prix >= :prixMin)
            AND (:prixMax IS NULL    OR a.prix <= :prixMax)
            AND (:adresse IS NULL    OR LOWER(a.adresse) LIKE LOWER(CONCAT('%',:adresse,'%')))
        """)
    Page<Annonce> searchActive(
        @Param("typeBienId") Long typeBienId,
        @Param("prixMin")    Double prixMin,
        @Param("prixMax")    Double prixMax,
        @Param("adresse")    String adresse,
        Pageable pageable
    );

    // Vérifie l'existence d'une annonce non archivée
    boolean existsByIdAndIsArchivedFalse(Long id);

    // getById active
    Optional<Annonce> findByIdAndIsArchivedFalse(Long id);
}
