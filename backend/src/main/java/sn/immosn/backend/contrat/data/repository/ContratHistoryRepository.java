package sn.immosn.backend.contrat.data.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.immosn.backend.contrat.data.entity.ContratHistory;

public interface ContratHistoryRepository extends JpaRepository<ContratHistory, Long> {
    Page<ContratHistory> findByContratIdOrderByCreatedAtDesc(Long contratId, Pageable pageable);
}
