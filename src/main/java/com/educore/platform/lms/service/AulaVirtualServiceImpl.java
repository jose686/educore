package com.educore.platform.lms.service;

import com.educore.platform.lms.model.Inscripcion;
import com.educore.platform.lms.repository.InscripcionRepository;
import com.educore.platform.users.service.UserPublicService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementación de AulaVirtualService para gestionar inscripciones en el LMS.
 */
@Service
@Slf4j
public class AulaVirtualServiceImpl implements AulaVirtualService {

    private final InscripcionRepository inscripcionRepository;
    private final UserPublicService userPublicService;

    public AulaVirtualServiceImpl(InscripcionRepository inscripcionRepository, UserPublicService userPublicService) {
        this.inscripcionRepository = inscripcionRepository;
        this.userPublicService = userPublicService;
    }

    @Override
    @Transactional
    public void matricularAlumno(String email, Long cursoId) {
        log.info("[AULA-VIRTUAL] Iniciando matriculación para email: {} en cursoId: {}", email, cursoId);

        Optional<Long> userIdOpt = userPublicService.getUserIdByEmail(email);
        if (userIdOpt.isEmpty()) {
            log.error("[AULA-VIRTUAL] ERROR: No se encontró usuario con email '{}'. Matrícula cancelada.", email);
            return;
        }

        Long userId = userIdOpt.get();
        
        // Log de nivel INFO indicando qué userId y qué cursoId se están procesando
        log.info("[AULA-VIRTUAL] Procesando inscripción - userId: {}, cursoId: {}", userId, cursoId);

        if (inscripcionRepository.existsByStudentIdAndLmsCursoId(userId, cursoId)) {
            log.warn("[AULA-VIRTUAL] El alumno (userId: {}) ya está matriculado en el curso: {}. Se omite la duplicación.", userId, cursoId);
            return;
        }

        Inscripcion inscripcion = Inscripcion.builder()
                .studentId(userId)
                .lmsCursoId(cursoId)
                .build();

        inscripcionRepository.save(inscripcion);
        log.info("[AULA-VIRTUAL] ✅ Matrícula guardada con éxito en la BD - studentId: {}, lmsCursoId: {}", userId, cursoId);

        try {
            userPublicService.promoverAEstudianteSiEsVisitante(email);
            log.info("[AULA-VIRTUAL] Rol de usuario verificado/actualizado para email: {}", email);
        } catch (Exception e) {
            log.error("[AULA-VIRTUAL] Error al promover el rol de usuario para email: '{}': {}", email, e.getMessage(), e);
        }
    }
}
