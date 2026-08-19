package com.educore.platform.lms.service;

import com.educore.platform.lms.model.Inscripcion;
import com.educore.platform.lms.repository.InscripcionRepository;
import com.educore.platform.users.service.UserPublicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para AulaVirtualServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class AulaVirtualServiceImplTest {

    @Mock
    private InscripcionRepository inscripcionRepository;

    @Mock
    private UserPublicService userPublicService;

    @InjectMocks
    private AulaVirtualServiceImpl aulaVirtualService;

    private String email;
    private Long studentId;
    private Long cursoId;

    @BeforeEach
    void setUp() {
        email = "student@educore.com";
        studentId = 1L;
        cursoId = 100L;
    }

    @Test
    void matricularAlumno_ShouldDoNothing_WhenUserNotFound() {
        when(userPublicService.getUserIdByEmail(email)).thenReturn(Optional.empty());

        aulaVirtualService.matricularAlumno(email, cursoId);

        verify(inscripcionRepository, never()).existsByStudentIdAndLmsCursoId(any(), any());
        verify(inscripcionRepository, never()).save(any());
    }

    @Test
    void matricularAlumno_ShouldDoNothing_WhenEnrollmentAlreadyExists() {
        when(userPublicService.getUserIdByEmail(email)).thenReturn(Optional.of(studentId));
        when(inscripcionRepository.existsByStudentIdAndLmsCursoId(studentId, cursoId)).thenReturn(true);

        aulaVirtualService.matricularAlumno(email, cursoId);

        verify(inscripcionRepository, never()).save(any());
    }

    @Test
    void matricularAlumno_ShouldEnrollAndPromoteUser_WhenValid() {
        when(userPublicService.getUserIdByEmail(email)).thenReturn(Optional.of(studentId));
        when(inscripcionRepository.existsByStudentIdAndLmsCursoId(studentId, cursoId)).thenReturn(false);

        aulaVirtualService.matricularAlumno(email, cursoId);

        verify(inscripcionRepository, times(1)).save(any(Inscripcion.class));
        verify(userPublicService, times(1)).promoverAEstudianteSiEsVisitante(email);
    }

    @Test
    void matricularAlumno_ShouldEnroll_WhenPromotionThrowsException() {
        when(userPublicService.getUserIdByEmail(email)).thenReturn(Optional.of(studentId));
        when(inscripcionRepository.existsByStudentIdAndLmsCursoId(studentId, cursoId)).thenReturn(false);
        doThrow(new RuntimeException("Promotion error")).when(userPublicService).promoverAEstudianteSiEsVisitante(email);

        // Debería capturar el error interno y completar sin lanzar excepción hacia afuera
        aulaVirtualService.matricularAlumno(email, cursoId);

        verify(inscripcionRepository, times(1)).save(any(Inscripcion.class));
    }
}
