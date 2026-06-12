package sn.immosn.backend.lead.data.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.immosn.backend.lead.data.entity.Lead;
import sn.immosn.backend.lead.data.entity.StatutLead;

import java.util.List;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {

    // EntityGraph sur ManyToOne uniquement (client, annonce, visite) — safe avec pagination.
    // annonce.images (ElementCollection) délibérément exclus pour éviter la pagination en mémoire.
    @EntityGraph(attributePaths = {"client", "annonce", "visite"})
    Page<Lead> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"client", "annonce", "visite"})
    Page<Lead> findByStatutOrderByCreatedAtDesc(StatutLead statut, Pageable pageable);

    Page<Lead> findByClientIdOrderByCreatedAtDesc(Long clientId, Pageable pageable);

    List<Lead> findByVisiteId(Long visiteId);

    boolean existsByVisiteId(Long visiteId);

    long countByStatut(StatutLead statut);
}
