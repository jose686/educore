package com.educore.platform.store.repository;

import com.educore.platform.store.model.PromocionCurso;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad PromocionCurso.
 */
public interface PromocionCursoRepository extends JpaRepository<PromocionCurso, Long> {
    List<PromocionCurso> findByCursoIdAndFechaInicioBeforeAndFechaFinAfter(Long cursoId, LocalDateTime now1, LocalDateTime now2);
}
