package sn.immosn.backend.client.web.discussion.mapper;

import org.springframework.stereotype.Component;
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
        String senderName = m.getSenderRole() == SenderRole.ADMIN ? "Agence IMMOSN" : m.getDiscussion().getClient().getNomComplet();
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

        return new DiscussionResponseDto(
            d.getId(),
            d.getAnnonce().getId(),
            d.getAnnonce().getLibelle(),
            d.getAnnonce().getAdresse(),
            image,
            d.getClient().getId(),
            d.getClient().getNomComplet(),
            messageDtos,
            unreadCount,
            d.getCreatedAt()
        );
    }

    public DiscussionListDto toListDto(Discussion d, long unreadCount) {
        Message last = d.getMessages().isEmpty() ? null : d.getMessages().get(d.getMessages().size() - 1);

        String image = (d.getAnnonce().getImages() != null && !d.getAnnonce().getImages().isEmpty())
            ? d.getAnnonce().getImages().get(0) : null;

        return new DiscussionListDto(
            d.getId(),
            d.getAnnonce().getId(),
            d.getAnnonce().getLibelle(),
            d.getAnnonce().getAdresse(),
            image,
            d.getClient().getId(),
            d.getClient().getNomComplet(),
            last != null ? last.getContenu() : null,
            last != null ? DiscussionListDto.SenderRoleDto.valueOf(last.getSenderRole().name()) : null,
            unreadCount,
            d.getCreatedAt(),
            last != null ? last.getCreatedAt() : d.getCreatedAt()
        );
    }
}
