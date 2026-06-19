package sn.immosn.backend.signalement.data.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.immosn.backend.signalement.data.entity.SignalementHistory;

@Repository
public interface SignalementHistoryRepository extends JpaRepository<SignalementHistory, Long> {
    Page<SignalementHistory> findBySignalementIdOrderByCreatedAtDesc(Long signalementId, Pageable pageable);
}
