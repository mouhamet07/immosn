package sn.immosn.backend.auth.data.repository;

import sn.immosn.backend.auth.data.entity.Role;
import sn.immosn.backend.auth.data.entity.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role,Long> {
    Optional<Role> findByRole(RoleType role);

}
