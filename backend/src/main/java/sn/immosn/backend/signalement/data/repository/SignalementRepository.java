package sn.immosn.backend.signalement.data.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sn.immosn.backend.signalement.data.entity.Signalement;
import sn.immosn.backend.signalement.data.entity.StatutSignalement;

import java.util.List;

@Repository
public interface SignalementRepository extends JpaRepository<Signalement, Long> {

    Page<Signalement> findByClientIdOrderByCreatedAtDesc(Long clientId, Pageable pageable);

    Page<Signalement> findByClientIdAndStatutOrderByCreatedAtDesc(
        Long clientId, StatutSignalement statut, Pageable pageable);

    Page<Signalement> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Signalement> findByStatutOrderByCreatedAtDesc(StatutSignalement statut, Pageable pageable);

    long countByIsReadFalse();

    long countByStatut(StatutSignalement statut);

    @Modifying
    @Query("UPDATE Signalement s SET s.isRead = true WHERE s.id = :id")
    void markAsRead(@Param("id") Long id);

    // JOIN FETCH client : élimine le N+1 du dashboard (1 query au lieu de 1+5+5)
    @Query("""
        SELECT s FROM Signalement s
        JOIN FETCH s.client
        """)
    List<Signalement> findRecentForDashboard(Pageable pageable);
}
