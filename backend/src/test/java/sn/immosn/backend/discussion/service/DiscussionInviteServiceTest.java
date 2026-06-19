package sn.immosn.backend.discussion.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.immosn.backend.annonce.data.entity.Annonce;
import sn.immosn.backend.annonce.data.entity.TypeBienAnnonce;
import sn.immosn.backend.annonce.data.repository.AnnonceRepository;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.client.web.discussion.dto.ContactInviteRequestDto;
import sn.immosn.backend.client.web.discussion.dto.DiscussionInviteResponseDto;
import sn.immosn.backend.client.web.discussion.mapper.DiscussionMapper;
import sn.immosn.backend.discussion.data.entity.Discussion;
import sn.immosn.backend.discussion.data.entity.Message;
import sn.immosn.backend.discussion.data.entity.SenderRole;
import sn.immosn.backend.discussion.data.repository.DiscussionRepository;
import sn.immosn.backend.discussion.data.repository.MessageRepository;
import sn.immosn.backend.discussion.service.impl.DiscussionServiceImpl;
import sn.immosn.backend.prospect.data.entity.Prospect;
import sn.immosn.backend.prospect.data.repository.ProspectRepository;
import sn.immosn.backend.shared.exception.EntityNotFoundException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiscussionService — contact invité (parcours visiteur)")
class DiscussionInviteServiceTest {

    @Mock DiscussionRepository discussionRepository;
    @Mock MessageRepository messageRepository;
    @Mock AnnonceRepository annonceRepository;
    @Mock UserRepository userRepository;
    @Mock ProspectRepository prospectRepository;
    @org.mockito.Spy DiscussionMapper discussionMapper = new DiscussionMapper();

    @InjectMocks DiscussionServiceImpl service;

    private Annonce annonce;

    @BeforeEach
    void setUp() {
        TypeBienAnnonce type = new TypeBienAnnonce();
        type.setLibelle("Appartement");
        annonce = Annonce.builder()
            .libelle("Appart Plateau")
            .prix(BigDecimal.valueOf(45_000_000))
            .adresse("Plateau, Dakar")
            .nbrPieces(3)
            .surface(90.0)
            .typeBien(type)
            .isArchived(false)
            .build();
        try {
            var f = Annonce.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(annonce, 2L);
        } catch (Exception ignored) {}
    }

    private ContactInviteRequestDto request() {
        return new ContactInviteRequestDto(
            2L, "Sow", "Ibrahima", "+221770000002", "ibrahima@email.com",
            "Plateau, Dakar", "Bonjour, ce bien est-il toujours disponible ?"
        );
    }

    @Test
    @DisplayName("createInviteDiscussion — crée prospect + discussion invité avec premier message")
    void createInvite_createsDiscussionWithGuestToken() {
        when(annonceRepository.findByIdAndIsArchivedFalse(2L)).thenReturn(Optional.of(annonce));
        when(prospectRepository.findFirstByEmailOrderByCreatedAtAsc("ibrahima@email.com"))
            .thenReturn(Optional.empty());
        when(prospectRepository.save(any(Prospect.class))).thenAnswer(inv -> {
            Prospect p = inv.getArgument(0);
            p.setId(5L);
            return p;
        });
        when(discussionRepository.save(any(Discussion.class))).thenAnswer(inv -> {
            Discussion d = inv.getArgument(0);
            d.setId(8L);
            return d;
        });
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        DiscussionInviteResponseDto dto = service.createInviteDiscussion(request());

        assertThat(dto.id()).isEqualTo(8L);
        assertThat(dto.guestToken()).isNotBlank();
        assertThat(dto.guestEmail()).isEqualTo("ibrahima@email.com");
        assertThat(dto.messages()).hasSize(1);
        assertThat(dto.messages().get(0).contenu()).contains("toujours disponible");

        // La discussion invité n'a pas de client User, mais un prospect + guestToken
        ArgumentCaptor<Discussion> captor = ArgumentCaptor.forClass(Discussion.class);
        verify(discussionRepository).save(captor.capture());
        assertThat(captor.getValue().getClient()).isNull();
        assertThat(captor.getValue().getProspect()).isNotNull();
        assertThat(captor.getValue().getGuestToken()).isNotBlank();

        // Le message du visiteur est enregistré comme partie non-admin (CLIENT)
        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(msgCaptor.capture());
        assertThat(msgCaptor.getValue().getSenderRole()).isEqualTo(SenderRole.CLIENT);
    }

    @Test
    @DisplayName("getDiscussionByToken — token inconnu → EntityNotFoundException")
    void getByToken_notFound() {
        when(discussionRepository.findByGuestToken("inconnu")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDiscussionByToken("inconnu"))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("sendInviteMessage — ajoute un message à la discussion identifiée par le token")
    void sendInviteMessage_appendsMessage() {
        Prospect prospect = Prospect.builder().nom("Sow").email("ibrahima@email.com")
            .telephone("+221770000002").token("ptok").build();
        Discussion discussion = Discussion.builder()
            .prospect(prospect).annonce(annonce).guestToken("gtok").guestEmail("ibrahima@email.com")
            .build();
        discussion.setId(8L);

        when(discussionRepository.findByGuestToken("gtok")).thenReturn(Optional.of(discussion));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = service.sendInviteMessage("gtok",
            new sn.immosn.backend.client.web.discussion.dto.MessageCreateRequestDto("Merci !"));

        assertThat(dto.contenu()).isEqualTo("Merci !");
        assertThat(dto.senderRole()).isEqualTo(SenderRole.CLIENT);
    }
}
