package sn.immosn.backend.signalement.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import sn.immosn.backend.client.web.signalement.dto.SignalementHistoryDto;
import sn.immosn.backend.signalement.data.entity.Signalement;
import sn.immosn.backend.signalement.data.entity.StatutSignalement;

public interface SignalementHistoryService {

    void record(Signalement signalement, StatutSignalement ancienStatut, StatutSignalement nouveauStatut,
                String action, String motif, String auteurEmail);

    Page<SignalementHistoryDto> getHistory(Long signalementId, Pageable pageable);
}
