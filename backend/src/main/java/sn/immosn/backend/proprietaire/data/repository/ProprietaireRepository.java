package sn.immosn.backend.proprietaire.data.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.immosn.backend.proprietaire.data.entity.Proprietaire;

@Repository
public interface ProprietaireRepository extends JpaRepository<Proprietaire, Long> {

    Page<Proprietaire> findAllByOrderByNomCompletAsc(Pageable pageable);

    Page<Proprietaire> findByIsArchivedFalseOrderByNomCompletAsc(Pageable pageable);

    long countByIsArchivedFalse();
}
