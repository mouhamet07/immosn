package sn.immosn.backend.lead.data.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.immosn.backend.lead.data.entity.LeadHistory;

public interface LeadHistoryRepository extends JpaRepository<LeadHistory, Long> {
    Page<LeadHistory> findByLeadIdOrderByCreatedAtDesc(Long leadId, Pageable pageable);
}
