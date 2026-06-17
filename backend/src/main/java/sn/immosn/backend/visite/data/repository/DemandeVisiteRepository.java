package sn.immosn.backend.visite.data.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sn.immosn.backend.visite.data.entity.DemandeVisite;
import sn.immosn.backend.visite.data.entity.StatutDemandeVisite;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DemandeVisiteRepository extends JpaRepository<DemandeVisite, Long> {

    Page<DemandeVisite> findByClientIdAndIsArchivedFalseOrderByCreatedAtDesc(Long clientId, Pageable pageable);

    Page<DemandeVisite> findByClientIdAndStatutAndIsArchivedFalseOrderByCreatedAtDesc(
        Long clientId, StatutDemandeVisite statut, Pageable pageable);

    Page<DemandeVisite> findByIsArchivedFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<DemandeVisite> findByStatutAndIsArchivedFalseOrderByCreatedAtDesc(
        StatutDemandeVisite statut, Pageable pageable);

    Optional<DemandeVisite> findByIdAndIsArchivedFalse(Long id);

    // Toutes les demandes d'un prospect (pour le rattachement lors de la conversion — Sprint 3)
    List<DemandeVisite> findByProspectId(Long prospectId);

    // Admin queries — no archived filter so admins can list all visits including archived ones
    Page<DemandeVisite> findByStatut(StatutDemandeVisite statut, Pageable pageable);

    boolean existsByClientIdAndAnnonceIdAndStatutIn(
        Long clientId, Long annonceId, java.util.List<StatutDemandeVisite> statuts);

    long countByStatutAndIsArchivedFalse(StatutDemandeVisite statut);

    @Query("SELECT COUNT(v) FROM DemandeVisite v WHERE v.dateVisite >= :start AND v.dateVisite < :end AND v.isArchived = false")
    long countByDateVisiteBetweenAndIsArchivedFalse(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // JOIN FETCH annonce + client : élimine le N+1 du dashboard (1 query au lieu de 1+5+5+5)
    @Query("""
        SELECT v FROM DemandeVisite v
        JOIN FETCH v.annonce
        JOIN FETCH v.client
        WHERE v.isArchived = false
        """)
    List<DemandeVisite> findRecentForDashboard(Pageable pageable);
}
