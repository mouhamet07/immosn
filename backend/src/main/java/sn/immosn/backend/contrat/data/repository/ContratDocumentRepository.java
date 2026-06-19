package sn.immosn.backend.contrat.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.immosn.backend.contrat.data.entity.ContratDocument;

import java.util.List;

@Repository
public interface ContratDocumentRepository extends JpaRepository<ContratDocument, Long> {

    List<ContratDocument> findByContratIdOrderByCreatedAtDesc(Long contratId);

    List<ContratDocument> findByContratIdInOrderByCreatedAtDesc(List<Long> contratIds);

    void deleteByIdAndContratId(Long id, Long contratId);
}
