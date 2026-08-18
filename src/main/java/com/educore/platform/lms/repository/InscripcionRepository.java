package com.educore.platform.lms.repository;

import com.educore.platform.lms.model.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad Inscripcion.
 */
@Repository
public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    List<Inscripcion> findByStudentId(Long studentId);

    Optional<Inscripcion> findByStudentIdAndLmsCursoId(Long studentId, Long lmsCursoId);

    boolean existsByStudentIdAndLmsCursoId(Long studentId, Long lmsCursoId);

    @Modifying
    @Transactional
    int deleteByStudentIdAndLmsCursoId(Long studentId, Long lmsCursoId);

    List<Inscripcion> findByStudentIdAndLmsCursoIdAndFechaFinAfter(Long studentId, Long lmsCursoId, LocalDateTime fecha);

    List<Inscripcion> findByStudentIdAndFechaFinAfter(Long studentId, LocalDateTime fecha);

    boolean existsByStudentIdAndLmsCursoIdAndFechaFinAfter(Long studentId, Long lmsCursoId, LocalDateTime fecha);
}
