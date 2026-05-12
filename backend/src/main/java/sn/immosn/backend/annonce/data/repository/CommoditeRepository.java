package sn.immosn.backend.annonce.data.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sn.immosn.backend.annonce.data.entity.Commodite;

@Repository
public interface CommoditeRepository extends JpaRepository<Commodite, Long> {
    List<Commodite> findByIsArchivedFalse();
    List<Commodite> findByIdInAndIsArchivedFalse(List<Long> ids);
}
