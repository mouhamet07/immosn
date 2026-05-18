package sn.immosn.backend.contrat.data.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.immosn.backend.contrat.data.entity.Contrat;
import sn.immosn.backend.contrat.data.entity.StatutContrat;

import java.util.Optional;

@Repository
public interface ContratRepository extends JpaRepository<Contrat, Long> {

    Page<Contrat> findByClientIdOrderByCreatedAtDesc(Long clientId, Pageable pageable);

    Page<Contrat> findByClientIdAndStatutOrderByCreatedAtDesc(Long clientId, StatutContrat statut, Pageable pageable);

    Page<Contrat> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Contrat> findByStatutOrderByCreatedAtDesc(StatutContrat statut, Pageable pageable);

    Optional<Contrat> findByIdAndClientId(Long id, Long clientId);

    long countByStatut(StatutContrat statut);
}
