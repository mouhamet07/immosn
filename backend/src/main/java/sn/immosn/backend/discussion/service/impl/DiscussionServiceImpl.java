package sn.immosn.backend.discussion.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import sn.immosn.backend.discussion.service.GuestDiscussionNotificationService;
import sn.immosn.backend.prospect.data.entity.Prospect;
import sn.immosn.backend.prospect.data.repository.ProspectRepository;
import sn.immosn.backend.shared.exception.EntityNotFoundException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiscussionServiceImpl implements DiscussionService {

    private static final Logger log = LoggerFactory.getLogger(DiscussionServiceImpl.class);

    private final DiscussionRepository discussionRepository;
    private final MessageRepository messageRepository;
    private final AnnonceRepository annonceRepository;
    private final UserRepository userRepository;
    private final ProspectRepository prospectRepository;
    private final DiscussionMapper discussionMapper;
    private final GuestDiscussionNotificationService guestNotificationService;

    /**
     * Crée une discussion ou retourne l'existante (idempotent).
     * Le message initial n'est sauvegardé qu'à la création (évite les doublons).
     */
    @Override
    @Transactional
    public DiscussionResponseDto createOrGetDiscussion(DiscussionCreateRequestDto request, String clientEmail) {
        User client = loadUser(clientEmail);
        Annonce annonce = annonceRepository.findByIdAndIsArchivedFalse(request.annonceId())
            .orElseThrow(() -> new EntityNotFoundException("Annonce non trouvée : id=" + request.annonceId()));

        boolean[] isNew = {false};
        Discussion discussion = discussionRepository
            .findByClientIdAndAnnonceId(client.getId(), annonce.getId())
            .orElseGet(() -> {
                Discussion d = Discussion.builder()
                    .client(client)
                    .annonce(annonce)
                    .build();
                Discussion saved = discussionRepository.save(d);
                log.info("[{}] USER:{} CLIENT CREATE_DISCUSSION DISCUSSION:{}",
                    LocalDateTime.now(), client.getId(), saved.getId());
                isNew[0] = true;
                return saved;
            });

        if (isNew[0]) {
            Message message = Message.builder()
                .contenu(request.premierMessage())
                .senderRole(SenderRole.CLIENT)
                .discussion(discussion)
                .build();
            messageRepository.save(message);
            discussion.getMessages().add(message);
        }

        long unread = discussionRepository.countUnread(discussion.getId(), SenderRole.CLIENT);
        return discussionMapper.toResponseDto(discussion, unread);
    }

    /**
     * Ouverture d'une discussion par un visiteur non authentifié.
     * Crée (ou réutilise par email) un Prospect, ouvre une discussion invité (guestToken)
     * et y dépose le premier message. Le visiteur conserve le guestToken pour la suite.
     */
    @Override
    @Transactional
    public DiscussionInviteResponseDto createInviteDiscussion(ContactInviteRequestDto request) {
        Annonce annonce = annonceRepository.findByIdAndIsArchivedFalse(request.annonceId())
            .orElseThrow(() -> new EntityNotFoundException("Annonce non trouvée : id=" + request.annonceId()));

        Prospect prospect = prospectRepository.findFirstByEmailOrderByCreatedAtAsc(request.email())
            .orElseGet(() -> prospectRepository.save(Prospect.builder()
                .nom(request.nom())
                .prenom(request.prenom())
                .email(request.email())
                .telephone(request.telephone())
                .adresse(request.adresse())
                .token(UUID.randomUUID().toString())
                .build()));

        Discussion discussion = Discussion.builder()
            .prospect(prospect)
            .annonce(annonce)
            .guestToken(UUID.randomUUID().toString())
            .guestEmail(request.email())
            .build();
        Discussion saved = discussionRepository.save(discussion);

        // Le visiteur est la partie « non-admin » : on réutilise SenderRole.CLIENT
        // pour rester compatible avec countUnread/markAsRead existants (ADMIN vs non-ADMIN).
        Message message = Message.builder()
            .contenu(request.premierMessage())
            .senderRole(SenderRole.CLIENT)
            .discussion(saved)
            .build();
        messageRepository.save(message);
        saved.getMessages().add(message);

        log.info("[{}] GUEST:{} CREATE_DISCUSSION DISCUSSION:{}",
            LocalDateTime.now(), prospect.getEmail(), saved.getId());
        return discussionMapper.toInviteDto(saved);
    }

    /**
     * Poursuite d'une conversation invité via le guestToken.
     */
    @Override
    @Transactional
    public MessageResponseDto sendInviteMessage(String guestToken, MessageCreateRequestDto request) {
        Discussion discussion = discussionRepository.findByGuestToken(guestToken)
            .orElseThrow(() -> new EntityNotFoundException("Discussion introuvable"));

        Message message = Message.builder()
            .contenu(request.contenu())
            .senderRole(SenderRole.CLIENT)
            .discussion(discussion)
            .build();
        Message savedMsg = messageRepository.save(message);
        log.info("[{}] GUEST_TOKEN:{} SEND_MESSAGE DISCUSSION:{}",
            LocalDateTime.now(), guestToken, discussion.getId());
        return discussionMapper.toMessageDto(savedMsg);
    }

    /**
     * Relecture d'une discussion invité via le guestToken (marque les réponses admin comme lues).
     */
    @Override
    @Transactional
    public DiscussionInviteResponseDto getDiscussionByToken(String guestToken) {
        Discussion discussion = discussionRepository.findByGuestToken(guestToken)
            .orElseThrow(() -> new EntityNotFoundException("Discussion introuvable"));
        messageRepository.markAsRead(discussion.getId(), SenderRole.ADMIN);
        Discussion refreshed = discussionRepository.findByGuestToken(guestToken)
            .orElseThrow(() -> new EntityNotFoundException("Discussion introuvable"));
        return discussionMapper.toInviteDto(refreshed);
    }

    /**
     * Liste des discussions d'un client, triées par dernière activité.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<DiscussionListDto> getClientDiscussions(String clientEmail, Pageable pageable) {
        User client = loadUser(clientEmail);
        // Tri par dernière activité (dernier message), pas par date de création
        Pageable unordered = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return discussionRepository
            .findByClientIdOrderByLastMessageDesc(client.getId(), unordered)
            .map(d -> discussionMapper.toListDto(d,
                discussionRepository.countUnread(d.getId(), SenderRole.CLIENT)));
    }

    /**
     * Toutes les discussions (admin/super_admin), triées par dernière activité.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<DiscussionListDto> getAllDiscussions(Pageable pageable) {
        // Tri par dernière activité : les discussions récemment actives remontent en tête
        Pageable unordered = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return discussionRepository
            .findAllOrderByLastMessageDesc(unordered)
            .map(d -> discussionMapper.toListDto(d,
                discussionRepository.countUnread(d.getId(), SenderRole.ADMIN)));
    }

    /**
     * Retourne les messages d'une discussion en marquant les messages de l'interlocuteur comme lus.
     * La discussion est rechargée après le bulk UPDATE pour avoir les états isRead corrects.
     */
    @Override
    @Transactional
    public DiscussionResponseDto getDiscussion(Long discussionId, String userEmail, boolean isAdmin) {
        Discussion discussion = loadDiscussion(discussionId);

        if (!isAdmin && !discussion.getClient().getEmail().equals(userEmail)) {
            throw new EntityNotFoundException("Discussion introuvable");
        }

        // Marquer les messages de l'interlocuteur comme lus
        // (clearAutomatically=true dans le repository invalide le cache L1 Hibernate)
        SenderRole toMarkRead = isAdmin ? SenderRole.CLIENT : SenderRole.ADMIN;
        messageRepository.markAsRead(discussionId, toMarkRead);

        // Recharger la discussion après le bulk UPDATE pour refléter les isRead corrects
        Discussion refreshed = loadDiscussion(discussionId);

        long unread = discussionRepository.countUnread(discussionId,
            isAdmin ? SenderRole.ADMIN : SenderRole.CLIENT);
        return discussionMapper.toResponseDto(refreshed, unread);
    }

    /**
     * Envoie un message dans une discussion.
     */
    @Override
    @Transactional
    public MessageResponseDto sendMessage(Long discussionId, MessageCreateRequestDto request,
                                          String senderEmail, boolean isAdmin) {
        Discussion discussion = loadDiscussion(discussionId);

        if (!isAdmin && !discussion.getClient().getEmail().equals(senderEmail)) {
            throw new EntityNotFoundException("Discussion introuvable");
        }

        SenderRole role = isAdmin ? SenderRole.ADMIN : SenderRole.CLIENT;
        Message message = Message.builder()
            .contenu(request.contenu())
            .senderRole(role)
            .discussion(discussion)
            .build();

        Message saved = messageRepository.save(message);
        log.info("[{}] USER_EMAIL:{} {} SEND_MESSAGE DISCUSSION:{}",
            LocalDateTime.now(), senderEmail, role, discussionId);

        // Réponse de l'admin à un visiteur sans compte : seul l'email peut le prévenir.
        if (isAdmin && discussion.getClient() == null) {
            guestNotificationService.notifyGuestOfReply(discussion, request.contenu());
        }

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
