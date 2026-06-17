package sn.immosn.backend.discussion.data.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sn.immosn.backend.discussion.data.entity.Discussion;

import java.util.Optional;

@Repository
public interface DiscussionRepository extends JpaRepository<Discussion, Long> {

    // Discussions d'un client, triées par date du dernier message (plus récent en premier)
    @Query("""
        SELECT d FROM Discussion d
        WHERE d.client.id = :clientId
        ORDER BY (
            SELECT COALESCE(MAX(m.createdAt), d.createdAt)
            FROM Message m WHERE m.discussion = d
        ) DESC
        """)
    Page<Discussion> findByClientIdOrderByLastMessageDesc(
            @Param("clientId") Long clientId, Pageable pageable);

    // Toutes les discussions (admin), triées par date du dernier message (plus récent en premier)
    @Query("""
        SELECT d FROM Discussion d
        ORDER BY (
            SELECT COALESCE(MAX(m.createdAt), d.createdAt)
            FROM Message m WHERE m.discussion = d
        ) DESC
        """)
    Page<Discussion> findAllOrderByLastMessageDesc(Pageable pageable);

    // Vérifier si une discussion existe déjà entre un client et une annonce
    Optional<Discussion> findByClientIdAndAnnonceId(Long clientId, Long annonceId);

    // Retrouver une discussion invité par son token de suivi non devinable
    Optional<Discussion> findByGuestToken(String guestToken);

    // INNER JOIN FETCH client : volontaire. Le dashboard n'affiche que les discussions de clients
    // authentifiés (il lit client.nomComplet). Les discussions invité (client_id NULL) sont
    // naturellement exclues — le suivi invité passe par findByGuestToken, hors dashboard.
    @Query("""
        SELECT d FROM Discussion d
        JOIN FETCH d.client
        JOIN FETCH d.annonce
        ORDER BY d.createdAt DESC
        """)
    List<Discussion> findTopRecentForDashboard(Pageable pageable);

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
