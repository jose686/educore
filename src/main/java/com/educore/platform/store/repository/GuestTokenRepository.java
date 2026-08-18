package com.educore.platform.store.repository;

import com.educore.platform.store.model.GuestToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad GuestToken.
 */
public interface GuestTokenRepository extends JpaRepository<GuestToken, Long> {
    Optional<GuestToken> findByTokenAndActivoTrue(String token);
}
