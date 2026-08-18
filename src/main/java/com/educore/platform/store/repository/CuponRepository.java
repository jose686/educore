package com.educore.platform.store.repository;

import com.educore.platform.store.model.Cupon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad Cupon.
 */
public interface CuponRepository extends JpaRepository<Cupon, Long> {
    Optional<Cupon> findByCodigoAndActivoTrue(String codigo);
}
