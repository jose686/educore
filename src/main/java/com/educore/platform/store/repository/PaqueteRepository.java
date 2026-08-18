package com.educore.platform.store.repository;

import com.educore.platform.store.model.Paquete;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad Paquete.
 */
public interface PaqueteRepository extends JpaRepository<Paquete, Long> {
    List<Paquete> findByActivoTrue();
}
