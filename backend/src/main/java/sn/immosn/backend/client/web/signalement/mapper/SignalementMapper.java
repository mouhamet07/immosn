package sn.immosn.backend.client.web.signalement.mapper;

import org.springframework.stereotype.Component;
import sn.immosn.backend.client.web.signalement.dto.SignalementResponseDto;
import sn.immosn.backend.signalement.data.entity.Signalement;

@Component
public class SignalementMapper {

    public SignalementResponseDto toDto(Signalement s) {
        return new SignalementResponseDto(
            s.getId(),
            s.getContrat().getId(),
            s.getContrat().getAnnonce().getLibelle(),
            s.getClient().getId(),
            s.getClient().getNomComplet(),
            s.getContenu(),
            s.getStatut(),
            s.isRead(),
            s.getReponseAdmin(),
            s.getCreatedAt()
        );
    }
}
