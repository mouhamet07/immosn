package sn.immosn.backend.visite.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.immosn.backend.visite.data.entity.RapportVisite;

import java.util.Optional;

@Repository
public interface RapportVisiteRepository extends JpaRepository<RapportVisite, Long> {

    Optional<RapportVisite> findByDemandeVisiteId(Long demandeVisiteId);

    boolean existsByDemandeVisiteId(Long demandeVisiteId);
}
