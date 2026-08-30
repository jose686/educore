package com.educore.platform.store.repository;

import com.educore.platform.store.model.PaqueteCreditos;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad PaqueteCreditos.
 */
public interface PaqueteCreditosRepository extends JpaRepository<PaqueteCreditos, Long> {
    List<PaqueteCreditos> findByActivoTrueOrderByOrdenVisualizacionAsc();
}
