package sn.immosn.backend.visite.data.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.immosn.backend.visite.data.entity.VisiteHistory;

public interface VisiteHistoryRepository extends JpaRepository<VisiteHistory, Long> {
    Page<VisiteHistory> findByVisiteIdOrderByCreatedAtDesc(Long visiteId, Pageable pageable);
}
