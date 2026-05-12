package sn.immosn.backend.annonce.data.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import sn.immosn.backend.annonce.data.entity.Commodite;

public interface CommoditeRepository extends JpaRepository<Commodite, Long> {
    List<Commodite> findByIsArchivedFalse();
    List<Commodite> findByIdInAndIsArchivedFalse(List<Long> ids);
}
