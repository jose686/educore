package com.educore.platform.lms.repository;

import com.educore.platform.lms.model.Leccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de acceso a datos para la entidad Leccion.
 */
@Repository
public interface LeccionRepository extends JpaRepository<Leccion, Long> {

    @Query("SELECT MAX(l.orden) FROM Leccion l WHERE l.modulo.id = :moduloId")
    Integer findMaxOrdenByModuloId(@Param("moduloId") Long moduloId);
}

