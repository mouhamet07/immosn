package sn.immosn.backend.auth.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sn.immosn.backend.auth.data.entity.BlacklistedToken;

import java.util.Optional;

@Repository
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {
    boolean existsByToken(String token);
    Optional<BlacklistedToken> findByToken(String token);
}
