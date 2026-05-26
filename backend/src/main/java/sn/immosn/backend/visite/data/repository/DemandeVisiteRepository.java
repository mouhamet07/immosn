package sn.immosn.backend.visite.data.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.immosn.backend.visite.data.entity.DemandeVisite;
import sn.immosn.backend.visite.data.entity.StatutDemandeVisite;

import java.util.Optional;

@Repository
public interface DemandeVisiteRepository extends JpaRepository<DemandeVisite, Long> {

    Page<DemandeVisite> findByClientIdAndIsArchivedFalseOrderByCreatedAtDesc(Long clientId, Pageable pageable);

    Page<DemandeVisite> findByClientIdAndStatutAndIsArchivedFalseOrderByCreatedAtDesc(
        Long clientId, StatutDemandeVisite statut, Pageable pageable);

    Page<DemandeVisite> findByIsArchivedFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<DemandeVisite> findByStatutAndIsArchivedFalseOrderByCreatedAtDesc(
        StatutDemandeVisite statut, Pageable pageable);

    Optional<DemandeVisite> findByIdAndIsArchivedFalse(Long id);

    boolean existsByClientIdAndAnnonceIdAndStatutIn(
        Long clientId, Long annonceId, java.util.List<StatutDemandeVisite> statuts);
}
