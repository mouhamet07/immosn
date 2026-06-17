package sn.immosn.backend.client.web.discussion.mapper;

import org.springframework.stereotype.Component;
import sn.immosn.backend.client.web.discussion.dto.DiscussionInviteResponseDto;
import sn.immosn.backend.client.web.discussion.dto.DiscussionListDto;
import sn.immosn.backend.client.web.discussion.dto.DiscussionResponseDto;
import sn.immosn.backend.client.web.discussion.dto.MessageResponseDto;
import sn.immosn.backend.discussion.data.entity.Discussion;
import sn.immosn.backend.discussion.data.entity.Message;
import sn.immosn.backend.discussion.data.entity.SenderRole;

import java.util.List;

@Component
public class DiscussionMapper {

    public MessageResponseDto toMessageDto(Message m) {
        String senderName = m.getSenderRole() == SenderRole.ADMIN
            ? "Agence IMMOSN"
            : resolveClientName(m.getDiscussion());
        return new MessageResponseDto(
            m.getId(),
            m.getContenu(),
            m.getSenderRole(),
            senderName,
            m.isRead(),
            m.getCreatedAt()
        );
    }

    public DiscussionResponseDto toResponseDto(Discussion d, long unreadCount) {
        List<MessageResponseDto> messageDtos = d.getMessages().stream()
            .map(this::toMessageDto)
            .toList();

        String image = (d.getAnnonce().getImages() != null && !d.getAnnonce().getImages().isEmpty())
            ? d.getAnnonce().getImages().get(0) : null;

        // Client nullable (discussions invité) : clientId=null et nom dérivé du prospect.
        Long clientId = d.getClient() != null ? d.getClient().getId() : null;
        return new DiscussionResponseDto(
            d.getId(),
            d.getAnnonce().getId(),
            d.getAnnonce().getLibelle(),
            d.getAnnonce().getAdresse(),
            image,
            clientId,
            resolveClientName(d),
            messageDtos,
            unreadCount,
            d.getCreatedAt()
        );
    }

    /** Réponse dédiée au flux invité : inclut le token de suivi de la discussion. */
    public DiscussionInviteResponseDto toInviteDto(Discussion d) {
        List<MessageResponseDto> messageDtos = d.getMessages().stream()
            .map(this::toMessageDto)
            .toList();

        String image = (d.getAnnonce().getImages() != null && !d.getAnnonce().getImages().isEmpty())
            ? d.getAnnonce().getImages().get(0) : null;

        return new DiscussionInviteResponseDto(
            d.getId(),
            d.getGuestToken(),
            d.getAnnonce().getId(),
            d.getAnnonce().getLibelle(),
            d.getAnnonce().getAdresse(),
            image,
            resolveClientName(d),
            d.getGuestEmail(),
            messageDtos,
            d.getCreatedAt()
        );
    }

    /** Nom affiché du côté « client » : compte authentifié, sinon prospect, sinon « Visiteur ». */
    private String resolveClientName(Discussion d) {
        if (d.getClient() != null) {
            return d.getClient().getNomComplet();
        }
        if (d.getProspect() != null) {
            String prenom = d.getProspect().getPrenom() != null ? d.getProspect().getPrenom() + " " : "";
            String nom = d.getProspect().getNom() != null ? d.getProspect().getNom() : "";
            String complet = (prenom + nom).trim();
            return complet.isEmpty() ? "Visiteur" : complet;
        }
        return "Visiteur";
    }

    public DiscussionListDto toListDto(Discussion d, long unreadCount) {
        Message last = d.getMessages().isEmpty() ? null : d.getMessages().get(d.getMessages().size() - 1);

        String image = (d.getAnnonce().getImages() != null && !d.getAnnonce().getImages().isEmpty())
            ? d.getAnnonce().getImages().get(0) : null;

        Long clientId = d.getClient() != null ? d.getClient().getId() : null;
        return new DiscussionListDto(
            d.getId(),
            d.getAnnonce().getId(),
            d.getAnnonce().getLibelle(),
            d.getAnnonce().getAdresse(),
            image,
            clientId,
            resolveClientName(d),
            last != null ? last.getContenu() : null,
            last != null ? DiscussionListDto.SenderRoleDto.valueOf(last.getSenderRole().name()) : null,
            unreadCount,
            d.getCreatedAt(),
            last != null ? last.getCreatedAt() : d.getCreatedAt()
        );
    }
}
