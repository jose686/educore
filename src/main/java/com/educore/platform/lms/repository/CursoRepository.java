package com.educore.platform.lms.repository;

import com.educore.platform.lms.model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de acceso a datos para la entidad Curso.
 */
@Repository
public interface CursoRepository extends JpaRepository<Curso, Long> {
}
