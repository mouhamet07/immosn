package sn.immosn.backend.discussion.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sn.immosn.backend.discussion.data.entity.Message;
import sn.immosn.backend.discussion.data.entity.SenderRole;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Messages d'une discussion, triés chronologiquement
    List<Message> findByDiscussionIdOrderByCreatedAtAsc(Long discussionId);

    // Marquer tous les messages non lus comme lus (par rôle opposé).
    // clearAutomatically = true : invalide le cache L1 Hibernate après le bulk UPDATE
    // pour que le rechargement ultérieur reflète les nouvelles valeurs isRead.
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Message m SET m.isRead = true
        WHERE m.discussion.id = :discussionId
          AND m.senderRole = :senderRole
          AND m.isRead = false
        """)
    void markAsRead(@Param("discussionId") Long discussionId,
                    @Param("senderRole") SenderRole senderRole);
}
