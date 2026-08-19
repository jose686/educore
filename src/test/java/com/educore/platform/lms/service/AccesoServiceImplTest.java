package com.educore.platform.lms.service;

import com.educore.platform.lms.model.Inscripcion;
import com.educore.platform.lms.repository.InscripcionRepository;
import com.educore.platform.store.model.Paquete;
import com.educore.platform.store.repository.PaqueteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para AccesoServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class AccesoServiceImplTest {

    @Mock
    private InscripcionRepository inscripcionRepository;

    @Mock
    private PaqueteRepository paqueteRepository;

    @InjectMocks
    private AccesoServiceImpl accesoService;

    private Long studentId;
    private Long cursoId;

    @BeforeEach
    void setUp() {
        studentId = 1L;
        cursoId = 100L;
    }

    @Test
    void tieneAcceso_ShouldReturnFalse_WhenNoEnrollmentExists() {
        when(inscripcionRepository.findByStudentIdAndLmsCursoId(studentId, cursoId)).thenReturn(Optional.empty());

        assertFalse(accesoService.tieneAcceso(studentId, cursoId));
    }

    @Test
    void tieneAcceso_ShouldReturnTrue_WhenPermanentEnrollmentExists() {
        Inscripcion inscripcion = Inscripcion.builder()
                .studentId(studentId)
                .lmsCursoId(cursoId)
                .fechaFin(null) // Permanente
                .build();

        when(inscripcionRepository.findByStudentIdAndLmsCursoId(studentId, cursoId)).thenReturn(Optional.of(inscripcion));

        assertTrue(accesoService.tieneAcceso(studentId, cursoId));
    }

    @Test
    void tieneAcceso_ShouldReturnTrue_WhenVaildTemporaryEnrollmentExists() {
        Inscripcion inscripcion = Inscripcion.builder()
                .studentId(studentId)
                .lmsCursoId(cursoId)
                .fechaFin(LocalDateTime.now().plusDays(10)) // Vigente
                .build();

        when(inscripcionRepository.findByStudentIdAndLmsCursoId(studentId, cursoId)).thenReturn(Optional.of(inscripcion));

        assertTrue(accesoService.tieneAcceso(studentId, cursoId));
    }

    @Test
    void tieneAcceso_ShouldReturnFalse_WhenExpiredTemporaryEnrollmentExists() {
        Inscripcion inscripcion = Inscripcion.builder()
                .studentId(studentId)
                .lmsCursoId(cursoId)
                .fechaFin(LocalDateTime.now().minusDays(1)) // Expirada
                .build();

        when(inscripcionRepository.findByStudentIdAndLmsCursoId(studentId, cursoId)).thenReturn(Optional.of(inscripcion));

        assertFalse(accesoService.tieneAcceso(studentId, cursoId));
    }

    @Test
    void crearInscripcionesDePaquete_ShouldThrowException_WhenPackageNotFound() {
        when(paqueteRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> accesoService.crearInscripcionesDePaquete(10L, studentId));
    }

    @Test
    void crearInscripcionesDePaquete_ShouldSaveEnrollmentsForNewCourses() {
        Paquete paquete = Paquete.builder()
                .id(10L)
                .cursoIds(Set.of(200L, 300L))
                .build();

        when(paqueteRepository.findById(10L)).thenReturn(Optional.of(paquete));
        when(inscripcionRepository.existsByStudentIdAndLmsCursoId(studentId, 200L)).thenReturn(false);
        when(inscripcionRepository.existsByStudentIdAndLmsCursoId(studentId, 300L)).thenReturn(true);

        accesoService.crearInscripcionesDePaquete(10L, studentId);

        verify(inscripcionRepository, times(1)).save(any(Inscripcion.class));
    }
}
