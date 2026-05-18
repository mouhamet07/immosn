package sn.immosn.backend.discussion.data.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sn.immosn.backend.discussion.data.entity.Discussion;

import java.util.Optional;

@Repository
public interface DiscussionRepository extends JpaRepository<Discussion, Long> {

    // Discussions d'un client spécifique, triées par date décroissante
    Page<Discussion> findByClientIdOrderByCreatedAtDesc(Long clientId, Pageable pageable);

    // Toutes les discussions (admin), triées par date décroissante
    Page<Discussion> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Vérifier si une discussion existe déjà entre un client et une annonce
    Optional<Discussion> findByClientIdAndAnnonceId(Long clientId, Long annonceId);

    // Compter les messages non lus d'une discussion pour un rôle donné
    @Query("""
        SELECT COUNT(m) FROM Message m
        WHERE m.discussion.id = :discussionId
          AND m.isRead = false
          AND m.senderRole <> :excludeRole
        """)
    long countUnread(@Param("discussionId") Long discussionId,
                     @Param("excludeRole") sn.immosn.backend.discussion.data.entity.SenderRole excludeRole);
}
