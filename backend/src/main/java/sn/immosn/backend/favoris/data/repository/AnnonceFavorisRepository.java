package sn.immosn.backend.favoris.data.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sn.immosn.backend.favoris.data.entity.AnnonceFavoris;
import sn.immosn.backend.favoris.data.entity.AnnonceFavorisId;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnnonceFavorisRepository extends JpaRepository<AnnonceFavoris, AnnonceFavorisId> {

    /** Liste paginée des favoris actifs (annonces non archivées) */
    @Query("SELECT f FROM AnnonceFavoris f JOIN f.annonce a WHERE f.client.id = :clientId AND a.isArchived = false ORDER BY f.createdAt DESC")
    Page<AnnonceFavoris> findActiveByClientId(@Param("clientId") Long clientId, Pageable pageable);

    /** IDs des favoris actifs — pour le store frontend (annonces non archivées uniquement) */
    @Query("SELECT f FROM AnnonceFavoris f JOIN f.annonce a WHERE f.client.id = :clientId AND a.isArchived = false")
    List<AnnonceFavoris> findAllActiveByClientId(@Param("clientId") Long clientId);

    /** Liste paginée des favoris (pour FavorisView avec détails) */
    Page<AnnonceFavoris> findByClientIdOrderByCreatedAtDesc(Long clientId, Pageable pageable);

    /** Liste complète des favoris — pour charger tous les IDs dans le store frontend */
    List<AnnonceFavoris> findAllByClientId(Long clientId);

    Optional<AnnonceFavoris> findByClientIdAndAnnonceId(Long clientId, Long annonceId);

    boolean existsByClientIdAndAnnonceId(Long clientId, Long annonceId);

    /** Vérifie si une annonce NON ARCHIVÉE est en favori — utilisé par checkFavoris() */
    @Query("SELECT COUNT(f) > 0 FROM AnnonceFavoris f WHERE f.client.id = :clientId AND f.annonce.id = :annonceId AND f.annonce.isArchived = false")
    boolean existsActiveByClientIdAndAnnonceId(@Param("clientId") Long clientId, @Param("annonceId") Long annonceId);

    void deleteByClientIdAndAnnonceId(Long clientId, Long annonceId);
}
