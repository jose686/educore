package com.educore.platform.lms.service;

import com.educore.platform.lms.model.Inscripcion;
import com.educore.platform.lms.repository.InscripcionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/**
 * Implementación del servicio AccesoService.
 */
@Service
@Transactional
public class AccesoServiceImpl implements AccesoService {

    private final InscripcionRepository inscripcionRepository;
    private final com.educore.platform.store.repository.PaqueteRepository paqueteRepository;

    public AccesoServiceImpl(InscripcionRepository inscripcionRepository,
                              com.educore.platform.store.repository.PaqueteRepository paqueteRepository) {
        this.inscripcionRepository = inscripcionRepository;
        this.paqueteRepository = paqueteRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tieneAcceso(Long studentId, Long cursoId) {
        var inscripcionOpt = inscripcionRepository.findByStudentIdAndLmsCursoId(studentId, cursoId);
        if (inscripcionOpt.isEmpty()) {
            return false;
        }
        Inscripcion inscripcion = inscripcionOpt.get();
        // Acceso permanente (fechaFin == null) o temporal vigente
        return inscripcion.getFechaFin() == null || inscripcion.getFechaFin().isAfter(LocalDateTime.now());
    }

    @Override
    @Transactional
    public void crearInscripcionesDePaquete(Long paqueteId, Long studentId) {
        com.educore.platform.store.model.Paquete paquete = paqueteRepository.findById(paqueteId)
                .orElseThrow(() -> new IllegalArgumentException("Paquete no encontrado con ID: " + paqueteId));

        for (Long cursoId : paquete.getCursoIds()) {
            if (!inscripcionRepository.existsByStudentIdAndLmsCursoId(studentId, cursoId)) {
                Inscripcion inscripcion = Inscripcion.builder()
                        .studentId(studentId)
                        .lmsCursoId(cursoId)
                        .fechaInicio(LocalDateTime.now())
                        .fechaFin(LocalDateTime.now().plusDays(365))
                        .build();
                inscripcionRepository.save(inscripcion);
            }
        }
    }
}
