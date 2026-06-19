package sn.immosn.backend.prospect.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.immosn.backend.prospect.data.entity.Prospect;

import java.util.Optional;

@Repository
public interface ProspectRepository extends JpaRepository<Prospect, Long> {

    Optional<Prospect> findByToken(String token);

    /** Réutilise le prospect existant pour un même email afin de regrouper ses demandes/discussions. */
    Optional<Prospect> findFirstByEmailOrderByCreatedAtAsc(String email);
}
