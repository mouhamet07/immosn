package sn.immosn.backend.signalement.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.immosn.backend.client.web.signalement.dto.SignalementHistoryDto;
import sn.immosn.backend.signalement.data.entity.Signalement;
import sn.immosn.backend.signalement.data.entity.SignalementHistory;
import sn.immosn.backend.signalement.data.entity.StatutSignalement;
import sn.immosn.backend.signalement.data.repository.SignalementHistoryRepository;
import sn.immosn.backend.signalement.service.SignalementHistoryService;

@Service
@RequiredArgsConstructor
public class SignalementHistoryServiceImpl implements SignalementHistoryService {

    private final SignalementHistoryRepository historyRepository;

    @Override
    @Transactional
    public void record(Signalement signalement, StatutSignalement ancienStatut, StatutSignalement nouveauStatut,
                       String action, String motif, String auteurEmail) {
        historyRepository.save(SignalementHistory.builder()
            .signalement(signalement)
            .ancienStatut(ancienStatut)
            .nouveauStatut(nouveauStatut)
            .action(action)
            .motif(motif)
            .auteurEmail(auteurEmail)
            .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SignalementHistoryDto> getHistory(Long signalementId, Pageable pageable) {
        return historyRepository.findBySignalementIdOrderByCreatedAtDesc(signalementId, pageable)
            .map(h -> new SignalementHistoryDto(
                h.getId(), h.getAncienStatut(), h.getNouveauStatut(),
                h.getAuteurEmail(), h.getAction(), h.getMotif(), h.getCreatedAt()));
    }
}
