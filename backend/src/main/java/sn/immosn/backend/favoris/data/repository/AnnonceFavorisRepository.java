package sn.immosn.backend.favoris.data.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.immosn.backend.favoris.data.entity.AnnonceFavoris;
import sn.immosn.backend.favoris.data.entity.AnnonceFavorisId;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnnonceFavorisRepository extends JpaRepository<AnnonceFavoris, AnnonceFavorisId> {

    /** Liste paginée des favoris (pour FavorisView avec détails) */
    Page<AnnonceFavoris> findByClientIdOrderByCreatedAtDesc(Long clientId, Pageable pageable);

    /** Liste complète des favoris — pour charger tous les IDs dans le store frontend */
    List<AnnonceFavoris> findAllByClientId(Long clientId);

    Optional<AnnonceFavoris> findByClientIdAndAnnonceId(Long clientId, Long annonceId);

    boolean existsByClientIdAndAnnonceId(Long clientId, Long annonceId);

    void deleteByClientIdAndAnnonceId(Long clientId, Long annonceId);
}
