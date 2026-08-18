package com.educore.platform.lms.repository;

import com.educore.platform.lms.model.Modulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de acceso a datos para la entidad Modulo.
 */
@Repository
public interface ModuloRepository extends JpaRepository<Modulo, Long> {

    @Query("SELECT MAX(m.orden) FROM Modulo m WHERE m.curso.id = :cursoId")
    Integer findMaxOrdenByCursoId(@Param("cursoId") Long cursoId);
}

