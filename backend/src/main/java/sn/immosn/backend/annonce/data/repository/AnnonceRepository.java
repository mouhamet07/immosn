package sn.immosn.backend.annonce.data.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sn.immosn.backend.annonce.data.entity.Annonce;

// AnnonceRepository.java
@Repository
public interface AnnonceRepository extends JpaRepository<Annonce, Long> {

    // Liste paginée des annonces actives (soft delete : archived = false)
    Page<Annonce> findByIsArchivedFalse(Pageable pageable);

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
