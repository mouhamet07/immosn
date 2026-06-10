package sn.immosn.backend.visite.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import sn.immosn.backend.client.web.visite.dto.VisiteHistoryDto;
import sn.immosn.backend.visite.data.entity.DemandeVisite;
import sn.immosn.backend.visite.data.entity.StatutDemandeVisite;

import java.time.LocalDateTime;

public interface VisiteHistoryService {
    void record(DemandeVisite visite, StatutDemandeVisite ancienStatut, StatutDemandeVisite nouveauStatut,
                String action, String commentaire,
                LocalDateTime ancienneDateVisite, LocalDateTime nouvelleDateVisite);
    Page<VisiteHistoryDto> getHistory(Long visiteId, Pageable pageable);
}
