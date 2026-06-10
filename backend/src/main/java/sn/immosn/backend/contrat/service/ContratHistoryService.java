package sn.immosn.backend.contrat.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import sn.immosn.backend.client.web.contrat.dto.ContratHistoryDto;
import sn.immosn.backend.contrat.data.entity.Contrat;
import sn.immosn.backend.contrat.data.entity.StatutContrat;

public interface ContratHistoryService {
    void record(Contrat contrat, StatutContrat ancienStatut, StatutContrat nouveauStatut,
                String action, String commentaire);
    Page<ContratHistoryDto> getHistory(Long contratId, Pageable pageable);
}
