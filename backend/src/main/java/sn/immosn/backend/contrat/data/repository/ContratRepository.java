package sn.immosn.backend.contrat.data.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import sn.immosn.backend.contrat.data.entity.Contrat;
import sn.immosn.backend.contrat.data.entity.StatutContrat;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContratRepository extends JpaRepository<Contrat, Long> {

    Page<Contrat> findByClientIdOrderByCreatedAtDesc(Long clientId, Pageable pageable);

    Page<Contrat> findByClientIdAndStatutOrderByCreatedAtDesc(Long clientId, StatutContrat statut, Pageable pageable);

    Page<Contrat> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Contrat> findByStatutOrderByCreatedAtDesc(StatutContrat statut, Pageable pageable);

    Optional<Contrat> findByIdAndClientId(Long id, Long clientId);

    long countByStatut(StatutContrat statut);

    // Vérifie les contrats actifs liés à une annonce (bloque l'archivage)
    long countByAnnonceIdAndStatut(Long annonceId, StatutContrat statut);

    // Job d'expiration : contrats dont dateFin < aujourd'hui et statut ACTIF
    java.util.List<Contrat> findByStatutAndDateFinBefore(
        StatutContrat statut, java.time.LocalDate date);

    // Navigation croisée Lead → Contrat (un lead produit au plus un contrat)
    Optional<Contrat> findFirstByLeadId(Long leadId);

    // Batch-load pour éliminer le N+1 sur la liste des leads (1 requête pour toute la page)
    @Query("SELECT c FROM Contrat c WHERE c.lead.id IN :leadIds")
    List<Contrat> findByLeadIdIn(@org.springframework.data.repository.query.Param("leadIds") List<Long> leadIds);

    // JOIN FETCH annonce + client : élimine le N+1 du dashboard (1 query au lieu de 1+5+5+5)
    @Query("""
        SELECT c FROM Contrat c
        JOIN FETCH c.annonce
        JOIN FETCH c.client
        """)
    List<Contrat> findRecentForDashboard(Pageable pageable);
}
