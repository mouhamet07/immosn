package sn.immosn.backend.discussion.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import sn.immosn.backend.client.web.discussion.dto.*;

public interface DiscussionService {

    // Client : créer ou récupérer une discussion existante pour une annonce
    DiscussionResponseDto createOrGetDiscussion(DiscussionCreateRequestDto request, String clientEmail);

    // Client : liste de ses discussions
    Page<DiscussionListDto> getClientDiscussions(String clientEmail, Pageable pageable);

    // Admin : toutes les discussions
    Page<DiscussionListDto> getAllDiscussions(Pageable pageable);

    // Détail d'une discussion (messages) — vérifie l'accès
    DiscussionResponseDto getDiscussion(Long discussionId, String userEmail, boolean isAdmin);

    // Envoyer un message dans une discussion
    MessageResponseDto sendMessage(Long discussionId, MessageCreateRequestDto request, String senderEmail, boolean isAdmin);
}
