package sn.immosn.backend.discussion.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.immosn.backend.annonce.data.entity.Annonce;
import sn.immosn.backend.annonce.data.repository.AnnonceRepository;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.client.web.discussion.dto.*;
import sn.immosn.backend.client.web.discussion.mapper.DiscussionMapper;
import sn.immosn.backend.discussion.data.entity.Discussion;
import sn.immosn.backend.discussion.data.entity.Message;
import sn.immosn.backend.discussion.data.entity.SenderRole;
import sn.immosn.backend.discussion.data.repository.DiscussionRepository;
import sn.immosn.backend.discussion.data.repository.MessageRepository;
import sn.immosn.backend.discussion.service.DiscussionService;
import sn.immosn.backend.shared.exception.EntityNotFoundException;

@Service
@RequiredArgsConstructor
public class DiscussionServiceImpl implements DiscussionService {

    private static final Logger log = LoggerFactory.getLogger(DiscussionServiceImpl.class);

    private final DiscussionRepository discussionRepository;
    private final MessageRepository    messageRepository;
    private final AnnonceRepository    annonceRepository;
    private final UserRepository       userRepository;
    private final DiscussionMapper     discussionMapper;

    /**
     * Crée une discussion ou retourne l'existante (idempotent).
     * La contrainte UNIQUE (client_id, annonce_id) en base garantit l'unicité même en cas de concurrent.
     */
    @Override
    @Transactional
    public DiscussionResponseDto createOrGetDiscussion(DiscussionCreateRequestDto request, String clientEmail) {
        User    client  = loadUser(clientEmail);
        Annonce annonce = annonceRepository.findByIdAndIsArchivedFalse(request.annonceId())
            .orElseThrow(() -> new EntityNotFoundException("Annonce non trouvée : id=" + request.annonceId()));

        // Idempotent : récupère l'existante ou crée une nouvelle
        Discussion discussion = discussionRepository
            .findByClientIdAndAnnonceId(client.getId(), annonce.getId())
            .orElseGet(() -> {
                Discussion d = Discussion.builder()
                    .client(client)
                    .annonce(annonce)
                    .build();
                Discussion saved = discussionRepository.save(d);
                log.info("Discussion créée : id={}, clientId={}, annonceId={}",
                    saved.getId(), client.getId(), annonce.getId());
                return saved;
            });

        // Ajouter le message
        Message message = Message.builder()
            .contenu(request.premierMessage())
            .senderRole(SenderRole.CLIENT)
            .discussion(discussion)
            .build();
        messageRepository.save(message);
        discussion.getMessages().add(message);

        long unread = discussionRepository.countUnread(discussion.getId(), SenderRole.CLIENT);
        return discussionMapper.toResponseDto(discussion, unread);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DiscussionListDto> getClientDiscussions(String clientEmail, Pageable pageable) {
        User client = loadUser(clientEmail);
        return discussionRepository
            .findByClientIdOrderByCreatedAtDesc(client.getId(), pageable)
            .map(d -> discussionMapper.toListDto(d,
                discussionRepository.countUnread(d.getId(), SenderRole.CLIENT)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DiscussionListDto> getAllDiscussions(Pageable pageable) {
        return discussionRepository
            .findAllByOrderByCreatedAtDesc(pageable)
            .map(d -> discussionMapper.toListDto(d,
                discussionRepository.countUnread(d.getId(), SenderRole.ADMIN)));
    }

    @Override
    @Transactional
    public DiscussionResponseDto getDiscussion(Long discussionId, String userEmail, boolean isAdmin) {
        Discussion discussion = loadDiscussion(discussionId);

        if (!isAdmin && !discussion.getClient().getEmail().equals(userEmail)) {
            throw new EntityNotFoundException("Discussion non trouvée");
        }

        // Marquer les messages de l'interlocuteur comme lus
        SenderRole toMarkRead = isAdmin ? SenderRole.CLIENT : SenderRole.ADMIN;
        messageRepository.markAsRead(discussionId, toMarkRead);

        long unread = discussionRepository.countUnread(discussionId,
            isAdmin ? SenderRole.ADMIN : SenderRole.CLIENT);
        return discussionMapper.toResponseDto(discussion, unread);
    }

    @Override
    @Transactional
    public MessageResponseDto sendMessage(Long discussionId, MessageCreateRequestDto request,
                                          String senderEmail, boolean isAdmin) {
        Discussion discussion = loadDiscussion(discussionId);

        if (!isAdmin && !discussion.getClient().getEmail().equals(senderEmail)) {
            throw new EntityNotFoundException("Discussion non trouvée");
        }

        SenderRole role = isAdmin ? SenderRole.ADMIN : SenderRole.CLIENT;
        Message message = Message.builder()
            .contenu(request.contenu())
            .senderRole(role)
            .discussion(discussion)
            .build();

        Message saved = messageRepository.save(message);
        log.debug("Message envoyé : discussionId={}, senderRole={}", discussionId, role);
        return discussionMapper.toMessageDto(saved);
    }

    private User loadUser(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé : email=" + email));
    }

    private Discussion loadDiscussion(Long id) {
        return discussionRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Discussion non trouvée : id=" + id));
    }
}
